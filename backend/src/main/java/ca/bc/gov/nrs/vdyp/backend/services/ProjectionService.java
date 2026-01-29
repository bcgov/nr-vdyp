
package ca.bc.gov.nrs.vdyp.backend.services;

import static ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionStateException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionUnauthorizedException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.ModelParameters;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.Exceptions;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRequestParametersValidator;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Implements the projection endpoints. These methods return Responses rather than Response objects because these
 * responses are not JSON objects and contain no links.
 */
@ApplicationScoped
public class ProjectionService {

	public static final Logger logger = LoggerFactory.getLogger(ProjectionService.class);
	private final EntityManager em;
	private final ProjectionResourceAssembler assembler;
	private final ProjectionRepository repository;
	private final ProjectionFileSetService fileSetService;
	private final ProjectionBatchMappingService batchMappingService;
	private final ProjectionStatusCodeLookup statusLookup;
	private final CalculationEngineCodeLookup calclationEngineLookup;
	private final ObjectMapper objectMapper;

	private static final String FILE_SET_IDENTIFIER = "file set";
	private static final String FILE_IDENTIFIER = "file";
	private static final String FILE_ADD_ERROR = "Error adding file";
	private static final String FILE_DELETE_ERROR = "Error deleting file";
	private static final String FILE_GET_ERROR = "Error getting file";
	private static final String FILES_DELETE_ERROR = "Error deleting files";

	public ProjectionService(
			EntityManager em, ProjectionResourceAssembler assembler, ProjectionRepository repository,
			ProjectionFileSetService fileSetService, ProjectionBatchMappingService batchMappingService,
			ProjectionStatusCodeLookup statusLookup, CalculationEngineCodeLookup calclationEngineLookup,
			ObjectMapper objectMapper
	) {
		this.em = em;
		this.assembler = assembler;
		this.repository = repository;
		this.fileSetService = fileSetService;
		this.batchMappingService = batchMappingService;
		this.statusLookup = statusLookup;
		this.calclationEngineLookup = calclationEngineLookup;
		this.objectMapper = objectMapper;
	}

	static {
		// FIXME Would be better if we moved the stateful parts of the SINDEX library to an instanced singleton.
		// See VDYP-732
		PolygonProjectionRunner.initializeSiteIndexCurves();
	}

	public Response projectionHcsvPost(
			Boolean trialRun, //
			Parameters parameters, //
			InputStream polygonStream, //
			InputStream layersStream, //
			SecurityContext securityContext
	) throws AbstractProjectionRequestException {
		Response response;

		Map<String, InputStream> inputStreams = new HashMap<>();

		try {
			inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStream);
			inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layersStream);

			response = runProjection(ProjectionRequestKind.HCSV, inputStreams, trialRun, parameters, securityContext);
		} finally {
			for (var entry : inputStreams.entrySet()) {
				Utils.close(entry.getValue(), entry.getKey());
			}
		}

		return response;
	}

	@SuppressWarnings("unused")
	public Response projectionDcsvPost(
			Parameters parameters, FileUpload dcsvDataStream, Boolean trialRun, SecurityContext securityContext
	) throws ProjectionRequestValidationException, PolygonExecutionException {
		return Response.serverError().status(Status.NOT_IMPLEMENTED).build();
	}

	@SuppressWarnings("unused")
	public Response projectionScsvPost(
			Boolean trialRun, Parameters parameters, FileUpload polygonDataStream, FileUpload layersDataStream,
			FileUpload historyDataStream, FileUpload nonVegetationDataStream, FileUpload otherVegetationDataStream,
			FileUpload polygonIdDataStream, FileUpload speciesDataStream, FileUpload vriAdjustDataStream, Object object
	) throws ProjectionRequestValidationException, PolygonExecutionException {
		return Response.serverError().status(Status.NOT_IMPLEMENTED).build();
	}

	public Response runProjection(
			ProjectionRequestKind kind, Map<String, InputStream> inputStreams, Boolean isTrialRun, Parameters params,
			SecurityContext securityContext
	) throws AbstractProjectionRequestException {
		String projectionId = ProjectionService.buildProjectionId(kind);

		logger.info("<runProjection {} {}", kind, projectionId);

		try {
			// Included to generate JSON text of parameters as needed
			String serializedParametersText = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(params);
			logger.info(serializedParametersText);
		} catch (JsonProcessingException e) {
			logger.warn(MessageFormat.format("{0}: unable to log parameters JSON", projectionId), e);
		}

		boolean debugLoggingEnabled = params.getSelectedExecutionOptions()
				.contains(Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING.toString());
		if (debugLoggingEnabled) {
			MDC.put("projectionId", projectionId);
		}

		Response response;

		try (
				ProjectionRunner runner = new ProjectionRunner(
						ProjectionRequestKind.HCSV, projectionId, params, isTrialRun
				)
		) {
			logger.info("Running {} projection {}", kind, projectionId);

			runner.run(inputStreams);

			InputStream debugLogStream = new ByteArrayInputStream(new byte[0]);
			try {
				if (debugLoggingEnabled) {
					/* this is known from logback.xml */
					Path debugLogPath = Path.of("logs", projectionId + ".log");

					debugLogStream = FileHelper.getForReading(debugLogPath);
				}
			} catch (IOException e) {
				String message = Exceptions.getMessage(e, "Projection, when opening input files,");
				logger.warn(message);
			}

			response = buildOutputZipFile(runner, debugLogStream);

		} finally {
			logger.info(FINALIZE_SESSION_MARKER, ">runProjection {} {}", kind, projectionId);

			if (debugLoggingEnabled) {
				MDC.remove("projectionId");
			}
		}

		return response;
	}

	private static final DateTimeFormatter dateTimeFormatterForFilenames = DateTimeFormatter
			.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

	public static String buildProjectionId(ProjectionRequestKind projectionKind) {
		StringBuilder sb = new StringBuilder("projection-");
		sb.append(projectionKind).append("-");
		sb.append(dateTimeFormatterForFilenames.format(LocalDateTime.now()));
		return sb.toString();
	}

	private Response buildOutputZipFile(ProjectionRunner runner, InputStream debugLogStream) {
		logger.info("<buildOutputZipFile");

		InputStream yieldTableStream = null;
		InputStream progressLogStream = null;
		InputStream errorLogStream = null;

		try {
			var baos = new ByteArrayOutputStream();
			try (var zipOut = new ZipOutputStream(baos)) {

				progressLogStream = runner.getProgressStream();
				errorLogStream = runner.getErrorStream();

				for (YieldTable yieldTable : runner.getContext().getYieldTables()) {
					var yieldTableFileName = yieldTable.getOutputFormat().getYieldTableFileName();
					yieldTableStream = yieldTable.getAsStream();
					writeZipEntry(zipOut, yieldTableFileName, yieldTableStream.readAllBytes());
				}

				if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
					writeZipEntry(zipOut, "ProgressLog.txt", runner.getProgressStream().readAllBytes());
				}

				if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
					writeZipEntry(zipOut, "ErrorLog.txt", runner.getErrorStream().readAllBytes());
				}

				if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
					writeZipEntry(zipOut, "DebugLog.txt", debugLogStream.readAllBytes());
				}

				var projectionResultsIterator = runner.getProjectionResults();

				while (projectionResultsIterator.hasNext()) {
					var entry = projectionResultsIterator.next();
					var zipEntryName = entry.getKey().toString();
					var projectionResultsFile = Files.newInputStream(entry.getValue(), StandardOpenOption.READ);
					try {
						writeZipEntry(zipOut, zipEntryName, projectionResultsFile.readAllBytes());
					} finally {
						Utils.close(projectionResultsFile, zipEntryName);
					}
				}
			} catch (IOException e) {
				return Response.status(500)
						.entity(
								"Saw IOException when generating results zip file" + e.getMessage() != null
										? ": " + e.getMessage() : ""
						).build();
			}

			byte[] resultingByteArray = baos.toByteArray();

			logger.info("Output Zip file contains {} bytes", resultingByteArray.length);

			logger.info(">buildOutputZipFile");

			var outputFileName = "vdyp-output-" + java.time.LocalDateTime.now().format(dateTimeFormatter) + ".zip";

			return Response.ok(resultingByteArray).status(Status.CREATED)
					.header("content-disposition", "attachment;filename=\"" + outputFileName + "\"")
					.header("content-type", "application/octet-stream").build();

		} catch (Exception e) {
			String message = Exceptions.getMessage(e, "Projection, when creating output zip,");
			logger.error(message, e);

			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
		} finally {
			if (errorLogStream != null) {
				Utils.close(yieldTableStream, "ProjectionService.errorLog");
			}
			if (progressLogStream != null) {
				Utils.close(yieldTableStream, "ProjectionService.progressLog");
			}
			if (yieldTableStream != null) {
				Utils.close(yieldTableStream, "ProjectionService.yieldTable");
			}
		}
	}

	private void writeZipEntry(ZipOutputStream zipOut, String entryName, byte[] entry) throws IOException {
		ZipEntry projectionResultsEntry = new ZipEntry(entryName);
		zipOut.putNextEntry(projectionResultsEntry);
		zipOut.write(entry);
		zipOut.closeEntry();
	}

	public List<ProjectionModel> getAllProjectionsForUser(String vdypUserId) {
		if (vdypUserId == null)
			return Collections.emptyList();
		UUID vdypUserGuid = UUID.fromString(vdypUserId);
		return repository.findByOwner(vdypUserGuid).stream().map(assembler::toModel).toList();
	}

	@Transactional
	public ProjectionModel
			createNewProjection(VDYPUserModel actingUser, Parameters params, ModelParameters modelParameters)
					throws ProjectionServiceException {
		try {
			ProjectionEntity entity = new ProjectionEntity();
			entity.setOwnerUser(em.find(VDYPUserEntity.class, UUID.fromString(actingUser.getVdypUserGUID())));
			extractConvenienceParameters(params, entity);

			entity.setProjectionParameters(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(params));

			entity.setModelParameters(
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(modelParameters)
			);

			// Create the 3 File Sets for the projection Polygon, Layer and Result
			var fileSetMap = fileSetService.createFileSetForNewProjection(actingUser);
			for (var entry : fileSetMap.entrySet()) {
				ProjectionFileSetEntity fse = em.find(
						ProjectionFileSetEntity.class, UUID.fromString(entry.getValue().getProjectionFileSetGUID())
				);
				switch (entry.getKey().getCode()) {
				case FileSetTypeCodeModel.POLYGON -> entity.setPolygonFileSet(fse);
				case FileSetTypeCodeModel.LAYER -> entity.setLayerFileSet(fse);
				case FileSetTypeCodeModel.RESULTS -> entity.setResultFileSet(fse);
				default ->
					throw new ProjectionServiceException("Error creating projection: unknown FileSet Type created");
				}
			}

			// Set the Status to Draft
			entity.setProjectionStatusCode(statusLookup.requireEntity(ProjectionStatusCodeModel.DRAFT));

			// Set the calculation Engine Code to VDYP8
			entity.setCalculationEngineCode(calclationEngineLookup.requireEntity(CalculationEngineCodeModel.VDYP8));

			// Start Date and End Date should be null as they are about running the projection

			repository.persist(entity);
			return assembler.toModel(entity);
		} catch (JsonProcessingException e) {
			throw new ProjectionServiceException("Invalid parameter JSON", e);
		}
	}

	private static void extractConvenienceParameters(Parameters params, ProjectionEntity model) {
		// extract the report Title and description from the parameters
		// leave report title in for processing, remove description
		model.setReportTitle(params.getReportTitle());
		model.setReportDescription(params.getReportTitle()); // TODO update params to be able to read a description
	}

	public ProjectionEntity getProjectionEntity(UUID projectionGuid) throws ProjectionServiceException {
		Optional<ProjectionEntity> entity = repository.findByIdOptional(projectionGuid);
		if (entity.isEmpty()) {
			// Handle Projection does not exist
			throw new ProjectionNotFoundException(projectionGuid);
		}
		return entity.get();
	}

	@Transactional
	public ProjectionModel startBatchProjection(VDYPUserModel user, UUID projectionGUID)
			throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, user, ProjectionAction.UPDATE);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.UPDATE);

		// Check that the Projection has at least one polygon file and layer file
		List<FileMappingModel> files = fileSetService
				.getAllFiles(entity.getPolygonFileSet().getProjectionFileSetGUID(), user, false);
		if (files.isEmpty()) {
			throw new ProjectionValidationException(
					"Cannot start projection: No polygon files have been uploaded.", projectionGUID
			);
		}
		files = fileSetService.getAllFiles(entity.getLayerFileSet().getProjectionFileSetGUID(), user, false);
		if (files.isEmpty()) {
			throw new ProjectionValidationException(
					"Cannot start projection: No layer files have been uploaded.", projectionGUID
			);
		}
		try {
			// Check that the Parameters JSON is valid.
			Parameters params = objectMapper.readValue(entity.getProjectionParameters(), Parameters.class);
			ProjectionRequestParametersValidator.validate(params, ProjectionRequestKind.HCSV);
		} catch (Exception e) {
			throw new ProjectionValidationException("Invalid parameter JSON", e, projectionGUID);
		}
		ProjectionBatchMappingModel batchMappingModel = batchMappingService.startProjectionInBatch(entity);

		// set the status to running
		if (batchMappingModel != null && batchMappingModel.getBatchJobGUID() != null) {
			entity.setProjectionStatusCode(statusLookup.requireEntity(ProjectionStatusCodeModel.RUNNING));
			repository.persist(entity);
		}

		return assembler.toModel(entity);
	}

	public enum ProjectionAction {
		READ, UPDATE, DELETE
	}

	public void checkUserCanPerformAction(ProjectionEntity entity, VDYPUserModel actingUser, ProjectionAction action)
			throws ProjectionServiceException {
		if (actingUser.isSystemUser())
			return;
		switch (action) {
		case READ, UPDATE, DELETE:
			UUID vdypUserGuid = UUID.fromString(actingUser.getVdypUserGUID());
			if (!entity.getOwnerUser().getVdypUserGUID().equals(vdypUserGuid)) {
				throw new ProjectionUnauthorizedException(entity.getProjectionGUID(), vdypUserGuid);
			}
			break;
		}

	}

	public void checkProjectionStatusPermitsAction(ProjectionEntity entity, ProjectionAction action)
			throws ProjectionServiceException {
		if (entity.getProjectionStatusCode().getCode().equals(ProjectionStatusCodeModel.RUNNING)) {
			switch (action) {
			case UPDATE, DELETE:
				// TODO check againsdt some specific states throw ProjectionServiceException if htere is an issue
				throw new ProjectionStateException(
						entity.getProjectionGUID(), action.name(), entity.getProjectionStatusCode().getCode()
				);
			case READ:
				break;
			}

		}
	}

	public ProjectionModel getProjectionByID(UUID projectionGUID, VDYPUserModel actingUser)
			throws ProjectionServiceException {
		ProjectionEntity entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, actingUser, ProjectionAction.READ);
		return assembler.toModel(entity);
	}

	@Transactional
	public ProjectionModel editProjectionParameters(
			UUID projectionGUID, Parameters params, ModelParameters modelParameters, VDYPUserModel actingUser
	) throws ProjectionServiceException {
		ProjectionEntity existingEntity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(existingEntity, actingUser, ProjectionAction.UPDATE);
		checkProjectionStatusPermitsAction(existingEntity, ProjectionAction.UPDATE);

		extractConvenienceParameters(params, existingEntity);
		try {
			// Update the parameters of import
			existingEntity
					.setProjectionParameters(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(params));
			existingEntity.setModelParameters(
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(modelParameters)
			);
		} catch (JsonProcessingException e) {
			throw new ProjectionServiceException(
					"Error updating projection parameters", e, projectionGUID,
					UUID.fromString(actingUser.getVdypUserGUID())
			);
		}
		return assembler.toModel(existingEntity);
	}

	@Transactional
	public void deleteProjection(UUID projectionGUID, VDYPUserModel actingUser) throws ProjectionServiceException {
		ProjectionEntity entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, actingUser, ProjectionAction.DELETE);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.DELETE);

		try {
			// first get the information for other tables that will need to be deleted when this projection is deleted
			UUID[] fileSetIds = { entity.getPolygonFileSet().getProjectionFileSetGUID(),
					entity.getLayerFileSet().getProjectionFileSetGUID(),
					entity.getResultFileSet().getProjectionFileSetGUID() };

			// TODO before the projection is deleted the Batch mapping should be deleted if it exists

			// Delete the Projection
			repository.delete(entity);

			// Delete File Sets
			// the file Set service should cascade into the file mapping service to do the deletions of the actual files
			for (UUID id : fileSetIds) {
				if (repository.countUsesFileSet(id) == 0) {
					fileSetService.deleteFileSetById(id);
				}
			}

		} catch (Exception e) {
			throw new ProjectionServiceException(
					"Error deleting Projection", e, projectionGUID, UUID.fromString(actingUser.getVdypUserGUID())
			);
		}
	}

	private void validateIdentifier(UUID identifier, String message, String identifierType, UUID projectionGUID)
			throws ProjectionServiceException {
		if (identifier == null) {
			throw new ProjectionServiceException(
					message, new IllegalIdentifierException("No " + identifierType + " Provided"), projectionGUID
			);
		}
	}

	public List<FileMappingModel> getAllFileSetFiles(UUID projectionGUID, UUID fileSetGUID, VDYPUserModel user)
			throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, user, ProjectionAction.READ);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.READ);
		// check that the filesetGUID is set and is one of the file sets for this projection
		validateIdentifier(fileSetGUID, FILE_ADD_ERROR, FILE_SET_IDENTIFIER, projectionGUID);
		validateFileSetIsForProjection(entity, fileSetGUID, FILE_ADD_ERROR);
		return fileSetService.getAllFiles(fileSetGUID, user, false);
	}

	@Transactional
	public ProjectionModel addProjectionFile(UUID projectionGUID, UUID fileSetGUID, FileUpload file, VDYPUserModel user)
			throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, user, ProjectionAction.UPDATE);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.UPDATE);

		// check that the filesetGUID is set and is one of the file sets for this projection
		validateIdentifier(fileSetGUID, FILE_ADD_ERROR, FILE_SET_IDENTIFIER, projectionGUID);
		validateFileSetIsForProjection(entity, fileSetGUID, FILE_ADD_ERROR);

		fileSetService.addNewFileToFileSet(fileSetGUID, user, file);
		return getProjectionByID(projectionGUID, user);
	}

	private void validateFileSetIsForProjection(ProjectionEntity entity, UUID fileSetGUID, String message)
			throws ProjectionServiceException {
		if (!fileSetGUID.equals(entity.getPolygonFileSet().getProjectionFileSetGUID())
				&& !fileSetGUID.equals(entity.getLayerFileSet().getProjectionFileSetGUID()) && //
				!fileSetGUID.equals(entity.getResultFileSet().getProjectionFileSetGUID())) {
			throw new ProjectionServiceException(
					message,
					new IllegalIdentifierException(
							String.format("File set %s did not belong to the projection.", fileSetGUID)
					), entity.getProjectionGUID()
			);
		}
	}

	public FileMappingModel
			getFileForDownload(UUID projectionGUID, UUID fileSetGUID, UUID fileGUID, VDYPUserModel actingUser)
					throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, actingUser, ProjectionAction.READ);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.UPDATE);
		// check that the filesetGUID is set and is one of the file sets for this projection
		validateIdentifier(fileSetGUID, FILE_GET_ERROR, FILE_SET_IDENTIFIER, projectionGUID);
		validateIdentifier(fileGUID, FILE_GET_ERROR, FILE_IDENTIFIER, projectionGUID);
		validateFileSetIsForProjection(entity, fileSetGUID, FILE_GET_ERROR);
		return fileSetService.getFileForDownload(fileSetGUID, actingUser, fileGUID);
	}

	public void deleteFile(UUID projectionGUID, UUID fileSetGUID, UUID fileGUID, VDYPUserModel actingUser)
			throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, actingUser, ProjectionAction.READ);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.UPDATE);
		// check that the filesetGUID is set and is one of the file sets for this projection
		validateIdentifier(fileSetGUID, FILE_DELETE_ERROR, FILE_SET_IDENTIFIER, projectionGUID);
		validateIdentifier(fileGUID, FILE_DELETE_ERROR, FILE_IDENTIFIER, projectionGUID);
		validateFileSetIsForProjection(entity, fileSetGUID, FILE_DELETE_ERROR);
		fileSetService.deleteFileFromFileSet(fileSetGUID, actingUser, fileGUID);
	}

	public void deleteAllFiles(UUID projectionGUID, UUID fileSetGUID, VDYPUserModel actingUser)
			throws ProjectionServiceException {
		var entity = getProjectionEntity(projectionGUID);
		checkUserCanPerformAction(entity, actingUser, ProjectionAction.READ);
		checkProjectionStatusPermitsAction(entity, ProjectionAction.UPDATE);
		// check that the filesetGUID is set and is one of the file sets for this projection
		validateIdentifier(fileSetGUID, FILES_DELETE_ERROR, FILE_SET_IDENTIFIER, projectionGUID);
		validateFileSetIsForProjection(entity, fileSetGUID, FILES_DELETE_ERROR);
		fileSetService.deleteAllFilesFromFileSet(fileSetGUID, actingUser);
	}

}
