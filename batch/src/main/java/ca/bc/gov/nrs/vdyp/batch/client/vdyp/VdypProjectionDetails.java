package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VdypProjectionDetails(
		@JsonProperty("projectionGUID") String projectionGuid, //
		VdypProjectionFileSet polygonFileSet, //
		VdypProjectionFileSet layerFileSet, //
		VdypProjectionFileSet resultFileSet //
) {
	public record VdypProjectionFileSet(@JsonProperty("projectionFileSetGUID") String guid) {
	}
}
