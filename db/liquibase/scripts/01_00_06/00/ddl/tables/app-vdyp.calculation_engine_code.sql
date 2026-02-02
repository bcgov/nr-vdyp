ALTER TABLE "app-vdyp"."calculation_engine_code"
ALTER COLUMN "create_date" TYPE TIMESTAMP USING "create_date"::timestamp,
ALTER COLUMN "update_date" TYPE TIMESTAMP USING "update_date"::timestamp;

ALTER TABLE "app-vdyp"."calculation_engine_code"
ALTER COLUMN "create_date" SET DEFAULT now(),
ALTER COLUMN "update_date" SET DEFAULT now();
