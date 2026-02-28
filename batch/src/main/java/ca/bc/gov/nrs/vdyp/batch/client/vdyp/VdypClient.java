package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;

@Component
public class VdypClient {
	private final RestClient vdypBackendReliableRestClient;
	private final RestClient vdypBackendFastRestClient;

	public VdypClient(RestClient vdypBackendReliableRestClient, RestClient vdypBackendFastRestClient) {
		this.vdypBackendReliableRestClient = vdypBackendReliableRestClient;
		this.vdypBackendFastRestClient = vdypBackendFastRestClient;
	}

	public VdypProjectionDetails getProjectionDetails(String projectionGUID) {
		return vdypBackendReliableRestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/api/v8/projection/{projectionGUID}").build(projectionGUID))
				.retrieve().body(VdypProjectionDetails.class);
	}

	public List<FileMappingDetails> getFileSetFiles(String projectionGUID, String fileSetGUID) {
		return vdypBackendReliableRestClient.get()
				.uri(
						uriBuilder -> uriBuilder.path("/api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}")
								.build(projectionGUID, fileSetGUID)
				).retrieve().body(new ParameterizedTypeReference<>() {
				});
	}

	public void uploadFileToFileSet(String projectionGUID, String fileSetGUID, Path filePath) {
		Resource fileResource = new FileSystemResource(filePath);

		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", fileResource);

		vdypBackendReliableRestClient.post()
				.uri(
						b -> b.path("/api/v8/projection/{projectionGUID}/fileset/{fileSetGUID}/file")
								.build(projectionGUID, fileSetGUID)
				) //
				.contentType(MediaType.MULTIPART_FORM_DATA) //
				.body(form) //
				.retrieve() //
				.body(FileMappingDetails.class);
	}

	public void markComplete(String projectionGUID, boolean success) {
		vdypBackendReliableRestClient.post() //
				.uri(
						b -> b.path("/api/v8/projection/{projectionGUID}/complete") //
								.queryParam("success", success) //
								.build(projectionGUID)
				) //
				.retrieve() //
				.body(Void.class);
	}

	public void pushProgress(String projectionGUID, VDYPProjectionProgressUpdate payload) {
		vdypBackendFastRestClient.patch()
				.uri(b -> b.path("/api/v8/projection/{projectionGUID}/progress").build(projectionGUID))
				.contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Void.class);
	}

}
