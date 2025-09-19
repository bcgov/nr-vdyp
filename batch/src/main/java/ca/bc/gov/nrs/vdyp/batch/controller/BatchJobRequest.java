package ca.bc.gov.nrs.vdyp.batch.controller;

import jakarta.validation.constraints.Min;

public class BatchJobRequest {

	@Min(value = 1, message = "Partition size must be at least 1")
	private Long partitionSize;

	@Min(value = 1, message = "Max retry attempts must be at least 1")
	private Integer maxRetryAttempts;

	@Min(value = 1, message = "Retry backoff period must be at least 1ms")
	private Long retryBackoffPeriod;

	@Min(value = 1, message = "Max skip count must be at least 1")
	private Integer maxSkipCount;

	public BatchJobRequest() {
	}

	public BatchJobRequest(Long partitionSize) {
		this.partitionSize = partitionSize;
	}

	public Long getPartitionSize() {
		return partitionSize;
	}

	public void setPartitionSize(Long partitionSize) {
		this.partitionSize = partitionSize;
	}

	public Integer getMaxRetryAttempts() {
		return maxRetryAttempts;
	}

	public void setMaxRetryAttempts(Integer maxRetryAttempts) {
		this.maxRetryAttempts = maxRetryAttempts;
	}

	public Long getRetryBackoffPeriod() {
		return retryBackoffPeriod;
	}

	public void setRetryBackoffPeriod(Long retryBackoffPeriod) {
		this.retryBackoffPeriod = retryBackoffPeriod;
	}

	public Integer getMaxSkipCount() {
		return maxSkipCount;
	}

	public void setMaxSkipCount(Integer maxSkipCount) {
		this.maxSkipCount = maxSkipCount;
	}

	@Override
	public String toString() {
		return "BatchJobRequest{" + "partitionSize=" + partitionSize + ", maxRetryAttempts=" + maxRetryAttempts
				+ ", retryBackoffPeriod=" + retryBackoffPeriod + ", maxSkipCount=" + maxSkipCount + '}';
	}
}