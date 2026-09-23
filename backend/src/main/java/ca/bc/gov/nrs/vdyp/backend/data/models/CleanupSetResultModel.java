package ca.bc.gov.nrs.vdyp.backend.data.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Schema(description = "The outcome of evaluating one PVC job folder (and its warnings file) during a storage cleanup.")
@RegisterForReflection
public record CleanupSetResultModel(
		@JsonProperty("jobGuid") @Schema(description = "The batch job GUID this folder set belongs to.") String jobGuid,
		@JsonProperty("folderName") @Schema(description = "The PVC job folder's name.") String folderName,
		@JsonProperty("bytes") @Schema(description = "The folder set's size in bytes.") long bytes,
		@JsonProperty(
			"outcome"
		) @Schema(description = "What happened, or would happen, to this folder set.") CleanupOutcomeModel outcome,
		@JsonProperty(
			"detail"
		) @Schema(description = "Why the folder set was protected, or why deletion failed.") String detail
) {
}
