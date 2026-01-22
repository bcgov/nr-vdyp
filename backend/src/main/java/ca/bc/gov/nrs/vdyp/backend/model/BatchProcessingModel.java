package ca.bc.gov.nrs.vdyp.backend.model;

public record BatchProcessingModel(
		String id, //
		int errors, int warnings
) {
}
