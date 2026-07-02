package ca.bc.gov.nrs.vdyp.batch.model;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record VDYPProjectionProgressUpdate(
		String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped,
		int workers, String batchFailureTypeCode, String failureMessage
) {

	public VDYPProjectionProgressUpdate(
			String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped,
			int workers
	) {
		this(batchJobGUID, totalPolygons, polygonsProcessed, projectionErrors, polygonsSkipped, workers, null, null);
	}

	public VDYPProjectionProgressUpdate withFailure(String batchFailureTypeCode, String failureMessage) {
		return new VDYPProjectionProgressUpdate(
				batchJobGUID, totalPolygons, polygonsProcessed, projectionErrors, polygonsSkipped, workers,
				batchFailureTypeCode, failureMessage
		);
	}

}
