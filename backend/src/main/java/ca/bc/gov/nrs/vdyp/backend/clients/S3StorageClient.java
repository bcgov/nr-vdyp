package ca.bc.gov.nrs.vdyp.backend.clients;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;

/** Direct S3 access, including versions and delete markers absent from COMS. */
@ApplicationScoped
public class S3StorageClient {

	private final COMSS3Config config;
	private final String region;

	public S3StorageClient(
			COMSS3Config config, @ConfigProperty(name = "vdyp.coms.s3.region", defaultValue = "us-east-1") String region
	) {
		this.config = config;
		this.region = region;
	}

	public record StoredVersion(String key, String versionId, long size, boolean deleteMarker) {
	}

	S3Client openClient() {
		return S3Client.builder().endpointOverride(URI.create(config.endpoint())).region(Region.of(region))
				.credentialsProvider(
						StaticCredentialsProvider
								.create(AwsBasicCredentials.create(config.accessId(), config.secretAccessKey()))
				).forcePathStyle(true)
				.httpClientBuilder(
						UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(30))
								.socketTimeout(Duration.ofMinutes(4))
				).build();
	}

	public List<StoredVersion> list(String prefix) {
		try (var client = openClient()) {
			return list(client, config.bucket(), prefix);
		}
	}

	static List<StoredVersion> list(S3Client client, String bucket, String prefix) {
		var result = new ArrayList<StoredVersion>();
		var seen = new HashSet<List<String>>();
		String keyMarker = null;
		String versionMarker = null;
		while (true) {
			var page = client.listObjectVersions(
					ListObjectVersionsRequest.builder().bucket(bucket).prefix(prefix).keyMarker(keyMarker)
							.versionIdMarker(versionMarker).maxKeys(1000).build()
			);
			page.versions()
					.forEach(v -> result.add(new StoredVersion(v.key(), versionId(v.versionId()), v.size(), false)));
			page.deleteMarkers()
					.forEach(v -> result.add(new StoredVersion(v.key(), versionId(v.versionId()), 0, true)));
			if (!Boolean.TRUE.equals(page.isTruncated())) {
				return List.copyOf(result);
			}
			keyMarker = page.nextKeyMarker();
			versionMarker = page.nextVersionIdMarker();
			if (keyMarker == null || keyMarker.isEmpty()
					|| !seen.add(List.of(keyMarker, versionMarker == null ? "" : versionMarker))) {
				throw new IllegalStateException("S3 returned invalid or repeated pagination markers");
			}
		}
	}

	private static String versionId(String versionId) {
		// Explicit null-version deletion avoids adding a delete marker in a suspended bucket.
		if (versionId != null && versionId.isBlank()) {
			throw new IllegalStateException("S3 returned an empty version ID");
		}
		return versionId == null ? "null" : versionId;
	}

	public void delete(List<StoredVersion> versions) {
		try (var client = openClient()) {
			for (var version : versions) {
				client.deleteObject(
						DeleteObjectRequest.builder().bucket(config.bucket()).key(version.key())
								.versionId(version.versionId()).build()
				);
			}
		}
	}
}
