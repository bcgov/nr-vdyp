package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics.PartitionMetrics;

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

	public BatchMetrics initializeMetrics(@NonNull Long jobExecutionId, @NonNull String jobGuid)
			throws BatchMetricsException {
		synchronized (lock) {
			if (jobMetricsMap.containsKey(jobGuid)) {
				throw BatchMetricsException
						.handleMetricsFailure("Job metrics already exists", jobGuid, jobExecutionId, logger);
			}

			BatchMetrics metrics = new BatchMetrics(jobExecutionId, jobGuid);
			jobMetricsMap.put(jobGuid, metrics);
			jobMetricsByArrivalTime.add(jobGuid);

			logger.trace("[GUID: {}, EXEID: {}] Initialized metrics", jobGuid, jobExecutionId);
			return metrics;
		}
	}

	public void initializePartitionMetrics(
			@NonNull Long jobExecutionId, @NonNull String jobGuid, @NonNull String partitionName
	) throws BatchMetricsException {
		synchronized (lock) {
			BatchMetrics metrics = getJobMetrics(jobGuid);

			BatchMetrics.PartitionMetrics partitionMetrics = new BatchMetrics.PartitionMetrics(partitionName);
			metrics.getPartitionMetrics().put(partitionName, partitionMetrics);
		}

		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Initialized partition metrics", jobGuid, jobExecutionId,
				partitionName
		);
	}

	public void completePartitionMetrics(
			@NonNull Long jobExecutionId, @NonNull String jobGuid, @NonNull String partitionName, long writeCount,
			String exitCode
	) throws BatchMetricsException {
		var partitionMetrics = getPartitionMetrics(jobGuid, partitionName);

		partitionMetrics.complete(writeCount, exitCode);

		logger.trace(
				"[GUID: {}, EXEID: {}, Partition: {}] Completed partition metrics, written: {}, exitCode: {}", jobGuid,
				jobExecutionId, partitionName, writeCount, exitCode
		);
	}

	public void finalizeJobMetrics(
			@NonNull Long jobExecutionId, @NonNull String jobGuid, String status, long totalRead, long totalWritten
	) throws BatchMetricsException {
		BatchMetrics metrics = getJobMetrics(jobGuid);

		metrics.finalizeJob(status, totalRead, totalWritten);

		logger.trace(
				"[GUID: {}, EXEID: {}] Finalized metrics: status={}, read={}, written={}", jobGuid, jobExecutionId,
				status, totalRead, totalWritten
		);
	}

	public void recordRetryAttempt(
			@NonNull Long jobExecutionId, @NonNull String jobGuid, int attemptNumber, @NonNull Throwable error,
			boolean successful, String partitionName
	) throws BatchMetricsException {
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

		logger.warn(
				"[GUID: {}, Partition: {}] Recorded retry attempt #{} for job execution ID: {}, successful: {}, error: {}",
				jobGuid, partitionName, attemptNumber, jobExecutionId, successful, errorType
		);
	}

	public void recordSkip(
			@NonNull Long jobExecutionId, @NonNull String jobGuid, String featureId, @NonNull Throwable error,
			@NonNull String partitionName
	) throws BatchMetricsException {
		String errorType = error.getClass().getSimpleName();
		String errorMessage = error.getMessage() != null ? error.getMessage() : "No error message";

		synchronized (lock) {
			BatchMetrics metrics = getJobMetrics(jobGuid);

			metrics.incrementSkips();

			// Count skip reasons
			metrics.getSkipReasonCount().merge(errorType, 1, Integer::sum);

			// Create skip detail
			BatchMetrics.SkipDetail skipDetail = new BatchMetrics.SkipDetail(
					featureId, errorType, errorMessage, partitionName
			);

			metrics.getSkipDetails().add(skipDetail);
		}

		logger.warn(
				"[GUID: {}, Partition: {}] Recorded skip for job execution ID: {}, featureId: {}, error: {}", jobGuid,
				partitionName, jobExecutionId, featureId, errorType
		);
	}

	public void cleanupOldMetrics(int keepCount) throws BatchMetricsException {
		if (keepCount < 0) {
			throw BatchMetricsException
					.handleMetricsFailure("Keep count must be non-negative, got: " + keepCount, logger);
		}

		synchronized (lock) {
			int nItemsToRemove = jobMetricsMap.size() - keepCount;
			for (int i = 0; i < nItemsToRemove; i++) {
				String jobGuid = jobMetricsByArrivalTime.removeFirst();

				var jobMetrics = jobMetricsMap.remove(jobGuid);
				assert jobMetrics != null;
			}

			assert jobMetricsMap.size() == jobMetricsByArrivalTime.size();

			logger.trace("Cleaned up old metrics, removed {} least recent entries", nItemsToRemove);
		}
	}

	public boolean isJobMetricsPresent(@NonNull String jobGuid) {
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			return metrics != null;
		}
	}

	public BatchMetrics getJobMetrics(@NonNull String jobGuid) throws BatchMetricsException {
		synchronized (lock) {
			var metrics = jobMetricsMap.get(jobGuid);
			if (metrics == null) {
				throw BatchMetricsException.handleMetricsFailure("No metrics found", jobGuid, logger);
			}
			return metrics;
		}
	}

	private PartitionMetrics getPartitionMetrics(@NonNull String jobGuid, @NonNull String partitionName)
			throws BatchMetricsException {
		synchronized (lock) {
			var batchMetrics = getJobMetrics(jobGuid);
			var partitionMetrics = batchMetrics.getPartitionMetrics().get(partitionName);
			if (partitionMetrics == null) {
				throw BatchMetricsException.handleMetricsFailure(
						"Partition metrics not found for partition " + partitionName, jobGuid,
						batchMetrics.getJobExecutionId(), logger
				);
			}
			return partitionMetrics;
		}
	}
}
