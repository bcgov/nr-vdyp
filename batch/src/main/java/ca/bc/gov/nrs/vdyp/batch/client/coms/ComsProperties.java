package ca.bc.gov.nrs.vdyp.batch.client.coms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coms")
public record ComsProperties(String baseUrl, BasicAuth basicAuth, ObjectProps object, S3Access s3access) {
	public record BasicAuth(String username, String password) {
	}

	public record ObjectProps(int presignExpiresIn) {
	}

	public record S3Access(String accessKeyId, String secretAccessKey, String bucket, String endpoint) {
	}
}
