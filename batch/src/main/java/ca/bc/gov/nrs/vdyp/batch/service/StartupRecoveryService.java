package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
public class StartupRecoveryService implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(StartupRecoveryService.class);

	private static final String JOB_NAME = "VdypFetchAndPartitionJob";
	private static final String MISSING_PARTITION_INPUTS_EXIT_DESCRIPTION = "Marked FAILED during startup recovery because partition input directories are missing";

	private final JobExplorer jobExplorer;
	private final JobLauncher jobLauncher;
	private final Job fetchAndPartitionJob;
	private final BatchRecoveryMetadataService recoveryMetadataService;
	private final VdypClient vdypClient;

	private volatile boolean running = false;

	public StartupRecoveryService(
			JobExplorer jobExplorer, @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
			@Qualifier("fetchAndPartitionJob") Job fetchAndPartitionJob,
			BatchRecoveryMetadataService recoveryMetadataService, VdypClient vdypClient
	) {
		this.jobExplorer = jobExplorer;
		this.jobLauncher = jobLauncher;
		this.fetchAndPartitionJob = fetchAndPartitionJob;
		this.recoveryMetadataService = recoveryMetadataService;
		this.vdypClient = vdypClient;
	}

	@Override
	public void start() {
		try {
			Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(JOB_NAME);

			if (runningExecutions.isEmpty()) {
				logger.info("No stale running executions found for job {}", JOB_NAME);
				this.running = true;
				return;
			}

			for (JobExecution jobExecution : runningExecutions) {
				Long oldExecutionId = jobExecution.getId();

				logger.warn(
						"Recovering stale job execution. jobName={}, executionId={}, status={}", JOB_NAME,
						oldExecutionId, jobExecution.getStatus()
				);

				if (cannotRestartCompletedPartitionStep(jobExecution)) {
					JobExecution failedExecution = recoveryMetadataService
							.markStaleExecutionFailed(oldExecutionId, MISSING_PARTITION_INPUTS_EXIT_DESCRIPTION);
					notifyBackendOfRecoveryFailure(failedExecution);
					logger.warn(
							"Not restarting stale job execution because partition inputs are missing. executionId={}",
							oldExecutionId
					);
					continue;
				}

				recoveryMetadataService.markStaleExecutionFailed(oldExecutionId);

				JobExecution newExecution = jobLauncher.run(fetchAndPartitionJob, jobExecution.getJobParameters());

				logger.info(
						"Restarted stale job execution. oldExecutionId={}, newExecutionId={}", oldExecutionId,
						newExecution.getId()
				);
			}

			this.running = true;
		} catch (Exception e) {
			logger.error("Batch startup recovery failed. Refusing to start batch consumer.", e);
			throw new IllegalStateException("Batch startup recovery failed", e);
		}
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
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
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
