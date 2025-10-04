package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchIOException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

/**
 * Service for executing VDYP forest yield projections on batch data.
 */
@Service
public class VdypProjectionService {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionService.class);

	static {
		PolygonProjectionRunner.initializeSiteIndexCurves();
	}

	// ex) batch-1-partition0-projection-HCSV-2025_10_02_14_06_43_4933
	public static String buildBatchProjectionId(Long jobExecutionId, String partitionName,
			ProjectionRequestKind projectionKind) {
		StringBuilder sb = new StringBuilder("batch-");
		sb.append(jobExecutionId).append("-");
		sb.append(partitionName).append("-");
		sb.append("projection-").append(projectionKind).append("-");
		sb.append(BatchUtils.dateTimeFormatterForFilenames.format(LocalDateTime.now()));
		return sb.toString();
	}

	/**
	 * Performs VDYP projection for multiple BatchRecords in a chunk.
	 * This method processes a collection of complete polygons by
	 * creating combined input streams and running a single projection operation.
	 *
	 * @param batchRecords Collection of BatchRecords to process together
	 * @return Projection result summary for the entire chunk
	 */
	public String performProjectionForChunk(List<BatchRecord> batchRecords, String partitionName,
			Parameters projectionParameters, Long jobExecutionId, String jobBaseDir)
			throws IOException {
		logger.info("Starting VDYP projection for chunk of {} records in partition {}",
				batchRecords.size(), partitionName);

		if (batchRecords.isEmpty()) {
			return "No records to process in chunk";
		}

		Map<String, InputStream> inputStreams = null;
		try {
			Path outputPartitionDir = createOutputPartitionDir(partitionName, jobBaseDir);

			// Create combined input streams from all BatchRecords in the chunk
			inputStreams = createCombinedInputStreamsFromChunk(batchRecords);

			String batchProjectionId = buildBatchProjectionId(jobExecutionId, partitionName,
					ProjectionRequestKind.HCSV);

			try (ProjectionRunner runner = new ProjectionRunner(
					ProjectionRequestKind.HCSV, batchProjectionId, projectionParameters, false)) {

				logger.info("Running HCSV projection {} for chunk of {} records in partition {}",
						batchProjectionId, batchRecords.size(), partitionName);

				// Run the projection on the combined chunk data
				runner.run(inputStreams);

				// Store intermediate results for all records in chunk
				storeChunkIntermediateResults(runner, outputPartitionDir, batchProjectionId, batchRecords);

				String result = String.format(
						"Chunk projection completed for %d records in partition %s. Results stored",
						batchRecords.size(), partitionName);

				logger.info(
						"VDYP chunk projection completed for {} records in partition {}. Intermediate results stored",
						batchRecords.size(), partitionName);

				return result;

			}

		} catch (AbstractProjectionRequestException e) {
			throw handleChunkProjectionFailure(batchRecords, partitionName, e);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw BatchException.handleProjectionFailure(
					partitionName, batchRecords.size(), e, "Unexpected error during chunk projection", logger);
		} finally {
			if (inputStreams != null) {
				for (var entry : inputStreams.entrySet()) {
					Utils.close(entry.getValue(), entry.getKey());
				}
			}
		}
	}

	/**
	 * Creates a partition-specific output directory within the existing
	 * job-specific parent folder
	 */
	private Path createOutputPartitionDir(String partitionName, String jobBaseDir) throws IOException {
		if (jobBaseDir == null || jobBaseDir.trim().isEmpty()) {
			throw new IOException("Job base directory cannot be null or empty");
		}
		if (partitionName == null || partitionName.trim().isEmpty()) {
			throw new IOException("Partition name cannot be null or empty");
		}

		Path jobBasePath = Paths.get(jobBaseDir);

		// Convert input-partition name to output-partition name
		// inputPartitionName format: "input-partition0" -> "output-partition0"
		String inputPartitionName = BatchConstants.Partition.INPUT_PREFIX + "-" + partitionName;
		String outputPartitionName = inputPartitionName.replace(BatchConstants.Partition.INPUT_FOLDER_NAME_PREFIX,
				BatchConstants.Partition.OUTPUT_FOLDER_NAME_PREFIX);

		Path outputPartitionDir = jobBasePath.resolve(outputPartitionName);

		try {
			Files.createDirectories(outputPartitionDir);
		} catch (IOException e) {
			throw BatchIOException.handleIOException(
					outputPartitionDir, e,
					String.format("Failed to create output partition directory (job folder: %s)", jobBasePath),
					logger);
		}

		logger.info("Created output partition directory: {} for input partition: {} within job folder: {}",
				outputPartitionName, inputPartitionName, jobBasePath.getFileName());

		return outputPartitionDir;
	}

	/**
	 * Creates combined input streams from all BatchRecords in a chunk.
	 * This method combines all polygon and layer data into unified streams.
	 */
	private Map<String, InputStream> createCombinedInputStreamsFromChunk(List<BatchRecord> batchRecords)
			throws IOException {

		if (batchRecords.isEmpty()) {
			throw new IOException("Cannot create input streams from empty chunk");
		}

		return createCombinedInputStreamsFromRawData(batchRecords);
	}

	/**
	 * Creates combined input streams from raw CSV data in BatchRecords.
	 */
	private Map<String, InputStream> createCombinedInputStreamsFromRawData(List<BatchRecord> batchRecords)
			throws IOException {
		Map<String, InputStream> inputStreams = new HashMap<>();

		StringBuilder polygonCsv = new StringBuilder();
		StringBuilder layerCsv = new StringBuilder();

		// Add headers from first record
		if (!batchRecords.isEmpty()) {
			BatchRecord firstRecord = batchRecords.get(0);
			if (firstRecord.getPolygonHeader() != null) {
				polygonCsv.append(firstRecord.getPolygonHeader()).append("\n");
			}
			if (firstRecord.getLayerHeader() != null) {
				layerCsv.append(firstRecord.getLayerHeader()).append("\n");
			}
		}

		// Add all polygon and layer data
		for (BatchRecord batchRecord : batchRecords) {
			if (batchRecord.getRawPolygonData() != null) {
				polygonCsv.append(batchRecord.getRawPolygonData()).append("\n");
			}

			if (batchRecord.getRawLayerData() != null) {
				for (String layerLine : batchRecord.getRawLayerData()) {
					layerCsv.append(layerLine).append("\n");
				}
			}
		}

		// Validate - meaningful data
		if (polygonCsv.isEmpty() || layerCsv.isEmpty()) {
			throw new IOException(String.format(
					"Combined CSV data is empty or invalid (Polygon: %d bytes, Layer: %d bytes)",
					polygonCsv.length(), layerCsv.length()));
		}

		// Create input streams
		inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA,
				new ByteArrayInputStream(polygonCsv.toString().getBytes()));
		inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA,
				new ByteArrayInputStream(layerCsv.toString().getBytes()));

		logger.debug(
				"Created combined input streams from raw CSV data for chunk of {} records (Polygon: {} bytes, Layers: {} bytes)",
				batchRecords.size(), polygonCsv.length(), layerCsv.length());

		return inputStreams;
	}

	/**
	 * Handles VDYP chunk projection failures by logging with context and creating
	 * IOException.
	 */
	private IOException handleChunkProjectionFailure(List<BatchRecord> batchRecords, String partitionName,
			Exception cause) {
		String featureIds = batchRecords.stream()
				.map(BatchRecord::getFeatureId)
				.limit(5) // Show first 5 feature IDs
				.collect(Collectors.joining(", "));

		if (batchRecords.size() > 5) {
			featureIds += " and " + (batchRecords.size() - 5) + " more";
		}

		String contextualMessage = String.format(
				"VDYP chunk projection failed for %d records in partition %s (FEATURE_IDs: %s). Exception type: %s, Root cause: %s",
				batchRecords.size(),
				partitionName,
				featureIds,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : BatchConstants.ErrorMessage.NO_ERROR_MESSAGE);

		logger.error(contextualMessage, cause);

		return new IOException(contextualMessage, cause);
	}

	/**
	 * Stores intermediate results for all records in a chunk.
	 */
	private void storeChunkIntermediateResults(
			ProjectionRunner runner, Path partitionOutputDir, String projectionId,
			List<BatchRecord> batchRecords)
			throws IOException {

		logger.debug("Storing intermediate results for chunk projection {} ({} records)", projectionId,
				batchRecords.size());

		storeChunkYieldTables(runner, partitionOutputDir, projectionId, batchRecords);

		storeChunkLogs(runner, partitionOutputDir, projectionId, batchRecords);

		logger.debug(
				"Successfully stored intermediate results for chunk projection {} ({} records) in {}",
				projectionId, batchRecords.size(), partitionOutputDir);
	}

	/**
	 * Stores yield tables from chunk projection.
	 */
	private void storeChunkYieldTables(ProjectionRunner runner, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		if (runner == null || runner.getContext() == null) {
			logger.warn("Cannot store yield tables: ProjectionRunner or context is null");
			return;
		}

		var yieldTables = runner.getContext().getYieldTables();
		if (yieldTables == null || yieldTables.isEmpty()) {
			logger.debug("No yield tables to store for projection {}", projectionId);
			return;
		}

		for (YieldTable yieldTable : yieldTables) {
			storeYieldTable(yieldTable, partitionDir, projectionId, batchRecords);
		}
	}

	/**
	 * Stores a single yield table file.
	 */
	private void storeYieldTable(YieldTable yieldTable, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		if (yieldTable == null) {
			logger.warn("Skipping null yield table in projection {}", projectionId);
			return;
		}

		String yieldTableFileName = yieldTable.getOutputFormat().getYieldTableFileName();
		// Add chunk prefix to maintain traceability
		String prefixedFileName = String.format("YieldTables_%s_%s", projectionId, yieldTableFileName);
		Path yieldTablePath = partitionDir.resolve(prefixedFileName);

		try (InputStream yieldTableStream = yieldTable.getAsStream()) {
			if (yieldTableStream == null) {
				logger.warn("Skipping yield table with null stream: {}", prefixedFileName);
				return;
			}
			Files.copy(yieldTableStream, yieldTablePath, StandardCopyOption.REPLACE_EXISTING);
			logger.trace("Stored chunk yield table: {} for {} records", prefixedFileName, batchRecords.size());
		} catch (IOException e) {
			throw BatchIOException.handleFileCopyFailure(
					yieldTablePath, e, "Failed to store yield table", logger);
		}
	}

	/**
	 * Stores log files from chunk projection.
	 */
	private void storeChunkLogs(ProjectionRunner runner, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		if (runner == null || runner.getContext() == null || runner.getContext().getParams() == null) {
			logger.warn("Cannot store logs: ProjectionRunner, context, or params is null");
			return;
		}

		ValidatedParameters params = runner.getContext().getParams();

		// Store progress log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
			storeProgressLog(runner, partitionDir, projectionId, batchRecords);
		}

		// Store error log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
			storeErrorLog(runner, partitionDir, projectionId, batchRecords);
		}

		// Store debug log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
			storeDebugLog(partitionDir, projectionId, batchRecords);
		}
	}

	private void storeProgressLog(ProjectionRunner runner, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		String progressLogFileName = String.format("YieldTables_%s_ProgressLog.txt", projectionId);
		Path progressLogPath = partitionDir.resolve(progressLogFileName);

		try (InputStream progressStream = runner.getProgressStream()) {
			if (progressStream != null) {
				Files.copy(progressStream, progressLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored chunk progress log: {} for {} records", progressLogFileName, batchRecords.size());
			} else {
				logger.warn("Progress stream is null, skipping progress log: {}", progressLogFileName);
			}
		} catch (IOException e) {
			throw BatchIOException.handleFileWriteFailure(
					progressLogPath, e, "Failed to store progress log", logger);
		}
	}

	private void storeErrorLog(ProjectionRunner runner, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		String errorLogFileName = String.format("YieldTables_%s_ErrorLog.txt", projectionId);
		Path errorLogPath = partitionDir.resolve(errorLogFileName);

		try (InputStream errorStream = runner.getErrorStream()) {
			if (errorStream != null) {
				Files.copy(errorStream, errorLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored chunk error log: {} for {} records", errorLogFileName, batchRecords.size());
			} else {
				logger.warn("Error stream is null, skipping error log: {}", errorLogFileName);
			}
		} catch (IOException e) {
			throw BatchIOException.handleFileWriteFailure(
					errorLogPath, e, "Failed to store error log", logger);
		}
	}

	private void storeDebugLog(Path partitionDir, String projectionId, List<BatchRecord> batchRecords)
			throws IOException {
		String debugLogFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = partitionDir.resolve(debugLogFileName);

		try {
			Files.write(debugLogPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.trace("Created chunk debug log placeholder: {} for {} records", debugLogFileName,
					batchRecords.size());
		} catch (IOException e) {
			throw BatchIOException.handleFileWriteFailure(
					debugLogPath, e, "Failed to create debug log placeholder", logger);
		}
	}
}
