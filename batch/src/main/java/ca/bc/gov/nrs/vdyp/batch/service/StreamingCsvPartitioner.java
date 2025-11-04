package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Streaming CSV partitioner that partitions CSV files by FEATURE_ID. Creates separate CSV files for each partition
 * containing only the data for that partition's assigned FEATURE_IDs.
 */
@Component
public class StreamingCsvPartitioner {

	private static final Logger logger = LoggerFactory.getLogger(StreamingCsvPartitioner.class);

	public int partitionCsvFiles(
			MultipartFile polygonFile, MultipartFile layerFile, Integer partitionSize, Path jobBaseDir
	) throws IOException {

		validateParameters(polygonFile, layerFile, partitionSize, jobBaseDir);

		int totalFeatureIds = countTotalFeatureIds(polygonFile);
		int[] partitionSizes = calculatePartitionSizes(totalFeatureIds, partitionSize);

		Map<Long, Integer> featureIdToPartition = partitionPolygonFile(
				polygonFile, partitionSize, partitionSizes, jobBaseDir
		);

		partitionLayerFile(layerFile, partitionSize, jobBaseDir, featureIdToPartition);

		return featureIdToPartition.size();
	}

	private void validateParameters(
			MultipartFile polygonFile, MultipartFile layerFile, Integer partitionSize, Path jobBaseDir
	) {
		if (polygonFile == null) {
			throw new IllegalArgumentException("Polygon file cannot be null");
		}
		if (layerFile == null) {
			throw new IllegalArgumentException("Layer file cannot be null");
		}
		if (partitionSize == null) {
			throw new IllegalArgumentException("Partition size cannot be null");
		}
		if (partitionSize <= 0) {
			throw new IllegalArgumentException("Partition size must be positive, got: " + partitionSize);
		}
		if (jobBaseDir == null) {
			throw new IllegalArgumentException("Job base directory cannot be null");
		}
	}

	private int countTotalFeatureIds(MultipartFile polygonFile) throws IOException {
		int count = 0;
		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			String header = reader.readLine(); // Skip header
			if (header == null) {
				return 0;
			}
			String line;
			while ( (line = reader.readLine()) != null) {
				if (extractFeatureId(line) != null) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Calculate partition sizes for balanced distribution.
	 *
	 * @return array where index = partition number, value = number of records for that partition
	 */
	private int[] calculatePartitionSizes(int totalFeatureIds, int partitionSize) {
		int[] partitionSizes = new int[partitionSize];
		int chunkSize = totalFeatureIds / partitionSize;
		int remainder = totalFeatureIds % partitionSize;

		// First 'remainder' partitions get chunkSize + 1
		// Remaining partitions get chunkSize
		for (int i = 0; i < partitionSize; i++) {
			partitionSizes[i] = (i < remainder) ? chunkSize + 1 : chunkSize;
		}

		if (logger.isInfoEnabled()) {
			logger.info(
					"Total FEATURE_IDs: {}, Partition size: {}, Base chunk size: {}, Remainder: {}, Partition sizes: {}",
					totalFeatureIds, partitionSize, chunkSize, remainder, Arrays.toString(partitionSizes)
			);
		}
		return partitionSizes;
	}

	private Map<Long, Integer>
			partitionPolygonFile(MultipartFile polygonFile, int partitionSize, int[] partitionSizes, Path jobBaseDir)
					throws IOException {

		Map<Long, Integer> featureIdToPartition = new HashMap<>();

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {

			String header = reader.readLine();
			if (header == null) {
				throw new IOException("Polygon CSV file is empty or has no header");
			}

			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_POLYGON_FILE_NAME, header, partitionSize
			);

			try {
				processPolygonRecords(reader, writers, featureIdToPartition, partitionSizes);
			} finally {
				closeWriters(writers);
			}
		}

		logger.info("Processed and partitioned {} FEATURE_IDs", featureIdToPartition.size());
		return featureIdToPartition;
	}

	/**
	 * Reads exact number of records for each partition in sequence.
	 */
	@SuppressWarnings("javasecurity:S5131") // False positive: CSV file writing to internal storage, not HTTP response
	private void processPolygonRecords(
			BufferedReader reader, Map<Integer, PrintWriter> writers, Map<Long, Integer> featureIdToPartition,
			int[] partitionSizes
	) throws IOException {

		String line;
		int currentPartition = 0;
		int recordsInCurrentPartition = 0;

		while ( (line = reader.readLine()) != null) {
			Long featureId = extractFeatureId(line);
			if (featureId != null) {
				// Write to current partition
				featureIdToPartition.put(featureId, currentPartition);
				writers.get(currentPartition).println(line);
				recordsInCurrentPartition++;

				// Check if current partition is full, move to next
				if (recordsInCurrentPartition >= partitionSizes[currentPartition]) {
					currentPartition++;
					recordsInCurrentPartition = 0;

					// Safety check: don't exceed partition bounds
					if (currentPartition >= partitionSizes.length) {
						currentPartition = partitionSizes.length - 1;
					}
				}
			}
		}
	}

	private void partitionLayerFile(
			MultipartFile layerFile, int partitionSize, Path jobBaseDir, Map<Long, Integer> featureIdToPartition
	) throws IOException {

		try (
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(layerFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {

			String header = reader.readLine();
			if (header == null) {
				throw new IOException("Layer CSV file is empty or has no header");
			}

			Map<Integer, PrintWriter> writers = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_LAYER_FILE_NAME, header, partitionSize
			);

			try {
				processLayerRecords(reader, writers, featureIdToPartition);
			} finally {
				closeWriters(writers);
			}
		}
	}

	@SuppressWarnings("javasecurity:S5131") // False positive: CSV file writing to internal storage, not HTTP response
	private void processLayerRecords(
			BufferedReader reader, Map<Integer, PrintWriter> writers, Map<Long, Integer> featureIdToPartition
	) throws IOException {

		String line;
		while ( (line = reader.readLine()) != null) {
			Long featureId = extractFeatureId(line);
			if (featureId != null && featureIdToPartition.containsKey(featureId)) {
				int partition = featureIdToPartition.get(featureId);
				writers.get(partition).println(line);
			}
		}
	}

	/**
	 * Extract FEATURE_ID from the first field of a CSV line.
	 */
	private Long extractFeatureId(String csvLine) {
		if (csvLine == null || csvLine.trim().isEmpty()) {
			return null;
		}

		try {
			int commaIndex = csvLine.indexOf(',');
			if (commaIndex == -1) {
				// No comma found, entire line might be the FEATURE_ID
				return Long.parseLong(csvLine.trim());
			} else {
				// Extract first field before comma
				String featureIdStr = csvLine.substring(0, commaIndex).trim();
				return Long.parseLong(featureIdStr);
			}
		} catch (NumberFormatException e) {
			logger.debug("Could not parse FEATURE_ID from line: {}", csvLine);
			return null;
		}
	}

	/**
	 * Create PrintWriters for each partition.
	 */
	private Map<Integer, PrintWriter>
			createPartitionWriters(Path baseDir, String filename, String header, Integer partitionSize)
					throws IOException {

		if (baseDir == null) {
			throw new IllegalArgumentException("Base directory cannot be null");
		}
		if (filename == null || filename.isBlank()) {
			throw new IllegalArgumentException("Filename cannot be null or blank");
		}
		if (header == null) {
			throw new IllegalArgumentException("Header cannot be null");
		}
		if (partitionSize == null || partitionSize <= 0) {
			throw new IllegalArgumentException("Partition size must be positive, got: " + partitionSize);
		}

		Map<Integer, PrintWriter> writers = new HashMap<>();

		try {
			for (int i = 0; i < partitionSize; i++) {
				Path partitionDir = baseDir.resolve(BatchConstants.Partition.INPUT_FOLDER_NAME_PREFIX + i);
				Files.createDirectories(partitionDir);

				Path csvFile = partitionDir.resolve(filename);
				BufferedWriter bufferedWriter = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
				PrintWriter writer = new PrintWriter(bufferedWriter, false);
				writer.println(header); // Write header first
				writers.put(i, writer);
			}
			return writers;
		} catch (IOException e) {
			closeWriters(writers);
			throw e;
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
					logger.warn("Error closing writer", e);
				}
			}
		}
	}
}
