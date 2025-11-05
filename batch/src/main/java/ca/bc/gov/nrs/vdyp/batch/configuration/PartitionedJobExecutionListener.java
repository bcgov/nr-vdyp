package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job execution listener for partitioned VDYP batch job.
 */
@Component
public class PartitionedJobExecutionListener implements JobExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(PartitionedJobExecutionListener.class);

	private final BatchProperties batchProperties;

	// Thread safety for afterJob execution - using job execution ID as key
	private final ConcurrentHashMap<Long, Boolean> jobCompletionTracker = new ConcurrentHashMap<>();
	private final Object lock = new Object();

	public PartitionedJobExecutionListener(BatchProperties batchProperties) {
		this.batchProperties = batchProperties;
	}

	@Override
	public void beforeJob(@NonNull JobExecution jobExecution) {
		// Initialize tracking for this job execution
		jobCompletionTracker.put(jobExecution.getId(), false);

		String separator = "============================================================";
		logger.info(separator);
		logger.info("VDYP PARTITIONED JOB STARTING");

		Long partitionSize = jobExecution.getJobParameters().getLong(BatchConstants.Partition.SIZE);
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

		int actualPartitionSize;
		if (partitionSize != null) {
			actualPartitionSize = partitionSize.intValue();
		} else if (batchProperties.getPartition().getDefaultPartitionSize() > 0) {
			actualPartitionSize = batchProperties.getPartition().getDefaultPartitionSize();
		} else {
			throw new IllegalStateException(
					"batch.partition.default-partition-size must be configured in application.properties"
			);
		}

		logger.info("VDYP Grid Size: {}", actualPartitionSize);
		logger.info("Expected Partitions: {}", actualPartitionSize);
		logger.info("Job Execution ID: {}", jobExecution.getId());
		logger.info("Job GUID: {}", jobGuid);
		logger.info(separator);
	}

	@Override
	public void afterJob(@NonNull JobExecution jobExecution) {
		Long jobExecutionId = jobExecution.getId();
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

		// Check if this specific job execution has already been processed
		Boolean alreadyProcessed = jobCompletionTracker.get(jobExecutionId);
		if (alreadyProcessed == null || alreadyProcessed) {
			logger.info(
					"[GUID: {}] VDYP Job {} already processed or not tracked, skipping afterJob processing", jobGuid,
					jobExecutionId
			);
			return;
		}

		// Use synchronization to ensure only one thread processes this job completion
		synchronized (lock) {
			// Double-check after acquiring lock
			if (Boolean.TRUE.equals(jobCompletionTracker.get(jobExecutionId))) {
				logger.info(
						"[GUID: {}] VDYP Job {} already processed by another thread, skipping", jobGuid, jobExecutionId
				);
				return;
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
		}

		cleanupOldJobTracker(jobExecutionId);
	}

	/**
	 * Cleans up old job execution tracking to prevent memory leaks.
	 */
	private void cleanupOldJobTracker(Long currentJobId) {
		if (jobCompletionTracker.size() > 10) {
			jobCompletionTracker.entrySet().removeIf(entry -> entry.getKey() < currentJobId - 5);
		}
	}
}
