package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupDeletedFolderEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupRunEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupRequestModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupDeletedFolderRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupRunRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.StorageCleanupException;

@ExtendWith(MockitoExtension.class)
class StorageCleanupServiceTest {

	@Mock
	ProjectionBatchMappingRepository mappingRepository;
	@Mock
	VDYPUserRepository userRepository;
	@Mock
	StorageCleanupRunRepository runRepository;
	@Mock
	StorageCleanupDeletedFolderRepository deletedFolderRepository;
	@Mock
	@RestClient
	VDYPBatchClient batchClient;

	StorageCleanupService service;

	private static final UUID RUNNING_JOB_GUID = UUID.randomUUID();
	private static final UUID ACTING_USER_GUID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new StorageCleanupService(
				mappingRepository, userRepository, runRepository, deletedFolderRepository, batchClient
		);
	}

	private VDYPUserModel actingUser() {
		VDYPUserModel user = new VDYPUserModel();
		user.setVdypUserGUID(ACTING_USER_GUID.toString());
		return user;
	}

	@Test
	void cleanup_PassesProtectedBatchJobGuidsFromRepository() {
		when(mappingRepository.findProtectedBatchJobGuids()).thenReturn(List.of(RUNNING_JOB_GUID));
		when(batchClient.cleanupStorage(any())).thenReturn(
				new StorageCleanupReportModel(true, 0, 0, List.of(), List.of())
		);

		service.cleanup(actingUser(), true);

		ArgumentCaptor<StorageCleanupRequestModel> requestCaptor = ArgumentCaptor
				.forClass(StorageCleanupRequestModel.class);
		verify(batchClient).cleanupStorage(requestCaptor.capture());
		assertEquals(true, requestCaptor.getValue().dryRun());
		assertEquals(List.of(RUNNING_JOB_GUID.toString()), requestCaptor.getValue().protectedJobGuids());
	}

	@Test
	void cleanup_DryRun_DoesNotPersistAnything() {
		when(mappingRepository.findProtectedBatchJobGuids()).thenReturn(List.of());
		StorageCleanupReportModel report = new StorageCleanupReportModel(
				true, 1, 100,
				List.of(new CleanupSetResultModel("guid", "vdyp-batch-guid", 100, CleanupOutcomeModel.DELETABLE, null)),
				List.of()
		);
		when(batchClient.cleanupStorage(any())).thenReturn(report);

		StorageCleanupReportModel result = service.cleanup(actingUser(), true);

		assertSame(report, result);
		verify(runRepository, never()).persist(any(StorageCleanupRunEntity.class));
		verify(deletedFolderRepository, never()).persist(any(StorageCleanupDeletedFolderEntity.class));
		verify(userRepository, never()).findById(any());
	}

	@Test
	void cleanup_ActualRun_PersistsRunWithAggregatedCountsAndActingUser() {
		when(mappingRepository.findProtectedBatchJobGuids()).thenReturn(List.of());
		VDYPUserEntity userEntity = new VDYPUserEntity();
		when(userRepository.findById(ACTING_USER_GUID)).thenReturn(userEntity);

		StorageCleanupReportModel report = new StorageCleanupReportModel(
				false, 3, 500,
				List.of(
						new CleanupSetResultModel("guid-1", "vdyp-batch-guid-1", 300, CleanupOutcomeModel.DELETED, null),
						new CleanupSetResultModel("guid-2", "vdyp-batch-guid-2", 200, CleanupOutcomeModel.DELETED, null),
						new CleanupSetResultModel(
								"guid-3", "vdyp-batch-guid-3", 999, CleanupOutcomeModel.FAILED, "disk error"
						),
						new CleanupSetResultModel(
								"guid-4", "vdyp-batch-guid-4", 111, CleanupOutcomeModel.PROTECTED, "running"
						)
				),
				List.of()
		);
		when(batchClient.cleanupStorage(any())).thenReturn(report);

		service.cleanup(actingUser(), false);

		ArgumentCaptor<StorageCleanupRunEntity> runCaptor = ArgumentCaptor.forClass(StorageCleanupRunEntity.class);
		verify(runRepository).persist(runCaptor.capture());
		StorageCleanupRunEntity persistedRun = runCaptor.getValue();
		assertSame(userEntity, persistedRun.getRunByUser());
		assertEquals(2, persistedRun.getDeletedCount());
		assertEquals(1, persistedRun.getFailedCount());
		assertEquals(1, persistedRun.getProtectedCount());
		assertEquals(500, persistedRun.getBytesFreed());

		ArgumentCaptor<StorageCleanupDeletedFolderEntity> folderCaptor = ArgumentCaptor
				.forClass(StorageCleanupDeletedFolderEntity.class);
		verify(deletedFolderRepository, org.mockito.Mockito.times(2)).persist(folderCaptor.capture());
		List<String> deletedFolderNames = folderCaptor.getAllValues().stream()
				.map(StorageCleanupDeletedFolderEntity::getFolderName).toList();
		assertEquals(List.of("vdyp-batch-guid-1", "vdyp-batch-guid-2"), deletedFolderNames);
	}

	@Test
	void cleanup_BatchCallFails_ThrowsAndPersistsNothing() {
		when(mappingRepository.findProtectedBatchJobGuids()).thenReturn(List.of());
		when(batchClient.cleanupStorage(any())).thenThrow(new RuntimeException("batch unreachable"));

		assertThrows(StorageCleanupException.class, () -> service.cleanup(actingUser(), false));

		verify(runRepository, never()).persist(any(StorageCleanupRunEntity.class));
		verify(deletedFolderRepository, never()).persist(any(StorageCleanupDeletedFolderEntity.class));
	}

}
