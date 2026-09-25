package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupRequestModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;

@ExtendWith(MockitoExtension.class)
class StorageCleanupServiceTest {

	@Mock
	StorageCleanupRecorder recorder;
	@Mock
	@RestClient
	VDYPBatchClient batchClient;

	StorageCleanupService service;

	private static final UUID ACTING_USER_GUID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new StorageCleanupService(recorder, batchClient);
	}

	private VDYPUserModel actingUser() {
		VDYPUserModel user = new VDYPUserModel();
		user.setVdypUserGUID(ACTING_USER_GUID.toString());
		return user;
	}

	private StorageCleanupReportModel deletedReport() {
		return new StorageCleanupReportModel(
				false, 1, 100,
				List.of(new CleanupSetResultModel("guid", "vdyp-batch-guid", 100, CleanupOutcomeModel.DELETED, null)),
				List.of()
		);
	}

	@Test
	void cleanup_PassesDryRunToTheBatchService() {
		when(batchClient.cleanupStorage(any())).thenReturn(
				new StorageCleanupReportModel(true, 0, 0, List.of(), List.of())
		);

		service.cleanup(actingUser(), true);

		ArgumentCaptor<StorageCleanupRequestModel> requestCaptor = ArgumentCaptor
				.forClass(StorageCleanupRequestModel.class);
		verify(batchClient).cleanupStorage(requestCaptor.capture());
		assertEquals(true, requestCaptor.getValue().dryRun());
	}

	@Test
	void cleanup_DryRun_DoesNotRecordAnything() {
		StorageCleanupReportModel report = new StorageCleanupReportModel(
				true, 1, 100,
				List.of(new CleanupSetResultModel("guid", "vdyp-batch-guid", 100, CleanupOutcomeModel.DELETABLE, null)),
				List.of()
		);
		when(batchClient.cleanupStorage(any())).thenReturn(report);

		StorageCleanupReportModel result = service.cleanup(actingUser(), true);

		assertSame(report, result);
		verify(recorder, never()).recordCleanupRun(any(), any());
	}

	@Test
	void cleanup_ActualRun_RecordsTheRunAndReturnsTheReport() {
		StorageCleanupReportModel report = deletedReport();
		when(batchClient.cleanupStorage(any())).thenReturn(report);
		VDYPUserModel user = actingUser();

		StorageCleanupReportModel result = service.cleanup(user, false);

		assertSame(report, result);
		verify(recorder).recordCleanupRun(user, report);
	}

	@Test
	void cleanup_ActualRun_RecordingFails_StillReturnsTheReportBecauseFilesAreAlreadyDeleted() {
		StorageCleanupReportModel report = deletedReport();
		when(batchClient.cleanupStorage(any())).thenReturn(report);
		doThrow(new RuntimeException("permission denied for table storage_cleanup_run")).when(recorder)
				.recordCleanupRun(any(), any());

		StorageCleanupReportModel result = service.cleanup(actingUser(), false);

		assertSame(report, result);
	}

	@Test
	void cleanup_BatchCallFails_ThrowsAndRecordsNothing() {
		when(batchClient.cleanupStorage(any())).thenThrow(new RuntimeException("batch unreachable"));

		assertThrows(StorageCleanupException.class, () -> service.cleanup(actingUser(), false));

		verify(recorder, never()).recordCleanupRun(any(), any());
	}

}
