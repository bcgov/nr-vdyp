package ca.bc.gov.nrs.vdyp.batch.client.coms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ComsClientTest {
	@Mock
	RestClient comsRestClient;
	@Mock
	ComsProperties props;

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
		comsClient = new ComsClient(comsRestClient, props, new ObjectMapper());
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
}
