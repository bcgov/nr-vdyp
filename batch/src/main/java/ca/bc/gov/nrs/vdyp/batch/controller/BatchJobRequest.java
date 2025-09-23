package ca.bc.gov.nrs.vdyp.batch.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

public class BatchJobRequest {

	@Min(value = 1, message = "Partition size must be at least 1")
	private Long partitionSize;

	@Min(value = 1, message = "Max retry attempts must be at least 1")
	private Integer maxRetryAttempts;

	@Min(value = 1, message = "Retry backoff period must be at least 1ms")
	private Long retryBackoffPeriod;

	@Min(value = 1, message = "Max skip count must be at least 1")
	private Integer maxSkipCount;

	@NotNull(message = "VDYP projection parameters are required")
	private Parameters parameters;

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

	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "BatchJobRequest{" + "partitionSize=" + partitionSize + ", maxRetryAttempts=" + maxRetryAttempts
				+ ", retryBackoffPeriod=" + retryBackoffPeriod + ", maxSkipCount=" + maxSkipCount 
				+ ", parameters=" + (parameters != null ? "provided" : "null") + '}';
	}
}