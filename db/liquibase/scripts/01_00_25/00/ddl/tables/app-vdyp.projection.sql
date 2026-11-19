ALTER TABLE "app-vdyp"."projection"
    ADD COLUMN IF NOT EXISTS "queued_date" TIMESTAMP NULL;

COMMENT
ON COLUMN "app-vdyp"."projection"."queued_date"
	IS 'Queued Date is the date the projection was last submitted to the batch queue. Shown on the Admin Dashboard as the queue wait time of a queued projection. Null for projections never submitted to the batch queue.'
;
