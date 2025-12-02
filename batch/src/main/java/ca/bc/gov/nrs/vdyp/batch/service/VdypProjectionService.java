package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultStorageException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
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

	/**
	 * Performs VDYP projection for multiple BatchRecords in a chunk. This method processes a collection of complete
	 * polygons by creating combined input streams and running a single projection operation.
	 *
	 * @return Projection result summary for the entire chunk
	 */
	public String performProjectionForChunk(
			@NonNull List<BatchRecord> batchRecords, @NonNull String partitionName,
			@NonNull Parameters projectionParameters, @NonNull Long jobExecutionId, @NonNull String jobGuid,
			@NonNull String jobBaseDir
	) throws BatchResultStorageException, BatchProjectionException {
		logger.debug(
				"[GUID: {}, EXEID: {}] Starting VDYP projection for chunk of {} records in partition {}", jobGuid,
				jobExecutionId, batchRecords.size(), partitionName
		);

		Map<String, InputStream> inputStreams = null;
		try {
			Path outputPartitionDir = createOutputPartitionDir(jobGuid, jobExecutionId, partitionName, jobBaseDir);

			// Create combined input streams from all BatchRecords in the chunk
			inputStreams = createCombinedInputStreamsFromChunk(batchRecords);

			String batchProjectionId = BatchUtils
					.buildBatchProjectionId(jobExecutionId, partitionName, ProjectionRequestKind.HCSV);

			try (
					ProjectionRunner runner = new ProjectionRunner(
							ProjectionRequestKind.HCSV, batchProjectionId, projectionParameters, false
					)
			) {

				logger.debug(
						"[GUID: {}, EXEID: {}] Running HCSV projection {} for chunk of {} records in partition {}",
						jobGuid, jobExecutionId, batchProjectionId, batchRecords.size(), partitionName
				);

				// Run the projection on the combined chunk data
				runner.run(inputStreams);

				// Store intermediate results for all records in chunk
				storeChunkIntermediateResults(
						runner, outputPartitionDir, batchProjectionId, batchRecords, jobGuid, jobExecutionId
				);

				String result = String.format(
						"Chunk projection completed for %d records in partition %s. Results stored",
						batchRecords.size(), partitionName
				);

				logger.debug(
						"[GUID: {}, EXEID: {}] VDYP chunk projection completed for {} records in partition {}. Intermediate results stored",
						jobGuid, jobExecutionId, batchRecords.size(), partitionName
				);

				return result;

			}

		} catch (BatchResultStorageException e) {
			throw e;
		} catch (Exception e) {
			// All other exceptions from extended-core - wrap as BatchProjectionException with full context
			throw BatchProjectionException
					.handleProjectionFailure(e, batchRecords, jobGuid, jobExecutionId, partitionName, logger);
		} finally {
			if (inputStreams != null) {
				for (var entry : inputStreams.entrySet()) {
					Utils.close(entry.getValue(), entry.getKey());
				}
			}
		}
	}

	/**
	 * Creates a partition-specific output directory within the existing job-specific parent folder. Only logs when
	 * directory is actually created (not when it already exists).
	 *
	 * @throws BatchResultStorageException if directory creation fails
	 */
	private Path createOutputPartitionDir(String jobGuid, Long jobExecutionId, String partitionName, String jobBaseDir)
			throws BatchResultStorageException {
		Path jobBasePath = Paths.get(jobBaseDir);

		String inputPartitionName = BatchUtils.buildInputPartitionFolderName(partitionName);
		String outputPartitionName = BatchUtils.buildOutputPartitionFolderName(partitionName);

		Path outputPartitionDir = jobBasePath.resolve(outputPartitionName);

		try {
			// Check if directory already exists before creating
			boolean alreadyExists = Files.exists(outputPartitionDir);

			Files.createDirectories(outputPartitionDir);

			// Only log when directory is actually created (first chunk of partition)
			if (!alreadyExists) {
				logger.debug(
						"[GUID: {}, EXEID: {}] Created output partition directory: {} for input partition: {} within job folder: {}",
						jobGuid, jobExecutionId, outputPartitionName, inputPartitionName, jobBasePath.getFileName()
				);
			}
		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					outputPartitionDir, e,
					"Failed to create output partition directory (job folder: " + jobBasePath + ")", jobGuid,
					jobExecutionId, logger
			);
		}

		return outputPartitionDir;
	}

	/**
	 * Creates combined input streams from all BatchRecords in a chunk. This method combines all polygon and layer data
	 * from raw CSV into unified streams for VDYP projection.
	 */
	private Map<String, InputStream> createCombinedInputStreamsFromChunk(List<BatchRecord> batchRecords) {
		Map<String, InputStream> inputStreams = new HashMap<>();

		StringBuilder polygonCsv = new StringBuilder();
		StringBuilder layerCsv = new StringBuilder();

		// Add all polygon and layer data
		for (BatchRecord batchRecord : batchRecords) {
			polygonCsv.append(batchRecord.getRawPolygonData()).append("\n");

			for (String layerLine : batchRecord.getRawLayerData()) {
				layerCsv.append(layerLine).append("\n");
			}
		}

		// Add trailing empty line to match original file structure
		// This is critical for extended-core to correctly detect end of file
		polygonCsv.append("\n");
		layerCsv.append("\n");

		// Create input streams
		// FIXME See VDYP-833 Comment
		byte[] polygonBytes = polygonCsv.toString().getBytes(StandardCharsets.UTF_8);
		byte[] layerBytes = layerCsv.toString().getBytes(StandardCharsets.UTF_8);

		inputStreams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(polygonBytes));
		inputStreams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(layerBytes));

		logger.trace(
				"Created combined input streams from raw CSV data for chunk of {} records (Polygon: {} chars -> {} bytes, Layers: {} chars -> {} bytes)",
				batchRecords.size(), polygonCsv.length(), polygonBytes.length, layerCsv.length(), layerBytes.length
		);

		return inputStreams;
	}

	/**
	 * Stores intermediate results for all records in a chunk.
	 *
	 * @throws BatchResultStorageException if result storage fails
	 */
	private void storeChunkIntermediateResults(
			ProjectionRunner runner, Path partitionOutputDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {

		logger.debug(
				"Storing intermediate results for chunk projection {} ({} records)", projectionId, batchRecords.size()
		);

		storeChunkYieldTables(runner, partitionOutputDir, projectionId, batchRecords, jobGuid, jobExecutionId);

		storeChunkLogs(runner, partitionOutputDir, projectionId, batchRecords, jobGuid, jobExecutionId);

		logger.debug(
				"Successfully stored intermediate results for chunk projection {} ({} records) in {}", projectionId,
				batchRecords.size(), partitionOutputDir
		);
	}

	/**
	 * Stores yield tables from chunk projection.
	 *
	 * @throws BatchResultStorageException if yield table storage fails
	 */
	private void storeChunkYieldTables(
			ProjectionRunner runner, Path partitionDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
		if (runner == null || runner.getContext() == null) {
			logger.warn("Cannot store yield tables: ProjectionRunner or context is null");
			return;
		}

		var yieldTables = runner.getContext().getYieldTables();
		if (yieldTables == null || yieldTables.isEmpty()) {
			logger.warn(
					"WARNING: No yield tables returned from extended-core for projection {} ({} input records)",
					projectionId, batchRecords.size()
			);
			return;
		}

		logger.trace(
				"Extended-core returned {} yield table(s) for projection {} ({} input records)", yieldTables.size(),
				projectionId, batchRecords.size()
		);

		for (YieldTable yieldTable : yieldTables) {
			storeYieldTable(yieldTable, partitionDir, projectionId, batchRecords, jobGuid, jobExecutionId);
		}
	}

	/**
	 * Stores a single yield table file.
	 *
	 * @throws BatchResultStorageException if file copy fails
	 */
	private void storeYieldTable(
			YieldTable yieldTable, Path partitionDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
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
						prefixedFileName, batchRecords.size()
				);
			} else {
				logger.trace(
						"Stored yield table: {} ({} bytes written from extended-core for {} input records)",
						prefixedFileName, bytesWritten, batchRecords.size()
				);
			}
		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					yieldTablePath, e, "Failed to store yield table", jobGuid, jobExecutionId, logger
			);
		}
	}

	/**
	 * Stores log files from chunk projection.
	 *
	 * @throws BatchResultStorageException if log file storage fails
	 */
	private void storeChunkLogs(
			ProjectionRunner runner, Path partitionDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
		if (runner == null || runner.getContext() == null || runner.getContext().getParams() == null) {
			logger.warn("Cannot store logs: ProjectionRunner, context, or params is null");
			return;
		}

		ValidatedParameters params = runner.getContext().getParams();

		// Store progress log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
			storeProgressLog(runner, partitionDir, projectionId, batchRecords, jobGuid, jobExecutionId);
		}

		// Store error log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
			storeErrorLog(runner, partitionDir, projectionId, batchRecords, jobGuid, jobExecutionId);
		}

		// Store debug log if enabled
		if (params.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
			storeDebugLog(partitionDir, projectionId, batchRecords, jobGuid, jobExecutionId);
		}
	}

	private void storeProgressLog(
			ProjectionRunner runner, Path partitionDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
		String progressLogFileName = String.format("YieldTables_%s_ProgressLog.txt", projectionId);
		Path progressLogPath = partitionDir.resolve(progressLogFileName);

		try (InputStream progressStream = runner.getProgressStream()) {
			if (progressStream != null) {
				long bytesWritten = Files.copy(progressStream, progressLogPath, StandardCopyOption.REPLACE_EXISTING);

				if (bytesWritten == 0) {
					logger.warn(
							"WARNING: Progress log created but is EMPTY: {} (0 bytes, {} input records)",
							progressLogFileName, batchRecords.size()
					);
				} else {
					logger.trace(
							"Stored progress log: {} ({} bytes from extended-core for {} input records)",
							progressLogFileName, bytesWritten, batchRecords.size()
					);
				}
			} else {
				logger.warn("Progress stream is null, skipping progress log: {}", progressLogFileName);
			}
		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					progressLogPath, e, "Failed to store progress log", jobGuid, jobExecutionId, logger
			);
		}
	}

	private void storeErrorLog(
			ProjectionRunner runner, Path partitionDir, String projectionId, List<BatchRecord> batchRecords,
			String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
		String errorLogFileName = String.format("YieldTables_%s_ErrorLog.txt", projectionId);
		Path errorLogPath = partitionDir.resolve(errorLogFileName);

		try (InputStream errorStream = runner.getErrorStream()) {
			if (errorStream != null) {
				long bytesWritten = Files.copy(errorStream, errorLogPath, StandardCopyOption.REPLACE_EXISTING);

				if (bytesWritten == 0) {
					logger.trace(
							"Error log created (empty - no errors): {} for {} input records", errorLogFileName,
							batchRecords.size()
					);
				} else {
					logger.trace(
							"Error log contains data: {} ({} bytes from extended-core for {} input records)",
							errorLogFileName, bytesWritten, batchRecords.size()
					);
				}
			} else {
				logger.warn("Error stream is null, skipping error log: {}", errorLogFileName);
			}
		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					errorLogPath, e, "Failed to store error log", jobGuid, jobExecutionId, logger
			);
		}
	}

	private void storeDebugLog(
			Path partitionDir, String projectionId, List<BatchRecord> batchRecords, String jobGuid, Long jobExecutionId
	) throws BatchResultStorageException {
		String debugLogFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = partitionDir.resolve(debugLogFileName);

		try {
			Files.write(debugLogPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.trace(
					"Created chunk debug log placeholder: {} for {} records", debugLogFileName, batchRecords.size()
			);
		} catch (IOException e) {
			throw BatchResultStorageException.handleResultStorageFailure(
					debugLogPath, e, "Failed to create debug log placeholder", jobGuid, jobExecutionId, logger
			);
		}
	}
}
