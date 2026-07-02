package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.projectionEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionBatchMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchJobModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.ProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

@ExtendWith(MockitoExtension.class)
class ProjectionBatchMappingServiceTest {

	@Mock
	ProjectionBatchMappingRepository repository;
	@Mock
	ProjectionBatchMappingResourceAssembler assembler;
	@Mock
	BatchFailureTypeCodeLookup failureLookup;
	@Mock
	@RestClient
	VDYPBatchClient batchClient;

	ProjectionBatchMappingService service;

	@BeforeEach
	void setUp() {
		service = new ProjectionBatchMappingService(repository, assembler, failureLookup, batchClient);
	}

	@Test
	void startProjectionInBatch_happyPath_persistsEntity_andReturnsModel() throws Exception {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();
		Parameters projectionParameters = new Parameters(); // replace with your real parameters type if desired

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());
		projectionEntity.setProjectionParameters(projectionParameters.toString());

		UUID batchJobGuid = UUID.randomUUID();
		int errors = 0;
		int warnings = 0;
		BatchJobModel mockModel = new BatchJobModel(batchJobGuid.toString(), errors, warnings);

		when(batchClient.startBatchProcessWithGUID(projectionGuid, projectionParameters.toString()))
				.thenReturn(mockModel);

		service.startProjectionInBatch(projectionEntity);

		// Verify persist called and capture entity
		verify(batchClient, times(1)).startBatchProcessWithGUID(projectionGuid, projectionParameters.toString());
		verify(repository, times(1)).persist(any(ProjectionBatchMappingEntity.class));
		verify(assembler, times(1)).toModel(any(ProjectionBatchMappingEntity.class));

		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void cancelProjection_happyPath_callsBatchClientCancel() throws Exception {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();
		UUID batchJobGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		ProjectionBatchMappingEntity mappingEntity = new ProjectionBatchMappingEntity();
		mappingEntity.setBatchJobGUID(batchJobGuid);
		mappingEntity.setProjection(projectionEntity);

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.of(mappingEntity));

		// Act
		service.cancelProjection(projectionEntity);

		// Verify cancel called
		verify(repository, times(1)).findByProjectionGUID(projectionGuid);
		verify(batchClient, times(1)).stopBatchJob(batchJobGuid);
		verify(repository, times(1)).delete(mappingEntity);
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void cancelProjection_missingBatchMapping_cancelsBatchJobByProjectionGuid() throws ProjectionServiceException {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		// Act
		service.cancelProjection(projectionEntity);

		verify(repository, times(1)).findByProjectionGUID(projectionGuid);
		verify(batchClient, times(1)).stopBatchJobByProjection(projectionGuid);
		verify(batchClient, never()).stopBatchJob(any());
		verify(repository, never()).delete(any());
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void deleteMappingForProjection_happyPath_deletesMapping() {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		ProjectionBatchMappingEntity mappingEntity = new ProjectionBatchMappingEntity();
		mappingEntity.setProjection(projectionEntity);

		when(repository.listByProjectionGUID(projectionGuid)).thenReturn(List.of(mappingEntity));

		// Act
		service.deleteMappingsForProjection(projectionEntity);

		// Verify delete called
		verify(repository, times(1)).listByProjectionGUID(projectionGuid);
		verify(repository, times(1)).delete(mappingEntity);
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void deleteMappingForProjection_projectionHasNoMapping_doesntDelete() {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		when(repository.listByProjectionGUID(projectionGuid)).thenReturn(List.of());

		// Act
		service.deleteMappingsForProjection(projectionEntity);

		// Verify delete called
		verify(repository, times(1)).listByProjectionGUID(projectionGuid);
		verify(repository, never()).delete(any());
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void updateProgress_projectionHasMapping_updatesCorrectly() throws ProjectionServiceException {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		UUID batchJobGuid = UUID.randomUUID();
		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());
		ProjectionBatchMappingEntity mappingEntity = new ProjectionBatchMappingEntity();
		mappingEntity.setBatchJobGUID(batchJobGuid);

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.of(mappingEntity));

		ProjectionProgressUpdate update = new ProjectionProgressUpdate(batchJobGuid, 100, 10, 20, 3, null, null);
		// Act
		service.updateProgress(projectionEntity, update);
		assertEquals(100, mappingEntity.getPolygonCount());
		assertEquals(20, mappingEntity.getErrorCount());
		assertEquals(10, mappingEntity.getCompletedPolygonCount());
		assertEquals(3, mappingEntity.getWorkerCount());
	}

	@Test
	void updateProgress_projectionHasNoMapping_createsStreamedMappingAndUpdatesProgress()
			throws ProjectionServiceException {
		UUID projectionGuid = UUID.randomUUID();
		UUID batchJobGuid = UUID.randomUUID();
		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());
		ProjectionProgressUpdate update = new ProjectionProgressUpdate(batchJobGuid, 100, 10, 20, 3, null, null);

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		service.updateProgress(projectionEntity, update);

		ArgumentCaptor<ProjectionBatchMappingEntity> entityCaptor = ArgumentCaptor
				.forClass(ProjectionBatchMappingEntity.class);
		verify(repository).findByProjectionGUID(projectionGuid);
		verify(repository).persist(entityCaptor.capture());

		ProjectionBatchMappingEntity entity = entityCaptor.getValue();
		assertEquals(batchJobGuid, entity.getBatchJobGUID());
		assertEquals(projectionEntity, entity.getProjection());
		assertEquals(100, entity.getPolygonCount());
		assertEquals(20, entity.getErrorCount());
		assertEquals(10, entity.getCompletedPolygonCount());
		assertEquals(0, entity.getWarningCount());
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void updateFailureDetails_projectionHasMapping_updatesCorrectly() throws ProjectionServiceException {
		UUID projectionGuid = UUID.randomUUID();
		UUID batchJobGuid = UUID.randomUUID();
		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());
		ProjectionBatchMappingEntity mappingEntity = new ProjectionBatchMappingEntity();
		String failureTypeCode = "PROCESS";

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.of(mappingEntity));

		service.updateFailureDetails(projectionEntity, batchJobGuid, failureTypeCode, "Projection failed");

		assertEquals("Projection failed", mappingEntity.getFailureMessage());
	}

	@Test
	void updateFailureDetails_projectionHasNoMapping_createsMappingWhenBatchJobProvided()
			throws ProjectionServiceException {
		UUID projectionGuid = UUID.randomUUID();
		UUID batchJobGuid = UUID.randomUUID();
		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());
		String failureTypeCode = "INPUT";

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		service.updateFailureDetails(projectionEntity, batchJobGuid, failureTypeCode, "Input failed");

		ArgumentCaptor<ProjectionBatchMappingEntity> entityCaptor = ArgumentCaptor
				.forClass(ProjectionBatchMappingEntity.class);
		verify(repository).persist(entityCaptor.capture());

		ProjectionBatchMappingEntity entity = entityCaptor.getValue();
		assertEquals(batchJobGuid, entity.getBatchJobGUID());
		assertEquals(projectionEntity, entity.getProjection());
		assertEquals("Input failed", entity.getFailureMessage());
	}

	@Test
	void updateFailureDetails_projectionHasNoMappingAndNoBatchJob_throwsException() {
		UUID projectionGuid = UUID.randomUUID();
		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		assertThrows(
				ProjectionServiceException.class,
				() -> service.updateFailureDetails(projectionEntity, null, null, "Projection failed")
		);
	}
}
