package ca.bc.gov.nrs.vdyp.backend.model;

public record COMSObjectVersion(
		String id,
		String s3VersionId,
		String objectId,
		boolean deleteMarker
) {
}
