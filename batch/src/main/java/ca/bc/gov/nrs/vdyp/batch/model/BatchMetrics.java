package ca.bc.gov.nrs.vdyp.batch.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batch job execution metrics and detailed error information for batch processing.
 */
public class BatchMetrics {

	private Long jobExecutionId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String status;

	// Retry metrics - thread-safe counters
	private final AtomicInteger totalRetryAttempts = new AtomicInteger(0);
	private final AtomicInteger successfulRetries = new AtomicInteger(0);
	private final AtomicInteger failedRetries = new AtomicInteger(0);
	private final List<RetryDetail> retryDetails = new ArrayList<>();

	// Skip metrics - thread-safe counters
	private final AtomicInteger totalSkips = new AtomicInteger(0);
	private final Map<String, Integer> skipReasonCount = new ConcurrentHashMap<>();
	private final List<SkipDetail> skipDetails = new ArrayList<>();

	// Partition metrics
	private final Map<String, PartitionMetrics> partitionMetrics = new ConcurrentHashMap<>();

	// Processing statistics - thread-safe counters
	private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
	private final AtomicLong totalRecordsRead = new AtomicLong(0);
	private final AtomicLong totalRecordsWritten = new AtomicLong(0);
	private double averageProcessingTime = 0.0;

	public BatchMetrics() {
	}

	public BatchMetrics(Long jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
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
			Long recordId, String recordData, String errorType, String errorMessage, LocalDateTime timestamp,
			String partitionName, Long lineNumber
	) {
		public SkipDetail(
				Long recordId, String recordData, String errorType, String errorMessage, String partitionName,
				Long lineNumber
		) {
			this(recordId, recordData, errorType, errorMessage, LocalDateTime.now(), partitionName, lineNumber);
		}
	}

	/**
	 * Partition-level metrics
	 */
	public static class PartitionMetrics {
		private String partitionName;
		private long recordsProcessed = 0;
		private long recordsRead = 0;
		private long recordsWritten = 0;
		private int retryCount = 0;
		private int skipCount = 0;
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private String exitCode;

		public PartitionMetrics(String partitionName) {
			this.partitionName = partitionName;
			this.startTime = LocalDateTime.now();
		}

		public String getPartitionName() {
			return partitionName;
		}

		public void setPartitionName(String partitionName) {
			this.partitionName = partitionName;
		}

		public long getRecordsProcessed() {
			return recordsProcessed;
		}

		public void setRecordsProcessed(long recordsProcessed) {
			this.recordsProcessed = recordsProcessed;
		}

		public long getRecordsRead() {
			return recordsRead;
		}

		public void setRecordsRead(long recordsRead) {
			this.recordsRead = recordsRead;
		}

		public long getRecordsWritten() {
			return recordsWritten;
		}

		public void setRecordsWritten(long recordsWritten) {
			this.recordsWritten = recordsWritten;
		}

		public int getRetryCount() {
			return retryCount;
		}

		public void setRetryCount(int retryCount) {
			this.retryCount = retryCount;
		}

		public int getSkipCount() {
			return skipCount;
		}

		public void setSkipCount(int skipCount) {
			this.skipCount = skipCount;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}

		public String getExitCode() {
			return exitCode;
		}

		public void setExitCode(String exitCode) {
			this.exitCode = exitCode;
		}
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
	}

	public void setJobExecutionId(Long jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getTotalRetryAttempts() {
		return totalRetryAttempts.get();
	}

	public void setTotalRetryAttempts(int totalRetryAttempts) {
		this.totalRetryAttempts.set(totalRetryAttempts);
	}

	public int incrementRetryAttempts() {
		return totalRetryAttempts.incrementAndGet();
	}

	public int getSuccessfulRetries() {
		return successfulRetries.get();
	}

	public void setSuccessfulRetries(int successfulRetries) {
		this.successfulRetries.set(successfulRetries);
	}

	public int incrementSuccessfulRetries() {
		return successfulRetries.incrementAndGet();
	}

	public int getFailedRetries() {
		return failedRetries.get();
	}

	public void setFailedRetries(int failedRetries) {
		this.failedRetries.set(failedRetries);
	}

	public int incrementFailedRetries() {
		return failedRetries.incrementAndGet();
	}

	public List<RetryDetail> getRetryDetails() {
		return retryDetails;
	}

	public void setRetryDetails(List<RetryDetail> retryDetails) {
		this.retryDetails.clear();
		if (retryDetails != null) {
			this.retryDetails.addAll(retryDetails);
		}
	}

	public int getTotalSkips() {
		return totalSkips.get();
	}

	public void setTotalSkips(int totalSkips) {
		this.totalSkips.set(totalSkips);
	}

	public int incrementSkips() {
		return totalSkips.incrementAndGet();
	}

	public Map<String, Integer> getSkipReasonCount() {
		return skipReasonCount;
	}

	public void setSkipReasonCount(Map<String, Integer> skipReasonCount) {
		this.skipReasonCount.clear();
		if (skipReasonCount != null) {
			this.skipReasonCount.putAll(skipReasonCount);
		}
	}

	public List<SkipDetail> getSkipDetails() {
		return skipDetails;
	}

	public void setSkipDetails(List<SkipDetail> skipDetails) {
		this.skipDetails.clear();
		if (skipDetails != null) {
			this.skipDetails.addAll(skipDetails);
		}
	}

	public Map<String, PartitionMetrics> getPartitionMetrics() {
		return partitionMetrics;
	}

	public void setPartitionMetrics(Map<String, PartitionMetrics> partitionMetrics) {
		this.partitionMetrics.clear();
		if (partitionMetrics != null) {
			this.partitionMetrics.putAll(partitionMetrics);
		}
	}

	public long getTotalRecordsProcessed() {
		return totalRecordsProcessed.get();
	}

	public void setTotalRecordsProcessed(long totalRecordsProcessed) {
		this.totalRecordsProcessed.set(totalRecordsProcessed);
	}

	public long getTotalRecordsRead() {
		return totalRecordsRead.get();
	}

	public void setTotalRecordsRead(long totalRecordsRead) {
		this.totalRecordsRead.set(totalRecordsRead);
	}

	public long getTotalRecordsWritten() {
		return totalRecordsWritten.get();
	}

	public void setTotalRecordsWritten(long totalRecordsWritten) {
		this.totalRecordsWritten.set(totalRecordsWritten);
	}

	public double getAverageProcessingTime() {
		return averageProcessingTime;
	}

	public void setAverageProcessingTime(double averageProcessingTime) {
		this.averageProcessingTime = averageProcessingTime;
	}
}
