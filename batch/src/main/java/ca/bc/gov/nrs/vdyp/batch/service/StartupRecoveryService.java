package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.configuration.BatchOwnershipProperties;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
public class StartupRecoveryService implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(StartupRecoveryService.class);

	private static final String JOB_NAME = "VdypFetchAndPartitionJob";
	private static final String MISSING_PARTITION_INPUTS_EXIT_DESCRIPTION = "Marked FAILED during startup recovery because partition input directories are missing";

	private final JobExplorer jobExplorer;
	private final Job fetchAndPartitionJob;
	private final BatchRecoveryMetadataService recoveryMetadataService;
	private final VdypClient vdypClient;
	private final BatchOwnershipProperties ownershipProperties;
	private final JobOwnershipService ownershipService;
	private final ServerCapacityService serverCapacityService;
	private final ClaimBoundJobLauncher claimBoundJobLauncher;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread recoveryThread;

	public StartupRecoveryService(
			JobExplorer jobExplorer, @Qualifier("fetchAndPartitionJob") Job fetchAndPartitionJob,
			BatchRecoveryMetadataService recoveryMetadataService, VdypClient vdypClient,
			BatchOwnershipProperties ownershipProperties, JobOwnershipService ownershipService,
			ServerCapacityService serverCapacityService, ClaimBoundJobLauncher claimBoundJobLauncher
	) {
		this.jobExplorer = jobExplorer;
		this.fetchAndPartitionJob = fetchAndPartitionJob;
		this.recoveryMetadataService = recoveryMetadataService;
		this.vdypClient = vdypClient;
		this.ownershipProperties = ownershipProperties;
		this.ownershipService = ownershipService;
		this.serverCapacityService = serverCapacityService;
		this.claimBoundJobLauncher = claimBoundJobLauncher;
	}

	@Override
	public void start() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		recoveryThread = new Thread(this::recoveryLoop, "vdyp-batch-recovery");
		recoveryThread.start();
	}

	private void recoveryLoop() {
		while (running.get()) {
			try {
				recoverClaimableExecutions();
				sleepWithJitter();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				running.set(false);
			} catch (Exception e) {
				logger.error("Batch recovery scan failed; will retry on next interval", e);
				try {
					sleepWithJitter();
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					running.set(false);
				}
			}
		}
	}

	void recoverClaimableExecutions() {
		try {
			Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(JOB_NAME);

			if (runningExecutions.isEmpty()) {
				logger.debug("No running executions found for job {}", JOB_NAME);
				return;
			}

			for (JobExecution jobExecution : runningExecutions) {
				recoverIfClaimable(jobExecution);
			}
		} catch (Exception e) {
			logger.error("Batch recovery pass failed", e);
		}
	}

	/**
	 * Attempts one expired execution before a worker accepts new queue work.
	 *
	 * @return true when an expired execution was claimed and restarted
	 */
	public boolean recoverNextExpiredExecution() {
		try {
			for (JobExecution jobExecution : jobExplorer.findRunningJobExecutions(JOB_NAME)) {
				if (recoverIfClaimable(jobExecution)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			logger.error("Prioritized batch recovery check failed", e);
			return false;
		}
	}

	private boolean recoverIfClaimable(JobExecution jobExecution) throws JobExecutionException {
		Long oldExecutionId = jobExecution.getId();
		String projectionGuid = projectionGuid(jobExecution);
		if (ownershipService.isOwnedLocally(projectionGuid)) {
			return false;
		}

		Optional<JobClaim> existingClaim = ownershipService.findProjectionClaim(projectionGuid);
		if (existingClaim.isEmpty() && !ownershipProperties.isRecoverLegacyExecutionsWithoutClaim()) {
			logger.warn(
					"Skipping running legacy execution with no claim. projectionGuid={}, executionId={}. Drain old workers or enable batch.ownership.recover-legacy-executions-without-claim after confirming no old replicas are active.",
					projectionGuid, oldExecutionId
			);
			return false;
		}
		if (existingClaim.isPresent() && existingClaim.get().leaseExpiryTime().isAfter(Instant.now())) {
			logger.debug(
					"Skipping live claimed execution. projectionGuid={}, executionId={}, ownerId={}, leaseExpiryTime={}",
					projectionGuid, oldExecutionId, existingClaim.get().ownerId(), existingClaim.get().leaseExpiryTime()
			);
			return false;
		}

		if (!serverCapacityService.hasAvailableCapacity()) {
			logger.debug(
					"No local batch thread capacity available for recovery. projectionGuid={}, executionId={}",
					projectionGuid, oldExecutionId
			);
			return false;
		}

		JobClaim claim = ownershipService.tryAcquire(projectionGuid, "recovery").orElse(null);
		if (claim == null) {
			return false;
		}

		logger.warn(
				"Recovering claimed stale job execution. projectionGuid={}, oldExecutionId={}, status={}",
				projectionGuid, oldExecutionId, jobExecution.getStatus()
		);

		if (cannotRestartCompletedPartitionStep(jobExecution)) {
			JobExecution failedExecution = recoveryMetadataService
					.markStaleExecutionFailed(oldExecutionId, MISSING_PARTITION_INPUTS_EXIT_DESCRIPTION);
			notifyBackendOfRecoveryFailure(failedExecution);
			ownershipService.releaseUnboundClaim(claim);
			logger.warn(
					"Not restarting stale execution because partition inputs are missing. executionId={}",
					oldExecutionId
			);
			return false;
		}

		recoveryMetadataService.markStaleExecutionFailed(oldExecutionId);
		JobExecution newExecution = claimBoundJobLauncher
				.launch(fetchAndPartitionJob, jobExecution.getJobParameters(), claim);
		logger.info(
				"Restarted stale job execution. projectionGuid={}, oldExecutionId={}, newExecutionId={}",
				projectionGuid, oldExecutionId, newExecution.getId()
		);
		return true;
	}

	private String projectionGuid(JobExecution jobExecution) {
		String projectionGuid = jobExecution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
		if (projectionGuid == null || projectionGuid.isBlank()) {
			throw new IllegalStateException("Running batch execution has no projection GUID: " + jobExecution.getId());
		}
		return projectionGuid;
	}

	private void sleepWithJitter() throws InterruptedException {
		long baseMillis = Math.max(1, ownershipProperties.getRecoveryScanInterval().toMillis());
		long jitterMillis = ThreadLocalRandom.current().nextLong(Math.max(1, baseMillis / 4));
		Thread.sleep(baseMillis + jitterMillis);
	}

	private boolean cannotRestartCompletedPartitionStep(JobExecution jobExecution) {
		if (!isStepCompleted(jobExecution, BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME)
				|| isStepCompleted(jobExecution, BatchConstants.Job.MASTER_STEP_NAME)) {
			return false;
		}

		JobParameters jobParameters = jobExecution.getJobParameters();
		String jobBaseDir = jobParameters.getString(BatchConstants.Job.BASE_DIR);
		if (jobBaseDir == null || jobBaseDir.isBlank()) {
			return true;
		}

		int partitionCount = partitionCount(jobExecution);
		for (int i = 0; i < partitionCount; i++) {
			String partitionName = BatchConstants.Partition.PREFIX + i;
			Path partitionDir = Paths.get(jobBaseDir, BatchUtils.buildInputPartitionFolderName(partitionName));
			if (Files.notExists(partitionDir)) {
				logger.warn(
						"Cannot restart stale job execution {} from masterStep because partition directory is missing: {}",
						jobExecution.getId(), partitionDir
				);
				return true;
			}
		}

		return false;
	}

	private boolean isStepCompleted(JobExecution jobExecution, String stepName) {
		return jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> stepName.equals(stepExecution.getStepName()))
				.anyMatch(stepExecution -> BatchStatus.COMPLETED.equals(stepExecution.getStatus()));
	}

	private int partitionCount(JobExecution jobExecution) {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		if (executionContext.containsKey(BatchConstants.Job.COMPUTED_PARTITIONS)) {
			return executionContext.getInt(BatchConstants.Job.COMPUTED_PARTITIONS);
		}

		Long parameterValue = jobExecution.getJobParameters().getLong(BatchConstants.Partition.NUMBER);
		return parameterValue == null ? 0 : parameterValue.intValue();
	}

	private void notifyBackendOfRecoveryFailure(JobExecution jobExecution) {
		String projectionGuid = jobExecution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
		if (projectionGuid == null || projectionGuid.isBlank()) {
			logger.warn(
					"[GUID: {}] Cannot notify backend of recovery failure because projection GUID is missing", jobGuid
			);
			return;
		}

		try {
			vdypClient.markComplete(projectionGuid, false, BatchUtils.buildFailureProgress(jobGuid, jobExecution));
		} catch (Exception e) {
			logger.warn(
					"[GUID: {}] Failed to notify backend of unrestartable stale job {}: {}", jobGuid,
					jobExecution.getId(), e.getMessage()
			);
		}
	}

	@Override
	public void stop() {
		this.running.set(false);
		if (recoveryThread != null) {
			recoveryThread.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}
}
