package ca.bc.gov.nrs.vdyp.batch.client.coms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ComsClientTest {
	@Mock
	RestClient comsRestClient;
	@SuppressWarnings("rawtypes")
	@Mock
	RestClient.RequestHeadersUriSpec uriSpec;

	@SuppressWarnings("rawtypes")
	@Mock
	RestClient.RequestHeadersSpec headersSpec;

	@Mock
	RestClient.ResponseSpec responseSpec;

	ComsClient comsClient;

	@BeforeEach
	void setUp() {
		comsClient = new ComsClient(comsRestClient, new ObjectMapper());
	}

	@SuppressWarnings("unchecked")
	@Test
	void test_GetPresignedUrl_parsesJSONStringCorrectly() throws IOException {
		String objectId = "abc123";
		String expectedUrl = "https://example.com/presigned";
		String jsonEncodedString = "\"https://example.com/presigned\"";

		when(comsRestClient.get()).thenReturn(uriSpec);

		// IMPORTANT: disambiguate the uri(Function<UriBuilder, URI>) overload
		when(uriSpec.uri(anyUriFunction())).thenReturn(headersSpec);

		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(String.class)).thenReturn(jsonEncodedString);

		// when
		String result = comsClient.getPresignedUrl(objectId);

		// then
		assertEquals(expectedUrl, result);
	}

	@SuppressWarnings("unchecked")
	private static Function<UriBuilder, URI> anyUriFunction() {
		return (Function<UriBuilder, URI>) any(Function.class);
	}

	private static final String BASE_URL = "http://localhost";

	@Test
	void updateObject_setsUriAndHeaders_andCallsBodyString(@TempDir Path tempDir) throws Exception {
		// Arrange
		RestClient restClient = mock(RestClient.class);
		ComsClient client = new ComsClient(restClient, new ObjectMapper());

		String objectId = "obj-123";
		Path file = tempDir.resolve("upload.txt");
		Files.writeString(file, "hello");

		RestClient.RequestBodyUriSpec mockUriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
		RestClient.RequestBodySpec afterBodySpec = mock(RestClient.RequestBodySpec.class); // <- key
		RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

		when(restClient.put()).thenReturn(mockUriSpec);
		when(mockUriSpec.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(bodySpec);
		when(bodySpec.headers(any())).thenReturn(bodySpec);

		// IMPORTANT: whatever overload body(...) resolves to, return a different spec
		doReturn(afterBodySpec).when(bodySpec).body(any());

		when(afterBodySpec.retrieve()).thenReturn(mockResponseSpec);
		when(mockResponseSpec.onStatus(any(), any())).thenReturn(mockResponseSpec);
		when(mockResponseSpec.body(String.class)).thenReturn("ok");

		// Act
		client.updateObject(objectId, file);

		// Assert URI
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
		verify(mockUriSpec).uri(uriCaptor.capture());

		URI built = uriCaptor.getValue().apply(new DefaultUriBuilderFactory(BASE_URL).builder());
		assertEquals(BASE_URL + "/object/" + objectId, built.toString());

		// Assert headers
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
		verify(bodySpec).headers(headersCaptor.capture());

		HttpHeaders headers = new HttpHeaders();
		headersCaptor.getValue().accept(headers);

		assertEquals(String.valueOf(Files.size(file)), headers.getFirst(HttpHeaders.CONTENT_LENGTH));
		assertEquals(Files.probeContentType(file), headers.getFirst(HttpHeaders.CONTENT_TYPE));

		// Assert terminal call
		verify(mockResponseSpec).body(String.class);
	}
}
