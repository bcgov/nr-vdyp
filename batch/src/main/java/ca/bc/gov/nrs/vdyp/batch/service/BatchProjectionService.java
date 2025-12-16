package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultStorageException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.batch.util.BatchRangeInputStream;
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
public class BatchProjectionService {

	private static final Logger logger = LoggerFactory.getLogger(BatchProjectionService.class);

	static {
		PolygonProjectionRunner.initializeSiteIndexCurves();
	}

	/**
	 * Performs VDYP projection for a chunk by streaming data directly from partition files. This method creates input
	 * streams for the specified record range and runs a single projection operation, avoiding memory duplication.
	 *
	 * @return Projection result summary for the chunk
	 */
	public String performProjectionForChunk(
			@NonNull BatchChunkMetadata chunkMetadata, @NonNull Parameters projectionParameters,
			@NonNull Long jobExecutionId, @NonNull String jobGuid
	) throws BatchResultStorageException, BatchProjectionException {

		String partitionName = chunkMetadata.getPartitionName();
		String jobBaseDir = chunkMetadata.getJobBaseDir();
		long polygonStartByte = chunkMetadata.getPolygonStartByte();
		int polygonRecordCount = chunkMetadata.getPolygonRecordCount();

		logger.debug(
				"[GUID: {}, EXEID: {}] Starting VDYP projection for chunk (polygonStartByte={}, polygonRecordCount={}) in partition {}",
				jobGuid, jobExecutionId, polygonStartByte, polygonRecordCount, partitionName
		);

		Map<String, InputStream> inputStreams = null;
		try {
			Path outputPartitionDir = createOutputPartitionDir(partitionName, jobBaseDir);

			// Create input streams directly from partition files
			inputStreams = createInputStreamsFromChunkMetadata(chunkMetadata);

			String batchProjectionId = BatchUtils
					.buildBatchProjectionId(jobExecutionId, partitionName, ProjectionRequestKind.HCSV);

			try (
					ProjectionRunner runner = new ProjectionRunner(
							ProjectionRequestKind.HCSV, batchProjectionId, projectionParameters, false
					)
			) {

				logger.debug(
						"[GUID: {}, EXEID: {}] Running HCSV projection {} for chunk of {} records in partition {}",
						jobGuid, jobExecutionId, batchProjectionId, polygonRecordCount, partitionName
				);

				// Run the projection on the streamed data
				runner.run(inputStreams);

				// Store intermediate results
				storeChunkIntermediateResults(runner, outputPartitionDir, batchProjectionId, polygonRecordCount);

				String result = String.format(
						"Chunk projection completed for %d records in partition %s. Results stored", polygonRecordCount,
						partitionName
				);

				logger.debug(
						"[GUID: {}, EXEID: {}] VDYP chunk projection completed for {} records in partition {}. Intermediate results stored",
						jobGuid, jobExecutionId, polygonRecordCount, partitionName
				);

				return result;

			}

		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					e, "Failed to store projection results", jobGuid, jobExecutionId, logger
			);
		} catch (Exception e) {
			// All other exceptions from extended-core - wrap as BatchProjectionException
			throw BatchProjectionException
					.handleProjectionFailure(e, chunkMetadata, jobGuid, jobExecutionId, partitionName, logger);
		} finally {
			if (inputStreams != null) {
				for (var entry : inputStreams.entrySet()) {
					Utils.close(entry.getValue(), entry.getKey());
				}
			}
		}
	}

	/**
	 * Creates a partition-specific output directory within the existing job-specific parent folder.
	 *
	 * @throws IOException if directory creation fails
	 */
	private Path createOutputPartitionDir(String partitionName, String jobBaseDir) throws IOException {
		Path jobBasePath = Paths.get(jobBaseDir);
		String outputPartitionName = BatchUtils.buildOutputPartitionFolderName(partitionName);
		Path outputPartitionDir = jobBasePath.resolve(outputPartitionName);

		// Check if directory already exists before creating
		boolean alreadyExists = Files.exists(outputPartitionDir);
		Files.createDirectories(outputPartitionDir);

		// Only log when directory is actually created (first chunk of partition)
		if (!alreadyExists) {
			String inputPartitionName = BatchUtils.buildInputPartitionFolderName(partitionName);
			logger.trace(
					"Created output partition directory: {} for input partition: {} within job folder: {}",
					outputPartitionName, inputPartitionName, jobBasePath.getFileName()
			);
		}

		return outputPartitionDir;
	}

	/**
	 * Creates input streams by reading directly from partition files using byte offsets. This method streams data
	 * without loading entire CSV content into memory, using FileChannel for efficient random access.
	 *
	 * Both polygon and layer streams use byte offset metadata calculated by BatchItemReader to jump directly to the
	 * required data positions, avoiding full file scans.
	 *
	 * The startByte offsets are calculated by BatchItemReader to point to data records only, skipping any headers that
	 * may have been present in the partitioned files.
	 *
	 * Note: Caller is responsible for closing the returned InputStreams (handled in finally block of
	 * performProjectionForChunk)
	 *
	 * @throws IOException if file streaming fails
	 */
	@SuppressWarnings("java:S2095") // Streams are closed by caller in finally block
	private Map<String, InputStream> createInputStreamsFromChunkMetadata(BatchChunkMetadata chunkMetadata)
			throws IOException {

		String partitionName = chunkMetadata.getPartitionName();
		String jobBaseDir = chunkMetadata.getJobBaseDir();
		long polygonStartByte = chunkMetadata.getPolygonStartByte();
		int polygonRecordCount = chunkMetadata.getPolygonRecordCount();
		long layerStartByte = chunkMetadata.getLayerStartByte();
		int layerRecordCount = chunkMetadata.getLayerRecordCount();

		// Get partition directory
		String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
		Path partitionDir = Paths.get(jobBaseDir, inputPartitionFolderName);

		Path polygonFile = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
		Path layerFile = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

		// Create polygon stream starting at the calculated byte offset
		InputStream polygonStream = BatchRangeInputStream.create(polygonFile, polygonStartByte, polygonRecordCount);

		// Create layer stream starting at the calculated byte offset (or empty stream if no data)
		InputStream layerStream;
		if (layerRecordCount > 0) {
			layerStream = BatchRangeInputStream.create(layerFile, layerStartByte, layerRecordCount);
		} else {
			// No layer records for this polygon chunk - create empty stream
			layerStream = java.io.InputStream.nullInputStream();
		}

		Map<String, InputStream> inputStreams = new HashMap<>();
		inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStream);
		inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerStream);

		logger.trace(
				"Created input streams from partition files for chunk: polygonStartByte={}, polygonRecordCount={}, layerStartByte={}, layerRecordCount={}, partition={}",
				polygonStartByte, polygonRecordCount, layerStartByte, layerRecordCount, partitionName
		);

		return inputStreams;
	}

	/**
	 * Stores intermediate results for a chunk.
	 *
	 * @throws IOException if result storage fails
	 */
	private void storeChunkIntermediateResults(
			ProjectionRunner runner, Path partitionOutputDir, String projectionId, int recordCount
	) throws IOException {

		logger.debug("Storing intermediate results for chunk projection {} ({} records)", projectionId, recordCount);

		storeChunkYieldTables(runner, partitionOutputDir, projectionId, recordCount);

		storeChunkLogs(runner, partitionOutputDir, projectionId, recordCount);

		logger.debug(
				"Successfully stored intermediate results for chunk projection {} ({} records) in {}", projectionId,
				recordCount, partitionOutputDir
		);
	}

	/**
	 * Stores yield tables from chunk projection.
	 *
	 * @throws IOException if yield table storage fails
	 */
	private void storeChunkYieldTables(ProjectionRunner runner, Path partitionDir, String projectionId, int recordCount)
			throws IOException {
		if (runner == null || runner.getContext() == null) {
			logger.warn("Cannot store yield tables: ProjectionRunner or context is null");
			return;
		}

		var yieldTables = runner.getContext().getYieldTables();
		if (yieldTables == null || yieldTables.isEmpty()) {
			logger.warn(
					"WARNING: No yield tables returned from extended-core for projection {} ({} input records)",
					projectionId, recordCount
			);
			return;
		}

		logger.trace(
				"Extended-core returned {} yield table(s) for projection {} ({} input records)", yieldTables.size(),
				projectionId, recordCount
		);

		for (YieldTable yieldTable : yieldTables) {
			storeYieldTable(yieldTable, partitionDir, projectionId, recordCount);
		}
	}

	/**
	 * Stores a single yield table file.
	 *
	 * @throws IOException if file copy fails
	 */
	private void storeYieldTable(YieldTable yieldTable, Path partitionDir, String projectionId, int recordCount)
			throws IOException {
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

			// Copy stream and track actual bytes written
			long bytesWritten = Files.copy(yieldTableStream, yieldTablePath, StandardCopyOption.REPLACE_EXISTING);

			if (bytesWritten == 0) {
				logger.trace(
						"WARNING: Yield table file created but is EMPTY: {} (0 bytes written, {} input records)",
						prefixedFileName, recordCount
				);
			} else {
				logger.trace(
						"Stored yield table: {} ({} bytes written from extended-core for {} input records)",
						prefixedFileName, bytesWritten, recordCount
				);
			}
		}
	}

	/**
	 * Stores log files from chunk projection.
	 *
	 * @throws IOException if log file storage fails
	 */
	private void storeChunkLogs(ProjectionRunner runner, Path partitionDir, String projectionId, int recordCount)
			throws IOException {
		if (runner == null || runner.getContext() == null || runner.getContext().getParams() == null) {
			logger.warn("Cannot store logs: ProjectionRunner, context, or params is null");
			return;
		}

		ValidatedParameters params = runner.getContext().getParams();

		// Store progress log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
			storeProgressLog(runner, partitionDir, projectionId, recordCount);
		}

		// Store error log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
			storeErrorLog(runner, partitionDir, projectionId, recordCount);
		}

		// Store debug log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
			storeDebugLog(partitionDir, projectionId, recordCount);
		}
	}

	private void storeProgressLog(ProjectionRunner runner, Path partitionDir, String projectionId, int recordCount)
			throws IOException {
		String progressLogFileName = String.format("YieldTables_%s_ProgressLog.txt", projectionId);
		Path progressLogPath = partitionDir.resolve(progressLogFileName);

		try (InputStream progressStream = runner.getProgressStream()) {
			if (progressStream != null) {
				long bytesWritten = Files.copy(progressStream, progressLogPath, StandardCopyOption.REPLACE_EXISTING);

				if (bytesWritten == 0) {
					logger.warn(
							"WARNING: Progress log created but is EMPTY: {} (0 bytes, {} input records)",
							progressLogFileName, recordCount
					);
				} else {
					logger.trace(
							"Stored progress log: {} ({} bytes from extended-core for {} input records)",
							progressLogFileName, bytesWritten, recordCount
					);
				}
			} else {
				logger.warn("Progress stream is null, skipping progress log: {}", progressLogFileName);
			}
		}
	}

	private void storeErrorLog(ProjectionRunner runner, Path partitionDir, String projectionId, int recordCount)
			throws IOException {
		String errorLogFileName = String.format("YieldTables_%s_ErrorLog.txt", projectionId);
		Path errorLogPath = partitionDir.resolve(errorLogFileName);

		try (InputStream errorStream = runner.getErrorStream()) {
			if (errorStream != null) {
				long bytesWritten = Files.copy(errorStream, errorLogPath, StandardCopyOption.REPLACE_EXISTING);

				if (bytesWritten == 0) {
					logger.trace(
							"Error log created (empty - no errors): {} for {} input records", errorLogFileName,
							recordCount
					);
				} else {
					logger.trace(
							"Error log contains data: {} ({} bytes from extended-core for {} input records)",
							errorLogFileName, bytesWritten, recordCount
					);
				}
			} else {
				logger.warn("Error stream is null, skipping error log: {}", errorLogFileName);
			}
		}
	}

	private void storeDebugLog(Path partitionDir, String projectionId, int recordCount) throws IOException {
		String debugLogFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = partitionDir.resolve(debugLogFileName);

		Files.write(debugLogPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		logger.trace("Created chunk debug log placeholder: {} for {} records", debugLogFileName, recordCount);
	}
}
