package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import java.io.IOException;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class VdypClient {
	private final RestClient vdypBackendRestClient;
	private final VdypProperties props;
	private final ObjectMapper mapper;

	public VdypClient(RestClient vdypBackendRestClient, VdypProperties props, ObjectMapper objectMapper) {
		this.vdypBackendRestClient = vdypBackendRestClient;
		this.props = props;
		this.mapper = objectMapper;
	}

	public VdypProjectionDetails getProjectionDetails(String projectionGUID) throws IOException {
		return vdypBackendRestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/api/v8/projection/{projectionGUID}").build(projectionGUID))
				.retrieve().body(VdypProjectionDetails.class);
	}

	public List<FileMappingDetails> getFileSetFiles(String projectionGUID, String fileSetGUID) throws IOException {
		return vdypBackendRestClient.get()
				.uri(
						uriBuilder -> uriBuilder.path("/api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}")
								.build(projectionGUID, fileSetGUID)
				).retrieve().body(new ParameterizedTypeReference<>() {
				});
	}
}
