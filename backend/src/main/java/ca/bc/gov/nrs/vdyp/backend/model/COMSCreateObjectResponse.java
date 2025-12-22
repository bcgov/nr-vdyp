package ca.bc.gov.nrs.vdyp.backend.model;

public record COMSCreateObjectResponse(
		String id, String url, // presigned upload URL (if requested)
		Integer expiresIn
) {
}
