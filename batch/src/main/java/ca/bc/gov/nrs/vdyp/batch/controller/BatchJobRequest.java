package ca.bc.gov.nrs.vdyp.batch.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

public class BatchJobRequest {

	@Min(value = 1, message = "Partition size must be at least 1")
	private Long partitionSize;

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


	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "BatchJobRequest{" + "partitionSize=" + partitionSize
				+ ", parameters=" + (parameters != null ? "provided" : "null") + '}';
	}
}