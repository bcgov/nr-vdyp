package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.user;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.services.ProjectionService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@ExtendWith(MockitoExtension.class)
public class ResultsZipStreamTest {
	@Mock
	ProjectionService projectionService;
	@Mock
	CurrentVDYPUser currentUser;
	@Mock
	Client client;

	@Mock
	WebTarget target;
	@Mock
	Invocation.Builder builder;
	@Mock
	Response upstream;

	private ProjectionEndpoint endpoint;

	@BeforeEach
	void setUp() {
		endpoint = new ProjectionEndpoint(projectionService, currentUser, client);
	}

	@Test
	void streamResultsZip_ok_returnsStreamingOutput_and_streamsBytes_and_setsContentDisposition() throws Exception {
		UUID guid = UUID.randomUUID();
		VDYPUserModel user = user(UUID.randomUUID());
		URL url = new URL("https://example.com/out.zip");
		byte[] zipBytes = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x11, 0x22, 0x33 };

		when(currentUser.getUser()).thenReturn(user);

		var file = mock(FileMappingModel.class);
		when(projectionService.getResultSetFile(guid, user)).thenReturn(file);
		when(file.getDownloadURL()).thenReturn(url);

		when(client.target(url.toString())).thenReturn(target);
		when(target.request("application/zip")).thenReturn(builder);
		when(builder.get()).thenReturn(upstream);

		when(upstream.getStatus()).thenReturn(200);
		when(upstream.readEntity(InputStream.class)).thenReturn(new ByteArrayInputStream(zipBytes));

		Response resp = endpoint.streamResultsZip(guid, mock(HttpHeaders.class));

		assertEquals(200, resp.getStatus());
		assertEquals(
				"attachment; filename=\"vdyp_output_" + guid + ".zip\"",
				resp.getHeaderString(HttpHeaders.CONTENT_DISPOSITION)
		);

		assertTrue(resp.getEntity() instanceof StreamingOutput);
		StreamingOutput so = (StreamingOutput) resp.getEntity();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		so.write(out);

		assertArrayEquals(zipBytes, out.toByteArray());

		verify(projectionService).getResultSetFile(guid, user);
		verify(upstream).close();
	}

	@Test
	void streamResultsZip_upstream404_throwsWebApplicationException_withSameStatus_and_closesUpstream()
			throws Exception {
		UUID guid = UUID.randomUUID();
		VDYPUserModel user = user(UUID.randomUUID());
		URL url = new URL("https://example.com/missing.zip");

		when(currentUser.getUser()).thenReturn(user);

		var file = mock(FileMappingModel.class);
		when(projectionService.getResultSetFile(guid, user)).thenReturn(file);
		when(file.getDownloadURL()).thenReturn(url);

		when(client.target(url.toString())).thenReturn(target);
		when(target.request("application/zip")).thenReturn(builder);
		when(builder.get()).thenReturn(upstream);

		when(upstream.getStatus()).thenReturn(404);
		when(upstream.readEntity(String.class)).thenReturn("not found");

		Response resp = endpoint.streamResultsZip(guid, mock(HttpHeaders.class));
		StreamingOutput so = (StreamingOutput) resp.getEntity();

		WebApplicationException ex = assertThrows(
				WebApplicationException.class, () -> so.write(new ByteArrayOutputStream())
		);

		assertEquals(404, ex.getResponse().getStatus());
		assertEquals(MediaType.TEXT_PLAIN_TYPE, ex.getResponse().getMediaType());

		verify(upstream).close();
	}

	@Test
	void streamResultsZip_upstream500_throwsWebApplicationException_withSameStatus_and_closesUpstream()
			throws Exception {
		UUID guid = UUID.randomUUID();
		VDYPUserModel user = user(UUID.randomUUID());
		URL url = new URL("https://example.com/error.zip");

		when(currentUser.getUser()).thenReturn(user);

		var file = mock(FileMappingModel.class);
		when(projectionService.getResultSetFile(guid, user)).thenReturn(file);
		when(file.getDownloadURL()).thenReturn(url);

		when(client.target(url.toString())).thenReturn(target);
		when(target.request("application/zip")).thenReturn(builder);
		when(builder.get()).thenReturn(upstream);

		when(upstream.getStatus()).thenReturn(500);
		when(upstream.readEntity(String.class)).thenReturn("upstream error");

		Response resp = endpoint.streamResultsZip(guid, mock(HttpHeaders.class));
		StreamingOutput so = (StreamingOutput) resp.getEntity();

		WebApplicationException ex = assertThrows(
				WebApplicationException.class, () -> so.write(new ByteArrayOutputStream())
		);

		assertEquals(500, ex.getResponse().getStatus());

		verify(upstream).close();
	}
}
