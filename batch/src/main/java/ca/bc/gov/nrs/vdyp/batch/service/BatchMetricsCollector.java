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

	public BatchMetrics initializeMetrics(Long jobExecutionId, String jobGuid) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		try {
			BatchMetrics metrics = new BatchMetrics(jobExecutionId, jobGuid);
			jobMetricsMap.put(jobExecutionId, metrics);
			logger.debug("[GUID: {}] Initialized metrics for job execution ID: {}", jobGuid, jobExecutionId);
			return metrics;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to initialize metrics for job execution ID: " + jobExecutionId + ", GUID: " + jobGuid, e
			);
		}
	}

	public void initializePartitionMetrics(Long jobExecutionId, String jobGuid, String partitionName) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
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
					"[{}] [GUID: {}] Initialized partition metrics for job execution ID: {}", partitionName, jobGuid,
					jobExecutionId
			);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to initialize partition metrics for partition: " + partitionName + " in job: "
							+ jobExecutionId + ", GUID: " + jobGuid,
					e
			);
		}
	}

	public void completePartitionMetrics(
			Long jobExecutionId, String jobGuid, String partitionName, long writeCount, String exitCode
	) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
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
						"No partition metrics found for partition: " + partitionName + " in job: " + jobExecutionId
				);
			}

			partitionMetrics.setEndTime(LocalDateTime.now());
			partitionMetrics.setRecordsWritten(writeCount);
			partitionMetrics.setExitCode(exitCode);

			logger.info(
					"[{}] [GUID: {}] Completed partition metrics for job execution ID: {}, written: {}, exitCode: {}",
					partitionName, jobGuid, jobExecutionId, writeCount, exitCode
			);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to complete partition metrics for partition: " + partitionName + " in job: "
							+ jobExecutionId + ", GUID: " + jobGuid,
					e
			);
		}
	}

	public void
			finalizeJobMetrics(Long jobExecutionId, String jobGuid, String status, long totalRead, long totalWritten) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
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
					"[GUID: {}] Finalized job execution ID: {} metrics: status={}, read={}, written={}", jobGuid,
					jobExecutionId, status, totalRead, totalWritten
			);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException("Failed to finalize job metrics for job execution ID: " + jobExecutionId, e);
		}
	}

	public void recordRetryAttempt(
			Long jobExecutionId, String jobGuid, int attemptNumber, Throwable error, boolean successful,
			String partitionName
	) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
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
					attemptNumber, errorType, errorMessage, successful, partitionName
			);

			metrics.getRetryDetails().add(retryDetail);

			logger.debug(
					"[GUID: {}, Partition: {}] Recorded retry attempt #{} for job execution ID: {}, successful: {}, error: {}",
					jobGuid, partitionName, attemptNumber, jobExecutionId, successful, errorType
			);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException(
					"Failed to record retry attempt for job execution ID: " + jobExecutionId + ", [GUID: " + jobGuid
							+ "]",
					e
			);
		}
	}

	public void recordSkip(
			Long jobExecutionId, String jobGuid, Long recordId, BatchRecord batchRecord, Throwable error,
			String partitionName, Long lineNumber
	) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		if (jobGuid == null || jobGuid.isBlank()) {
			throw new BatchException("Job GUID cannot be null or blank");
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
					recordId, recordData, errorType, errorMessage, partitionName, lineNumber
			);

			metrics.getSkipDetails().add(skipDetail);

			logger.warn(
					"[GUID: {}, Partition: {}] Recorded skip for job execution ID: {}, line: {}, recordId: {}, error: {}",
					jobGuid, partitionName, jobExecutionId, lineNumber, recordId, errorType
			);
		} catch (BatchException e) {
			throw e;
		} catch (Exception e) {
			throw new BatchException("Failed to record skip event for job execution ID: " + jobExecutionId, e);
		}
	}

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

	public BatchMetrics getJobMetrics(Long jobExecutionId) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}
		return jobMetricsMap.get(jobExecutionId);
	}
}
