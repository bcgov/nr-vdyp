package ca.bc.gov.nrs.vdyp.batch.model;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import ca.bc.gov.nrs.vdyp.ecore.projection.AlertCounts;

@RegisterReflectionForBinding
public record VDYPProjectionProgressUpdate(
		String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped,
		int workers, AlertCounts alertCounts, String batchFailureTypeCode, String failureMessage
) {

	public VDYPProjectionProgressUpdate(
			String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped,
			int workers, AlertCounts alertCounts
	) {
		this(
				batchJobGUID, totalPolygons, polygonsProcessed, projectionErrors, polygonsSkipped, workers, alertCounts,
				null, null
		);
	}

	public VDYPProjectionProgressUpdate(
			String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped,
			int workers
	) {
		this(batchJobGUID, totalPolygons, polygonsProcessed, projectionErrors, polygonsSkipped, workers, null);
	}

	public VDYPProjectionProgressUpdate withFailure(String batchFailureTypeCode, String failureMessage) {
		return new VDYPProjectionProgressUpdate(
				batchJobGUID, totalPolygons, polygonsProcessed, projectionErrors, polygonsSkipped, workers, alertCounts,
				batchFailureTypeCode, failureMessage
		);
	}

}
