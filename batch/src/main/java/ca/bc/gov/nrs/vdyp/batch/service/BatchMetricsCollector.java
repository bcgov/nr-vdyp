package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
		BatchMetrics metrics = new BatchMetrics(jobExecutionId);
		jobMetricsMap.put(jobExecutionId, metrics);
		return metrics;
	}

	/**
	 * Initialize metrics for a new job execution
	 */
	public void initializeJobMetrics(Long jobExecutionId) {
		initializeMetrics(jobExecutionId);
	}

	/**
	 * Initialize partition-specific metrics.
	 */
	public void initializePartitionMetrics(Long jobExecutionId, String partitionName, long startLine, long endLine) {
		BatchMetrics metrics = getJobMetrics(jobExecutionId);
		if (metrics != null) {
			BatchMetrics.PartitionMetrics partitionMetrics = new BatchMetrics.PartitionMetrics(partitionName);
			partitionMetrics.setStartTime(LocalDateTime.now());
			metrics.getPartitionMetrics().put(partitionName, partitionMetrics);
			logger.info(
					"[{}] Initialized partition metrics for job {}, lines {}-{}", partitionName, jobExecutionId,
					startLine, endLine
			);
		}
	}

	/**
	 * Complete partition-specific metrics.
	 */
	public void completePartitionMetrics(Long jobExecutionId, String partitionName, long writeCount, String exitCode) {
		BatchMetrics metrics = getJobMetrics(jobExecutionId);
		if (metrics != null) {
			BatchMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics().get(partitionName);
			if (partitionMetrics != null) {
				partitionMetrics.setEndTime(LocalDateTime.now());
				partitionMetrics.setRecordsWritten((int) writeCount);
				partitionMetrics.setExitCode(exitCode);
				logger.info(
						"[{}] Completed partition metrics for job {}, written: {}, exitCode: {}", partitionName, jobExecutionId,
						writeCount, exitCode
				);
			}
		}
	}

	/**
	 * Finalize job metrics with totals.
	 */
	public void finalizeJobMetrics(Long jobExecutionId, String status, long totalRead, long totalWritten) {
		BatchMetrics metrics = getJobMetrics(jobExecutionId);
		if (metrics != null) {
			metrics.setEndTime(LocalDateTime.now());
			metrics.setStatus(status);
			metrics.setTotalRecordsRead(totalRead);
			metrics.setTotalRecordsWritten(totalWritten);
			metrics.setTotalRecordsProcessed(totalWritten);
			logger.info(
					"Finalized job {} metrics: status={}, read={}, written={}", jobExecutionId, status, totalRead,
					totalWritten
			);
		}
	}

	/**
	 * Record a retry attempt.
	 */
	public void recordRetryAttempt(
			Long jobExecutionId, Long recordId, BatchRecord batchRecord, int attemptNumber, Throwable error,
			boolean successful, String partitionName
	) {
		BatchMetrics metrics = getJobMetrics(jobExecutionId);
		if (metrics != null) {
			metrics.setTotalRetryAttempts(metrics.getTotalRetryAttempts() + 1);

			if (successful) {
				metrics.setSuccessfulRetries(metrics.getSuccessfulRetries() + 1);
			} else {
				metrics.setFailedRetries(metrics.getFailedRetries() + 1);
			}

			// Create retry detail
			String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
			String errorMessage = error != null ? error.getMessage() : "No error message";

			BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
					recordId, batchRecord != null ? batchRecord.toString() : "null", attemptNumber, errorType, errorMessage,
					successful, partitionName
			);

			metrics.getRetryDetails().add(retryDetail);
		}
	}

	/**
	 * Record a skip event.
	 */
	public void recordSkip(
			Long jobExecutionId, Long recordId, BatchRecord batchRecord, Throwable error, String partitionName,
			Long lineNumber
	) {
		BatchMetrics metrics = getJobMetrics(jobExecutionId);
		if (metrics != null) {
			metrics.setTotalSkips(metrics.getTotalSkips() + 1);

			// Count skip reasons
			String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
			metrics.getSkipReasonCount().merge(errorType, 1, Integer::sum);

			// Create skip detail
			String errorMessage = error != null ? error.getMessage() : "No error message";
			String recordData = batchRecord != null ? batchRecord.toString() : "null";

			BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
					recordId, recordData, errorType, errorMessage, partitionName, lineNumber
			);

			metrics.getSkipDetails().add(skipDetail);
		}
	}

	/**
	 * Clean up old metrics to prevent memory leaks.
	 */
	public void cleanupOldMetrics(int keepCount) {
		if (jobMetricsMap.size() > keepCount) {
			// Remove oldest entries, keeping only the most recent ones
			jobMetricsMap.entrySet().stream().sorted(Map.Entry.<Long, BatchMetrics>comparingByKey().reversed())
					.skip(keepCount).map(Map.Entry::getKey).forEach(jobMetricsMap::remove);
		}
	}

	/**
	 * Get metrics for a specific job execution.
	 *
	 * @param jobExecutionId The job execution ID
	 * @return BatchMetrics instance or null if not found
	 */
	public BatchMetrics getJobMetrics(Long jobExecutionId) {
		return jobMetricsMap.get(jobExecutionId);
	}

	/**
	 * Get all job metrics.
	 *
	 * @return Map of all job metrics keyed by job execution ID
	 */
	public Map<Long, BatchMetrics> getAllJobMetrics() {
		return new ConcurrentHashMap<>(jobMetricsMap);
	}

	/**
	 * Update metrics for a job execution.
	 *
	 * @param jobExecutionId The job execution ID
	 * @param metrics        The updated metrics
	 */
	public void updateMetrics(Long jobExecutionId, BatchMetrics metrics) {
		jobMetricsMap.put(jobExecutionId, metrics);
	}

	/**
	 * Remove metrics for a completed job (cleanup).
	 *
	 * @param jobExecutionId The job execution ID
	 */
	public void removeMetrics(Long jobExecutionId) {
		jobMetricsMap.remove(jobExecutionId);
	}

	/**
	 * Clear all metrics (for testing or maintenance).
	 */
	public void clearAllMetrics() {
		jobMetricsMap.clear();
	}
}