
package ca.bc.gov.nrs.vdyp.backend.services;

import static ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.Exceptions;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
/**
 * Implements the projection endpoints. These methods return Responses rather than Response objects because these
 * responses are not JSON objects and contain no links.
 */
public class ProjectionService {

	private static final Logger logger = LoggerFactory.getLogger(ProjectionService.class);

	public Response projectionHcsvPost(
			Boolean trialRun, //
			Parameters parameters, //
			InputStream polygonStream, //
			InputStream layersStream, //
			SecurityContext securityContext
	) throws ProjectionRequestException {
		Response response;

		Map<String, InputStream> inputStreams = new HashMap<>();

		try {
			if (trialRun) {
				if (polygonStream.available() == 0) {
					polygonStream.close();
					polygonStream = FileHelper
							.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_POLY.csv");
				}

				if (layersStream.available() == 0) {
					layersStream.close();
					layersStream = FileHelper
							.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_LAYER.csv");
				}
			}

			inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStream);
			inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layersStream);

			response = runProjection(ProjectionRequestKind.HCSV, inputStreams, trialRun, parameters, securityContext);
		} catch (IOException e) {
			String message = Exceptions.getMessage(e, "Projection, when opening input files,");

			logger.error(message, e);

			response = Response.serverError().status(500).entity(message).build();
		} finally {
			for (var entry : inputStreams.entrySet()) {
				try {
					entry.getValue().close();
				} catch (IOException e) {
					logger.warn("Unable to close {}; reason: {}", entry.getKey(), e.getMessage());
				}
			}
		}

		return response;
	}

	public Response projectionDcsvPost(
			Parameters parameters, FileUpload dcsvDataStream, Boolean trialRun, SecurityContext securityContext
	) throws ProjectionRequestValidationException, ProjectionInternalExecutionException {
		return Response.serverError().status(501).build();
	}

	public Response projectionScsvPost(
			Boolean trialRun, Parameters parameters, FileUpload polygonDataStream, FileUpload layersDataStream,
			FileUpload historyDataStream, FileUpload nonVegetationDataStream, FileUpload otherVegetationDataStream,
			FileUpload polygonIdDataStream, FileUpload speciesDataStream, FileUpload vriAdjustDataStream, Object object
	) throws ProjectionRequestValidationException, ProjectionInternalExecutionException {
		return Response.serverError().status(501).build();
	}

	private Response runProjection(
			ProjectionRequestKind kind, Map<String, InputStream> inputStreams, Boolean isTrialRun, Parameters params,
			SecurityContext securityContext
	) throws ProjectionRequestException {

		String projectionId = ProjectionService.buildId(kind);

		logger.info("<runProjection {} {}", kind, projectionId);

		boolean debugLoggingEnabled = params.getSelectedExecutionOptions()
				.contains(Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING.toString());
		if (debugLoggingEnabled) {
			MDC.put("projectionId", projectionId);
		}

		Response response = Response.serverError().status(500).build();

		try {
			logger.info("Running {} projection {}", kind, projectionId);

			var runner = new ProjectionRunner(ProjectionRequestKind.HCSV, projectionId, params, isTrialRun);

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

		} catch (Exception e) {
			logger.error("Failure in runProjection", e);
		} finally {
			logger.info(FINALIZE_SESSION_MARKER, ">runProjection {} {}", kind, projectionId);

			if (debugLoggingEnabled) {
				MDC.remove("projectionId");
				// TODO: uncomment once mechanism is validated
				// FileHelper.delete(debugLogPath);
			}
		}

		return response;
	}

	private static final DateTimeFormatter dateTimeFormatterForFilenames = DateTimeFormatter
			.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

	public static String buildId(ProjectionRequestKind projectionKind) {
		StringBuilder sb = new StringBuilder("projection-");
		sb.append(projectionKind).append("-");
		sb.append(dateTimeFormatterForFilenames.format(LocalDateTime.now()));
		return sb.toString();
	}

	private Response buildOutputZipFile(ProjectionRunner runner, InputStream debugLogStream) {
		logger.info("<buildOutputZipFile");

		try {
			InputStream yieldTableStream = runner.getYieldTable();
			InputStream progressLogStream = runner.getProgressStream();
			InputStream errorLogStream = runner.getErrorStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zipOut = new ZipOutputStream(baos);

			ZipEntry yieldTableZipEntry = new ZipEntry("YieldTable.csv");
			zipOut.putNextEntry(yieldTableZipEntry);
			zipOut.write(yieldTableStream.readAllBytes());

			ZipEntry logOutputEntry = new ZipEntry("ProgressLog.txt");
			zipOut.putNextEntry(logOutputEntry);
			zipOut.write(progressLogStream.readAllBytes());

			ZipEntry errorOutputZipEntry = new ZipEntry("ErrorLog.txt");
			zipOut.putNextEntry(errorOutputZipEntry);
			zipOut.write(errorLogStream.readAllBytes());

			ZipEntry debugOutputZipEntry = new ZipEntry("DebugLog.txt");
			zipOut.putNextEntry(debugOutputZipEntry);
			zipOut.write(debugLogStream.readAllBytes());

			zipOut.close();

			byte[] resultingByteArray = baos.toByteArray();

			logger.info("Output Zip file contains {} bytes", resultingByteArray.length);

			logger.info(">buildOutputZipFile");

			var outputFileName = "vdyp-output-" + java.time.LocalDateTime.now().format(dateTimeFormatter) + ".zip";

			return Response.ok(resultingByteArray).status(Status.CREATED)
					.header("content-disposition", "attachment;filename=\"" + outputFileName + "\"").build();

		} catch (ProjectionInternalExecutionException | IOException e) {
			String message = Exceptions.getMessage(e, "Projection, when creating output zip,");
			logger.error(message, e);

			return Response.serverError().status(500).entity(message).build();
		}
	}
}
