package ca.bc.gov.nrs.vdyp.batch.model;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@RegisterReflectionForBinding
public record VDYPProjectionProgressUpdate(
		String batchJobGUID, int totalPolygons, int polygonsProcessed, int projectionErrors, int polygonsSkipped
) {

}
