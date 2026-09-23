package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "The result of scanning (and optionally deleting) leftover PVC job folders.")
public record StorageCleanupReportModel(
		@JsonProperty("dryRun") @Schema(description = "Whether this run only previewed candidates.") boolean dryRun,
		@JsonProperty("scanned") @Schema(description = "Number of PVC job folder sets evaluated.") int scanned,
		@JsonProperty("totalBytes") @Schema(
				description = "Total size in bytes of deletable (preview) or deleted (actual run) folder sets."
		) long totalBytes,
		@JsonProperty(
			"sets"
		) @Schema(description = "The outcome for every evaluated folder set.") List<CleanupSetResultModel> sets,
		@JsonProperty("skippedNames") @Schema(
				description = "PVC root entries that did not match the expected job folder naming and were left alone."
		) List<String> skippedNames
) {
}
