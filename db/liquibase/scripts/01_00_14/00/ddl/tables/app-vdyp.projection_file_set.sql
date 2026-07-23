ALTER TABLE "app-vdyp"."projection_file_set"
	ADD COLUMN "coms_bucket_id" varchar(255) NULL
;

COMMENT ON COLUMN "app-vdyp"."projection_file_set"."coms_bucket_id"
	IS 'COMS Bucket Id is the cached identifier of the COMS bucket backing this file set, avoiding a COMS lookup on every file add/delete'
;
