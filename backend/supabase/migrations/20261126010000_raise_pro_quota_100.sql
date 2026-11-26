begin;

create or replace function public.tazer_entitlement(p_user_id uuid, p_consume boolean)
returns table (tier text, monthly_limit integer, remaining integer, resets_at text, granted boolean)
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_month date := date_trunc('month', statement_timestamp() at time zone 'UTC')::date;
  v_tier text;
  v_limit constant integer := 100;
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
