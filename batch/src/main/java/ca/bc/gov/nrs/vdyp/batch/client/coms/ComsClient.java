package ca.bc.gov.nrs.vdyp.batch.client.coms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ComsClient {
	private final RestClient comsRestClient;
	private final ComsProperties props;
	private final ObjectMapper mapper;

	public ComsClient(RestClient comsRestClient, ComsProperties props, ObjectMapper objectMapper) {
		this.comsRestClient = comsRestClient;
		this.props = props;
		this.mapper = objectMapper;
	}

	public String getPresignedUrl(String objectId) throws IOException {
		String jsonStringUrl = comsRestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/object/{objectId}").queryParam("download", "url").build(objectId))
				.retrieve().body(String.class);

		return mapper.readValue(jsonStringUrl, String.class);
	}

	public void updateObject(String objectId, Path filePath) throws IOException {

		long contentLength = Files.size(filePath);

		String resolvedContentType = Files.probeContentType(filePath);

		comsRestClient.put() //
				.uri(uriBuilder -> uriBuilder.path("/object/{objectId}").build(objectId)) //
				.headers(headers -> {
					headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
					headers.set(HttpHeaders.CONTENT_TYPE, resolvedContentType);
				}) //
				.body(outputStream -> Files.copy(filePath, outputStream)) //
				.retrieve().onStatus(
						HttpStatusCode::isError, //
						(req, res) -> {
							throw new IOException("COMS updateObject failed: HTTP " + res.getStatusCode());
						}
				) //
				.body(String.class);
	}
}
