package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics.PartitionMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

/**
 * Service for collecting and managing batch job metrics.
 */
@Service
public class BatchMetricsCollector {

	private static final Logger logger = LoggerFactory.getLogger(BatchMetricsCollector.class);

	// Use job GUID as the key for job metrics, recorded by arrival time
	private final Map<String, BatchMetrics> jobMetricsMap = new HashMap<>();
	private final LinkedList<String> jobMetricsByArrivalTime = new LinkedList<>();
	private final Object lock = new Object();

	public BatchMetrics initializeMetrics(Long jobExecutionId, String jobGuid) {
		if (jobExecutionId == null) {
			throw new BatchException("Job execution ID cannot be null");
		}

		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		try {
			synchronized (lock) {
				if (jobMetricsMap.containsKey(jobGuid)) {
					throw new BatchException(
							"job metrics already exists for GUID: " + jobGuid + ", job execution ID: " + jobExecutionId
					);
				}

				BatchMetrics metrics = new BatchMetrics(jobExecutionId, jobGuid);
				jobMetricsMap.put(jobGuid, metrics);
				jobMetricsByArrivalTime.add(jobGuid);

				logger.debug("[GUID: {}] Initialized metrics for job execution ID: {}", jobGuid, jobExecutionId);
				return metrics;
			}
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
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		try {
			synchronized (lock) {
				BatchMetrics metrics = getJobMetrics(jobGuid);

				BatchMetrics.PartitionMetrics partitionMetrics = new BatchMetrics.PartitionMetrics(partitionName);
				metrics.getPartitionMetrics().put(partitionName, partitionMetrics);
			}

			logger.debug(
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
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		try {
			var partitionMetrics = getPartitionMetrics(jobGuid, partitionName);

			partitionMetrics.setEndTime(LocalDateTime.now());
			partitionMetrics.setRecordsWritten(writeCount);
			partitionMetrics.setExitCode(exitCode);

			logger.debug(
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
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		try {
			BatchMetrics metrics = getJobMetrics(jobGuid);

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
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		// MDJ: Is it -ever- the case that error would be null? That is, that an operation would be
		// retried without an error occurring OR an error occurring but it not being known? It would
		// be good to -require- that error not be null. In any case, "Unknown" is accurate only in
		// the latter case (unknown error). In the former case, "No error" is better.

		try {
			String errorType = "Unknown";

			synchronized (lock) {
				BatchMetrics metrics = getJobMetrics(jobGuid);

				metrics.incrementRetryAttempts();
				if (successful) {
					metrics.incrementSuccessfulRetries();
				} else {
					metrics.incrementFailedRetries();
				}

				// Create retry detail
				String errorMessage = "No error message";
				if (error != null) {
					errorType = error.getClass().getSimpleName();
					if (error.getMessage() != null) {
						errorMessage = error.getMessage();
					}
				}

				BatchMetrics.RetryDetail retryDetail = new BatchMetrics.RetryDetail(
						attemptNumber, errorType, errorMessage, successful, partitionName
				);

				metrics.getRetryDetails().add(retryDetail);
			}

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
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("Job GUID cannot be null or blank");
		}

		// MDJ: Can batchRecord be null? recordId? partitionName? If so, this is very odd. In general,
		// require parameters not be null (using @NotNull to indicate this) unless it the interface
		// explicitly allows it. This will make the code much tighter and easier to understand.

		// MDJ: Is it -ever- the case that error would be null? That is, that an operation would be
		// retried without an error occurring OR an error occurring but it not being known? It would
		// be good to -require- that error not be null. In any case, "Unknown" is accurate only in
		// the latter case (unknown error). In the former case, "No error" is better.
		try {
			String errorType = "Unknown";

			synchronized (lock) {
				BatchMetrics metrics = getJobMetrics(jobGuid);

				metrics.incrementSkips();

				String errorMessage = "No error message";

				// Count skip reasons
				if (error != null) {
					errorType = error.getClass().getSimpleName();
					metrics.getSkipReasonCount().merge(errorType, 1, Integer::sum);

					if (error.getMessage() != null) {
						errorMessage = error.getMessage();
					}
				}

				// Create skip detail
				String recordData = batchRecord != null ? batchRecord.toString() : "null";

				BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
						recordId, recordData, errorType, errorMessage, partitionName, lineNumber
				);

				metrics.getSkipDetails().add(skipDetail);
			}

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
			synchronized (lock) {
				int nItemsToRemove = jobMetricsMap.size() - keepCount;
				for (int i = 0; i < nItemsToRemove; i++) {
					String jobGuid = jobMetricsByArrivalTime.removeFirst();

					var jobMetrics = jobMetricsMap.remove(jobGuid);
					assert jobMetrics != null;
				}

				assert jobMetricsMap.size() == jobMetricsByArrivalTime.size();

				logger.debug("Cleaned up old metrics, removed {} least recent entries", nItemsToRemove);
			}
		} catch (Exception e) {
			throw new BatchException("Failed to cleanup old metrics", e);
		}
	}

	public boolean isJobMetricsPresent(String jobGuid) {
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("jobGuid cannot be null or blank");
		}
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			return metrics != null;
		}
	}

	/**
	 * Return the BatchMetrics for the given jobGuid.
	 *
	 * @param jobGuid
	 * @return as described
	 * @throws BatchException if jobGuid is null/blank or no BatchMetrics instance exists for the given jobGuid.
	 */
	public BatchMetrics getJobMetrics(String jobGuid) {
		if (StringUtils.isBlank(jobGuid)) {
			throw new BatchException("jobGuid cannot be null or blank");
		}
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			if (metrics == null) {
				throw new BatchException("No metrics found for job GUID: " + jobGuid);
			}
			return metrics;
		}
	}

	private PartitionMetrics getPartitionMetrics(String jobGuid, String partitionName) {
		synchronized (lock) {
			var batchMetrics = getJobMetrics(jobGuid);
			var partitionMetrics = batchMetrics.getPartitionMetrics().get(partitionName);
			if (partitionMetrics == null) {
				throw new BatchException(
						"Partition metrics not found for partition " + partitionName + " of job GUID: " + jobGuid
				);
			}
			return partitionMetrics;
		}
	}
}
