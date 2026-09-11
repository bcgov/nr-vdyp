package ca.bc.gov.nrs.vdyp.backend.data.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Batch service persistent-storage utilization.")
public record BatchStorageStatusModel(
		@JsonProperty(
			"percentFull"
		) @Schema(minimum = "0", description = "Percentage of storage in use.") double percentFull,
		@JsonProperty(
			"usedBytes"
		) @Schema(minimum = "0", description = "Storage currently used, in bytes.") long usedBytes,
		@JsonProperty(
			"totalBytes"
		) @Schema(minimum = "0", description = "Total storage capacity, in bytes.") long totalBytes,
		@JsonProperty(
			"expectedBytes"
		) @Schema(minimum = "0", description = "Expected storage requirement, in bytes.") long expectedBytes,
		@JsonProperty("outOfSpec") @Schema(
				description = "Whether utilization appears to be correct based on usage heuristic."
		) boolean outOfSpec,
		@JsonProperty("thresholdPercent") @Schema(
				minimum = "0", maximum = "100", description = "Configured utilization threshold percentage."
		) int thresholdPercent
) {
}
