package ca.bc.gov.nrs.vdyp.backend.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ProjectionProgressUpdate(
		int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped
) {
}
