package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class JobOwnershipService {

	private static final Logger logger = LoggerFactory.getLogger(JobOwnershipService.class);

	private final BatchOwnershipProperties properties;
	private final BatchWorkerIdentity identity;
	private final JobOwnershipRepository repository;
	private final OwnedJobRegistry registry;
	private final AtomicBoolean acceptingNewWork = new AtomicBoolean(true);
	private final AtomicBoolean shutdownDrain = new AtomicBoolean(false);
	private final Counter claimSuccess;
	private final Counter claimFailed;
	private final Counter takeoverSuccess;
	private final Counter renewFailed;
	private final Counter leaseLost;

	public JobOwnershipService(
			BatchOwnershipProperties properties, BatchWorkerIdentity identity, JobOwnershipRepository repository,
			OwnedJobRegistry registry, MeterRegistry meterRegistry, ServerCapacityService serverCapacityService
	) {
		this.properties = properties;
		this.identity = identity;
		this.repository = repository;
		this.registry = registry;
		this.claimSuccess = meterRegistry.counter("vdyp.batch.claim.attempts", "result", "success");
		this.claimFailed = meterRegistry.counter("vdyp.batch.claim.attempts", "result", "failed");
		this.takeoverSuccess = meterRegistry.counter("vdyp.batch.claim.takeovers", "result", "success");
		this.renewFailed = meterRegistry.counter("vdyp.batch.lease.renewals", "result", "failed");
		this.leaseLost = meterRegistry.counter("vdyp.batch.lease.lost");
		meterRegistry
				.gauge("vdyp.batch.threads.active.local", serverCapacityService, ServerCapacityService::activeThreads);
		meterRegistry.gauge(
				"vdyp.batch.threads.capacity.local", serverCapacityService, ServerCapacityService::maximumThreads
		);
		meterRegistry.gauge(
				"vdyp.batch.threads.capacity.available", serverCapacityService, ServerCapacityService::availableThreads
		);
		meterRegistry
				.gauge("vdyp.batch.jobs.claims.active.global", repository, JobOwnershipRepository::countActiveClaims);
	}

	public boolean isAcceptingNewWork() {
		return !properties.isEnabled() || acceptingNewWork.get() && !shutdownDrain.get();
	}

	public void stopAcceptingNewWork() {
		acceptingNewWork.set(false);
	}

	/**
	 * Permanently stop local intake for this process while it finishes its currently owned executions.
	 */
	public void beginShutdownDrain() {
		shutdownDrain.set(true);
		stopAcceptingNewWork();
	}

	public void resumeAcceptingNewWork() {
		if (!shutdownDrain.get()) {
			acceptingNewWork.set(true);
		}
	}

	public Optional<JobClaim> tryAcquire(String projectionGuid, String reason) {
		if (!properties.isEnabled()) {
			return Optional.of(disabledClaim(projectionGuid));
		}

		UUID leaseToken = UUID.randomUUID();
		Optional<JobClaim> existingClaim = repository.findByProjectionGuid(projectionGuid);
		Optional<JobClaim> claim = repository
				.acquire(projectionGuid, identity.ownerId(), leaseToken, properties.getLeaseDuration());
		if (claim.isPresent()) {
			claimSuccess.increment();
			if (existingClaim.isPresent()) {
				takeoverSuccess.increment();
			}
			logger.info(
					"Acquired batch job claim. projectionGuid={}, ownerId={}, reason={}", projectionGuid,
					identity.ownerId(), reason
			);
		} else {
			claimFailed.increment();
			logger.info(
					"Could not acquire batch job claim. projectionGuid={}, ownerId={}, reason={}", projectionGuid,
					identity.ownerId(), reason
			);
		}
		return claim;
	}

	public void registerClaim(JobClaim claim) {
		registry.register(new OwnedJob(claim));
		logger.info("Registered locally owned batch job claim. projectionGuid={}", claim.projectionGuid());
	}

	/**
	 * Fences work at an execution boundary. This method only verifies ownership; the heartbeat service is solely
	 * responsible for renewing leases.
	 */
	public void assertCurrentOwner(JobExecution jobExecution) {
		if (!properties.isEnabled()) {
			return;
		}

		String projectionGuid = projectionGuid(jobExecution);
		OwnedJob ownedJob = registry.findByProjectionGuid(projectionGuid).orElse(null);
		if (ownedJob == null) {
			throw new IllegalStateException("Current process does not own batch job projection " + projectionGuid);
		}
		if (!repository.isCurrent(ownedJob.claim())) {
			ownedJob.markLeaseLost();
			throw new IllegalStateException(
					"Current process no longer owns batch job projection " + ownedJob.claim().projectionGuid()
							+ " for execution " + jobExecution.getId()
			);
		}
	}

	public void heartbeatOwnedJobs() {
		if (!properties.isEnabled()) {
			return;
		}

		boolean renewFailure = false;
		for (OwnedJob ownedJob : registry.ownedJobs()) {
			try {
				boolean renewed = repository.renew(ownedJob.claim(), properties.getLeaseDuration());
				if (!renewed) {
					handleConfirmedLeaseLoss(ownedJob);
				}
			} catch (Exception e) {
				renewFailure = true;
				renewFailed.increment();
				stopAcceptingNewWork();
				logger.warn(
						"Failed to renew batch job lease. projectionGuid={}, error={}",
						ownedJob.claim().projectionGuid(), e.getMessage()
				);
			}
		}

		if (!renewFailure) {
			resumeAcceptingNewWork();
		}
	}

	public void finalizeOwnedExecution(JobExecution jobExecution) {
		finalizeOwnedClaim(projectionGuid(jobExecution));
	}

	public void finalizeOwnedClaim(String projectionGuid) {
		registry.removeByProjectionGuid(projectionGuid).ifPresent(ownedJob -> {
			if (properties.isEnabled() && !ownedJob.leaseLost()) {
				repository.release(ownedJob.claim());
			}
		});
	}

	public void releaseUnboundClaim(JobClaim claim) {
		if (properties.isEnabled()) {
			repository.release(claim);
		}
	}

	public boolean isOwnedLocally(String projectionGuid) {
		return registry.findByProjectionGuid(projectionGuid).isPresent();
	}

	public Optional<JobClaim> findProjectionClaim(String projectionGuid) {
		if (!properties.isEnabled()) {
			return Optional.empty();
		}
		return repository.findByProjectionGuid(projectionGuid);
	}

	public int activeLocalJobs() {
		return registry.size();
	}

	private void handleConfirmedLeaseLoss(OwnedJob ownedJob) {
		if (!ownedJob.markLeaseLost()) {
			return;
		}
		leaseLost.increment();
		stopAcceptingNewWork();
		logger.error(
				"Confirmed batch job lease loss. Local work will fail its next ownership check. projectionGuid={}",
				ownedJob.claim().projectionGuid()
		);
	}

	private String projectionGuid(JobExecution jobExecution) {
		String projectionGuid = jobExecution.getJobParameters()
				.getString(ca.bc.gov.nrs.vdyp.batch.util.BatchConstants.GuidInput.PROJECTION_GUID);
		if (projectionGuid == null || projectionGuid.isBlank()) {
			throw new IllegalStateException("Batch execution has no projection GUID: " + jobExecution.getId());
		}
		return projectionGuid;
	}

	private JobClaim disabledClaim(String projectionGuid) {
		return new JobClaim(
				projectionGuid, identity.ownerId(), new UUID(0, 0), java.time.Instant.EPOCH, java.time.Instant.MAX
		);
	}
}
