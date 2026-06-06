package ca.bc.gov.nrs.vdyp.batch.client.coms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PresignedFileFetcher {
	private final RestClient restClient;

	@Autowired
	public PresignedFileFetcher() {
		this(RestClient.create());
	}


	PresignedFileFetcher(RestClient restClient) {
		this.restClient = Objects.requireNonNull(restClient, "restClient");
	}

	public void downloadToFile(String url, Path target) throws IOException {
		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		URI uri = URI.create(url);

		try {
			restClient.get().uri(uri).exchange((request, response) -> {
				HttpStatusCode statusCode = response.getStatusCode();
				if (statusCode.isError()) {
					throw new IOException(
							"COMS download failed: HTTP " + statusCode.value() + " " + response.getStatusText()
					);
				}

				try (InputStream in = response.getBody()) {
					Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
				}

				return null;
			}, true);
		} catch (RestClientException e) {
			if (e.getCause() instanceof IOException ioException) {
				throw ioException;
			}

			throw new IOException("Failed to download presigned COMS object: " + uri, e);
		}
	}
}
