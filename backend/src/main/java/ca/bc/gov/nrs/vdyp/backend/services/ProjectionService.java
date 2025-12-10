
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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.Exceptions;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;
import jakarta.enterprise.context.ApplicationScoped;
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
	private final ProjectionResourceAssembler assembler;
	private final ProjectionRepository repository;

	public ProjectionService(ProjectionResourceAssembler assembler, ProjectionRepository repository) {
		this.assembler = assembler;
		this.repository = repository;
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

	private ObjectMapper jsonObjectMapper = new ObjectMapper();

	private Response runProjection(
			ProjectionRequestKind kind, Map<String, InputStream> inputStreams, Boolean isTrialRun, Parameters params,
			SecurityContext securityContext
	) throws AbstractProjectionRequestException {
		String projectionId = ProjectionService.buildProjectionId(kind);

		logger.info("<runProjection {} {}", kind, projectionId);

		try {
			// Included to generate JSON text of parameters as needed
			String serializedParametersText = jsonObjectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(params);
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

}
