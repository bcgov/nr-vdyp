package ca.bc.gov.nrs.vdyp.backend.data.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Schema(description = "Request to scan the batch service's PVC for leftover job folders and, optionally, delete them.")
@RegisterForReflection
public record StorageCleanupRequestModel(
		@JsonProperty(
			"dryRun"
		) @Schema(description = "When true, candidates are evaluated but nothing is deleted.") boolean dryRun
) {
}
