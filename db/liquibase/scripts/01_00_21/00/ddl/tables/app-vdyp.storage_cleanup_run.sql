/* Drop Tables */
DROP TABLE IF EXISTS "app-vdyp"."storage_cleanup_run" CASCADE
;

/* Create Tables */
CREATE TABLE "app-vdyp"."storage_cleanup_run" (
	"storage_cleanup_run_guid" UUID NOT NULL,      -- storage_cleanup_run_guid is a unique identifier for the record.
	"vdyp_user_guid" UUID NOT NULL,                -- vdyp_user_guid: Is a foreign key to vdyp_user: The administrator who ran the PVC storage cleanup.
	"run_date" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Run Date is the time the cleanup delete run executed.
	"deleted_count" INTEGER NOT NULL,              -- Deleted Count is the number of job folders deleted by this run.
	"failed_count" INTEGER NOT NULL,               -- Failed Count is the number of job folders this run attempted to delete but could not.
	"protected_count" INTEGER NOT NULL,            -- Protected Count is the number of job folders this run left in place because they were still in use.
	"bytes_freed" BIGINT NOT NULL,                 -- Bytes Freed is the total size in bytes reclaimed by this run's deletions.
	"revision_count" DECIMAL(10) NOT NULL DEFAULT 0, -- REVISION_COUNT is the number of times that the row of data has been changed. The column is used for optimistic locking via application code.
	"create_user" VARCHAR(64) NOT NULL,            -- CREATE_USER is an audit column that indicates the user that created the record.
	"create_date" TIMESTAMP NOT NULL DEFAULT now(), -- CREATE_DATE is the date and time the row of data was created.
	"update_user" VARCHAR(64) NOT NULL,            -- UPDATE_USER is an audit column that indicates the user that updated the record.
	"update_date" TIMESTAMP NOT NULL DEFAULT now()  -- UPDATE_DATE is the date and time the row of data was updated.
)
TABLESPACE	PG_DEFAULT
;

/* Create Primary Keys, Indexes, Uniques, Checks */

ALTER TABLE "app-vdyp"."storage_cleanup_run" ADD CONSTRAINT "stg_run_pk"
	PRIMARY KEY ("storage_cleanup_run_guid")
;

CREATE INDEX "stg_run_user_idx"
	ON "app-vdyp"."storage_cleanup_run" ("vdyp_user_guid")
;

CREATE INDEX "stg_run_date_idx"
	ON "app-vdyp"."storage_cleanup_run" ("run_date")
;

/* Create Foreign Key Constraints */

ALTER TABLE "app-vdyp"."storage_cleanup_run" ADD CONSTRAINT "stg_run_user_fk"
	FOREIGN KEY ("vdyp_user_guid") REFERENCES "app-vdyp"."vdyp_user" ("vdyp_user_guid") ON DELETE No Action ON UPDATE No Action
;

/* Create Table Comments */

COMMENT ON TABLE "app-vdyp"."storage_cleanup_run"
	IS 'Storage Cleanup Run records one admin-triggered PVC storage cleanup delete run: who ran it, when, and the outcome counts.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."storage_cleanup_run_guid"
	IS 'storage_cleanup_run_guid is a unique identifier for the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."vdyp_user_guid"
	IS 'vdyp_user_guid: Is a foreign key to vdyp_user: The administrator who ran the PVC storage cleanup.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."run_date"
	IS 'Run Date is the time the cleanup delete run executed.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."deleted_count"
	IS 'Deleted Count is the number of job folders deleted by this run.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."failed_count"
	IS 'Failed Count is the number of job folders this run attempted to delete but could not.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."protected_count"
	IS 'Protected Count is the number of job folders this run left in place because they were still in use.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."bytes_freed"
	IS 'Bytes Freed is the total size in bytes reclaimed by this run''s deletions.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."revision_count"
	IS 'REVISION_COUNT is the number of times that the row of data has been changed. The column is used for optimistic locking via application code.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."create_user"
	IS 'CREATE_USER is an audit column that indicates the user that created the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."create_date"
	IS 'CREATE_DATE is the date and time the row of data was created.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."update_user"
	IS 'UPDATE_USER is an audit column that indicates the user that updated the record.'
;

COMMENT ON COLUMN "app-vdyp"."storage_cleanup_run"."update_date"
	IS 'UPDATE_DATE is the date and time the row of data was updated.'
;
