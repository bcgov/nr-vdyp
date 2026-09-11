package ca.bc.gov.nrs.vdyp.backend.data.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configured batch processing thread capacity.")
public record BatchThreadCapacityModel(
		@JsonProperty(
			"threadCapacity"
		) @Schema(minimum = "0", description = "Maximum concurrent batch workers.") int threadCapacity
) {
}
