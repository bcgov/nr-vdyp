/* Restore Lease Token Column */
ALTER TABLE "batch"."batch_job_claim"
RENAME COLUMN "lease_token_guid" TO "lease_token";

COMMENT ON COLUMN "batch"."batch_job_claim"."lease_token"
	IS 'Lease Token is a randomly generated UUID to secure updates to the claim.';
