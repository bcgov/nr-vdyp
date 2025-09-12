package ca.bc.gov.nrs.vdyp.batch.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batch job execution metrics and detailed error information for batch processing.
 */
public class BatchMetrics {

	private Long jobExecutionId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String status;

	// Retry metrics
	private int totalRetryAttempts = 0;
	private int successfulRetries = 0;
	private int failedRetries = 0;
	private List<RetryDetail> retryDetails = new ArrayList<>();

	// Skip metrics
	private int totalSkips = 0;
	private Map<String, Integer> skipReasonCount = new ConcurrentHashMap<>();
	private List<SkipDetail> skipDetails = new ArrayList<>();

	// Partition metrics
	private Map<String, PartitionMetrics> partitionMetrics = new ConcurrentHashMap<>();

	// Processing statistics
	private long totalRecordsProcessed = 0;
	private long totalRecordsRead = 0;
	private long totalRecordsWritten = 0;
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
			Long recordId, String recordData, int attemptNumber, String errorType, String errorMessage,
			LocalDateTime timestamp, boolean successful, String partitionName
	) {
		public RetryDetail(
				Long recordId, String recordData, int attemptNumber, String errorType, String errorMessage,
				boolean successful, String partitionName
		) {
			this(
					recordId, recordData, attemptNumber, errorType, errorMessage, LocalDateTime.now(), successful,
					partitionName
			);
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
		private int recordsProcessed = 0;
		private int recordsRead = 0;
		private int recordsWritten = 0;
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

		public int getRecordsProcessed() {
			return recordsProcessed;
		}

		public void setRecordsProcessed(int recordsProcessed) {
			this.recordsProcessed = recordsProcessed;
		}

		public int getRecordsRead() {
			return recordsRead;
		}

		public void setRecordsRead(int recordsRead) {
			this.recordsRead = recordsRead;
		}

		public int getRecordsWritten() {
			return recordsWritten;
		}

		public void setRecordsWritten(int recordsWritten) {
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
		return totalRetryAttempts;
	}

	public void setTotalRetryAttempts(int totalRetryAttempts) {
		this.totalRetryAttempts = totalRetryAttempts;
	}

	public int getSuccessfulRetries() {
		return successfulRetries;
	}

	public void setSuccessfulRetries(int successfulRetries) {
		this.successfulRetries = successfulRetries;
	}

	public int getFailedRetries() {
		return failedRetries;
	}

	public void setFailedRetries(int failedRetries) {
		this.failedRetries = failedRetries;
	}

	public List<RetryDetail> getRetryDetails() {
		return retryDetails;
	}

	public void setRetryDetails(List<RetryDetail> retryDetails) {
		this.retryDetails = retryDetails;
	}

	public int getTotalSkips() {
		return totalSkips;
	}

	public void setTotalSkips(int totalSkips) {
		this.totalSkips = totalSkips;
	}

	public Map<String, Integer> getSkipReasonCount() {
		return skipReasonCount;
	}

	public void setSkipReasonCount(Map<String, Integer> skipReasonCount) {
		this.skipReasonCount = skipReasonCount;
	}

	public List<SkipDetail> getSkipDetails() {
		return skipDetails;
	}

	public void setSkipDetails(List<SkipDetail> skipDetails) {
		this.skipDetails = skipDetails;
	}

	public Map<String, PartitionMetrics> getPartitionMetrics() {
		return partitionMetrics;
	}

	public void setPartitionMetrics(Map<String, PartitionMetrics> partitionMetrics) {
		this.partitionMetrics = partitionMetrics;
	}

	public long getTotalRecordsProcessed() {
		return totalRecordsProcessed;
	}

	public void setTotalRecordsProcessed(long totalRecordsProcessed) {
		this.totalRecordsProcessed = totalRecordsProcessed;
	}

	public long getTotalRecordsRead() {
		return totalRecordsRead;
	}

	public void setTotalRecordsRead(long totalRecordsRead) {
		this.totalRecordsRead = totalRecordsRead;
	}

	public long getTotalRecordsWritten() {
		return totalRecordsWritten;
	}

	public void setTotalRecordsWritten(long totalRecordsWritten) {
		this.totalRecordsWritten = totalRecordsWritten;
	}

	public double getAverageProcessingTime() {
		return averageProcessingTime;
	}

	public void setAverageProcessingTime(double averageProcessingTime) {
		this.averageProcessingTime = averageProcessingTime;
	}
}
