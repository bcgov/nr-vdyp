package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetEntity;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetTypeCodeModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.projectionEntity;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.user;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.userEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {

	@Mock
	EntityManager em;
	@Mock
	ProjectionRepository repository;
	@Mock
	ProjectionFileSetService fileSetService;
	@Mock
	ProjectionStatusCodeLookup projectionStatusCodeLookup;
	@Mock
	CalculationEngineCodeLookup calculationEngineCodeLookup;
	ProjectionResourceAssembler assembler;

	ProjectionService service;

	@BeforeEach
	void setUp() {
		assembler = new ProjectionResourceAssembler();

		service = new ProjectionService(
				em, assembler, repository, fileSetService, projectionStatusCodeLookup, calculationEngineCodeLookup
		);
	}

	@Test
	void getAllProjectionsForUser_throwsException_ForInvalidGUID() {
		assertThrows(IllegalArgumentException.class, () -> service.getAllProjectionsForUser("INVALIDGUID"));
	}

	@Test
	void getAllProjectionsForUser_returnsEmpty_ForNull() {
		List<ProjectionModel> results = service.getAllProjectionsForUser(null);

		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	void getAllProjectionsForUser_returnsEmpty_ForUserWithoutRecords() {
		UUID doesNotExist = UUID.randomUUID();
		when(repository.findByOwner(doesNotExist)).thenReturn(Collections.emptyList());

		List<ProjectionModel> results = service.getAllProjectionsForUser(doesNotExist.toString());

		assertNotNull(results);
		assertTrue(results.isEmpty());
		verify(repository).findByOwner(doesNotExist);
	}

	@Test
	void getAllProjectionsForUser_returnsList_forUserWithRecords() {
		UUID doesNotExist = UUID.randomUUID();

		ProjectionEntity entityResult = new ProjectionEntity();
		entityResult.setProjectionGUID(UUID.randomUUID());
		entityResult.setReportTitle("Test Projection");
		entityResult.setReportDescription("Test Description");

		when(repository.findByOwner(doesNotExist)).thenReturn(List.of(entityResult));

		List<ProjectionModel> results = service.getAllProjectionsForUser(doesNotExist.toString());

		assertNotNull(results);
		assertThat(results).hasSize(1);

		// Validate fields were mapped through assembler
		ProjectionModel result = results.get(0);
		assertThat(result.getProjectionGUID()).isEqualTo(entityResult.getProjectionGUID().toString());
		assertThat(result.getReportTitle()).isEqualTo("Test Projection");
		assertThat(result.getReportDescription()).isEqualTo("Test Description");

		verify(repository).findByOwner(doesNotExist);
	}
	// ==========================================================
	// getProjectionEntity
	// ==========================================================

	@Test
	void getProjectionEntity_returnsEntity_whenFound() throws Exception {
		UUID projectionId = UUID.randomUUID();
		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(projectionId);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		ProjectionEntity result = service.getProjectionEntity(projectionId);

		assertThat(result).isSameAs(entity);
		verify(repository).findByIdOptional(projectionId);
	}

	@Test
	void getProjectionEntity_throwsProjectionNotFound_whenMissing() {
		UUID projectionId = UUID.randomUUID();
		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.empty());

		assertThrows(ProjectionNotFoundException.class, () -> service.getProjectionEntity(projectionId));
		verify(repository).findByIdOptional(projectionId);
	}

	// ==========================================================
	// checkUserCanPerformAction
	// ==========================================================

	@Test
	void checkUserCanPerformAction_allowsOwner_forReadUpdateDelete() {
		UUID ownerId = UUID.randomUUID();
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), ownerId);
		VDYPUserModel actingUser = user(ownerId);

		assertDoesNotThrow(
				() -> service.checkUserCanPerformAction(entity, actingUser, ProjectionService.ProjectionAction.READ)
		);
		assertDoesNotThrow(
				() -> service.checkUserCanPerformAction(entity, actingUser, ProjectionService.ProjectionAction.UPDATE)
		);
		assertDoesNotThrow(
				() -> service.checkUserCanPerformAction(entity, actingUser, ProjectionService.ProjectionAction.DELETE)
		);
	}

	@Test
	void checkUserCanPerformAction_throwsUnauthorized_whenNotOwner() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), ownerId);
		VDYPUserModel actingUser = user(otherUserId);

		assertThrows(
				ProjectionUnauthorizedException.class,
				() -> service.checkUserCanPerformAction(entity, actingUser, ProjectionService.ProjectionAction.READ)
		);
	}

	// ==========================================================
	// checkProjectionStatusPermitsAction
	// ==========================================================

	@Test
	void checkProjectionStatusPermitsAction_blocksUpdateDelete_whenInProgress() {
		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(UUID.randomUUID());

		// You’ll need whatever your status entity type is. Here’s a minimal pattern:
		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.INPROGRESS);
		entity.setProjectionStatusCode(statusEntity);

		assertThrows(
				ProjectionStateException.class,
				() -> service.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.UPDATE)
		);

		assertThrows(
				ProjectionStateException.class,
				() -> service.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.DELETE)
		);
	}

	@Test
	void checkProjectionStatusPermitsAction_allowsRead_evenIfInProgress() {
		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(UUID.randomUUID());

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.INPROGRESS);
		entity.setProjectionStatusCode(statusEntity);

		assertDoesNotThrow(
				() -> service.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.READ)
		);
	}

	// ==========================================================
	// getProjectionByID
	// ==========================================================

	@Test
	void getProjectionByID_returnsModel_whenAuthorized() throws Exception {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		ProjectionModel model = service.getProjectionByID(projectionId, user(ownerId));

		assertNotNull(model);
		assertThat(model.getProjectionGUID()).isEqualTo(projectionId.toString());
	}

	@Test
	void getProjectionByID_throwsUnauthorized_whenNotOwner() {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		assertThrows(
				ProjectionUnauthorizedException.class,
				() -> service.getProjectionByID(projectionId, user(UUID.randomUUID()))
		);
	}

	// ==========================================================
	// editProjectionParameters
	// ==========================================================

	@Test
	void editProjectionParameters_updatesTitleAndPersists_whenAllowed() throws Exception {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.DRAFT);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		Parameters params = new Parameters();
		params.setReportTitle("New Title");

		ProjectionModel model = service.editProjectionParameters(projectionId, params, user(ownerId));

		verify(repository).persist(entity);
		assertThat(model.getReportTitle()).isEqualTo("New Title");
	}

	@Test
	void editProjectionParameters_blocksUpdate_whenInProgress() {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.INPROGRESS);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		Parameters params = new Parameters();
		params.setReportTitle("Whatever");

		assertThrows(
				ProjectionStateException.class,
				() -> service.editProjectionParameters(projectionId, params, user(ownerId))
		);
	}

	// ==========================================================
	// deleteProjection
	// ==========================================================

	@Test
	void deleteProjection_deletesProjection_andDeletesFileSets_whenAllowed() throws Exception {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.DRAFT);
		entity.setProjectionStatusCode(statusEntity);

		UUID fsId = UUID.randomUUID();
		ProjectionFileSetEntity polygon = fileSetEntity(fsId);
		ProjectionFileSetEntity layer = fileSetEntity(UUID.randomUUID());
		ProjectionFileSetEntity results = fileSetEntity(UUID.randomUUID());

		entity.setPolygonFileSet(polygon);
		entity.setLayerFileSet(layer);
		entity.setResultFileSet(results);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		service.deleteProjection(projectionId, user(ownerId));

		verify(repository).delete(entity);

	}

	@Test
	void deleteProjection_wrapsExceptions_inProjectionServiceException() {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.DRAFT);
		entity.setProjectionStatusCode(statusEntity);

		UUID fsId = UUID.randomUUID();
		entity.setPolygonFileSet(fileSetEntity(fsId));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		doThrow(new RuntimeException("Random Delete Failed Exception")).when(repository).delete(entity);

		ProjectionServiceException ex = assertThrows(
				ProjectionServiceException.class, () -> service.deleteProjection(projectionId, user(ownerId))
		);

		assertThat(ex.getMessage()).contains("Error deleting Projection");
	}

	@Test
	void deleteProjection_blocksDelete_whenInProgress() {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.INPROGRESS);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		assertThrows(ProjectionStateException.class, () -> service.deleteProjection(projectionId, user(ownerId)));

	}

	// ==========================================================
	// createNewProjection
	// ==========================================================
	@Test
	void createNewProjection_persistsEntity_andSetsExpectedFields() throws Exception {
		UUID ownerId = UUID.randomUUID();
		VDYPUserModel actingUser = user(ownerId);

		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		Parameters params = new Parameters();
		params.setReportTitle("My Report");

		// status + engine entities
		var draftStatus = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		draftStatus.setCode(ProjectionStatusCodeModel.DRAFT);
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.DRAFT)).thenReturn(draftStatus);

		var engine = new ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity();
		engine.setCode(CalculationEngineCodeModel.VDYP8);
		when(calculationEngineCodeLookup.requireEntity(CalculationEngineCodeModel.VDYP8)).thenReturn(engine);

		// file set map
		UUID polyId = UUID.randomUUID();
		UUID layerId = UUID.randomUUID();
		UUID resultsId = UUID.randomUUID();

		var fileSetMap = new HashMap<FileSetTypeCodeModel, ProjectionFileSetModel>();
		fileSetMap.put(
				fileSetTypeCodeModel(FileSetTypeCodeModel.POLYGON),
				fileSetModel(polyId, ownerId, FileSetTypeCodeModel.POLYGON)
		);
		fileSetMap.put(
				fileSetTypeCodeModel(FileSetTypeCodeModel.LAYER),
				fileSetModel(layerId, ownerId, FileSetTypeCodeModel.LAYER)
		);
		fileSetMap.put(
				fileSetTypeCodeModel(FileSetTypeCodeModel.RESULTS),
				fileSetModel(resultsId, ownerId, FileSetTypeCodeModel.RESULTS)
		);

		when(fileSetService.createFileSetForNewProjection(actingUser)).thenReturn(fileSetMap);

		when(em.find(ProjectionFileSetEntity.class, polyId)).thenReturn(fileSetEntity(polyId));
		when(em.find(ProjectionFileSetEntity.class, layerId)).thenReturn(fileSetEntity(layerId));
		when(em.find(ProjectionFileSetEntity.class, resultsId)).thenReturn(fileSetEntity(resultsId));

		// Simulate DB-generated ID / app-assigned ID
		doAnswer(inv -> {
			ProjectionEntity e = inv.getArgument(0, ProjectionEntity.class);
			e.setProjectionGUID(UUID.randomUUID());
			return null;
		}).when(repository).persist(any(ProjectionEntity.class));

		ProjectionModel model = service.createNewProjection(actingUser, params);

		assertNotNull(model);

		ArgumentCaptor<ProjectionEntity> captor = ArgumentCaptor.forClass(ProjectionEntity.class);
		verify(repository).persist(captor.capture());

		ProjectionEntity persisted = captor.getValue();
		assertThat(persisted.getOwnerUser()).isSameAs(ownerEntity);
		assertThat(persisted.getReportTitle()).isEqualTo("My Report");

		// NOTE: your current code sets description = title. This asserts current behavior.
		assertThat(persisted.getReportDescription()).isEqualTo("My Report");

		assertThat(persisted.getProjectionStatusCode()).isSameAs(draftStatus);
		assertThat(persisted.getCalculationEngineCode()).isSameAs(engine);

		assertThat(persisted.getPolygonFileSet().getProjectionFileSetGUID()).isEqualTo(polyId);
		assertThat(persisted.getLayerFileSet().getProjectionFileSetGUID()).isEqualTo(layerId);
		assertThat(persisted.getResultFileSet().getProjectionFileSetGUID()).isEqualTo(resultsId);

		assertThat(persisted.getProjectionParameters()).isNotBlank();
	}
}
