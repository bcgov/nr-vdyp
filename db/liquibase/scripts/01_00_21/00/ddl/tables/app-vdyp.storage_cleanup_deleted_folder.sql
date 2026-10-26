/* Drop Tables */
DROP TABLE IF EXISTS "app-vdyp"."storage_cleanup_deleted_folder" CASCADE
;

/* Create Tables */
CREATE TABLE "app-vdyp"."storage_cleanup_deleted_folder" (
	"storage_cleanup_deleted_folder_guid" UUID NOT NULL, -- storage_cleanup_deleted_folder_guid is a unique identifier for the record.
	"storage_cleanup_run_guid" UUID NOT NULL,      -- storage_cleanup_run_guid: Is a foreign key to storage_cleanup_run: The cleanup run that deleted this folder.
	"folder_name" VARCHAR(100) NOT NULL,           -- Folder Name is the deleted PVC job folder's name (e.g. vdyp-batch-{UUID}).
	"size_bytes" BIGINT NOT NULL,                  -- Size Bytes is the folder's size in bytes at the time it was deleted.
	"revision_count" DECIMAL(10) NOT NULL DEFAULT 0, -- REVISION_COUNT is the number of times that the row of data has been changed. The column is used for optimistic locking via application code.
	"create_user" VARCHAR(64) NOT NULL,            -- CREATE_USER is an audit column that indicates the user that created the record.
	"create_date" TIMESTAMP NOT NULL DEFAULT now(), -- CREATE_DATE is the date and time the row of data was created.
	"update_user" VARCHAR(64) NOT NULL,            -- UPDATE_USER is an audit column that indicates the user that updated the record.
	"update_date" TIMESTAMP NOT NULL DEFAULT now()  -- UPDATE_DATE is the date and time the row of data was updated.
)
TABLESPACE	PG_DEFAULT
;

/* Create Primary Keys, Indexes, Uniques, Checks */

ALTER TABLE "app-vdyp"."storage_cleanup_deleted_folder" ADD CONSTRAINT "stg_fldr_pk"
	PRIMARY KEY ("storage_cleanup_deleted_folder_guid")
;

CREATE INDEX "stg_fldr_run_idx"
	ON "app-vdyp"."storage_cleanup_deleted_folder" ("storage_cleanup_run_guid")
;

/* Create Foreign Key Constraints */

ALTER TABLE "app-vdyp"."storage_cleanup_deleted_folder" ADD CONSTRAINT "stg_fldr_run_fk"
	FOREIGN KEY ("storage_cleanup_run_guid") REFERENCES "app-vdyp"."storage_cleanup_run" ("storage_cleanup_run_guid") ON DELETE CASCADE ON UPDATE No Action
;

/* Create Table Comments */

COMMENT ON TABLE "app-vdyp"."storage_cleanup_deleted_folder"
	IS 'Storage Cleanup Deleted Folder records one PVC job folder deleted by a storage cleanup run.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."storage_cleanup_deleted_folder_guid"
	IS 'storage_cleanup_deleted_folder_guid is a unique identifier for the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."storage_cleanup_run_guid"
	IS 'storage_cleanup_run_guid: Is a foreign key to storage_cleanup_run: The cleanup run that deleted this folder.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."folder_name"
	IS 'Folder Name is the deleted PVC job folder''s name (e.g. vdyp-batch-{UUID}).'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."size_bytes"
	IS 'Size Bytes is the folder''s size in bytes at the time it was deleted.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."revision_count"
	IS 'REVISION_COUNT is the number of times that the row of data has been changed. The column is used for optimistic locking via application code.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."create_user"
	IS 'CREATE_USER is an audit column that indicates the user that created the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."create_date"
	IS 'CREATE_DATE is the date and time the row of data was created.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."update_user"
	IS 'UPDATE_USER is an audit column that indicates the user that updated the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_deleted_folder"."update_date"
	IS 'UPDATE_DATE is the date and time the row of data was updated.'
;
