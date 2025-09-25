-- Role: "proxy_vdyp_rest"
-- DROP ROLE "proxy_vdyp_rest";

CREATE ROLE "proxy_vdyp_rest" WITH
  LOGIN
  NOSUPERUSER
  INHERIT
  NOCREATEDB
  NOCREATEROLE
  NOREPLICATION
  PASSWORD '${POSTGRES_PROXY_USER_PASSWORD}';
  
ALTER ROLE proxy_vdyp_rest SET search_path TO "app-vdyp";

ALTER USER proxy_vdyp_rest set TIMEZONE to 'America/New_York';

COMMENT ON ROLE "proxy_vdyp_rest" IS 'Proxy account for Variable Density Yield Projection system.';

GRANT "app_vdyp_rest_proxy" TO "proxy_vdyp_rest";

GRANT USAGE ON SCHEMA "app-vdyp" TO "app_vdyp_rest_proxy";
