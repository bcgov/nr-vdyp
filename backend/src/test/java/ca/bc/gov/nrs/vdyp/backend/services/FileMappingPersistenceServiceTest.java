package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class FileMappingPersistenceServiceTest {
	@Mock
	EntityManager em;
	@Mock
	FileMappingRepository repository;

	FileMappingPersistenceService service;

	@BeforeEach
	void setUp() {
		service = new FileMappingPersistenceService(em, repository, new FileMappingResourceAssembler());
	}

	@Test
	void persistFileMapping_persistsEntityAndReturnsModel() {
		UUID fileSetGUID = UUID.randomUUID();
		UUID comsObjectGUID = UUID.randomUUID();
		UUID fileMappingGUID = UUID.randomUUID();
		String filename = "large-input.csv";

		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		when(em.find(ProjectionFileSetEntity.class, fileSetGUID)).thenReturn(fileSetEntity);
		doAnswer(invocation -> {
			FileMappingEntity entity = invocation.getArgument(0, FileMappingEntity.class);
			entity.setFileMappingGUID(fileMappingGUID);
			return null;
		}).when(repository).persist(any(FileMappingEntity.class));

		FileMappingModel result = service.persistFileMapping(comsObjectGUID, fileSetGUID, filename);

		assertNotNull(result);
		assertEquals(fileMappingGUID.toString(), result.getFileMappingGUID());
		assertEquals(comsObjectGUID.toString(), result.getComsObjectGUID());
		assertEquals(filename, result.getFilename());
		assertEquals(fileSetGUID.toString(), result.getProjectionFileSet().getProjectionFileSetGUID());

		ArgumentCaptor<FileMappingEntity> captor = ArgumentCaptor.forClass(FileMappingEntity.class);
		verify(repository).persist(captor.capture());

		FileMappingEntity persisted = captor.getValue();
		assertEquals(comsObjectGUID, persisted.getComsObjectGUID());
		assertEquals(filename, persisted.getFilename());
		assertEquals(fileSetEntity, persisted.getProjectionFileSet());
	}
}
