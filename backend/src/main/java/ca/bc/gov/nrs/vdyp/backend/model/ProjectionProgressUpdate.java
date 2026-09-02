package ca.bc.gov.nrs.vdyp.backend.model;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@Schema(description = "Progress and failure information reported by the batch processing service.")
public record ProjectionProgressUpdate(
		@Schema(format = "uuid", description = "Batch job identifier.") UUID batchJobGUID,
		@Schema(minimum = "0", description = "Total number of polygons in the projection.") int totalPolygons,
		@Schema(minimum = "0", description = "Number of polygons projected so far.") int polygonsProcessed,
		@Schema(minimum = "0", description = "Number of polygon processing errors.") int projectionErrors,
		@Schema(
				minimum = "0", description = "Number of polygons that were not able to be projected."
		) int polygonsSkipped, @Schema(minimum = "0", description = "Number of active processing workers.") int workers,
		@Schema(description = "Batch failure classification code, when applicable.") String batchFailureTypeCode,
		@Schema(description = "Batch failure detail, when applicable.") String failureMessage
) {
}
