package ca.bc.gov.nrs.vdyp.backend.model;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ProjectionProgressUpdate(
		UUID batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped
) {
}
