package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import io.micrometer.common.lang.NonNull;

/**
 * Streaming CSV partitioner that partitions CSV files by FEATURE_ID. Creates separate CSV files for each partition
 * containing only the data for that partition's assigned FEATURE_IDs.
 *
 * Assumptions: Both input polygon/layer files must be sorted by FEATURE_ID
 *
 * Partitioning Guarantees: - Each partition's layer file contains ONLY layers for FEATURE_IDs present in that
 * partition's polygon file - Partitioned layer files maintain FEATURE_ID sort order from the original input layer file
 */
@Component
public class BatchInputPartitioner {

	private static final Logger logger = LoggerFactory.getLogger(BatchInputPartitioner.class);

	/**
	 * Partitions polygon and layer CSV files by FEATURE_ID into separate partition files.
	 *
	 * @param polygonFile   The polygon CSV file to partition
	 * @param layerFile     The layer CSV file to partition
	 * @param numPartitions The number of partitions to create
	 * @param jobBaseDir    The base directory for the job
	 * @param jobGuid
	 * @return The total number of unique FEATURE_IDs processed
	 * @throws BatchPartitionException if partitioning fails (I/O errors or validation failures)
	 */
	public int partitionCsvFiles(
			@NonNull MultipartFile polygonFile, @NonNull MultipartFile layerFile, @NonNull Integer numPartitions,
			@NonNull Path jobBaseDir, @NonNull String jobGuid
	) throws BatchPartitionException {
		int totalFeatureIds = countTotalFeatureIds(polygonFile, jobGuid);
		int[] featuresPerPartition = calculateFeaturesPerPartition(totalFeatureIds, numPartitions);

		Map<String, Integer> featureIdToPartition = partitionPolygonFile(
				polygonFile, numPartitions, featuresPerPartition, jobBaseDir, jobGuid
		);

		partitionLayerFile(layerFile, numPartitions, jobBaseDir, featureIdToPartition, jobGuid);

		return totalFeatureIds;
	}

	/**
	 * Partitions polygon and layer CSV files by FEATURE_ID into separate partition files.
	 *
	 * @param polygonFile   The polygon CSV file to partition
	 * @param layerFile     The layer CSV file to partition
	 * @param numPartitions The number of partitions to create
	 * @param jobBaseDir    The base directory for the job
	 * @param jobGuid
	 * @return The total number of unique FEATURE_IDs processed
	 * @throws BatchPartitionException if partitioning fails (I/O errors or validation failures)
	 */
	public int partitionCsvFiles(
			@NonNull Path polygonFile, @NonNull Path layerFile, @NonNull Integer numPartitions,
			@NonNull Path jobBaseDir, @NonNull String jobGuid
	) throws BatchPartitionException {

		try (
				BufferedReader polyReader = Files.newBufferedReader(polygonFile, StandardCharsets.UTF_8);
				BufferedReader layerReader = Files.newBufferedReader(layerFile, StandardCharsets.UTF_8)
		) {
			return partitionCSVReaders(polyReader, layerReader, numPartitions, jobBaseDir, jobGuid);
		} catch (IOException e) {
			throw BatchPartitionException.handlePartitionFailure(e, "Failed to open CSV files", jobGuid, logger);
		}
	}

	/**
	 * Partition polygon and layer CSV readers by FEATURE_ID into separate partition files. Works round robin style,
	 * this assumes that output files are permitted to vary in order as long as all FEATURE_IDS are reported in the
	 * output The ordering issue could be fixed by merging the files in a round robin style as well.
	 *
	 * @param polygonReader A Buffered reader of the polygon CSV file to partition
	 * @param layerReader   a Buffered reader of the layer CSV file to partition
	 * @param numPartitions the number of partitions to create
	 * @param jobBaseDir    The base directory for the job (A Property of the job)
	 * @param jobGuid       The unique identifier for the job (A Property of the job)
	 * @return the number of unique feature ids that have been written to the polygon partition files
	 * @throws BatchPartitionException
	 * @throws IOException
	 */
	private int partitionCSVReaders(
			BufferedReader polygonReader, BufferedReader layerReader, Integer numPartitions, Path jobBaseDir,
			String jobGuid
	) throws BatchPartitionException, IOException {
		int uniqueFeatureIdCount = 0;
		Map<Integer, PrintWriter> polygonWriters = null;
		Map<Integer, PrintWriter> layerWriters = null;
		try (PrintWriter warningWriter = createWarningWriter(jobBaseDir)) {
			// Get the first non blank line from each file
			String polygonLine = readNextNonBlankLine(polygonReader);
			String layerLine = readNextNonBlankLine(layerReader);

			if (polygonLine == null || layerLine == null) {
				throw new IllegalArgumentException("Input files contain no data lines");
			}

			if (BatchUtils.isHeaderLine(polygonLine)) {
				polygonLine = readNextNonBlankLine(polygonReader);
			}
			if (BatchUtils.isHeaderLine(layerLine)) {
				layerLine = readNextNonBlankLine(layerReader);
			}

			polygonWriters = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_POLYGON_FILE_NAME, numPartitions, jobGuid
			);
			layerWriters = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_LAYER_FILE_NAME, numPartitions, jobGuid
			);
			Integer layerFeatureId = null;
			while (polygonLine != null) {
				Integer polygonFeatureId = BatchUtils.extractFeatureIdInteger(polygonLine);
				if (polygonFeatureId == null) {
					// Decide: skip or fail. I would fail in polygon file because it drives the join.
					throw BatchPartitionException.handlePartitionFailure(
							String.format("Polygon row missing FEATURE_ID: %s", polygonLine), jobGuid, logger
					);
				}
				uniqueFeatureIdCount++;
				int partition = (uniqueFeatureIdCount - 1) % numPartitions;
				// write the polygon to it's partition round robin style
				polygonWriters.get(partition).println(polygonLine);
				polygonLine = readNextNonBlankLine(polygonReader);

				// write all matching layer lines to the same partition
				// First advance past orphan layer lines featureId < polygonFeatureId
				layerFeatureId = BatchUtils.extractFeatureIdInteger(layerLine);
				while (layerFeatureId == null || layerFeatureId.compareTo(polygonFeatureId) < 0) {
					handleOrphanLayerLine(warningWriter, layerLine, jobGuid);
					layerLine = readNextNonBlankLine(layerReader);
					layerFeatureId = BatchUtils.extractFeatureIdInteger(layerLine);
				}
				// Now write all matching layer lines
				while (layerFeatureId != null && layerFeatureId.compareTo(polygonFeatureId) == 0) {
					layerWriters.get(partition).println(layerLine);
					layerLine = readNextNonBlankLine(layerReader);
					layerFeatureId = BatchUtils.extractFeatureIdInteger(layerLine);
				}
			}
			// Check for any remaining lines in the Layer file and log warnings about orphaned lines
			if (layerFeatureId != null) {
				handleOrphanLayerLine(warningWriter, layerLine, jobGuid);
			}
			// Check for additional lines after the last polygon
			while (layerLine != null) {
				handleOrphanLayerLine(warningWriter, layerLine, jobGuid);
				layerLine = readNextNonBlankLine(layerReader);
			}

		} catch (Exception e) {
			throw BatchPartitionException.handlePartitionFailure(e, "Error partitioning input files", jobGuid, logger);
		} finally {
			closeWriters(polygonWriters);
			closeWriters(layerWriters);
		}
		return uniqueFeatureIdCount;
	}

	private void handleOrphanLayerLine(PrintWriter warningWriter, String layerLine, String jobGuid) {
		String message = String.format(
				"Job GUID: [%s] Orphan layer line has no matching polygon [Out of order or invalid feature id, skipping: %s",
				jobGuid, layerLine
		);

		// log orphan layer line
		logger.warn(message);
		warningWriter.println(message);
	}

	private static String readNextNonBlankLine(BufferedReader reader) throws IOException {
		String line;
		while ( (line = reader.readLine()) != null) {
			if (!line.isBlank()) {
				return line;
			}
		}
		return null;
	}

	/**
	 * Count total FEATURE_IDs in polygon file, skipping header lines if present.
	 *
	 * @return The number of valid FEATURE_IDs found
	 * @throws BatchPartitionException if file cannot be read or contains no valid FEATURE_IDs
	 */
	private int countTotalFeatureIds(MultipartFile polygonFile, String jobGuid) throws BatchPartitionException {
		int count;

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			count = BatchUtils.countDataRecords(reader);
		} catch (IOException e) {
			throw BatchPartitionException.handlePartitionFailure(
					e, "Failed to read polygon file while counting FEATURE_IDs", jobGuid, logger
			);
		}

		if (count == 0) {
			throw BatchPartitionException.handlePartitionFailure(
					"Polygon file contains no valid FEATURE_IDs. Please provide a CSV file with at least one valid polygon record.",
					jobGuid, logger
			);
		}

		return count;
	}

	/**
	 * Detects and returns the header line from a CSV file, if present.
	 *
	 * @param file    The file to check for a header
	 * @param jobGuid The job GUID for error reporting
	 * @return The header line if found, or null if no header exists. Callers MUST check for null.
	 * @throws BatchPartitionException if file cannot be read
	 */
	private String detectHeader(MultipartFile file, String jobGuid) throws BatchPartitionException {
		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			String line;
			while ( (line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					if (BatchUtils.isHeaderLine(line)) {
						logger.trace("Found header line");
						return line;
					}
					// First non-blank line is not a header
					return null;
				}
			}
		} catch (IOException e) {
			throw BatchPartitionException
					.handlePartitionFailure(e, "Failed to read file for header detection", jobGuid, logger);
		}
		return null;
	}

	/**
	 * Calculate number of features per partition for balanced distribution.
	 *
	 * @param totalFeatureIds Total number of features to distribute
	 * @param numPartitions   Number of partitions to create
	 * @return array where index = partition number, value = number of features for that partition
	 */
	private int[] calculateFeaturesPerPartition(int totalFeatureIds, int numPartitions) {
		int[] featuresPerPartition = new int[numPartitions];
		int baseFeatureCount = totalFeatureIds / numPartitions;
		int extraFeatures = totalFeatureIds % numPartitions;

		// First 'extraFeatures' partitions get baseFeatureCount + 1
		// Remaining partitions get baseFeatureCount
		for (int i = 0; i < numPartitions; i++) {
			featuresPerPartition[i] = (i < extraFeatures) ? baseFeatureCount + 1 : baseFeatureCount;
		}

		logger.debug(
				"Distributing {} features across {} partitions: base count = {}, extra features = {}, distribution = {}",
				totalFeatureIds, numPartitions, baseFeatureCount, extraFeatures, featuresPerPartition
		);

		return featuresPerPartition;
	}

	/**
	 * Partition polygon file by FEATURE_ID. Headers are optional - if present, they are detected and written to
	 * partition files. If not present, partition files will contain only data lines.
	 *
	 * @param polygonFile          The polygon CSV file to partition
	 * @param numPartitions        The number of partitions to create
	 * @param featuresPerPartition Array of feature counts for each partition (determines distribution)
	 * @param jobBaseDir           The base directory for the job
	 * @param jobGuid              The unique identifier for the job
	 * @return Map of FEATURE_ID to partition number
	 * @throws BatchPartitionException if file I/O fails
	 */
	private Map<String, Integer> partitionPolygonFile(
			MultipartFile polygonFile, int numPartitions, int[] featuresPerPartition, Path jobBaseDir, String jobGuid
	) throws BatchPartitionException {

		// FIXME: VDYP-869
		Map<String, Integer> featureIdToPartition = new HashMap<>();

		String headerLine = detectHeader(polygonFile, jobGuid);

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_POLYGON_FILE_NAME, numPartitions, jobGuid
			);

			try {
				processPolygonRecords(reader, writers, featureIdToPartition, featuresPerPartition);
			} finally {
				closeWriters(writers);
			}
		} catch (IOException e) {
			throw BatchPartitionException
					.handlePartitionFailure(e, "Failed to partition polygon file", jobGuid, logger);
		}

		logger.debug("Processed and partitioned {} FEATURE_IDs", featureIdToPartition.size());
		return featureIdToPartition;
	}

	/**
	 * Reads exact number of records for each partition in sequence.
	 *
	 * @param reader               The BufferedReader for the polygon file
	 * @param writers              Map of partition number to PrintWriter
	 * @param featureIdToPartition Map to populate with FEATURE_ID to partition mappings
	 * @param featuresPerPartition Array of feature counts for each partition
	 * @throws IOException if reading from the file fails (wrapped at component boundary by caller)
	 */
	@SuppressWarnings("javasecurity:S5131") // False positive: CSV file writing to internal storage, not HTTP response
	private void processPolygonRecords(
			BufferedReader reader, Map<Integer, PrintWriter> writers, Map<String, Integer> featureIdToPartition,
			int[] featuresPerPartition
	) throws IOException {
		PartitionState state = new PartitionState(featuresPerPartition);

		String line;
		while ( (line = reader.readLine()) != null) {
			if (shouldSkipLine(line, state)) {
				continue;
			}

			String featureId = BatchUtils.extractFeatureId(line);
			if (featureId != null) {
				writeToPartition(line, featureId, state, writers, featureIdToPartition, featuresPerPartition);
			}
		}
	}

	/**
	 * Determines if a line should be skipped (blank or header).
	 */
	private boolean shouldSkipLine(String line, PartitionState state) {
		boolean shouldSkip = line.isBlank() || (!state.headerChecked && BatchUtils.isHeaderLine(line));

		if (!state.headerChecked && !line.isBlank()) {
			state.headerChecked = true;
		}

		return shouldSkip;
	}

	/**
	 * Writes a record to the appropriate partition and updates state.
	 */
	@SuppressWarnings("javasecurity:S5131") // False positive: CSV file writing to internal storage, not HTTP response
	private void writeToPartition(
			String line, String featureId, PartitionState state, Map<Integer, PrintWriter> writers,
			Map<String, Integer> featureIdToPartition, int[] featuresPerPartition
	) {
		featureIdToPartition.put(featureId, state.currentPartition);
		writers.get(state.currentPartition).println(line);
		state.recordsInCurrentPartition++;

		// Check if current partition is full, move to next
		if (state.recordsInCurrentPartition >= featuresPerPartition[state.currentPartition]) {
			state.moveToNextPartition(featuresPerPartition.length);
		}
	}

	/**
	 * Helper class to track partition processing state.
	 */
	private static class PartitionState {
		int currentPartition = 0;
		int recordsInCurrentPartition = 0;
		boolean headerChecked = false;

		PartitionState(int[] featuresPerPartition) {
			// Constructor for potential future initialization
		}

		void moveToNextPartition(int maxPartitions) {
			currentPartition++;
			recordsInCurrentPartition = 0;

			// Safety check: don't exceed partition bounds
			if (currentPartition >= maxPartitions) {
				currentPartition = maxPartitions - 1;
			}
		}
	}

	/**
	 * Partition layer file by FEATURE_ID. Headers are optional - if present, they are detected and written to partition
	 * files. If not present, partition files will contain only data lines.
	 *
	 * @param featureIdToPartition Map of FEATURE_ID to partition number from polygon partitioning
	 * @throws BatchPartitionException if file I/O fails
	 */
	private void partitionLayerFile(
			MultipartFile layerFile, int numPartitions, Path jobBaseDir, Map<String, Integer> featureIdToPartition,
			String jobGuid
	) throws BatchPartitionException {

		String headerLine = detectHeader(layerFile, jobGuid);

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(layerFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_LAYER_FILE_NAME, numPartitions, jobGuid
			);

			try {
				processLayerRecords(reader, writers, featureIdToPartition);
			} finally {
				closeWriters(writers);
			}
		} catch (IOException e) {
			throw BatchPartitionException.handlePartitionFailure(e, "Failed to partition layer file", jobGuid, logger);
		}
	}

	/**
	 * Processes layer file records and writes them to appropriate partition files.
	 *
	 * @param reader               The BufferedReader for the layer file
	 * @param writers              Map of partition number to PrintWriter
	 * @param featureIdToPartition Map of FEATURE_ID to partition number
	 * @throws IOException if reading from the file fails (wrapped at component boundary by caller)
	 */
	@SuppressWarnings("javasecurity:S5131") // False positive: CSV file writing to internal storage, not HTTP response
	private void processLayerRecords(
			BufferedReader reader, Map<Integer, PrintWriter> writers, Map<String, Integer> featureIdToPartition
	) throws IOException {
		String line;
		boolean headerChecked = false;

		while ( (line = reader.readLine()) != null) {
			// Skip blank lines and optionally skip header (only checked once)
			boolean shouldSkip = line.isBlank() || (!headerChecked && BatchUtils.isHeaderLine(line));

			if (!headerChecked && !line.isBlank()) {
				headerChecked = true;
			}

			if (shouldSkip) {
				continue;
			}

			String featureId = BatchUtils.extractFeatureId(line);
			if (featureId != null && featureIdToPartition.containsKey(featureId)) {
				int partition = featureIdToPartition.get(featureId);
				writers.get(partition).println(line);
			}
		}
	}

	private PrintWriter createWarningWriter(Path jobBaseDir) throws IOException {
		Path warningFile = jobBaseDir.resolve(jobBaseDir + BatchConstants.Partition.WARNING_FILE_NAME);
		BufferedWriter bufferedWriter = Files.newBufferedWriter(warningFile, StandardCharsets.UTF_8);
		return new PrintWriter(bufferedWriter, false);
	}

	/**
	 * Create PrintWriters for each partition.
	 *
	 * @return Map of partition number to PrintWriter
	 * @throws BatchPartitionException if directory creation or file writing fails
	 */
	private Map<Integer, PrintWriter>
			createPartitionWriters(Path jobBaseDir, String filename, Integer numPartitions, String jobGuid)
					throws BatchPartitionException {
		Map<Integer, PrintWriter> writers = new HashMap<>();

		try {
			for (int i = 0; i < numPartitions; i++) {
				Path partitionDir = jobBaseDir.resolve(BatchConstants.Partition.INPUT_FOLDER_NAME_PREFIX + i);
				Files.createDirectories(partitionDir);

				Path csvFile = partitionDir.resolve(filename);
				BufferedWriter bufferedWriter = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
				PrintWriter writer = new PrintWriter(bufferedWriter, false);

				writers.put(i, writer);
			}
			return writers;
		} catch (IOException e) {
			closeWriters(writers);
			throw BatchPartitionException.handlePartitionFailure(
					e, "Failed to create partition writers for file '" + filename + "'", jobGuid, logger
			);
		}
	}

	/**
	 * Close all writers, flushing buffers before closing.
	 */
	private void closeWriters(Map<Integer, PrintWriter> writers) {
		if (writers != null) {
			for (PrintWriter writer : writers.values()) {
				try {
					writer.flush();
					writer.close();
				} catch (Exception e) {
					logger.error("Error closing writer", e);
				}
			}
		}
	}
}
