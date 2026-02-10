package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.batchMappingModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileMappingModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetEntity;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetTypeCodeModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.projectionEntity;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.statusCode;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.user;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.userEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
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
	@Mock
	ProjectionBatchMappingService batchMappingService;
	ProjectionResourceAssembler assembler;

	ProjectionService service;

	@BeforeEach
	void setUp() {
		assembler = new ProjectionResourceAssembler();

		service = new ProjectionService(
				em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
				calculationEngineCodeLookup, new ObjectMapper()
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
	void checkUserCanPerformAction_allowsSystem_forCompleteStoreResults() {
		UUID ownerId = UUID.randomUUID();
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), ownerId);
		VDYPUserModel actingUser = user(ownerId);
		UserTypeCodeModel systemUserType = new UserTypeCodeModel();
		systemUserType.setCode(UserTypeCodeModel.SYSTEM);
		VDYPUserModel systemUser = new VDYPUserModel();
		systemUser.setUserTypeCode(systemUserType);
		assertThrows(
				ProjectionUnauthorizedException.class,
				() -> service.checkUserCanPerformAction(
						entity, actingUser, ProjectionService.ProjectionAction.COMPLETE_PROJECTION
				)
		);
		assertThrows(
				ProjectionUnauthorizedException.class,
				() -> service
						.checkUserCanPerformAction(entity, actingUser, ProjectionService.ProjectionAction.STORE_RESULTS)
		);
		assertDoesNotThrow(
				() -> service.checkUserCanPerformAction(
						entity, systemUser, ProjectionService.ProjectionAction.COMPLETE_PROJECTION
				)
		);
		assertDoesNotThrow(
				() -> service
						.checkUserCanPerformAction(entity, systemUser, ProjectionService.ProjectionAction.STORE_RESULTS)
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
		statusEntity.setCode(ProjectionStatusCodeModel.RUNNING);
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

	@ParameterizedTest
	@ValueSource(
			strings = { ProjectionStatusCodeModel.DRAFT, ProjectionStatusCodeModel.READY,
					ProjectionStatusCodeModel.FAILED }
	)
	void checkProjectionStatusPermitsAction_blocksCompleteStore_whenInDraft(String statusCode) {
		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(UUID.randomUUID());

		// You’ll need whatever your status entity type is. Here’s a minimal pattern:
		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(statusCode);
		entity.setProjectionStatusCode(statusEntity);

		assertThrows(
				ProjectionStateException.class,
				() -> service.checkProjectionStatusPermitsAction(
						entity, ProjectionService.ProjectionAction.COMPLETE_PROJECTION
				)
		);

		assertThrows(
				ProjectionStateException.class,
				() -> service
						.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.STORE_RESULTS)
		);
	}

	@Test
	void checkProjectionStatusPermitsAction_allowsRead_evenIfInProgress() {
		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(UUID.randomUUID());

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.RUNNING);
		entity.setProjectionStatusCode(statusEntity);

		assertDoesNotThrow(
				() -> service.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.READ)
		);
		assertDoesNotThrow(
				() -> service.checkProjectionStatusPermitsAction(
						entity, ProjectionService.ProjectionAction.COMPLETE_PROJECTION
				)
		);
		assertDoesNotThrow(
				() -> service
						.checkProjectionStatusPermitsAction(entity, ProjectionService.ProjectionAction.STORE_RESULTS)
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

		ModelParameters modelParameters = null;

		ProjectionModel model = service.editProjectionParameters(projectionId, params, modelParameters, user(ownerId));

		assertThat(model.getReportTitle()).isEqualTo("New Title");
	}

	@Test
	void editProjectionParameters_blocksUpdate_whenInProgress() {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.RUNNING);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		Parameters params = new Parameters();
		params.setReportTitle("Whatever");

		ModelParameters modelParameters = null;

		assertThrows(
				ProjectionStateException.class,
				() -> service.editProjectionParameters(projectionId, params, modelParameters, user(ownerId))
		);
	}

	@Test
	void editProjectionParameters_throwsProjectionServiceException_whenJsonProcessingException()
			throws JsonProcessingException {
		ObjectMapper failingMapper = mock(ObjectMapper.class);
		ObjectWriter writer = mock(ObjectWriter.class);

		when(failingMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
		when(writer.writeValueAsString(any(Parameters.class))).thenReturn("{ }");
		when(writer.writeValueAsString(any(ModelParameters.class))).thenThrow(new JsonProcessingException("boom") {
		});

		service = new ProjectionService(
				em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
				calculationEngineCodeLookup, failingMapper
		);

		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.DRAFT);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));

		Parameters params = new Parameters();
		params.setReportTitle("Whatever");

		ModelParameters modelParameters = new ModelParameters(
				List.of(), "", "", "", "", "", "", "", "", "", 55.5f, 0f, 13f, 10000.0f, "7.4+", ""
		);

		assertThrows(
				ProjectionServiceException.class,
				() -> service.editProjectionParameters(projectionId, params, modelParameters, user(ownerId))
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
		when(repository.countUsesFileSet(fsId)).thenReturn(0L);
		when(repository.countUsesFileSet(layer.getProjectionFileSetGUID())).thenReturn(0L);
		when(repository.countUsesFileSet(results.getProjectionFileSetGUID())).thenReturn(0L);

		service.deleteProjection(projectionId, user(ownerId));

		verify(repository).delete(entity);
		verify(fileSetService).deleteFileSetById(fsId);
		verify(fileSetService).deleteFileSetById(layer.getProjectionFileSetGUID());
		verify(fileSetService).deleteFileSetById(results.getProjectionFileSetGUID());
		verify(batchMappingService).deleteMappingsForProjection(entity);
	}

	@Test
	void deleteProjection_deletesProjection_leavesFileSets_whenShared() throws Exception {
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
		when(repository.countUsesFileSet(fsId)).thenReturn(1L);
		when(repository.countUsesFileSet(layer.getProjectionFileSetGUID())).thenReturn(1L);
		when(repository.countUsesFileSet(results.getProjectionFileSetGUID())).thenReturn(1L);

		service.deleteProjection(projectionId, user(ownerId));

		verify(repository).delete(entity);
		verify(fileSetService, never()).deleteFileSetById(fsId);
		verify(fileSetService, never()).deleteFileSetById(layer.getProjectionFileSetGUID());
		verify(fileSetService, never()).deleteFileSetById(results.getProjectionFileSetGUID());
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
		statusEntity.setCode(ProjectionStatusCodeModel.RUNNING);
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

		ModelParameters modelParamsJson = null;

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

		ProjectionModel model = service.createNewProjection(actingUser, params, modelParamsJson);

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

	// ==============================
	// addProjectionFile
	// ==============================
	@Test
	void addProjectionFile_nullFileSet_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		ProjectionFileSetEntity fileSetEntity = fileSetEntity(UUID.randomUUID());
		entity.setResultFileSet(fileSetEntity);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.addProjectionFile(projectionGUID, null, null, actingUser)
		);
	}

	@Test
	void addProjectionFile_invalidFileSet_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.addProjectionFile(projectionGUID, UUID.randomUUID(), null, actingUser)
		);

	}

	@Test
	void addProjectionFile_polygonFileSet_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.addProjectionFile(
				projectionGUID, entity.getPolygonFileSet().getProjectionFileSetGUID(), null, actingUser
		);
		verify(fileSetService)
				.addNewFileToFileSet(entity.getPolygonFileSet().getProjectionFileSetGUID(), actingUser, null);
	}

	@Test
	void addProjectionFile_layerFileSet_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.addProjectionFile(
				projectionGUID, entity.getLayerFileSet().getProjectionFileSetGUID(), null, actingUser
		);
		verify(fileSetService)
				.addNewFileToFileSet(entity.getLayerFileSet().getProjectionFileSetGUID(), actingUser, null);
	}

	@Test
	void addProjectionFile_resultFileSet_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		VDYPUserModel actingUser = new VDYPUserModel();
		UserTypeCodeModel model = new UserTypeCodeModel();
		model.setCode(UserTypeCodeModel.SYSTEM);
		actingUser.setUserTypeCode(model);

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.addProjectionFile(
				projectionGUID, entity.getResultFileSet().getProjectionFileSetGUID(), null, actingUser
		);
		verify(fileSetService)
				.addNewFileToFileSet(entity.getResultFileSet().getProjectionFileSetGUID(), actingUser, null);
	}

	@Test
	void deleteProjectionFile_validInput_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID fileId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.deleteFile(projectionGUID, entity.getResultFileSet().getProjectionFileSetGUID(), fileId, actingUser);
		verify(fileSetService)
				.deleteFileFromFileSet(entity.getResultFileSet().getProjectionFileSetGUID(), actingUser, fileId);

	}

	@Test
	void deleteProjectionFileSetFiles_validInput_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.deleteAllFiles(projectionGUID, entity.getResultFileSet().getProjectionFileSetGUID(), actingUser);
		verify(fileSetService)
				.deleteAllFilesFromFileSet(entity.getResultFileSet().getProjectionFileSetGUID(), actingUser);

	}

	@Test
	void getProjectionFile_nullFile_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.getFileForDownload(
						projectionGUID, entity.getResultFileSet().getProjectionFileSetGUID(), null, actingUser
				)
		);
	}

	@Test
	void getProjectionFile_validInput_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID fileId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		service.getFileForDownload(
				projectionGUID, entity.getResultFileSet().getProjectionFileSetGUID(), fileId, actingUser
		);
		verify(fileSetService)
				.getFileForDownload(entity.getResultFileSet().getProjectionFileSetGUID(), actingUser, fileId);

	}

	@ParameterizedTest
	@ValueSource(
			strings = { ProjectionStatusCodeModel.RUNNING, ProjectionStatusCodeModel.READY,
					ProjectionStatusCodeModel.FAILED }
	)
	void startBatchProjection_notDraft_throwsException(String statusCode) {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, statusCode);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(ProjectionStateException.class, () -> service.startBatchProjection(actingUser, projectionGUID));
	}

	@Test
	void startBatchProjection_noPolygonFile_throwsException() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID polygonFileSetGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());
		entity.setPolygonFileSet(fileSetEntity(polygonFileSetGUID));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getAllFiles(polygonFileSetGUID, actingUser, false)).thenReturn(List.of());
		assertThrows(
				ProjectionValidationException.class, () -> service.startBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void startBatchProjection_noLayerFile_throwsException() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID polygonFileSetGUID = UUID.randomUUID();
		UUID layerFileSetGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		FileMappingModel polygonFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());
		entity.setPolygonFileSet(fileSetEntity(polygonFileSetGUID));
		entity.setLayerFileSet(fileSetEntity(layerFileSetGUID));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getAllFiles(polygonFileSetGUID, actingUser, false)).thenReturn(List.of(polygonFileModel));
		when(fileSetService.getAllFiles(layerFileSetGUID, actingUser, false)).thenReturn(List.of());
		assertThrows(
				ProjectionValidationException.class, () -> service.startBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void startBatchProjection_noParameters_throwsException() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID polygonFileSetGUID = UUID.randomUUID();
		UUID layerFileSetGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		FileMappingModel polygonFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		FileMappingModel layerFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());
		entity.setPolygonFileSet(fileSetEntity(polygonFileSetGUID));
		entity.setLayerFileSet(fileSetEntity(layerFileSetGUID));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getAllFiles(polygonFileSetGUID, actingUser, false)).thenReturn(List.of(polygonFileModel));
		when(fileSetService.getAllFiles(layerFileSetGUID, actingUser, false)).thenReturn(List.of(layerFileModel));
		assertThrows(
				ProjectionValidationException.class, () -> service.startBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void startBatchProjection_invalidParameters_throwsException() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID polygonFileSetGUID = UUID.randomUUID();
		UUID layerFileSetGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		FileMappingModel polygonFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		FileMappingModel layerFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());
		entity.setPolygonFileSet(fileSetEntity(polygonFileSetGUID));
		entity.setLayerFileSet(fileSetEntity(layerFileSetGUID));
		entity.setProjectionParameters("{ invalid json ");

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getAllFiles(polygonFileSetGUID, actingUser, false)).thenReturn(List.of(polygonFileModel));
		when(fileSetService.getAllFiles(layerFileSetGUID, actingUser, false)).thenReturn(List.of(layerFileModel));
		assertThrows(
				ProjectionValidationException.class, () -> service.startBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void startBatchProjection_defaultParameters_succeeds() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID polygonFileSetGUID = UUID.randomUUID();
		UUID layerFileSetGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.DRAFT);
		VDYPUserModel actingUser = new VDYPUserModel();
		FileMappingModel polygonFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		FileMappingModel layerFileModel = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());
		entity.setPolygonFileSet(fileSetEntity(polygonFileSetGUID));
		entity.setLayerFileSet(fileSetEntity(layerFileSetGUID));
		entity.setProjectionParameters("{\"ageStart\":0,\"ageEnd\":100,\"ageIncrement\":10}");

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getAllFiles(polygonFileSetGUID, actingUser, false)).thenReturn(List.of(polygonFileModel));
		when(fileSetService.getAllFiles(layerFileSetGUID, actingUser, false)).thenReturn(List.of(layerFileModel));
		when(batchMappingService.startProjectionInBatch(any())).thenReturn(batchMappingModel(UUID.randomUUID()));
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.RUNNING))
				.thenReturn(statusCode(ProjectionStatusCodeModel.RUNNING));

		ProjectionModel model = service.startBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.RUNNING, model.getProjectionStatusCode().getCode());

		verify(batchMappingService, times(1)).startProjectionInBatch(any());
	}

	@ParameterizedTest
	@ValueSource(
			strings = { ProjectionStatusCodeModel.READY, //
					ProjectionStatusCodeModel.FAILED, //
					ProjectionStatusCodeModel.DRAFT }
	)
	void cancelBatchProcessing_notRunning_throwsException(String statusCode) {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, statusCode);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));

		assertThrows(ProjectionStateException.class, () -> service.cancelBatchProjection(actingUser, projectionGUID));
	}

	@Test
	void cancelBatchProcessing_callsBatchService_statusDraft() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		VDYPUserModel actingUser = new VDYPUserModel();

		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.DRAFT))
				.thenReturn(statusCode(ProjectionStatusCodeModel.DRAFT));

		ProjectionModel model = service.cancelBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.DRAFT, model.getProjectionStatusCode().getCode());

		verify(batchMappingService, times(1)).cancelProjection(any());
	}
}
