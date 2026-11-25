begin;

create table public.account_entitlements (
  user_id uuid primary key references auth.users(id) on delete cascade,
  tier text not null check (tier in ('FREE', 'PRO', 'BYOK'))
);

create table public.analysis_usage (
  user_id uuid not null references auth.users(id) on delete cascade,
  month_start date not null check (extract(day from month_start) = 1),
  used integer not null check (used >= 0),
  primary key (user_id, month_start)
);

alter table public.account_entitlements enable row level security;
alter table public.analysis_usage enable row level security;
revoke all on table public.account_entitlements, public.analysis_usage from public, anon, authenticated, service_role;
grant select, update on table public.account_entitlements to service_role;
grant select, insert, update on table public.analysis_usage to service_role;

create function public.tazer_entitlement(p_user_id uuid, p_consume boolean)
returns table (tier text, monthly_limit integer, remaining integer, resets_at text, granted boolean)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_month date := date_trunc('month', statement_timestamp() at time zone 'UTC')::date;
  v_tier text;
  v_limit constant integer := 20;
  v_used integer;
begin
  -- A concurrent downgrade waits for the charge, or is seen before charging.
  select a.tier into v_tier
    from public.account_entitlements as a where a.user_id = p_user_id for share;

  tier := coalesce(v_tier, 'FREE');
  resets_at := to_char(v_month + interval '1 month', 'YYYY-MM-DD"T"HH24:MI:SS"Z"');
  granted := false;
  monthly_limit := 0;
  remaining := 0;
  if tier = 'PRO' then
    monthly_limit := v_limit;
    if p_consume then
      insert into public.analysis_usage as u (user_id, month_start, used)
      values (p_user_id, v_month, 1)
      on conflict (user_id, month_start) do update set used = u.used + 1
      where u.used < v_limit
      returning u.used into v_used;
      granted := found;
      remaining := v_limit - coalesce(v_used, v_limit);
    else
      select u.used into v_used from public.analysis_usage as u
        where u.user_id = p_user_id and u.month_start = v_month;
      remaining := greatest(0, v_limit - coalesce(v_used, 0));
    end if;
  end if;
  return next;
end;
$$;

revoke all on function public.tazer_entitlement(uuid, boolean) from public, anon, authenticated;
grant execute on function public.tazer_entitlement(uuid, boolean) to service_role;

commit;
