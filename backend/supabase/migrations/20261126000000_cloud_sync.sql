-- Each account serializes writes so a committed feed cursor cannot skip a later commit.
begin;

create sequence if not exists public.tazer_sync_change_id;
revoke all on sequence public.tazer_sync_change_id from public, anon, authenticated;

do $migration$
declare
  kind text;
  relation text;
begin
  foreach kind in array array['profile', 'tasks', 'subtasks', 'fixed_schedules', 'calendar_events', 'work_sessions'] loop
    relation := 'tazer_sync_' || kind;
    execute pg_catalog.format($ddl$
      create table if not exists public.%I (
        user_id uuid not null references auth.users(id) on delete cascade,
        row_id uuid not null,
        version bigint not null check (version between 1 and 9007199254740991),
        writer uuid not null,
        payload jsonb check (payload is null or
          (jsonb_typeof(payload) = 'object' and octet_length(payload::text) <= 65536)),
        deleted_at timestamptz,
        change_id bigint not null default nextval('public.tazer_sync_change_id'),
        primary key (user_id, row_id),
        check ((payload is null) = (deleted_at is not null))
      )
    $ddl$, relation);
    execute pg_catalog.format('create index if not exists %I on public.%I (user_id, change_id)', relation || '_feed', relation);
    execute pg_catalog.format('alter table public.%I enable row level security', relation);
    execute pg_catalog.format('drop policy if exists own_sync_rows on public.%I', relation);
    execute pg_catalog.format('create policy own_sync_rows on public.%I for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)', relation);
    execute pg_catalog.format('revoke all on public.%I from public, anon, authenticated', relation);
    execute pg_catalog.format('grant select on public.%I to authenticated', relation);
  end loop;
end
$migration$;

create or replace function public.tazer_sync_push(p_rows jsonb)
returns integer
language plpgsql security definer set search_path = ''
as $function$
declare
  owner uuid := auth.uid();
  item jsonb;
  kind text;
  entity uuid;
  content jsonb;
  affected integer;
  accepted integer := 0;
begin
  if owner is null then raise insufficient_privilege; end if;
  if p_rows is null or pg_catalog.jsonb_typeof(p_rows) <> 'array'
      or pg_catalog.jsonb_array_length(p_rows) not between 1 and 100
      or pg_catalog.octet_length(p_rows::text) > 1048576 then
    raise exception 'invalid sync batch' using errcode = '22023';
  end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(owner::text, 130));
  for item in select value from pg_catalog.jsonb_array_elements(p_rows) loop
    kind := item->>'kind';
    entity := (item->>'row_id')::uuid;
    content := nullif(item->'payload', 'null'::jsonb);
    if kind is null or kind not in ('profile','tasks','subtasks','fixed_schedules','calendar_events','work_sessions')
        or entity is null or not (item ? 'payload')
        or (kind = 'profile' and (entity <> owner or content is null))
        or (content is not null and content ?| array['ownerUid','user_id','id'])
        or (content is not null and kind in ('subtasks','work_sessions')
            and (content->>'taskId' is null or (content->>'taskId')::uuid is null)) then
      raise exception 'invalid sync row' using errcode = '22023';
    end if;
    execute pg_catalog.format($sql$
      insert into public.%I as existing (user_id, row_id, version, writer, payload, deleted_at)
      values ($1, $2, $3, $4, $5, case when $5 is null then pg_catalog.clock_timestamp() end)
      on conflict (user_id, row_id) do update set version = excluded.version, writer = excluded.writer,
        payload = excluded.payload, deleted_at = excluded.deleted_at, change_id = excluded.change_id
      where (excluded.version, excluded.writer) > (existing.version, existing.writer)
    $sql$, 'tazer_sync_' || kind)
    using owner, entity, (item->>'version')::bigint, (item->>'writer')::uuid, content;
    get diagnostics affected = row_count;
    accepted := accepted + affected;
  end loop;
  return accepted;
end
$function$;

create or replace function public.tazer_sync_pull(p_after bigint, p_limit integer)
returns table(kind text, row_id uuid, version bigint, writer uuid, payload jsonb, deleted_at timestamptz, change_id bigint)
language plpgsql stable security definer set search_path = ''
as $function$
declare
  owner uuid := auth.uid();
  query text;
begin
  if owner is null then raise insufficient_privilege; end if;
  if p_after is null or p_after < 0 or p_limit is null or p_limit not between 1 and 200 then
    raise exception 'invalid sync cursor' using errcode = '22023';
  end if;
  select pg_catalog.string_agg(pg_catalog.format(
    '(select %L::text, src.row_id, src.version, src.writer, src.payload, src.deleted_at, src.change_id
      from public.%I as src where src.user_id = $1 and src.change_id > $2 order by src.change_id limit $3)',
    entity, 'tazer_sync_' || entity), ' union all ')
  into query
  from pg_catalog.unnest(array['profile','tasks','subtasks','fixed_schedules','calendar_events','work_sessions']) as entity;
  return query execute query || ' order by 7 limit $3' using owner, p_after, p_limit;
end
$function$;

revoke all on function public.tazer_sync_push(jsonb), public.tazer_sync_pull(bigint, integer) from public, anon, authenticated;
grant execute on function public.tazer_sync_push(jsonb), public.tazer_sync_pull(bigint, integer) to authenticated;

commit;
