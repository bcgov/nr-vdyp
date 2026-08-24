/* Rename Lease Token Column */
ALTER TABLE "batch"."batch_job_claim"
RENAME COLUMN "lease_token" TO "lease_token_guid";

COMMENT ON COLUMN "batch"."batch_job_claim"."lease_token_guid"
	IS 'Lease Token GUID is a randomly generated UUID to secure updates to the claim.';
