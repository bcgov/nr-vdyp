ALTER TABLE "app-vdyp"."projection_batch_mapping"
    ADD COLUMN IF NOT EXISTS "batch_failure_type_code" VARCHAR (10);
ALTER TABLE "app-vdyp"."projection_batch_mapping" ADD COLUMN IF NOT EXISTS "failure_message" VARCHAR(200);

CREATE INDEX "prjbatmap_bftype_idx" ON "app-vdyp"."projection_batch_mapping" ("batch_failure_type_code" ASC)
;

/* Create Foreign Key Constraints */

ALTER TABLE "app-vdyp"."projection_batch_mapping"
    ADD CONSTRAINT "prjbatmap_bftype_fk"
        FOREIGN KEY ("batch_failure_type_code") REFERENCES "app-vdyp"."batch_failure_type_code" ("batch_failure_type_code") ON DELETE No Action ON UPDATE No Action
;

/* Create Table Comments, Sequences for Autonumber Columns */

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."batch_failure_type_code"
	IS 'batch_failure_type_code: Is a foreign key to batch_failure_type_code: Batch Failure Type Code is the mode that the batch failure will be used. Values are: - Input - Processing - Output'
;

COMMENT
ON COLUMN "app-vdyp"."projection_batch_mapping"."failure_message"
	IS 'Failure Message is the message reported when the batch process fails'
;
