package ca.bc.gov.nrs.vdyp.batch.client.coms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ExchangeFunction;

class PresignedFileFetcherTest {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void downloadToFile_streamsResponseBodyToTargetFile(@TempDir Path tempDir) throws Exception {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
		ConvertibleClientHttpResponse response = mock(ConvertibleClientHttpResponse.class);

		URI uri = URI.create("https://example.test/object.csv");
		when(restClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(uri)).thenReturn(headersSpec);

		byte[] content = createContent();
		CloseTrackingInputStream stream = new CloseTrackingInputStream(content);
		when(response.getStatusCode()).thenReturn(HttpStatus.OK);
		when(response.getBody()).thenReturn(stream);
		stubStreamingExchange(headersSpec, response);

		Path target = tempDir.resolve("downloads").resolve("object.csv");

		new PresignedFileFetcher(restClient).downloadToFile(uri.toString(), target);

		assertArrayEquals(content, Files.readAllBytes(target));
		assertTrue(stream.isClosed());
		verify(headersSpec, never()).retrieve();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void downloadToFile_throwsIOExceptionAndDoesNotReadBodyWhenResponseIsError(@TempDir Path tempDir) throws Exception {
		RestClient restClient = mock(RestClient.class);
		RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
		RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
		ConvertibleClientHttpResponse response = mock(ConvertibleClientHttpResponse.class);

		URI uri = URI.create("https://example.test/object.csv");
		when(restClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(uri)).thenReturn(headersSpec);
		when(response.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
		when(response.getStatusText()).thenReturn("Forbidden");
		stubStreamingExchange(headersSpec, response);

		Path target = tempDir.resolve("downloads").resolve("object.csv");

		IOException exception = assertThrows(
				IOException.class, () -> new PresignedFileFetcher(restClient).downloadToFile(uri.toString(), target)
		);

		assertTrue(exception.getMessage().contains("HTTP 403 Forbidden"));
		assertFalse(Files.exists(target));
		verify(response, never()).getBody();
		verify(headersSpec, never()).retrieve();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void
			stubStreamingExchange(RestClient.RequestHeadersSpec headersSpec, ConvertibleClientHttpResponse response) {
		doAnswer(invocation -> {
			ExchangeFunction<?> exchangeFunction = invocation.getArgument(0);
			try {
				exchangeFunction.exchange(mock(HttpRequest.class), response);
				return null;
			} catch (IOException e) {
				throw new ResourceAccessException("I/O error", e);
			}
		}).when(headersSpec).exchange(any(ExchangeFunction.class), eq(true));
	}

	private static byte[] createContent() {
		byte[] content = new byte[16 * 1024];
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) (i % 251);
		}
		return content;
	}

	private static final class CloseTrackingInputStream extends ByteArrayInputStream {
		private boolean closed;

		private CloseTrackingInputStream(byte[] buf) {
			super(buf);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}

		private boolean isClosed() {
			return closed;
		}
	}
}
