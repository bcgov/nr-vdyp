ALTER TABLE "app-vdyp"."file_mapping"
    ADD COLUMN IF NOT EXISTS "estimated_polygon_count" INTEGER NULL;

COMMENT
ON COLUMN "app-vdyp"."file_mapping"."estimated_polygon_count"
	IS 'Number of data rows in a polygon file, counted when the file is uploaded. Shown on the Admin Dashboard as the estimated polygon count of a queued projection. Null for non-polygon files and for files uploaded before this column was added.'
;
