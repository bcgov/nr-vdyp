package ca.bc.gov.nrs.vdyp.backend.data.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchJobModel(
		@JsonProperty("jobGuid") String id, //
		@JsonProperty("errors") int errors, //
		@JsonProperty("warnings") int warnings //
		// TODO polygons proccessed
		// total polygons
) {
}
