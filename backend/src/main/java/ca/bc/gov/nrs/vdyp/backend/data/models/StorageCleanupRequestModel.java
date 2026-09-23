package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Request to scan the batch service's PVC for leftover job folders and, optionally, delete them.")
public record StorageCleanupRequestModel(
		@JsonProperty(
			"dryRun"
		) @Schema(description = "When true, candidates are evaluated but nothing is deleted.") boolean dryRun,
		@JsonProperty("protectedJobGuids") @Schema(
				description = "Batch job GUIDs that must not be deleted because their projection is Running or Stuck."
		) List<String> protectedJobGuids
) {
}
