package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

/**
 * Service for collecting and managing batch job metrics.
 */
@Service
public class BatchMetricsCollector {

	private static final Logger logger = LoggerFactory.getLogger(BatchMetricsCollector.class);

	private final Map<Long, BatchMetrics> jobMetricsMap = new ConcurrentHashMap<>();

	/**
	 * Initialize metrics for a new job execution.
	 *
	 * @param jobExecutionId The job execution ID
	 * @return The initialized BatchMetrics instance
	 */
	public BatchMetrics initializeMetrics(Long jobExecutionId) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}

		try {
			BatchMetrics metrics = new BatchMetrics(jobExecutionId);
			jobMetricsMap.put(jobExecutionId, metrics);
			logger.debug("Initialized metrics for job execution ID: {}", jobExecutionId);
			return metrics;
		} catch (Exception e) {
			throw new BatchException("Failed to initialize metrics for job execution ID: " + jobExecutionId, e);
		}
	}

	/**
	 * Initialize partition-specific metrics.
	 */
	public void initializePartitionMetrics(Long jobExecutionId, String partitionName) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (partitionName == null || partitionName.isBlank()) {
			throw new BatchException("Partition name cannot be null or blank");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobExecutionId);
			if (metrics == null) {
				throw new BatchException("No metrics found for job execution ID: " + jobExecutionId);
			}

			BatchMetrics.PartitionMetrics partitionMetrics = new BatchMetrics.PartitionMetrics(partitionName);
			partitionMetrics.setStartTime(LocalDateTime.now());
			metrics.getPartitionMetrics().put(partitionName, partitionMetrics);
			logger.info(
					"[{}] Initialized partition metrics for job {}", partitionName, jobExecutionId);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to initialize partition metrics for partition: " + partitionName + " in job: "
							+ jobExecutionId,
					e);
		}
	}

	/**
	 * Complete partition-specific metrics.
	 */
	public void completePartitionMetrics(Long jobExecutionId, String partitionName, long writeCount, String exitCode) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (partitionName == null || partitionName.isBlank()) {
			throw new BatchException("Partition name cannot be null or blank");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobExecutionId);
			if (metrics == null) {
				throw new BatchException("No metrics found for job execution ID: " + jobExecutionId);
			}

			BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(partitionName);
			if (partitionMetrics == null) {
				throw new BatchException(
						"No partition metrics found for partition: " + partitionName + " in job: " + jobExecutionId);
			}

			partitionMetrics.setEndTime(LocalDateTime.now());
			partitionMetrics.setRecordsWritten(writeCount);
			partitionMetrics.setExitCode(exitCode);
			logger.info(
					"[{}] Completed partition metrics for job {}, written: {}, exitCode: {}", partitionName,
					jobExecutionId, writeCount, exitCode);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to complete partition metrics for partition: " + partitionName + " in job: "
							+ jobExecutionId,
					e);
		}
	}

	/**
	 * Finalize job metrics with totals.
	 */
	public void finalizeJobMetrics(Long jobExecutionId, String status, long totalRead, long totalWritten) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobExecutionId);
			if (metrics == null) {
				throw new BatchException("No metrics found for job execution ID: " + jobExecutionId);
			}

			metrics.setEndTime(LocalDateTime.now());
			metrics.setStatus(status);
			metrics.setTotalRecordsRead(totalRead);
			metrics.setTotalRecordsWritten(totalWritten);
			metrics.setTotalRecordsProcessed(totalWritten);
			logger.info(
					"Finalized job {} metrics: status={}, read={}, written={}", jobExecutionId, status, totalRead,
					totalWritten);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException("Failed to finalize job metrics for job execution ID: " + jobExecutionId, e);
		}
	}

	/**
	 * Record a retry attempt.
	 */
	public void recordRetryAttempt(
			Long jobExecutionId, Long recordId, BatchRecord batchRecord, int attemptNumber, Throwable error,
			boolean successful, String partitionName) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobExecutionId);
			if (metrics == null) {
				throw new BatchException("No metrics found for job execution ID: " + jobExecutionId);
			}

			// Thread-safe increment operations
			metrics.incrementRetryAttempts();

			if (successful) {
				metrics.incrementSuccessfulRetries();
			} else {
				metrics.incrementFailedRetries();
			}

			// Create retry detail
			String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
			String errorMessage = error != null ? error.getMessage() : "No error message";

			BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
					recordId, batchRecord != null ? batchRecord.toString() : "null", attemptNumber, errorType,
					errorMessage, successful, partitionName);

			synchronized (metrics.getRetryDetails()) {
				metrics.getRetryDetails().add(retryDetail);
			}
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException("Failed to record retry attempt for job execution ID: " + jobExecutionId, e);
		}
	}

	/**
	 * Record a skip event.
	 */
	public void recordSkip(
			Long jobExecutionId, Long recordId, BatchRecord batchRecord, Throwable error, String partitionName,
			Long lineNumber) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobExecutionId);
			if (metrics == null) {
				throw new BatchException("No metrics found for job execution ID: " + jobExecutionId);
			}

			// Thread-safe increment operation
			metrics.incrementSkips();

			// Count skip reasons
			String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
			metrics.getSkipReasonCount().merge(errorType, 1, Integer::sum);

			// Create skip detail
			String errorMessage = error != null ? error.getMessage() : "No error message";
			String recordData = batchRecord != null ? batchRecord.toString() : "null";

			BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
					recordId, recordData, errorType, errorMessage, partitionName, lineNumber);

			synchronized (metrics.getSkipDetails()) {
				metrics.getSkipDetails().add(skipDetail);
			}
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException("Failed to record skip event for job execution ID: " + jobExecutionId, e);
		}
	}

	/**
	 * Clean up old metrics to prevent memory leaks.
	 */
	public void cleanupOldMetrics(int keepCount) {
		if (keepCount < 0) {
			throw new BatchException("Keep count must be non-negative, got: " + keepCount);
		}

		try {
			if (jobMetricsMap.size() > keepCount) {
				// Remove oldest entries, keeping only the most recent ones
				jobMetricsMap.entrySet().stream().sorted(Map.Entry.<Long, BatchMetrics>comparingByKey().reversed())
						.skip(keepCount).map(Map.Entry::getKey).forEach(jobMetricsMap::remove);
				logger.debug("Cleaned up old metrics, kept {} most recent entries", keepCount);
			}
		} catch (Exception e) {
			throw new BatchException("Failed to cleanup old metrics", e);
		}
	}

	/**
	 * Get metrics for a specific job execution.
	 *
	 * @param jobExecutionId The job execution ID
	 * @return BatchMetrics instance or null if not found
	 */
	public BatchMetrics getJobMetrics(Long jobExecutionId) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		return jobMetricsMap.get(jobExecutionId);
	}
}
