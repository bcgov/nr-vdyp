CREATE UNIQUE INDEX IF NOT EXISTS "ux_projection_batch_mapping_one_prioritized"
    ON "app-vdyp"."projection_batch_mapping" ((true))
    WHERE "is_prioritized" = true;

COMMENT
ON INDEX "app-vdyp"."ux_projection_batch_mapping_one_prioritized"
	IS 'Guarantees at most one projection_batch_mapping row can have is_prioritized = true at a time.'
;
