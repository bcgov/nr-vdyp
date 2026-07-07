package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * Job execution listener for partitioned VDYP batch job.
 */
@Component
public class BatchJobExecutionListener implements JobExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchJobExecutionListener.class);

	// Thread safety for afterJob execution - using job execution ID as key
	private final Map<Long, Boolean> jobCompletionTracker = new HashMap<>();
	private final Object lock = new Object();

	@Override
	public void beforeJob(@NonNull JobExecution jobExecution) {
		// Initialize tracking for this job execution
		synchronized (lock) {
			jobCompletionTracker.put(jobExecution.getId(), false);
		}

		String separator = "============================================================";
		logger.info(separator);
		logger.info("VDYP PARTITIONED JOB STARTING");

		Long numPartitions = jobExecution.getJobParameters().getLong(BatchConstants.Partition.NUMBER);
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

		logger.info("VDYP Number of Partitions: {}", numPartitions);
		logger.info("Job Execution ID: {}", jobExecution.getId());
		logger.info("Job GUID: {}", jobGuid);
		logger.info(separator);
	}

	@Override
	public void afterJob(@NonNull JobExecution jobExecution) {
		Long jobExecutionId = jobExecution.getId();
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
		String jobBaseDir = jobExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);
		Path jobBasePath = jobBaseDir != null ? Paths.get(jobBaseDir) : null;

		// Use synchronization to ensure only one thread processes this job completion
		synchronized (lock) {
			// Check if this specific job execution has already been processed
			Boolean alreadyProcessed = jobCompletionTracker.get(jobExecutionId);
			if (alreadyProcessed == null || alreadyProcessed) {
				logger.info(
						"[GUID: {}] VDYP Job {} already processed or not tracked, skipping afterJob processing",
						jobGuid, jobExecutionId
				);
				return;
			}

			if (jobBasePath != null) {
				cleanupJobDirectory(jobGuid, jobBasePath);
			}

			// Mark this job as processed
			jobCompletionTracker.put(jobExecutionId, true);

			String separator = "============================================================";
			logger.info(separator);
			logger.info("VDYP PARTITIONED JOB COMPLETED");
			logger.info("Job GUID: {}", jobGuid);
			logger.info("Job Execution ID: {}", jobExecutionId);
			logger.info("Status: {}", jobExecution.getStatus());

			LocalDateTime startTime = jobExecution.getStartTime();
			LocalDateTime endTime = jobExecution.getEndTime();
			if (startTime != null && endTime != null) {
				Duration duration = Duration.between(startTime, endTime);
				long millis = duration.toMillis();
				long minutes = millis / (60 * 1000);
				long seconds = (millis % (60 * 1000)) / 1000;
				long remainingMillis = millis % 1000;
				logger.info("Duration: {}m {}s {}ms", minutes, seconds, remainingMillis);
			} else {
				logger.warn("Duration: Unable to calculate (missing time information)");
			}

			logger.info(separator);

			cleanupOldJobTracker(jobExecutionId);
		}
	}

	private void cleanupJobDirectory(String jobGuid, Path jobBasePath) {
		try {
			BatchUtils.deleteDirectoryRecursively(jobBasePath);
			logger.debug("[GUID: {}] Deleted job directory after S3 upload: {}", jobGuid, jobBasePath);
		} catch (IOException e) {
			logger.warn("[GUID: {}] Failed to delete job directory {}: {}", jobGuid, jobBasePath, e.getMessage());
		}

		Path warningsFile = Paths.get(jobBasePath + BatchConstants.Partition.WARNING_FILE_NAME);
		try {
			if (Files.deleteIfExists(warningsFile)) {
				logger.debug("[GUID: {}] Deleted warnings file: {}", jobGuid, warningsFile);
			}
		} catch (IOException e) {
			logger.warn("[GUID: {}] Failed to delete warnings file {}: {}", jobGuid, warningsFile, e.getMessage());
		}
	}

	/**
	 * Cleans up old job execution tracking to prevent memory leaks.
	 */
	private void cleanupOldJobTracker(Long currentJobId) {
		// Already inside synchronized block when called
		if (jobCompletionTracker.size() > 10) {
			jobCompletionTracker.entrySet().removeIf(entry -> entry.getKey() < currentJobId - 5);
		}
	}
}
