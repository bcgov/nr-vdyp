package ca.bc.gov.nrs.vdyp.batch.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics.PartitionMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import jakarta.validation.constraints.NotNull;

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

	public BatchMetrics initializeMetrics(@NotNull Long jobExecutionId, @NotNull String jobGuid) {
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

	public void initializePartitionMetrics(
			@NotNull Long jobExecutionId, @NotNull String jobGuid, @NotNull String partitionName
	) {
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
			@NotNull Long jobExecutionId, @NotNull String jobGuid, @NotNull String partitionName, long writeCount,
			String exitCode
	) {
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

	public void finalizeJobMetrics(
			@NotNull Long jobExecutionId, @NotNull String jobGuid, String status, long totalRead, long totalWritten
	) {
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
			@NotNull Long jobExecutionId, @NotNull String jobGuid, int attemptNumber, @NotNull Throwable error,
			boolean successful, String partitionName
	) {
		try {
			String errorType = error.getClass().getSimpleName();
			String errorMessage = error.getMessage() != null ? error.getMessage() : "No error message";

			synchronized (lock) {
				BatchMetrics metrics = getJobMetrics(jobGuid);

				metrics.incrementRetryAttempts();
				if (successful) {
					metrics.incrementSuccessfulRetries();
				} else {
					metrics.incrementFailedRetries();
				}

				// Create retry detail
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
			@NotNull Long jobExecutionId, @NotNull String jobGuid, Long recordId, @NotNull BatchRecord batchRecord,
			@NotNull Throwable error, @NotNull String partitionName, Long lineNumber
	) {
		try {
			String errorType = error.getClass().getSimpleName();
			String errorMessage = error.getMessage() != null ? error.getMessage() : "No error message";

			synchronized (lock) {
				BatchMetrics metrics = getJobMetrics(jobGuid);

				metrics.incrementSkips();

				// Count skip reasons
				metrics.getSkipReasonCount().merge(errorType, 1, Integer::sum);

				// Create skip detail
				String recordData = batchRecord.toString();

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

	public boolean isJobMetricsPresent(@NotNull String jobGuid) {
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			return metrics != null;
		}
	}

	public BatchMetrics getJobMetrics(@NotNull String jobGuid) {
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			if (metrics == null) {
				throw new BatchException("No metrics found for job GUID: " + jobGuid);
			}
			return metrics;
		}
	}

	private PartitionMetrics getPartitionMetrics(@NotNull String jobGuid, @NotNull String partitionName) {
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
