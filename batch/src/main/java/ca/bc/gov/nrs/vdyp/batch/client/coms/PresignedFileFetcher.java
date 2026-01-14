package ca.bc.gov.nrs.vdyp.batch.client.coms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PresignedFileFetcher {
	private static final Logger log = LoggerFactory.getLogger(PresignedFileFetcher.class);

	private final RestClient restClient = RestClient.create();

	public void downloadToFile(String url, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		URI uri = URI.create(url);
		Resource resource = restClient.get().uri(uri).retrieve().body(Resource.class);

		try (InputStream in = resource.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
