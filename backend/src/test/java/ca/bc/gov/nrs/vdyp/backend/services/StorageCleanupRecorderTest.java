package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupDeletedFolderEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.StorageCleanupRunEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupOutcomeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.CleanupSetResultModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.StorageCleanupReportModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupDeletedFolderRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.StorageCleanupRunRepository;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.VDYPUserRepository;

@ExtendWith(MockitoExtension.class)
class StorageCleanupRecorderTest {

	@Mock
	VDYPUserRepository userRepository;
	@Mock
	StorageCleanupRunRepository runRepository;
	@Mock
	StorageCleanupDeletedFolderRepository deletedFolderRepository;

	StorageCleanupRecorder recorder;

	private static final UUID ACTING_USER_GUID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		recorder = new StorageCleanupRecorder(userRepository, runRepository, deletedFolderRepository);
	}

	@Test
	void record_PersistsRunWithAggregatedCountsAndActingUser_AndOneRowPerDeletedFolder() {
		VDYPUserEntity userEntity = new VDYPUserEntity();
		when(userRepository.findById(ACTING_USER_GUID)).thenReturn(userEntity);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ACTING_USER_GUID.toString());

		StorageCleanupReportModel report = new StorageCleanupReportModel(
				false, 4, 500,
				List.of(
						new CleanupSetResultModel(
								"guid-1", "vdyp-batch-guid-1", 300, CleanupOutcomeModel.DELETED, null
						),
						new CleanupSetResultModel(
								"guid-2", "vdyp-batch-guid-2", 200, CleanupOutcomeModel.DELETED, null
						),
						new CleanupSetResultModel(
								"guid-3", "vdyp-batch-guid-3", 999, CleanupOutcomeModel.FAILED, "disk error"
						),
						new CleanupSetResultModel(
								"guid-4", "vdyp-batch-guid-4", 111, CleanupOutcomeModel.PROTECTED, "running"
						)
				), List.of()
		);

		recorder.record(actingUser, report);

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
		verify(deletedFolderRepository, times(2)).persist(folderCaptor.capture());
		List<String> deletedFolderNames = folderCaptor.getAllValues().stream()
				.map(StorageCleanupDeletedFolderEntity::getFolderName).toList();
		assertEquals(List.of("vdyp-batch-guid-1", "vdyp-batch-guid-2"), deletedFolderNames);
		assertEquals(
				List.of(300L, 200L),
				folderCaptor.getAllValues().stream().map(StorageCleanupDeletedFolderEntity::getSizeBytes).toList()
		);
	}

}
