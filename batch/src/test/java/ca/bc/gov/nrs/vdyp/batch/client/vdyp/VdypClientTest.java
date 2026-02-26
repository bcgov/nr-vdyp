package ca.bc.gov.nrs.vdyp.batch.client.vdyp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

@ExtendWith(MockitoExtension.class)
class VdypClientTest {

	private static final String BASE_URL = "http://localhost";

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private RestClient restClient;
	private RestClient fastRestClient;

	private VdypClient client;

	@BeforeEach
	void setUp() {
		client = new VdypClient(restClient, fastRestClient);
	}

	@Test
	void getProjectionDetails_buildsExpectedUri_andReturnsBody() {
		// Arrange
		String projectionGuid = "proj-123";
		VdypProjectionDetails expected = mock(VdypProjectionDetails.class);

		when(
				restClient.get().uri(ArgumentMatchers.<Function<UriBuilder, URI>>any()).retrieve()
						.body(VdypProjectionDetails.class)
		).thenReturn(expected);

		// Act
		VdypProjectionDetails actual = client.getProjectionDetails(projectionGuid);

		// Assert (return value)
		assertSame(expected, actual);

		// Assert (URI lambda)
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Function<UriBuilder, URI>> uriFnCaptor = ArgumentCaptor.forClass(Function.class);

		verify(restClient.get(), times(2)).uri(uriFnCaptor.capture());

		URI built = uriFnCaptor.getValue().apply(new DefaultUriBuilderFactory(BASE_URL).builder());
		assertEquals(BASE_URL + "/api/v8/projection/" + projectionGuid, built.toString());
	}

	@Test
	void getFileSetFiles_buildsExpectedUri_andReturnsList() {
		// Arrange
		String projectionGuid = "proj-123";
		String fileSetGuid = "fs-456";
		List<FileMappingDetails> expected = List.of(mock(FileMappingDetails.class));

		when(
				restClient.get().uri(ArgumentMatchers.<Function<UriBuilder, URI>>any()).retrieve()
						.body(ArgumentMatchers.<ParameterizedTypeReference<List<FileMappingDetails>>>any())
		).thenReturn(expected);

		// Act
		List<FileMappingDetails> actual = client.getFileSetFiles(projectionGuid, fileSetGuid);

		// Assert (return value)
		assertSame(expected, actual);

		// Assert (URI lambda)
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Function<UriBuilder, URI>> uriFnCaptor = ArgumentCaptor.forClass(Function.class);

		verify(restClient.get(), times(2)).uri(uriFnCaptor.capture());

		URI built = uriFnCaptor.getValue().apply(new DefaultUriBuilderFactory(BASE_URL).builder());
		assertEquals(BASE_URL + "/api/v8/projection/" + projectionGuid + "/fileset/" + fileSetGuid, built.toString());
	}

	@Test
	void uploadFileToFileSet_buildsUri_andSendsMultipart(@TempDir Path tempDir) throws Exception {
		String projectionGuid = "proj-123";
		String fileSetGuid = "fs-456";

		Path file = tempDir.resolve("upload.txt");
		Files.writeString(file, "hello");

		// mocks for the chain
		var req = mock(RestClient.RequestBodyUriSpec.class);
		var resp = mock(RestClient.ResponseSpec.class);

		when(restClient.post()).thenReturn(req);
		when(req.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(req);
		when(req.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(req);
		doReturn(req).when(req).body(any(MultiValueMap.class));
		when(req.retrieve()).thenReturn(resp);
		when(resp.body(FileMappingDetails.class)).thenReturn(mock(FileMappingDetails.class));

		// act
		client.uploadFileToFileSet(projectionGuid, fileSetGuid, file);

		// assert uri
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
		verify(req).uri(uriCaptor.capture());

		URI built = uriCaptor.getValue().apply(new DefaultUriBuilderFactory(BASE_URL).builder());
		assertEquals(
				BASE_URL + "/api/v8/projection/" + projectionGuid + "/fileset/" + fileSetGuid + "/file",
				built.toString()
		);

		// assert multipart contains file part
		@SuppressWarnings("unchecked")
		ArgumentCaptor<MultiValueMap<String, Object>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
		verify(req).body(formCaptor.capture());

		Object part = formCaptor.getValue().getFirst("file");
		assertInstanceOf(FileSystemResource.class, part);
	}

}
