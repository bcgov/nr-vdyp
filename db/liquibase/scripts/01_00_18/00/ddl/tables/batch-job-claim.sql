/* Create Tables */
CREATE TABLE "batch"."batch_job_claim" (
	projection_guid UUID PRIMARY KEY,     -- Projection GUID is the identifier of the projection being processed.
	owner_id VARCHAR(512) NOT NULL,       -- Owner ID is a unique string to the replica that secures claims
    lease_token UUID NOT NULL,            -- Lease Token is a randomly generated UUID known by the owner to secure updates
	acquired_time TIMESTAMP NOT NULL DEFAULT clock_timestamp(), -- Acquired Time is the time the claim was acquired
	lease_expiry_time TIMESTAMP NOT NULL, -- Lease expired time is the time the lease will expire if no updates occur
	version BIGINT NOT NULL DEFAULT 0     -- Version is incremented each time the record is updated, ensuring atomic updates
);

/* Create Indexes */
CREATE INDEX "batch_job_claim_lease_expiry_idx"
    ON "batch"."batch_job_claim" (lease_expiry_time);

/* Create Table Comments, Sequences for Autonumber Columns */

COMMENT ON TABLE "batch"."batch_job_claim"
	IS 'Batch Job Claim is used to Atomically track which Batch replica has the right to process a job.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."projection_guid"
	IS 'Projection GUID is the identifier of the projection being processed.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."owner_id"
	IS 'Owner ID is a unique identifier known by the replica that owns the claim to the Job.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."lease_token"
	IS 'Lease Token is a randomly generated UUID to secure updates to the claim.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."acquired_time"
	IS 'Acquired Time is the time the claim to the Job was acquired.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."lease_expiry_time"
	IS 'Lease Expiry Time is the time the claim to the job will expire if there are no heartbeat updates.'
;

COMMENT ON COLUMN "batch"."batch_job_claim"."version"
	IS 'Version is an atomically updated version to keep updates accurate.'
;
