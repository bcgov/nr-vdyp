--liquibase formatted sql
-- Role: "proxy_vdyp_rest"
-- DROP ROLE "proxy_vdyp_rest";
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'proxy_vdyp_rest') THEN
        -- Use EXECUTE + format to safely literal-quote the password
        EXECUTE format(
            'CREATE ROLE %I WITH LOGIN NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L',
            'proxy_vdyp_rest',
            '${VDYP_DB_PROXY_PASSWORD}'
        );
    ELSE
        EXECUTE format(
            'ALTER ROLE %I WITH LOGIN NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L',
            'proxy_vdyp_rest',
            '${VDYP_DB_PROXY_PASSWORD}'
        );
   END IF;
END$$;
  
ALTER ROLE proxy_vdyp_rest SET search_path TO "app-vdyp";

ALTER USER proxy_vdyp_rest set TIMEZONE to 'America/New_York';

COMMENT ON ROLE "proxy_vdyp_rest" IS 'Proxy account for Variable Density Yield Projection system.';

GRANT "app_vdyp_rest_proxy" TO "proxy_vdyp_rest";

GRANT USAGE ON SCHEMA "app-vdyp" TO "app_vdyp_rest_proxy";
