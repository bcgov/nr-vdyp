ALTER TABLE "app-vdyp"."projection_batch_mapping"
    ADD COLUMN IF NOT EXISTS "is_prioritized" BOOLEAN NOT NULL DEFAULT false;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."is_prioritized"
	IS 'True when an admin has prioritized this projection''s batch job. Cleared on every other mapping whenever a new one is prioritized, since only one job can hold priority at a time.'
;
