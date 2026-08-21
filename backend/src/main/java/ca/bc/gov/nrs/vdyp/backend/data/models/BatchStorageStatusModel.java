package ca.bc.gov.nrs.vdyp.backend.data.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchStorageStatusModel(
		@JsonProperty("percentFull") double percentFull, @JsonProperty("usedBytes") long usedBytes,
		@JsonProperty("totalBytes") long totalBytes, @JsonProperty("expectedBytes") long expectedBytes,
		@JsonProperty("outOfSpec") boolean outOfSpec, @JsonProperty("thresholdPercent") int thresholdPercent
) {
}
