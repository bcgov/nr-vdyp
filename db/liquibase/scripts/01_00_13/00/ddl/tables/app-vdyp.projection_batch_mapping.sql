ALTER TABLE "app-vdyp"."projection_batch_mapping" ADD COLUMN IF NOT EXISTS "failure_code" VARCHAR(10);
ALTER TABLE "app-vdyp"."projection_batch_mapping" ADD COLUMN IF NOT EXISTS "failure_message" VARCHAR(200);
