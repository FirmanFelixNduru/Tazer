\set ON_ERROR_STOP on
-- Run only against an empty local PostgreSQL database.
create role anon;
create role authenticated;
create schema auth;
create table auth.users(id uuid primary key);
create function auth.uid() returns uuid language sql stable
as $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
grant usage on schema auth to anon, authenticated;
insert into auth.users values
  ('11111111-1111-1111-1111-111111111111'),
  ('22222222-2222-2222-2222-222222222222');

\ir ../supabase/migrations/20261126000000_cloud_sync.sql
\ir ../supabase/migrations/20261126000000_cloud_sync.sql

set role anon;
do $$ begin
  begin
    perform public.tazer_sync_pull(0, 1);
    raise exception 'anonymous pull unexpectedly succeeded';
  exception when insufficient_privilege then null; end;
  begin
    perform count(*) from public.tazer_sync_tasks;
    raise exception 'anonymous read unexpectedly succeeded';
  exception when insufficient_privilege then null; end;
end $$;

set role authenticated;
set request.jwt.claim.sub = '11111111-1111-1111-1111-111111111111';
do $$ declare
  first_change bigint;
  second_change bigint;
  a jsonb := '{"kind":"tasks","row_id":"33333333-3333-3333-3333-333333333333","version":1,"writer":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","payload":{"title":"First"}}';
  b jsonb := '{"kind":"tasks","row_id":"33333333-3333-3333-3333-333333333333","version":1,"writer":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","payload":{"title":"Tie winner"}}';
begin
  assert public.tazer_sync_push(jsonb_build_array(a)) = 1;
  select change_id into strict first_change from public.tazer_sync_pull(0, 1);
  assert public.tazer_sync_push(jsonb_build_array(b)) = 1;
  assert public.tazer_sync_push(jsonb_build_array(a)) = 0;
  assert public.tazer_sync_push(jsonb_build_array(b)) = 0;
  select change_id into strict second_change from public.tazer_sync_pull(first_change, 1)
    where payload->>'title' = 'Tie winner';
  assert public.tazer_sync_push(jsonb_build_array(b || '{"version":2,"payload":null}')) = 1;
  assert (select count(*) from public.tazer_sync_pull(second_change, 1) where deleted_at is not null and payload is null) = 1;
  assert public.tazer_sync_push(jsonb_build_array(a)) = 0;
  begin
    update public.tazer_sync_tasks set version = 3;
    raise exception 'direct write unexpectedly succeeded';
  exception when insufficient_privilege then null; end;
  begin
    perform public.tazer_sync_push(jsonb_build_array(a || '{"kind":"profile","row_id":"22222222-2222-2222-2222-222222222222"}'));
    raise exception 'foreign profile unexpectedly succeeded';
  exception when invalid_parameter_value then null; end;
end $$;

set request.jwt.claim.sub = '22222222-2222-2222-2222-222222222222';
do $$ declare
  kind text;
  visible int;
begin
  assert (select count(*) from public.tazer_sync_pull(0, 200)) = 0;
  foreach kind in array array['profile','tasks','subtasks','fixed_schedules','calendar_events','work_sessions'] loop
    execute 'select count(*) from public.tazer_sync_' || kind into visible;
    assert visible = 0;
  end loop;
  assert public.tazer_sync_push('[{"kind":"tasks","row_id":"33333333-3333-3333-3333-333333333333","version":1,"writer":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","payload":{"title":"Account B"}}]') = 1;
  assert (select count(*) from public.tazer_sync_tasks where payload->>'title' = 'Account B') = 1;
end $$;

set request.jwt.claim.sub = '11111111-1111-1111-1111-111111111111';
do $$ begin
  assert (select count(*) from public.tazer_sync_tasks where payload is null) = 1;
  assert (select count(*) from public.tazer_sync_tasks where payload->>'title' = 'Account B') = 0;
end $$;
