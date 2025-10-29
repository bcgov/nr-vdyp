CREATE ROLE "app_vdyp_custodian";

CREATE ROLE "app_vdyp_rest_proxy";

GRANT "app_vdyp_rest_proxy" TO "proxy-vdyp-rest";

ALTER ROLE "proxy-vdyp-rest" SET search_path TO "app-vdyp";
