package ca.bc.gov.nrs.vdyp.backend.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.net.httpserver.HttpServer;

import ca.bc.gov.nrs.vdyp.backend.clients.S3StorageClient.StoredVersion;
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

class S3StorageClientTest {
	@Test
	void realSdkReadsVersionXmlAndDeletesNullVersionWithPathStyleAddressing() throws Exception {
		var requests = new CopyOnWriteArrayList<String>();
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
			if (exchange.getRequestMethod().equals("GET")) {
				byte[] body = ("<ListVersionsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
						+ "<Name>bucket</Name><Prefix>vdyp/fileset/</Prefix><IsTruncated>false</IsTruncated>"
						+ "<Version><Key>vdyp/fileset/empty file</Key><VersionId>null</VersionId><IsLatest>true</IsLatest>"
						+ "<Size>0</Size><LastModified>2026-09-10T00:00:00Z</LastModified></Version></ListVersionsResult>")
						.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/xml");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			} else {
				exchange.sendResponseHeaders(204, -1);
			}
			exchange.close();
		});
		server.start();
		try {
			var config = mock(COMSS3Config.class);
			when(config.endpoint()).thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
			when(config.bucket()).thenReturn("bucket");
			when(config.accessId()).thenReturn("test-access");
			when(config.secretAccessKey()).thenReturn("test-secret");
			var service = new S3StorageClient(config, "us-east-1");
			var inventory = service.list("vdyp/fileset/");
			assertEquals(List.of(new StoredVersion("vdyp/fileset/empty file", "null", 0, false)), inventory);
			service.delete(inventory);
			assertTrue(requests.get(0).startsWith("GET /bucket?"));
			assertTrue(requests.get(1).startsWith("DELETE /bucket/vdyp/fileset/empty%20file?"));
			assertTrue(requests.get(1).contains("versionId=null"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void paginatesWithBothMarkersAndIncludesNullVersionsAndDeleteMarkers() {
		var client = mock(S3Client.class);
		when(client.listObjectVersions(any(ListObjectVersionsRequest.class))).thenReturn(
				ListObjectVersionsResponse.builder().isTruncated(true).nextKeyMarker("file").nextVersionIdMarker("v1")
						.versions(ObjectVersion.builder().key("file").versionId("v1").size(100L).build()).build(),
				ListObjectVersionsResponse.builder().isTruncated(false)
						.versions(ObjectVersion.builder().key("empty").size(0L).build())
						.deleteMarkers(DeleteMarkerEntry.builder().key("deleted").versionId("marker").build()).build()
		);
		var versions = S3StorageClient.list(client, "bucket", "vdyp/fileset/");
		assertEquals(3, versions.size());
		assertEquals("null", versions.get(1).versionId());
		assertTrue(versions.get(2).deleteMarker());
		var requests = ArgumentCaptor.forClass(ListObjectVersionsRequest.class);
		verify(client, times(2)).listObjectVersions(requests.capture());
		assertEquals("file", requests.getAllValues().get(1).keyMarker());
		assertEquals("v1", requests.getAllValues().get(1).versionIdMarker());
		assertEquals("vdyp/fileset/", requests.getValue().prefix());
	}

	@Test
	void repeatedPaginationFailsInsteadOfReturningPartialInventory() {
		var client = mock(S3Client.class);
		when(client.listObjectVersions(any(ListObjectVersionsRequest.class))).thenReturn(
				ListObjectVersionsResponse.builder().isTruncated(true).nextKeyMarker("same").nextVersionIdMarker("same")
						.build()
		);
		assertThrows(IllegalStateException.class, () -> S3StorageClient.list(client, "bucket", "prefix"));
	}

	@Test
	void deleteUsesExactVersionIdsIncludingNullAndClosesClient() {
		var client = mock(S3Client.class);
		var config = mock(COMSS3Config.class);
		when(config.bucket()).thenReturn("bucket");
		var service = new S3StorageClient(config, "us-east-1") {
			@Override
			S3Client openClient() {
				return client;
			}
		};
		service.delete(
				List.of(
						new StoredVersion("file", "v1", 10, false), new StoredVersion("file", "null", 0, false),
						new StoredVersion("file", "marker", 0, true)
				)
		);
		var requests = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(client, times(3)).deleteObject(requests.capture());
		assertEquals(
				List.of("v1", "null", "marker"),
				requests.getAllValues().stream().map(DeleteObjectRequest::versionId).toList()
		);
		assertTrue(
				requests.getAllValues().stream().allMatch(r -> r.bucket().equals("bucket") && r.key().equals("file"))
		);
		verify(client).close();
	}
}
