package ca.bc.gov.nrs.vdyp.batch.client.coms;

import java.io.IOException;

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
				.uri(
						uriBuilder -> uriBuilder.path("/object/{objectId}").queryParam("download", "url")
								.build(objectId)
				).retrieve().body(String.class);

		return mapper.readValue(jsonStringUrl, String.class);
	}

}
