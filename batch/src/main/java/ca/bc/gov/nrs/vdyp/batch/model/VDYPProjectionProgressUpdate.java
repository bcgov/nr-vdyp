package ca.bc.gov.nrs.vdyp.batch.model;

public record VDYPProjectionProgressUpdate(
		int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped
) {

	@Override
	public String toString() {
		return String.format(
				"VDYPProjectionProgressUpdate [totalPolygons=%d, polygonsProcessed=%d]", totalPolygons,
				polygonsProcessed
		);
	}
}
