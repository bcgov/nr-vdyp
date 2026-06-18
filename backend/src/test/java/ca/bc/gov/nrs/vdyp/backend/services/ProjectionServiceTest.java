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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import ca.bc.gov.nrs.vdyp.backend.config.ProjectionExpiryConfig;
import ca.bc.gov.nrs.vdyp.backend.config.ProjectionLimitsConfig;
import ca.bc.gov.nrs.vdyp.backend.context.CurrentVDYPUser;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ProjectionEndpoint;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionValidationException;
import ca.bc.gov.nrs.vdyp.backend.messaging.publisher.BatchJobPublisher;
import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
import ca.bc.gov.nrs.vdyp.backend.model.ProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
	@Mock
	VDYPUserService userService;
	@Mock
	BatchJobPublisher batchJobPublisher;
	@Mock
	ProjectionExpiryConfig expiryConfig;
	ProjectionLimitsConfig limitsConfig;
	ProjectionResourceAssembler assembler;

	ProjectionService service;

	@BeforeEach
	void setUp() {
		assembler = new ProjectionResourceAssembler();
		limitsConfig = new ProjectionLimitsConfig(300);

		service = new ProjectionService(
				em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
				calculationEngineCodeLookup, userService, new ObjectMapper(), expiryConfig, limitsConfig,
				batchJobPublisher
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
	void validateMaximumPolygons_allowsFilesAtLimit(@TempDir Path tempDir) throws Exception {
		Path polygonFile = tempDir.resolve("polygon.csv");
		Files.writeString(polygonFile, "FEATURE_ID,MAP_ID,POLYGON_NUMBER\n\n1,082G055,1234\n2,082G055,5678\n");

		service = new ProjectionService(
				em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
				calculationEngineCodeLookup, userService, new ObjectMapper(), expiryConfig,
				new ProjectionLimitsConfig(2)
		);

		assertDoesNotThrow(() -> service.validateMaximumPolygons(polygonFile));
	}

	@Test
	void validateMaximumPolygons_throwsWhenFileExceedsLimit(@TempDir Path tempDir) throws Exception {
		Path polygonFile = tempDir.resolve("polygon.csv");
		Files.writeString(polygonFile, "\"FEATURE_ID\",MAP_ID,POLYGON_NUMBER\n1,082G055,1234\n2,082G055,5678\n");

		service = new ProjectionService(
				em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
				calculationEngineCodeLookup, userService, new ObjectMapper(), expiryConfig,
				new ProjectionLimitsConfig(1)
		);

		var exception = assertThrows(
				ProjectionRequestValidationException.class, () -> service.validateMaximumPolygons(polygonFile)
		);
		assertThat(exception.getValidationMessages().get(0).getMessage())
				.isEqualTo("Polygon file exceeds maximum polygon count of 1.");
	}

	@Test
	void projectionHcsvPost_pathInputsPassesValidatedStreamsToRunProjection(@TempDir Path tempDir) throws Exception {
		String polygonContents = "FEATURE_ID,MAP_ID,POLYGON_NUMBER\n1,082G055,1234\n";
		String layerContents = "FEATURE_ID,TREE_COVER_LAYER_ESTIMATED_ID\n1,99\n";
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");
		Files.writeString(polygonFile, polygonContents);
		Files.writeString(layerFile, layerContents);

		ProjectionService serviceSpy = spy(
				new ProjectionService(
						em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
						calculationEngineCodeLookup, userService, new ObjectMapper(), expiryConfig,
						new ProjectionLimitsConfig(1)
				)
		);
		Parameters parameters = new Parameters().ageStart(0).ageEnd(100).ageIncrement(10);
		SecurityContext securityContext = mock(SecurityContext.class);
		Response expectedResponse = Response.ok().build();

		doAnswer(invocation -> {
			Map<String, InputStream> inputStreams = invocation.getArgument(1);
			assertThat(inputStreams)
					.containsOnlyKeys(ParameterNames.HCSV_POLYGON_INPUT_DATA, ParameterNames.HCSV_LAYERS_INPUT_DATA);
			assertThat(
					new String(
							inputStreams.get(ParameterNames.HCSV_POLYGON_INPUT_DATA).readAllBytes(),
							StandardCharsets.UTF_8
					)
			).isEqualTo(polygonContents);
			assertThat(
					new String(
							inputStreams.get(ParameterNames.HCSV_LAYERS_INPUT_DATA).readAllBytes(),
							StandardCharsets.UTF_8
					)
			).isEqualTo(layerContents);
			return expectedResponse;
		}).when(serviceSpy).runProjection(
				eq(ProjectionRequestKind.HCSV), any(), eq(Boolean.TRUE), eq(parameters), eq(securityContext)
		);

		Response response = serviceSpy.projectionHcsvPost(true, parameters, polygonFile, layerFile, securityContext);

		assertThat(response).isSameAs(expectedResponse);
		verify(serviceSpy).runProjection(
				eq(ProjectionRequestKind.HCSV), any(), eq(Boolean.TRUE), eq(parameters), eq(securityContext)
		);
	}

	@Test
	void projectionHcsvPost_pathInputsThrowsWhenPolygonFileExceedsLimitBeforeOpeningLayerFile(@TempDir Path tempDir)
			throws Exception {
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path missingLayerFile = tempDir.resolve("missing-layer.csv");
		Files.writeString(polygonFile, "FEATURE_ID,MAP_ID,POLYGON_NUMBER\n1,082G055,1234\n2,082G055,5678\n");

		ProjectionService serviceSpy = spy(
				new ProjectionService(
						em, assembler, repository, fileSetService, batchMappingService, projectionStatusCodeLookup,
						calculationEngineCodeLookup, userService, new ObjectMapper(), expiryConfig,
						new ProjectionLimitsConfig(1)
				)
		);

		var exception = assertThrows(
				ProjectionRequestValidationException.class,
				() -> serviceSpy.projectionHcsvPost(false, new Parameters(), polygonFile, missingLayerFile, null)
		);

		assertThat(exception.getValidationMessages().get(0).getMessage())
				.isEqualTo("Polygon file exceeds maximum polygon count of 1.");
		verify(serviceSpy, never()).runProjection(any(), any(), any(), any(), any());
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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service
				.editProjectionParameters(projectionId, params, modelParameters, "Test Description", user(ownerId));

		assertThat(model.getReportTitle()).isEqualTo("New Title");
		assertThat(model.getReportDescription()).isEqualTo("Test Description");
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
				() -> service.editProjectionParameters(projectionId, params, modelParameters, "", user(ownerId))
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
				calculationEngineCodeLookup, userService, failingMapper, expiryConfig, limitsConfig
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
				List.of(), "", "", "", "", "", null, "", "", "", "", null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				55.5f, 0f, 13f, 10000.0f, "7.4+", ""
		);

		assertThrows(
				ProjectionServiceException.class,
				() -> service.editProjectionParameters(projectionId, params, modelParameters, null, user(ownerId))
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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.createNewProjection(actingUser, params, modelParamsJson, "Test Description");

		assertNotNull(model);

		ArgumentCaptor<ProjectionEntity> captor = ArgumentCaptor.forClass(ProjectionEntity.class);
		verify(repository).persist(captor.capture());

		ProjectionEntity persisted = captor.getValue();
		assertThat(persisted.getOwnerUser()).isSameAs(ownerEntity);
		assertThat(persisted.getReportTitle()).isEqualTo("My Report");
		assertThat(persisted.getReportDescription()).isEqualTo("Test Description");

		assertThat(persisted.getProjectionStatusCode()).isSameAs(draftStatus);
		assertThat(persisted.getCalculationEngineCode()).isSameAs(engine);

		assertThat(persisted.getPolygonFileSet().getProjectionFileSetGUID()).isEqualTo(polyId);
		assertThat(persisted.getLayerFileSet().getProjectionFileSetGUID()).isEqualTo(layerId);
		assertThat(persisted.getResultFileSet().getProjectionFileSetGUID()).isEqualTo(resultsId);

		assertThat(persisted.getProjectionParameters()).isNotBlank();
	}

	@Test
	void createNewProjection_setsEmptyDescription_whenReportDescriptionIsNull() throws Exception {
		UUID ownerId = UUID.randomUUID();
		VDYPUserModel actingUser = user(ownerId);

		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		Parameters params = new Parameters();

		var draftStatus = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		draftStatus.setCode(ProjectionStatusCodeModel.DRAFT);
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.DRAFT)).thenReturn(draftStatus);

		var engine = new ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity();
		engine.setCode(CalculationEngineCodeModel.VDYP8);
		when(calculationEngineCodeLookup.requireEntity(CalculationEngineCodeModel.VDYP8)).thenReturn(engine);

		var fileSetMap = new HashMap<FileSetTypeCodeModel, ProjectionFileSetModel>();
		UUID polyId = UUID.randomUUID();
		UUID layerId = UUID.randomUUID();
		UUID resultsId = UUID.randomUUID();
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
		doAnswer(inv -> {
			ProjectionEntity e = inv.getArgument(0, ProjectionEntity.class);
			e.setProjectionGUID(UUID.randomUUID());
			return null;
		}).when(repository).persist(any(ProjectionEntity.class));
		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());

		service.createNewProjection(actingUser, params, null, null);

		ArgumentCaptor<ProjectionEntity> captor = ArgumentCaptor.forClass(ProjectionEntity.class);
		verify(repository).persist(captor.capture());
		assertThat(captor.getValue().getReportDescription()).isEqualTo("");
	}

	@Test
	void editProjectionParameters_setsEmptyDescription_whenReportDescriptionIsNull() throws Exception {
		UUID projectionId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.DRAFT);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));
		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());

		Parameters params = new Parameters();
		ProjectionModel model = service.editProjectionParameters(projectionId, params, null, null, user(ownerId));

		assertThat(model.getReportDescription()).isEqualTo("");
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
	void startFileSetFileUpload_nullFileSet_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.startFileSetFileUpload(projectionGUID, null, "result.zip")
		);
	}

	@Test
	void startFileSetFileUpload_invalidFileSet_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.startFileSetFileUpload(projectionGUID, UUID.randomUUID(), "result.zip")
		);
	}

	@Test
	void startFileSetFileUpload_resultFileSet_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		UUID resultFileSetGUID = entity.getResultFileSet().getProjectionFileSetGUID();
		FileMappingModel expected = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.startFileSetFileUpload(resultFileSetGUID, "result.zip")).thenReturn(expected);

		FileMappingModel result = service.startFileSetFileUpload(projectionGUID, resultFileSetGUID, "result.zip");

		assertNotNull(result);
		verify(fileSetService).startFileSetFileUpload(resultFileSetGUID, "result.zip");
	}

	@Test
	void completeFileSetFileUpload_invalidFileSet_throwsException() {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(
				ProjectionServiceException.class,
				() -> service.completeFileSetFileUpload(projectionGUID, UUID.randomUUID(), UUID.randomUUID())
		);
	}

	@Test
	void completeFileSetFileUpload_resultFileSet_callsFileSetService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID fileMappingGUID = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.RUNNING);
		entity.setPolygonFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setLayerFileSet(fileSetEntity(UUID.randomUUID()));
		entity.setResultFileSet(fileSetEntity(UUID.randomUUID()));

		UUID resultFileSetGUID = entity.getResultFileSet().getProjectionFileSetGUID();
		FileMappingModel expected = fileMappingModel(fileMappingGUID, UUID.randomUUID());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(fileSetService.getFileMappingById(fileMappingGUID)).thenReturn(expected);

		FileMappingModel result = service.completeFileSetFileUpload(projectionGUID, resultFileSetGUID, fileMappingGUID);

		assertNotNull(result);
		verify(fileSetService).getFileMappingById(fileMappingGUID);
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
	@ValueSource(strings = { ProjectionStatusCodeModel.RUNNING, ProjectionStatusCodeModel.READY })
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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.startBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.RUNNING, model.getProjectionStatusCode().getCode());

		verify(batchMappingService, times(1)).startProjectionInBatch(any());
	}

	@ParameterizedTest
	@ValueSource(strings = { ProjectionStatusCodeModel.RUNNING, ProjectionStatusCodeModel.READY })
	void queueBatchProjection_notDraft_throwsException(String statusCode) {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, statusCode);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		assertThrows(ProjectionStateException.class, () -> service.queueForBatchProjection(actingUser, projectionGUID));
	}

	@Test
	void queueBatchProjection_noPolygonFile_throwsException() throws ProjectionServiceException {
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
				ProjectionValidationException.class, () -> service.queueForBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void queueBatchProjection_noLayerFile_throwsException() throws ProjectionServiceException {
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
				ProjectionValidationException.class, () -> service.queueForBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void queueBatchProjection_noParameters_throwsException() throws ProjectionServiceException {
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
				ProjectionValidationException.class, () -> service.queueForBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void queueBatchProjection_invalidParameters_throwsException() throws ProjectionServiceException {
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
				ProjectionValidationException.class, () -> service.queueForBatchProjection(actingUser, projectionGUID)
		);
	}

	@Test
	void queueBatchProjection_defaultParameters_succeeds() throws ProjectionServiceException {
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
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.QUEUED))
				.thenReturn(statusCode(ProjectionStatusCodeModel.QUEUED));

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.queueForBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.QUEUED, model.getProjectionStatusCode().getCode());

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

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.cancelBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.DRAFT, model.getProjectionStatusCode().getCode());

		verify(batchMappingService, times(1)).cancelProjection(any());
		verify(batchJobPublisher, never()).deleteQueuedRequest(any());
	}

	@Test
	void cancelBatchProcessing_queuedDeletesNatsRequest_statusDraft() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.QUEUED);
		VDYPUserModel actingUser = new VDYPUserModel();

		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(batchJobPublisher.deleteQueuedRequest(projectionGUID)).thenReturn(true);
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.DRAFT))
				.thenReturn(statusCode(ProjectionStatusCodeModel.DRAFT));

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.cancelBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.DRAFT, model.getProjectionStatusCode().getCode());

		verify(batchJobPublisher, times(1)).deleteQueuedRequest(projectionGUID);
		verify(batchMappingService, never()).cancelProjection(any());
	}

	@Test
	void cancelBatchProcessing_queuedFallsBackToBatchCancel_whenNatsRequestMissing() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(projectionGUID, ownerId, ProjectionStatusCodeModel.QUEUED);
		VDYPUserModel actingUser = new VDYPUserModel();

		actingUser.setVdypUserGUID(entity.getOwnerUser().getVdypUserGUID().toString());

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(batchJobPublisher.deleteQueuedRequest(projectionGUID)).thenReturn(false);
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.DRAFT))
				.thenReturn(statusCode(ProjectionStatusCodeModel.DRAFT));

		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());
		ProjectionModel model = service.cancelBatchProjection(actingUser, projectionGUID);
		assertEquals(ProjectionStatusCodeModel.DRAFT, model.getProjectionStatusCode().getCode());

		verify(batchJobPublisher, times(1)).deleteQueuedRequest(projectionGUID);
		verify(batchMappingService, times(1)).cancelProjection(entity);
	}

	@Test
	void cleanupExpiredProjections_doesNothing_whenNoExpiredProjections() {
		when(expiryConfig.daysUntilExpiry()).thenReturn(15);
		when(repository.findExpiredIDs(anyInt(), eq(15))).thenReturn(List.of());
		service.cleanupExpiredProjections();

		verify(repository, never()).delete(any());
	}

	@Test
	void cleanupExpiredProjections_deletesExpiredProjectionsByID() throws ProjectionServiceException {

		UUID expiredId1 = UUID.randomUUID();
		VDYPUserModel systemUser = user(UUID.randomUUID());
		UserTypeCodeModel systemType = new UserTypeCodeModel();
		systemType.setCode(UserTypeCodeModel.SYSTEM);
		systemUser.setUserTypeCode(systemType);

		ProjectionEntity entity = projectionEntity(expiredId1, UUID.randomUUID());

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

		when(expiryConfig.daysUntilExpiry()).thenReturn(15);
		when(repository.findExpiredIDs(anyInt(), eq(15))).thenReturn(List.of(expiredId1)).thenReturn(List.of());
		when(userService.getSystemUser()).thenReturn(systemUser);
		when(repository.findByIdOptional(expiredId1)).thenReturn(Optional.of(entity));
		when(repository.countUsesFileSet(fsId)).thenReturn(0L);
		when(repository.countUsesFileSet(layer.getProjectionFileSetGUID())).thenReturn(0L);
		when(repository.countUsesFileSet(results.getProjectionFileSetGUID())).thenReturn(0L);

		service.cleanupExpiredProjections();

		verify(repository).delete(entity);
		verify(fileSetService).deleteFileSetById(fsId);
		verify(fileSetService).deleteFileSetById(layer.getProjectionFileSetGUID());
		verify(fileSetService).deleteFileSetById(results.getProjectionFileSetGUID());
		verify(batchMappingService).deleteMappingsForProjection(entity);
	}

	@Test
	void cleanupExpiredProjections_skipsFailedProjectionsByID() throws ProjectionServiceException {
		VDYPUserModel systemUser = user(UUID.randomUUID());
		UserTypeCodeModel systemType = new UserTypeCodeModel();
		systemType.setCode(UserTypeCodeModel.SYSTEM);
		systemUser.setUserTypeCode(systemType);

		UUID expiredId1 = UUID.randomUUID();
		UUID expiredId2 = UUID.randomUUID();

		ProjectionEntity entity = projectionEntity(expiredId2, UUID.randomUUID());

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

		when(expiryConfig.daysUntilExpiry()).thenReturn(15);
		when(repository.findExpiredIDs(anyInt(), eq(15))).thenReturn(List.of(expiredId1, expiredId2))
				.thenReturn(List.of(expiredId1));
		when(userService.getSystemUser()).thenReturn(systemUser);
		when(repository.findByIdOptional(expiredId1)).thenReturn(Optional.empty());
		when(repository.findByIdOptional(expiredId2)).thenReturn(Optional.of(entity));

		service.cleanupExpiredProjections();

		verify(repository).delete(entity);
		verify(fileSetService).deleteFileSetById(fsId);
		verify(fileSetService).deleteFileSetById(layer.getProjectionFileSetGUID());
		verify(fileSetService).deleteFileSetById(results.getProjectionFileSetGUID());
		verify(batchMappingService).deleteMappingsForProjection(entity);
	}

	ObjectMapper mapper = new ObjectMapper();

	@Test
	void duplicateProjection_nullModelParameters_callsCopyFileSets()
			throws ProjectionServiceException, JsonProcessingException {
		UUID projectionId = UUID.randomUUID();
		UUID newProjectionId = UUID.randomUUID();

		UUID ownerId = UUID.randomUUID();
		VDYPUserModel actingUser = user(ownerId);
		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		String reportTitle = "Test Report";

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		Parameters parameters = new Parameters();
		parameters.setReportTitle(reportTitle);
		entity.setProjectionParameters(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters));

		ProjectionEntity newEntity = projectionEntity(newProjectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.RUNNING);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));
		when(repository.findByIdOptional(newProjectionId)).thenReturn(Optional.of(newEntity));

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

		ProjectionFileSetEntity polyFileSetEntity = fileSetEntity(polyId);
		ProjectionFileSetEntity layerFileSetEntity = fileSetEntity(layerId);

		when(em.find(ProjectionFileSetEntity.class, polyId)).thenReturn(polyFileSetEntity);
		when(em.find(ProjectionFileSetEntity.class, layerId)).thenReturn(layerFileSetEntity);
		when(em.find(ProjectionFileSetEntity.class, resultsId)).thenReturn(fileSetEntity(resultsId));

		newEntity.setPolygonFileSet(polyFileSetEntity);
		newEntity.setLayerFileSet(layerFileSetEntity);
		// Simulate DB-generated ID / app-assigned ID
		doAnswer(inv -> {
			ProjectionEntity e = inv.getArgument(0, ProjectionEntity.class);
			e.setProjectionGUID(newProjectionId);
			return null;
		}).when(repository).persist(any(ProjectionEntity.class));

		ProjectionModel model = service.duplicateProjection(projectionId, user(ownerId));

		assertNotNull(model);
		assertEquals(newProjectionId.toString(), model.getProjectionGUID());
		assertEquals(reportTitle + " - COPY", model.getReportTitle());

		verify(fileSetService).duplicateFilesFromTo(any(), eq(polyFileSetEntity));
		verify(fileSetService).duplicateFilesFromTo(any(), eq(layerFileSetEntity));
	}

	@Test
	void duplicateProjection_allNullParams_completes() throws ProjectionServiceException {
		UUID projectionId = UUID.randomUUID();
		UUID newProjectionId = UUID.randomUUID();

		UUID ownerId = UUID.randomUUID();
		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		ProjectionEntity newEntity = projectionEntity(newProjectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.READY);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));
		when(repository.findByIdOptional(newProjectionId)).thenReturn(Optional.of(newEntity));

		// Simulate DB-generated ID / app-assigned ID
		doAnswer(inv -> {
			ProjectionEntity e = inv.getArgument(0, ProjectionEntity.class);
			e.setProjectionGUID(newProjectionId);
			return null;
		}).when(repository).persist(any(ProjectionEntity.class));

		ProjectionModel model = service.duplicateProjection(projectionId, user(ownerId));

		assertNotNull(model);
		assertEquals(newProjectionId.toString(), model.getProjectionGUID());
	}

	@Test
	void duplicateProjection_providedModelParams_doesNotCopyFiles()
			throws ProjectionServiceException, JsonProcessingException {
		UUID projectionId = UUID.randomUUID();
		UUID newProjectionId = UUID.randomUUID();

		UUID ownerId = UUID.randomUUID();
		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		String reportTitle = "Test Report";
		ProjectionEntity entity = projectionEntity(projectionId, ownerId);
		Parameters parameters = new Parameters();
		parameters.setReportTitle(reportTitle);
		entity.setProjectionParameters(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters));
		entity.setModelParameters("{\"species\":[{\"code\":\"AC\",\"percent\":100.0}]}");

		ProjectionEntity newEntity = projectionEntity(newProjectionId, ownerId);

		var statusEntity = new ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity();
		statusEntity.setCode(ProjectionStatusCodeModel.READY);
		entity.setProjectionStatusCode(statusEntity);

		when(repository.findByIdOptional(projectionId)).thenReturn(Optional.of(entity));
		when(repository.findByIdOptional(newProjectionId)).thenReturn(Optional.of(newEntity));

		// Simulate DB-generated ID / app-assigned ID
		doAnswer(inv -> {
			ProjectionEntity e = inv.getArgument(0, ProjectionEntity.class);
			e.setProjectionGUID(newProjectionId);
			return null;
		}).when(repository).persist(any(ProjectionEntity.class));

		ProjectionModel model = service.duplicateProjection(projectionId, user(ownerId));

		assertNotNull(model);
		assertEquals(newProjectionId.toString(), model.getProjectionGUID());
	}

	// ==========================================================
	// ProjectionEndpoint - projectionHcsvPost
	// ==========================================================

	@Test
	void endpoint_projectionHcsvPost_nullPolygonData_returnsBadRequest() throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);
		FileUpload layerUpload = mock(FileUpload.class);

		Response response = endpoint.projectionHcsvPost(false, new Parameters(), null, layerUpload);

		assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity()).isEqualTo("Projection request failed: no polygon data supplied");
		verify(mockService, never()).projectionHcsvPost(
				any(Boolean.class), any(Parameters.class), any(Path.class), any(Path.class), any(SecurityContext.class)
		);
	}

	@Test
	void endpoint_projectionHcsvPost_nullLayerData_returnsBadRequest() throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);
		FileUpload polygonUpload = mock(FileUpload.class);

		Response response = endpoint.projectionHcsvPost(false, new Parameters(), polygonUpload, null);

		assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getEntity()).isEqualTo("Projection request failed: no layer data supplied");
		verify(mockService, never()).projectionHcsvPost(
				any(Boolean.class), any(Parameters.class), any(Path.class), any(Path.class), any(SecurityContext.class)
		);
	}

	@Test
	void endpoint_projectionHcsvPost_passesUploadedFilePathsToService(@TempDir Path tempDir) throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		FileUpload polygonUpload = mock(FileUpload.class);
		FileUpload layerUpload = mock(FileUpload.class);
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");
		Parameters parameters = new Parameters().ageStart(0).ageEnd(100);
		Response expectedResponse = Response.ok("projection-output").build();

		when(polygonUpload.uploadedFile()).thenReturn(polygonFile);
		when(layerUpload.uploadedFile()).thenReturn(layerFile);
		when(mockService.projectionHcsvPost(true, parameters, polygonFile, layerFile, null))
				.thenReturn(expectedResponse);

		Response response = endpoint.projectionHcsvPost(true, parameters, polygonUpload, layerUpload);

		assertThat(response).isSameAs(expectedResponse);
		verify(mockService).projectionHcsvPost(true, parameters, polygonFile, layerFile, null);
	}

	@Test
	void endpoint_projectionHcsvPost_validationException_returnsSerializedValidationMessages(@TempDir Path tempDir)
			throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		FileUpload polygonUpload = mock(FileUpload.class);
		FileUpload layerUpload = mock(FileUpload.class);
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");
		Parameters parameters = new Parameters();
		String validationMessage = "Polygon file exceeds maximum polygon count of 1.";

		when(polygonUpload.uploadedFile()).thenReturn(polygonFile);
		when(layerUpload.uploadedFile()).thenReturn(layerFile);
		when(mockService.projectionHcsvPost(false, parameters, polygonFile, layerFile, null)).thenThrow(
				new ProjectionRequestValidationException(
						List.of(new ValidationMessage(ValidationMessageKind.GENERIC, validationMessage))
				)
		);

		Response response = endpoint.projectionHcsvPost(false, parameters, polygonUpload, layerUpload);

		assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
		assertThat(response.getHeaderString("content-type")).isEqualTo("application/json");
		assertThat(response.getEntity()).isInstanceOf(String.class);
		assertThat((String) response.getEntity()).contains(validationMessage);
	}

	@Test
	void endpoint_projectionHcsvPost_unexpectedException_returnsInternalServerError(@TempDir Path tempDir)
			throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		FileUpload polygonUpload = mock(FileUpload.class);
		FileUpload layerUpload = mock(FileUpload.class);
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");
		Parameters parameters = new Parameters();

		when(polygonUpload.uploadedFile()).thenReturn(polygonFile);
		when(layerUpload.uploadedFile()).thenReturn(layerFile);
		when(mockService.projectionHcsvPost(false, parameters, polygonFile, layerFile, null))
				.thenThrow(new RuntimeException("service failed"));

		Response response = endpoint.projectionHcsvPost(false, parameters, polygonUpload, layerUpload);

		assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		assertThat(response.getEntity()).isEqualTo("service failed");
	}

	// ==========================================================
	// ProjectionEndpoint - createEmptyProjection
	// ==========================================================

	@Test
	void endpoint_createEmptyProjection_returnsCreated_andPassesReportDescription() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		Parameters params = new Parameters();
		String reportDescription = "My report description";

		ProjectionModel created = new ProjectionModel();
		created.setProjectionGUID(UUID.randomUUID().toString());
		when(mockService.createNewProjection(actingUser, params, null, reportDescription)).thenReturn(created);

		Response response = endpoint.createEmptyProjection(params, null, reportDescription);

		assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
		assertThat(response.getEntity()).isSameAs(created);
		verify(mockService).createNewProjection(actingUser, params, null, reportDescription);
	}

	@Test
	void endpoint_createEmptyProjection_returnsCreated_whenReportDescriptionIsNull() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		Parameters params = new Parameters();

		ProjectionModel created = new ProjectionModel();
		created.setProjectionGUID(UUID.randomUUID().toString());
		when(mockService.createNewProjection(actingUser, params, null, null)).thenReturn(created);

		Response response = endpoint.createEmptyProjection(params, null, null);

		assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
		verify(mockService).createNewProjection(actingUser, params, null, null);
	}

	// ==========================================================
	// ProjectionEndpoint - editProjectionParameters
	// ==========================================================

	@Test
	void endpoint_editProjectionParameters_returnsOk_andPassesReportDescription() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		Parameters params = new Parameters();
		String reportDescription = "Updated description";

		ProjectionModel updated = new ProjectionModel();
		updated.setProjectionGUID(projectionGUID.toString());
		when(mockService.editProjectionParameters(projectionGUID, params, null, reportDescription, actingUser))
				.thenReturn(updated);

		Response response = endpoint.editProjectionParameters(projectionGUID, params, null, reportDescription);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(response.getEntity()).isSameAs(updated);
		verify(mockService).editProjectionParameters(projectionGUID, params, null, reportDescription, actingUser);
	}

	@Test
	void endpoint_startFileSetFileUpload_returnsCreated() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		UUID fileSetGUID = UUID.randomUUID();
		String filename = "result.zip";

		FileMappingModel model = fileMappingModel(UUID.randomUUID(), UUID.randomUUID());
		when(mockService.startFileSetFileUpload(projectionGUID, fileSetGUID, filename)).thenReturn(model);

		Response response = endpoint.startFileSetFileUpload(projectionGUID, fileSetGUID, filename);

		assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
		assertThat(response.getEntity()).isSameAs(model);
		verify(mockService).startFileSetFileUpload(projectionGUID, fileSetGUID, filename);
	}

	@Test
	void endpoint_completeFileSetFileUpload_returnsOk() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		UUID fileSetGUID = UUID.randomUUID();
		UUID fileMappingGUID = UUID.randomUUID();

		FileMappingModel model = fileMappingModel(fileMappingGUID, UUID.randomUUID());
		when(mockService.completeFileSetFileUpload(projectionGUID, fileSetGUID, fileMappingGUID)).thenReturn(model);

		Response response = endpoint.completeFileSetFileUpload(projectionGUID, fileSetGUID, fileMappingGUID);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(response.getEntity()).isSameAs(model);
		verify(mockService).completeFileSetFileUpload(projectionGUID, fileSetGUID, fileMappingGUID);
	}

	@Test
	void endpoint_editProjectionParameters_returnsOk_whenReportDescriptionIsNull() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		Parameters params = new Parameters();

		ProjectionModel updated = new ProjectionModel();
		updated.setProjectionGUID(projectionGUID.toString());
		when(mockService.editProjectionParameters(projectionGUID, params, null, null, actingUser)).thenReturn(updated);

		Response response = endpoint.editProjectionParameters(projectionGUID, params, null, null);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		verify(mockService).editProjectionParameters(projectionGUID, params, null, null, actingUser);
	}

	@Test
	void enrichModel_nullModel_doesNothing() {
		ProjectionModel model = service.toRichModel(null, null);
		assertNull(model);
	}

	@Test
	void enrichModel_nullMapping_getsAModel() {
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), UUID.randomUUID());
		ProjectionModel model = service.toRichModel(entity, null);
		assertNotNull(model);
		assertEquals(entity.getProjectionGUID(), UUID.fromString(model.getProjectionGUID()));
		assertNull(model.getBatchMapping());
	}

	@Test
	void enrichModel_withMappingThatDoesNotContianProjection_getsAModel() {
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), UUID.randomUUID());
		Map<UUID, ProjectionBatchMappingModel> batchMappings = new HashMap<>();
		ProjectionModel model = service.toRichModel(entity, batchMappings);
		assertNotNull(model);
		assertEquals(entity.getProjectionGUID(), UUID.fromString(model.getProjectionGUID()));
		assertNull(model.getBatchMapping());
	}

	@Test
	void enrichModel_withMappingContainsProjection_getsAModelWithMapping() {
		ProjectionEntity entity = projectionEntity(UUID.randomUUID(), UUID.randomUUID());
		Map<UUID, ProjectionBatchMappingModel> batchMappings = new HashMap<>();
		ProjectionBatchMappingModel mapping = batchMappingModel(UUID.randomUUID());
		batchMappings.put(entity.getProjectionGUID(), mapping);
		ProjectionModel model = service.toRichModel(entity, batchMappings);
		assertNotNull(model);
		assertEquals(entity.getProjectionGUID(), UUID.fromString(model.getProjectionGUID()));
		assertEquals(mapping, model.getBatchMapping());
	}

	// ==========================================================
	// updateCompleteStatus
	// ==========================================================

	@Test
	void updateCompleteStatus_withNullProgressUpdate_doesNotCallBatchMappingService()
			throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		ProjectionEntity entity = projectionEntity(
				projectionGUID, UUID.randomUUID(), ProjectionStatusCodeModel.RUNNING
		);
		UserTypeCodeModel systemUserType = new UserTypeCodeModel();
		systemUserType.setCode(UserTypeCodeModel.SYSTEM);
		VDYPUserModel systemUser = new VDYPUserModel();
		systemUser.setUserTypeCode(systemUserType);

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.READY))
				.thenReturn(statusCode(ProjectionStatusCodeModel.READY));
		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());

		service.updateCompleteStatus(systemUser, projectionGUID, true, null);

		verify(batchMappingService, never()).updateProgress(any(), any());
	}

	@Test
	void updateCompleteStatus_withProgressUpdate_callsBatchMappingService() throws ProjectionServiceException {
		UUID projectionGUID = UUID.randomUUID();
		ProjectionEntity entity = projectionEntity(
				projectionGUID, UUID.randomUUID(), ProjectionStatusCodeModel.RUNNING
		);
		UserTypeCodeModel systemUserType = new UserTypeCodeModel();
		systemUserType.setCode(UserTypeCodeModel.SYSTEM);
		VDYPUserModel systemUser = new VDYPUserModel();
		systemUser.setUserTypeCode(systemUserType);
		ProjectionProgressUpdate progressUpdate = new ProjectionProgressUpdate(UUID.randomUUID(), 10, 8, 1, 1, 0);

		when(repository.findByIdOptional(projectionGUID)).thenReturn(Optional.of(entity));
		when(projectionStatusCodeLookup.requireEntity(ProjectionStatusCodeModel.READY))
				.thenReturn(statusCode(ProjectionStatusCodeModel.READY));
		when(expiryConfig.expiryFrom(any())).thenReturn(OffsetDateTime.now());

		service.updateCompleteStatus(systemUser, projectionGUID, true, progressUpdate);

		verify(batchMappingService).updateProgress(entity, progressUpdate);
	}

	// ==========================================================
	// ProjectionEndpoint - updateCompleteProjectionStatus
	// ==========================================================

	@Test
	void endpoint_updateCompleteProjectionStatus_returnsOk_withProgressUpdate() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		ProjectionProgressUpdate progressUpdate = new ProjectionProgressUpdate(UUID.randomUUID(), 10, 8, 1, 1, 0);

		ProjectionModel model = new ProjectionModel();
		model.setProjectionGUID(projectionGUID.toString());
		when(mockService.updateCompleteStatus(actingUser, projectionGUID, true, progressUpdate)).thenReturn(model);

		Response response = endpoint.updateCompleteProjectionStatus(projectionGUID, true, progressUpdate);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(response.getEntity()).isSameAs(model);
	}

	@Test
	void endpoint_updateCompleteProjectionStatus_returnsOk_withNullProgressUpdate() throws ProjectionServiceException {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		ProjectionModel model = new ProjectionModel();
		model.setProjectionGUID(projectionGUID.toString());
		when(mockService.updateCompleteStatus(actingUser, projectionGUID, false, null)).thenReturn(model);

		Response response = endpoint.updateCompleteProjectionStatus(projectionGUID, false, null);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		verify(mockService).updateCompleteStatus(actingUser, projectionGUID, false, null);
	}

	// ==========================================================
	// ProjectionEndpoint - streamResultsZip
	// ==========================================================

	@Test
	void endpoint_streamResultsZip_returnsOk_whenResultFilesExist() throws Exception {
		ProjectionService mockService = mock(ProjectionService.class);
		CurrentVDYPUser currentVDYPUser = mock(CurrentVDYPUser.class);
		VDYPUserModel actingUser = user(UUID.randomUUID());
		when(currentVDYPUser.getUser()).thenReturn(actingUser);

		ProjectionEndpoint endpoint = new ProjectionEndpoint(mockService, currentVDYPUser);

		UUID projectionGUID = UUID.randomUUID();
		UUID fileSetGUID = UUID.randomUUID();
		UUID fileMappingGUID = UUID.randomUUID();

		ProjectionEntity entity = new ProjectionEntity();
		entity.setProjectionGUID(projectionGUID);
		entity.setResultFileSet(fileSetEntity(fileSetGUID));

		FileMappingModel fileWithUrl = fileMappingModel(fileMappingGUID, UUID.randomUUID());
		fileWithUrl.setDownloadURL(new URL("http://example.com/result.zip"));

		when(mockService.getProjectionEntity(projectionGUID)).thenReturn(entity);
		when(mockService.getAllFileSetFiles(projectionGUID, fileSetGUID, actingUser)).thenReturn(List.of(fileWithUrl));
		when(mockService.getFileForDownload(projectionGUID, fileSetGUID, fileMappingGUID, actingUser))
				.thenReturn(fileWithUrl);

		Response response = endpoint.streamResultsZip(projectionGUID, null);

		assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
	}
}
