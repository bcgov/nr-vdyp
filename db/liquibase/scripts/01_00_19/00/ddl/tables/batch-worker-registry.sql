/* Create Tables */
CREATE TABLE "batch"."batch_worker_registry" (
	"worker_id" VARCHAR(512) PRIMARY KEY,    -- Worker ID uniquely identifies a registered batch service replica (used as Owner ID in batch_job_claim).
	"num_max_threads" INTEGER NOT NULL,      -- Num Maximum Threads is the maximum number of threads the replica offers to the total pool.
	"is_accepting_work" BOOLEAN NOT NULL,    -- Is Accepting Work indicates whether the batch worker is currently available to receive new jobs.
	"last_heartbeat_time" TIMESTAMP NOT NULL -- Last Heartbeat Time is the timestamp of the most recent liveness heartbeat reported by the batch worker.
);

/* Create Indexes */
CREATE INDEX "batch_worker_registry_heartbeat_idx"
	ON "batch"."batch_worker_registry" ("last_heartbeat_time");

/* Create Table Comments */
COMMENT ON TABLE "batch"."batch_worker_registry"
	IS 'Batch Worker Registry tracks registered batch service replicas, their processing capacity, availability for new work, and liveness.'
;

COMMENT ON COLUMN "batch"."batch_worker_registry"."worker_id"
	IS 'Worker ID uniquely identifies a registered batch service replica (used as Owner ID in batch_job_claim).'
;

COMMENT ON COLUMN "batch"."batch_worker_registry"."num_max_threads"
	IS 'Num Maximum Threads is the maximum number of threads the replica offers to the total pool.'
;

COMMENT ON COLUMN "batch"."batch_worker_registry"."is_accepting_work"
	IS 'Is Accepting Work indicates whether the batch worker is currently available to receive new jobs.'
;

COMMENT ON COLUMN "batch"."batch_worker_registry"."last_heartbeat_time"
	IS 'Last Heartbeat Time is the timestamp of the most recent liveness heartbeat reported by the batch worker.'
;
