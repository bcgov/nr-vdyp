ALTER TABLE "app-vdyp"."projection_batch_mapping"
    ADD COLUMN IF NOT EXISTS "error_message_count" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "error_polygon_count" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "warning_message_count" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "warning_polygon_count" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "info_message_count" INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "info_polygon_count" INT NOT NULL DEFAULT 0;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."error_message_count"
	IS 'Error Message Count is the number of error and fatal error alerts written to the error log during processing.'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."error_polygon_count"
	IS 'Error Polygon Count is the number of polygons with at least one error or fatal error alert.'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."warning_message_count"
	IS 'Warning Message Count is the number of warning alerts written to the error log during processing.'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."warning_polygon_count"
	IS 'Warning Polygon Count is the number of polygons with at least one warning alert.'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."info_message_count"
	IS 'Info Message Count is the number of information and status alerts written to the error log during processing.'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."info_polygon_count"
	IS 'Info Polygon Count is the number of polygons with at least one information or status alert.'
;
