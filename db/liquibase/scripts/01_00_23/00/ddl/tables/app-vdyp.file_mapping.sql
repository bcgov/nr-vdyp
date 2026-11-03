ALTER TABLE "app-vdyp"."file_mapping"
    ADD COLUMN IF NOT EXISTS "is_debug_mode_permitted" BOOLEAN NOT NULL DEFAULT false;

COMMENT
ON COLUMN "app-vdyp"."file_mapping"."is_debug_mode_permitted"
	IS 'True when the file is a polygon file with no more polygons than Debug Mode allows. Counted when the file is uploaded, since the file cannot be re-read cheaply afterwards.'
;
