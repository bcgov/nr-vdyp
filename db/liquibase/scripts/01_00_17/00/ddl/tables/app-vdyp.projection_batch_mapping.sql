ALTER TABLE "app-vdyp"."projection_batch_mapping"
    ADD COLUMN IF NOT EXISTS "last_progress_time" TIMESTAMP;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."last_progress_time"
	IS 'Last Progress Time is the timestamp of the most recent progress update that increased processed polygons, skipped polygons, or error count. Used to detect projections that are RUNNING but have stalled (STUCK).'
;
