package ca.bc.gov.nrs.vdyp.batch.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batch job execution metrics and detailed error information for batch processing.
 */
public class BatchMetrics {
	private final Long jobExecutionId;
	private final String jobGuid;
	private final LocalDateTime startTime;

	private String status;
	private LocalDateTime endTime;

	// Retry metrics - thread-safe counters and lock-free queue
	private final AtomicInteger totalRetryAttempts = new AtomicInteger(0);
	private final AtomicInteger successfulRetries = new AtomicInteger(0);
	private final AtomicInteger failedRetries = new AtomicInteger(0);
	private final Queue<RetryDetail> retryDetails = new ConcurrentLinkedQueue<>();

	// Skip metrics - thread-safe counters and lock-free queue
	private final AtomicInteger totalSkips = new AtomicInteger(0);
	private final Map<String, Integer> skipReasonCount = new ConcurrentHashMap<>();
	private final Queue<SkipDetail> skipDetails = new ConcurrentLinkedQueue<>();

	// Partition metrics
	private final Map<String, PartitionMetrics> partitionMetrics = new ConcurrentHashMap<>();

	// Processing statistics - thread-safe counters
	private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
	private final AtomicLong totalRecordsRead = new AtomicLong(0);
	private final AtomicLong totalRecordsWritten = new AtomicLong(0);
	private double averageProcessingTime = 0.0;

	public BatchMetrics(Long jobExecutionId, String jobGuid) {
		this.jobExecutionId = jobExecutionId;
		this.jobGuid = jobGuid;
		this.startTime = LocalDateTime.now();
		this.status = "STARTING";
	}

	/**
	 * Retry detail information
	 */
	public static record RetryDetail(
			int attemptNumber, String errorType, String errorMessage, LocalDateTime timestamp, boolean successful,
			String partitionName
	) {
		public RetryDetail(
				int attemptNumber, String errorType, String errorMessage, boolean successful, String partitionName
		) {
			this(attemptNumber, errorType, errorMessage, LocalDateTime.now(), successful, partitionName);
		}
	}

	/**
	 * Skip detail information
	 */
	public static record SkipDetail(
			String featureId, String errorType, String errorMessage, LocalDateTime timestamp, String partitionName
	) {
		public SkipDetail(String featureId, String errorType, String errorMessage, String partitionName) {
			this(featureId, errorType, errorMessage, LocalDateTime.now(), partitionName);
		}
	}

	/**
	 * Partition-level metrics with controlled state transitions.
	 *
	 * This class enforces proper lifecycle management: 1. Partition starts with initial state (name, startTime) 2.
	 * Statistics can be updated during execution (records, retries, skips) 3. Partition completes with final state
	 * (endTime, exitCode) set atomically 4. After completion, no further modifications are allowed
	 */
	public static class PartitionMetrics {
		private final String partitionName;
		private final LocalDateTime startTime;

		private long recordsProcessed = 0;
		private long recordsRead = 0;
		private long recordsWritten = 0;
		private int retryCount = 0;
		private int skipCount = 0;
		private LocalDateTime endTime;
		private String exitCode;

		public PartitionMetrics(String partitionName) {
			this.partitionName = partitionName;
			this.startTime = LocalDateTime.now();
		}

		public String getPartitionName() {
			return partitionName;
		}

		public long getRecordsProcessed() {
			return recordsProcessed;
		}

		public long getRecordsRead() {
			return recordsRead;
		}

		public long getRecordsWritten() {
			return recordsWritten;
		}

		public int getRetryCount() {
			return retryCount;
		}

		public int getSkipCount() {
			return skipCount;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		/**
		 * Gets the partition end time.
		 *
		 * @return The end time, or null if the partition has not completed yet. Callers MUST check for null before
		 *         using the returned value.
		 */
		public LocalDateTime getEndTime() {
			return endTime;
		}

		/**
		 * Gets the partition exit code.
		 *
		 * @return The exit code, or null if the partition has not completed yet. Callers MUST check for null before
		 *         using the returned value.
		 */
		public String getExitCode() {
			return exitCode;
		}

		/**
		 * Records that partition work is complete. Sets endTime and exitCode together atomically to ensure consistency.
		 *
		 * @param recordsWritten The final count of records written
		 * @param exitCode       The partition exit code (e.g., "COMPLETED", "FAILED")
		 * @throws IllegalStateException if partition is already completed
		 */
		public void complete(long recordsWritten, String exitCode) {
			if (this.endTime != null) {
				throw new IllegalStateException("Partition " + partitionName + " already completed at " + this.endTime);
			}
			this.recordsWritten = recordsWritten;
			this.endTime = LocalDateTime.now();
			this.exitCode = exitCode;
		}

		/**
		 * Increments the retry count for this partition.
		 */
		public void incrementRetryCount() {
			this.retryCount++;
		}

		/**
		 * Increments the skip count for this partition.
		 */
		public void incrementSkipCount() {
			this.skipCount++;
		}
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
	}

	public String getJobGuid() {
		return jobGuid;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	/**
	 * Gets the job end time.
	 *
	 * @return The end time, or null if the job has not completed yet. Callers MUST check for null before using the
	 *         returned value.
	 */
	public LocalDateTime getEndTime() {
		return endTime;
	}

	public String getStatus() {
		return status;
	}

	/**
	 * Finalize job execution with end time, status, and final record counts. Sets all completion-related fields
	 * together atomically to ensure consistency.
	 *
	 * @param status              The final job status (e.g., "COMPLETED", "FAILED")
	 * @param totalRecordsRead    The final count of records read
	 * @param totalRecordsWritten The final count of records written
	 * @throws IllegalStateException if job is already finalized
	 */
	public void finalizeJob(String status, long totalRecordsRead, long totalRecordsWritten) {
		if (this.endTime != null) {
			throw new IllegalStateException(
					"Job " + jobGuid + " already finalized at " + this.endTime + " with status " + this.status
			);
		}
		this.endTime = LocalDateTime.now();
		this.status = status;
		this.totalRecordsRead.set(totalRecordsRead);
		this.totalRecordsWritten.set(totalRecordsWritten);
		this.totalRecordsProcessed.set(totalRecordsWritten);
	}

	public int getTotalRetryAttempts() {
		return totalRetryAttempts.get();
	}

	public int incrementRetryAttempts() {
		return totalRetryAttempts.incrementAndGet();
	}

	public int getSuccessfulRetries() {
		return successfulRetries.get();
	}

	public int incrementSuccessfulRetries() {
		return successfulRetries.incrementAndGet();
	}

	public int getFailedRetries() {
		return failedRetries.get();
	}

	public int incrementFailedRetries() {
		return failedRetries.incrementAndGet();
	}

	public Queue<RetryDetail> getRetryDetails() {
		return retryDetails;
	}

	/**
	 * Get retry details as a list for JSON serialization compatibility.
	 */
	public List<RetryDetail> getRetryDetailsList() {
		return new ArrayList<>(retryDetails);
	}

	public int getTotalSkips() {
		return totalSkips.get();
	}

	public int incrementSkips() {
		return totalSkips.incrementAndGet();
	}

	public Map<String, Integer> getSkipReasonCount() {
		return skipReasonCount;
	}

	public Queue<SkipDetail> getSkipDetails() {
		return skipDetails;
	}

	/**
	 * Get skip details as a list for JSON serialization compatibility.
	 */
	public List<SkipDetail> getSkipDetailsList() {
		return new java.util.ArrayList<>(skipDetails);
	}

	public Map<String, PartitionMetrics> getPartitionMetrics() {
		return partitionMetrics;
	}

	public long getTotalRecordsProcessed() {
		return totalRecordsProcessed.get();
	}

	public long getTotalRecordsRead() {
		return totalRecordsRead.get();
	}

	public long getTotalRecordsWritten() {
		return totalRecordsWritten.get();
	}

	public double getAverageProcessingTime() {
		return averageProcessingTime;
	}

	public void setAverageProcessingTime(double averageProcessingTime) {
		this.averageProcessingTime = averageProcessingTime;
	}
}
