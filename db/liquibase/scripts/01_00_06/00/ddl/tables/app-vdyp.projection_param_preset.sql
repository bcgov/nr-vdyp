ALTER TABLE "app-vdyp"."projection_param_preset"
ALTER COLUMN "create_date" TYPE TIMESTAMP USING "create_date"::timestamp,
ALTER COLUMN "update_date" TYPE TIMESTAMP USING "update_date"::timestamp;

ALTER TABLE "app-vdyp"."projection_param_preset"
ALTER COLUMN "create_date" SET DEFAULT now(),
ALTER COLUMN "update_date" SET DEFAULT now();
