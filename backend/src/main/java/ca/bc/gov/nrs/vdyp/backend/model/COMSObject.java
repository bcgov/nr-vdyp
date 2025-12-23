package ca.bc.gov.nrs.vdyp.backend.model;

import java.time.OffsetDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public record COMSObject(
		String id, //
		String path, //
		@JsonProperty("public") boolean isPublic, //
		boolean active, //
		String bucketId, //
		String name, //
		OffsetDateTime lasstSyncDate, //
		String createdBy, //
		OffsetDateTime createdAt, //
		String updatedBy, //
		OffsetDateTime updatedAt, //
		OffsetDateTime lastModifiedDate, //
		Set<String> permissions
) {
}
