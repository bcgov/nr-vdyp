package ca.bc.gov.nrs.vdyp.batch.client.coms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PresignedFileFetcher {

	private final RestClient restClient = RestClient.create();

	public void downloadToFile(String url, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		Resource resource = restClient.get().uri(url).retrieve().body(Resource.class);

		try (InputStream in = resource.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
