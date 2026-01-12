package ca.bc.gov.nrs.vdyp.backend.model;

public record COMSCreateBucketRequest(
		String accessKeyId, //
		Boolean active, //
		String bucket, // default bucket everytime
		String bucketName, //
		String endpoint, String secretAccessKey, //
		String key
) {
}
