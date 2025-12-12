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
	 * @param polygonFile  The polygon CSV file to partition
	 * @param layerFile    The layer CSV file to partition
	 * @param numPartition The number of partitions to create
	 * @param jobBaseDir   The base directory for the job
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

		return featureIdToPartition.size();
	}

	/**
	 * Count total FEATURE_IDs in polygon file, skipping header lines if present.
	 *
	 * @return The number of valid FEATURE_IDs found
	 * @throws BatchPartitionException if file cannot be read or contains no valid FEATURE_IDs
	 */
	private int countTotalFeatureIds(MultipartFile polygonFile, String jobGuid) throws BatchPartitionException {
		int count = 0;
		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			String line;
			// Skip first line if it's a header
			String firstLine = reader.readLine();
			if (firstLine != null && !firstLine.trim().isEmpty() && !BatchUtils.isHeaderLine(firstLine)
					&& BatchUtils.extractFeatureId(firstLine) != null) {
				count++;
			}

			// Count remaining data lines (no need to check for headers anymore)
			while ( (line = reader.readLine()) != null) {
				if (!line.trim().isEmpty() && BatchUtils.extractFeatureId(line) != null) {
					count++;
				}
			}
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

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {

			// Read first line and check if it's a header (headers are optional)
			String firstLine = reader.readLine();
			String headerLine = null;

			if (firstLine != null && BatchUtils.isHeaderLine(firstLine)) {
				headerLine = firstLine;
				logger.trace("Found header in polygon file");
			}

			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_POLYGON_FILE_NAME, headerLine, numPartitions, jobGuid
			);

			try {
				// If first line was not a header, it's a data line - process it
				if (firstLine != null && !BatchUtils.isHeaderLine(firstLine)) {
					String featureId = BatchUtils.extractFeatureId(firstLine);
					if (featureId != null) {
						featureIdToPartition.put(featureId, 0);
						writers.get(0).println(firstLine);
					}
				}

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

		String line;
		int currentPartition = 0;
		int recordsInCurrentPartition = 0;

		while ( (line = reader.readLine()) != null) {
			String featureId = BatchUtils.extractFeatureId(line);
			if (featureId != null) {
				// Write to current partition
				featureIdToPartition.put(featureId, currentPartition);
				writers.get(currentPartition).println(line);
				recordsInCurrentPartition++;

				// Check if current partition is full, move to next
				if (recordsInCurrentPartition >= featuresPerPartition[currentPartition]) {
					currentPartition++;
					recordsInCurrentPartition = 0;

					// Safety check: don't exceed partition bounds
					if (currentPartition >= featuresPerPartition.length) {
						currentPartition = featuresPerPartition.length - 1;
					}
				}
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

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(layerFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {

			// Read first line and check if it's a header (headers are optional)
			String firstLine = reader.readLine();
			String headerLine = null;

			if (firstLine != null && BatchUtils.isHeaderLine(firstLine)) {
				headerLine = firstLine;
				logger.trace("Found header in layer file");
			}

			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_LAYER_FILE_NAME, headerLine, numPartitions, jobGuid
			);

			try {
				// If first line was not a header, it's a data line - process it
				if (firstLine != null && !BatchUtils.isHeaderLine(firstLine)) {
					String featureId = BatchUtils.extractFeatureId(firstLine);
					if (featureId != null && featureIdToPartition.containsKey(featureId)) {
						int partition = featureIdToPartition.get(featureId);
						writers.get(partition).println(firstLine);
					}
				}

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
		while ( (line = reader.readLine()) != null) {
			String featureId = BatchUtils.extractFeatureId(line);
			if (featureId != null && featureIdToPartition.containsKey(featureId)) {
				int partition = featureIdToPartition.get(featureId);
				writers.get(partition).println(line);
			}
		}
	}

	/**
	 * Create PrintWriters for each partition.
	 *
	 * @param header Optional header line (null if no header)
	 * @return Map of partition number to PrintWriter
	 * @throws BatchPartitionException if directory creation or file writing fails
	 */
	private Map<Integer, PrintWriter> createPartitionWriters(
			Path jobBaseDir, String filename, String header, Integer numPartitions, String jobGuid
	) throws BatchPartitionException {
		Map<Integer, PrintWriter> writers = new HashMap<>();

		try {
			for (int i = 0; i < numPartitions; i++) {
				Path partitionDir = jobBaseDir.resolve(BatchConstants.Partition.INPUT_FOLDER_NAME_PREFIX + i);
				Files.createDirectories(partitionDir);

				Path csvFile = partitionDir.resolve(filename);
				BufferedWriter bufferedWriter = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
				PrintWriter writer = new PrintWriter(bufferedWriter, false);

				// Write header only if present (headers are optional in input file)
				if (header != null) {
					writer.println(header);
				}

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
