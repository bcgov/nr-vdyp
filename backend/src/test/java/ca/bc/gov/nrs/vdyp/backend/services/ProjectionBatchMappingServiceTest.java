package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.projectionEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionBatchMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchJobModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;

@ExtendWith(MockitoExtension.class)
class ProjectionBatchMappingServiceTest {

	@Mock
	ProjectionBatchMappingRepository repository;
	@Mock
	ProjectionBatchMappingResourceAssembler assembler;
	@Mock
	@RestClient
	VDYPBatchClient batchClient;

	ProjectionBatchMappingService service;

	@BeforeEach
	void setUp() {
		service = new ProjectionBatchMappingService(repository, assembler, batchClient);
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
		verify(batchClient, times(1)).stopBatchJob(batchJobGuid);
		verify(repository, times(1)).delete(mappingEntity);
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void cancelProjection_invalidBatchMapping() throws Exception {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		// Act
		assertThrows(ProjectionServiceException.class, () -> service.cancelProjection(projectionEntity));

		// Verify cancel not called
		verify(batchClient, never()).stopBatchJob(projectionGuid);
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void deleteMappingForProjection_happyPath_deletesMapping() {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		ProjectionBatchMappingEntity mappingEntity = new ProjectionBatchMappingEntity();
		mappingEntity.setProjection(projectionEntity);

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.of(mappingEntity));

		// Act
		service.deleteMappingForProjection(projectionEntity);

		// Verify delete called
		verify(repository, times(1)).delete(mappingEntity);
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}

	@Test
	void deleteMappingForProjection_projectionHasNoMapping_doesntDelete() {
		// Arrange
		UUID projectionGuid = UUID.randomUUID();

		ProjectionEntity projectionEntity = projectionEntity(projectionGuid, UUID.randomUUID());

		when(repository.findByProjectionGUID(projectionGuid)).thenReturn(Optional.empty());

		// Act
		service.deleteMappingForProjection(projectionEntity);

		// Verify delete called
		verify(repository, never()).delete(any());
		verifyNoMoreInteractions(batchClient, repository, assembler);
	}
}
