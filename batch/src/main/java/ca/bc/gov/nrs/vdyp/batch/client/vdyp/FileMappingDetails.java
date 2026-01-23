package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileMappingDetails(
		@JsonProperty("fileMappingGUID") String fileMappingGuid, @JsonProperty("comsObjectGUID") String comsObjectGuid
) {
}
