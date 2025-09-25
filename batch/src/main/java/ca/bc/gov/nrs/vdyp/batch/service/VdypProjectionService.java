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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

/**
 * Service for executing VDYP forest yield projections on batch data.
 * 
 * Provides optimized processing capabilities for large-scale batch operations:
 * - Chunk-based projection processing to handle multiple FEATURE_IDs
 * efficiently
 * - Streaming CSV input/output to minimize memory usage
 * - Integration with VDYP extended-core projection engine
 * - Output file management for partitioned batch results
 */
@Service
public class VdypProjectionService {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionService.class);

	@Value("${batch.output.directory.default-path}")
	private String outputBasePath;

	static {
		PolygonProjectionRunner.initializeSiteIndexCurves();
	}

	public VdypProjectionService() {
		// no initialization required
	}

	/**
	 * Generates a unique projection ID for a chunk in a specific partition
	 */
	private String buildChunkProjectionId(String partitionName, int chunkSize) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
		return String.format(
				"batch-chunk-projection-%s-size-%d-%s", partitionName, chunkSize,
				formatter.format(LocalDateTime.now()));
	}

	/**
	 * Performs VDYP projection for multiple BatchRecords in a chunk.
	 * This method processes a collection of complete polygons efficiently by
	 * creating combined input streams and running a single projection operation.
	 *
	 * @param batchRecords  Collection of BatchRecords to process together
	 * @param partitionName Partition identifier for logging and output organization
	 * @param parameters    VDYP projection parameters
	 * @return Projection result summary for the entire chunk
	 * @throws IOException if projection fails
	 */
	public String performProjectionForChunk(List<BatchRecord> batchRecords, String partitionName,
			Parameters parameters)
			throws IOException {
		logger.info("Starting VDYP projection for chunk of {} records in partition {}",
				batchRecords.size(), partitionName);

		if (batchRecords.isEmpty()) {
			return "No records to process in chunk";
		}

		try {
			// Create partition-specific output directory
			Path partitionOutputDir = createPartitionOutputDir(partitionName);

			// Create combined input streams from all BatchRecords in the chunk
			Map<String, InputStream> inputStreams = createCombinedInputStreamsFromChunk(batchRecords);

			// Generate chunk-specific projection ID
			String projectionId = buildChunkProjectionId(partitionName, batchRecords.size());

			try (ProjectionRunner runner = new ProjectionRunner(
					ProjectionRequestKind.HCSV, projectionId, parameters, false)) {

				logger.info("Running HCSV projection {} for chunk of {} records in partition {}",
						projectionId, batchRecords.size(), partitionName);

				// Run the projection on the combined chunk data
				runner.run(inputStreams);

				// Store intermediate results for all records in chunk
				storeChunkIntermediateResults(runner, partitionOutputDir, projectionId, batchRecords);

				String result = String.format(
						"Chunk projection completed for %d records in partition %s. Results stored",
						batchRecords.size(), partitionName);

				logger.info(
						"VDYP chunk projection completed for {} records in partition {}. Intermediate results stored",
						batchRecords.size(), partitionName);

				return result;

			} finally {
				// Close input streams
				for (var entry : inputStreams.entrySet()) {
					Utils.close(entry.getValue(), entry.getKey());
				}
			}

		} catch (AbstractProjectionRequestException e) {
			throw handleChunkProjectionFailure(batchRecords, partitionName, e);
		}
	}

	/**
	 * Creates a partition-specific output directory
	 */
	private Path createPartitionOutputDir(String partitionName) throws IOException {
		Path baseOutputDir = Paths.get(outputBasePath);
		// partitionName already contains "partition" prefix from DynamicPartitioner
		Path partitionDir = baseOutputDir.resolve(partitionName);
		Files.createDirectories(partitionDir);
		return partitionDir;
	}

	/**
	 * Creates combined input streams from all BatchRecords in a chunk.
	 * This method efficiently combines all polygon and layer data into unified
	 * streams.
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
	private Map<String, InputStream> createCombinedInputStreamsFromRawData(List<BatchRecord> batchRecords) {
		Map<String, InputStream> inputStreams = new HashMap<>();

		// Build combined polygon CSV content
		StringBuilder polygonCsv = new StringBuilder();
		// Add header from first record
		if (!batchRecords.isEmpty() && batchRecords.get(0).getPolygonHeader() != null) {
			polygonCsv.append(batchRecords.get(0).getPolygonHeader()).append("\n");
		}
		// Add all polygon data
		for (BatchRecord batchRecord : batchRecords) {
			if (batchRecord.getRawPolygonData() != null) {
				polygonCsv.append(batchRecord.getRawPolygonData()).append("\n");
			}
		}

		// Build combined layer CSV content
		StringBuilder layerCsv = new StringBuilder();
		// Add header from first record
		if (!batchRecords.isEmpty() && batchRecords.get(0).getLayerHeader() != null) {
			layerCsv.append(batchRecords.get(0).getLayerHeader()).append("\n");
		}
		// Add all layer data
		for (BatchRecord batchRecord : batchRecords) {
			if (batchRecord.getRawLayerData() != null) {
				for (String layerLine : batchRecord.getRawLayerData()) {
					layerCsv.append(layerLine).append("\n");
				}
			}
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
				cause.getMessage() != null ? cause.getMessage() : "No error message available");

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

		// Store files directly in partition directory with chunk information
		// Store yield tables
		storeChunkYieldTables(runner, partitionOutputDir, projectionId, batchRecords);

		// Store logs if enabled
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
		for (YieldTable yieldTable : runner.getContext().getYieldTables()) {
			String yieldTableFileName = yieldTable.getOutputFormat().getYieldTableFileName();
			// Add chunk prefix to maintain traceability
			String prefixedFileName = String.format("YieldTables_CHUNK_%s_%s", projectionId, yieldTableFileName);
			Path yieldTablePath = partitionDir.resolve(prefixedFileName);

			try (InputStream yieldTableStream = yieldTable.getAsStream()) {
				Files.copy(yieldTableStream, yieldTablePath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored chunk yield table: {} for {} records", prefixedFileName, batchRecords.size());
			}
		}
	}

	/**
	 * Stores log files from chunk projection.
	 */
	private void storeChunkLogs(ProjectionRunner runner, Path partitionDir, String projectionId,
			List<BatchRecord> batchRecords) throws IOException {
		// Store progress log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
			String progressLogFileName = String.format("YieldTables_CHUNK_%s_ProgressLog.txt", projectionId);
			Path progressLogPath = partitionDir.resolve(progressLogFileName);

			try (InputStream progressStream = runner.getProgressStream()) {
				Files.copy(progressStream, progressLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored chunk progress log: {} for {} records", progressLogFileName, batchRecords.size());
			}
		}

		// Store error log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
			String errorLogFileName = String.format("YieldTables_CHUNK_%s_ErrorLog.txt", projectionId);
			Path errorLogPath = partitionDir.resolve(errorLogFileName);

			try (InputStream errorStream = runner.getErrorStream()) {
				Files.copy(errorStream, errorLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored chunk error log: {} for {} records", errorLogFileName, batchRecords.size());
			}
		}

		// Store debug log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
			String debugLogFileName = String.format("YieldTables_CHUNK_%s_DebugLog.txt", projectionId);
			Path debugLogPath = partitionDir.resolve(debugLogFileName);

			Files.write(debugLogPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.trace("Created chunk debug log placeholder: {} for {} records", debugLogFileName,
					batchRecords.size());
		}
	}
}
