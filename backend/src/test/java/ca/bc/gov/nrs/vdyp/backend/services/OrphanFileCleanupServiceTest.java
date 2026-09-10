package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.clients.S3StorageClient;
import ca.bc.gov.nrs.vdyp.backend.clients.S3StorageClient.StoredVersion;
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionFileSetRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.OrphanFileCleanupException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import jakarta.ws.rs.core.Response;

class OrphanFileCleanupServiceTest {
	final COMSClient coms = mock(COMSClient.class);
	final S3StorageClient s3 = mock(S3StorageClient.class);
	final ProjectionFileSetRepository repository = mock(ProjectionFileSetRepository.class);
	final COMSS3Config config = mock(COMSS3Config.class);
	final UUID id = UUID.randomUUID();
	final String prefix = "vdyp/fileset/" + id;
	final StoredVersion file = new StoredVersion(prefix + "/file.csv", "null", 100, false);
	OrphanFileCleanupService service;

	@BeforeEach
	void setup() {
		when(config.bucket()).thenReturn("storage");
		when(config.endpoint()).thenReturn("http://s3");
		when(s3.list("vdyp/fileset/")).thenReturn(List.of(file));
		service = new OrphanFileCleanupService(coms, config, repository, s3, true);
	}

	COMSBucket registration(String key, String bucketId) {
		return new COMSBucket("storage", null, bucketId, "http://s3", key, null);
	}

	@Test
	void previewFindsS3OnlyFilesAndCountsVersionsAndZeroBytes() {
		when(s3.list("vdyp/fileset/")).thenReturn(
				List.of(
						file, new StoredVersion(file.key(), "older", 200, false),
						new StoredVersion(prefix + "/empty", "null", 0, false),
						new StoredVersion(prefix + "/deleted", "marker", 0, true)
				)
		);
		var result = service.cleanup(true).folders().get(0);
		assertEquals("ORPHAN", result.outcome());
		assertEquals(List.of(), result.bucketIds());
		assertEquals(3, result.objects());
		assertEquals(4, result.versions());
		assertEquals(300, result.bytes());
		verify(s3, never()).delete(anyList());
		verify(coms, never()).deleteBucket(anyString(), anyBoolean());
	}

	@Test
	void deletesS3OnlyOrphanWithoutCreatingCOMSRegistration() {
		assertEquals("DELETED", service.cleanup(false).folders().get(0).outcome());
		verify(s3).delete(List.of(file));
		verify(coms, never()).createBucket(any());
		verify(coms, never()).deleteBucket(anyString(), anyBoolean());
	}

	@Test
	void protectsDatabaseFileSetWithoutCOMS() {
		when(repository.count("projectionFileSetGUID", id)).thenReturn(1L);
		assertEquals("REFERENCED", service.cleanup(false).folders().get(0).outcome());
		verify(s3, never()).delete(anyList());
	}

	@Test
	void protectsMismatchedCachedReferenceIncludingNestedRegistration() {
		when(coms.searchForBucket(null, null, null, null))
				.thenReturn(List.of(registration(prefix + "/nested", "nested")));
		when(repository.count("comsBucketId", "nested")).thenReturn(1L);
		assertEquals("REFERENCED", service.cleanup(false).folders().get(0).outcome());
		verify(s3, never()).delete(anyList());
	}

	@Test
	void deletesS3ThenVerifiesThenRemovesChildrenBeforeParent() {
		when(coms.searchForBucket(null, null, null, null))
				.thenReturn(List.of(registration(prefix, "parent"), registration(prefix + "/nested", "child")));
		when(coms.deleteBucket(anyString(), eq(false))).thenAnswer(i -> Response.noContent().build());
		assertEquals("DELETED", service.cleanup(false).folders().get(0).outcome());
		var order = inOrder(s3, coms);
		order.verify(s3).delete(List.of(file));
		order.verify(s3).list(prefix);
		order.verify(coms).deleteBucket("child", false);
		order.verify(coms).deleteBucket("parent", false);
	}

	@Test
	void s3FailureRetainsCOMS() {
		when(coms.searchForBucket(null, null, null, null)).thenReturn(List.of(registration(prefix, "parent")));
		doThrow(new IllegalStateException("denied")).when(s3).delete(anyList());
		assertEquals("FAILED", service.cleanup(false).folders().get(0).outcome());
		verify(coms, never()).deleteBucket(anyString(), anyBoolean());
	}

	@Test
	void remainingVersionsRetainCOMS() {
		when(coms.searchForBucket(null, null, null, null)).thenReturn(List.of(registration(prefix, "parent")));
		when(s3.list(prefix)).thenReturn(List.of(file));
		assertEquals("FAILED", service.cleanup(false).folders().get(0).outcome());
		verify(coms, never()).deleteBucket(anyString(), anyBoolean());
	}

	@Test
	void allDatabaseChecksFinishBeforeDeletion() {
		UUID other = UUID.randomUUID();
		when(s3.list("vdyp/fileset/"))
				.thenReturn(List.of(file, new StoredVersion("vdyp/fileset/" + other + "/file", "null", 1, false)));
		when(repository.count("projectionFileSetGUID", other)).thenThrow(new IllegalStateException("db unavailable"));
		assertThrows(IllegalStateException.class, () -> service.cleanup(false));
		verify(s3, never()).delete(anyList());
	}

	@Test
	void referenceAddedAfterInventoryPreventsDeletion() {
		when(repository.count("projectionFileSetGUID", id)).thenReturn(0L, 1L);
		assertEquals("REFERENCED", service.cleanup(false).folders().get(0).outcome());
		verify(s3, never()).delete(anyList());
	}

	@Test
	void malformedKeysAndOtherStorageAreUntouched() {
		when(s3.list("vdyp/fileset/"))
				.thenReturn(List.of(new StoredVersion(prefix + "-suffix/file", "null", 1, false)));
		when(coms.searchForBucket(null, null, null, null)).thenReturn(
				List.of(
						new COMSBucket("other", null, "other", "http://s3", prefix, null),
						registration("vdyp/fileset", "parent")
				)
		);
		var report = service.cleanup(false);
		assertTrue(report.folders().isEmpty());
		assertEquals(List.of(prefix + "-suffix/file"), report.skippedKeys());
		verify(s3, never()).delete(anyList());
	}

	@Test
	void emptyCOMSOnlyOrphanCanBeRetriedAfterRegistrationFailure() {
		when(s3.list("vdyp/fileset/")).thenReturn(List.of());
		when(coms.searchForBucket(null, null, null, null)).thenReturn(List.of(registration(prefix, "parent")));
		when(coms.deleteBucket("parent", false))
				.thenReturn(Response.serverError().build(), Response.noContent().build());
		assertEquals("FAILED", service.cleanup(false).folders().get(0).outcome());
		assertEquals("DELETED", service.cleanup(false).folders().get(0).outcome());
	}

	@Test
	void inventoryFailurePreventsDeletion() {
		when(s3.list("vdyp/fileset/")).thenThrow(new IllegalStateException("listing failed"));
		assertThrows(IllegalStateException.class, () -> service.cleanup(false));
		verifyNoInteractions(coms, repository);
		verify(s3, never()).delete(anyList());
	}

	@Test
	void disabledDeletionStillAllowsPreview() {
		service = new OrphanFileCleanupService(coms, config, repository, s3, false);
		assertThrows(OrphanFileCleanupException.class, () -> service.cleanup(false));
		assertEquals("ORPHAN", service.cleanup(true).folders().get(0).outcome());
	}
}
