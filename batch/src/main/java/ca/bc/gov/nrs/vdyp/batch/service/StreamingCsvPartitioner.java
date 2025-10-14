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

		// Validate parameters
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

		// Step 1: Scan and partition polygon CSV
		Map<Long, Integer> featureIdToPartition = new HashMap<>();
		String polygonHeader = null;
		Map<Integer, PrintWriter> polygonWriters = null;

		try (
				BufferedReader polygonReader = new BufferedReader(
						new InputStreamReader(polygonFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			polygonHeader = polygonReader.readLine(); // Read header
			if (polygonHeader == null) {
				throw new IOException("Polygon CSV file is empty or has no header");
			}

			polygonWriters = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_POLYGON_FILE_NAME, polygonHeader, partitionSize
			);

			String line;
			int partitionIndex = 0;
			while ( (line = polygonReader.readLine()) != null) {
				Long featureId = extractFeatureId(line);
				if (featureId != null) {
					// Determine partition for this FEATURE_ID
					int partition = partitionIndex % partitionSize;
					featureIdToPartition.put(featureId, partition);

					// Write to the appropriate partition file
					polygonWriters.get(partition).println(line);

					partitionIndex++;
				}
			}
		} finally {
			closeWriters(polygonWriters);
		}

		logger.info("Processed and partitioned {} FEATURE_IDs", featureIdToPartition.size());

		// Step 2: Process layer CSV
		String layerHeader = null;
		Map<Integer, PrintWriter> layerWriters = null;

		try (
				BufferedReader layerReader = new BufferedReader(
						new InputStreamReader(layerFile.getInputStream(), StandardCharsets.UTF_8)
				)
		) {
			layerHeader = layerReader.readLine(); // Read header
			if (layerHeader == null) {
				throw new IOException("Layer CSV file is empty or has no header");
			}

			layerWriters = createPartitionWriters(
					jobBaseDir, BatchConstants.Partition.INPUT_LAYER_FILE_NAME, layerHeader, partitionSize
			);

			String line;
			while ( (line = layerReader.readLine()) != null) {
				Long featureId = extractFeatureId(line);
				if (featureId != null && featureIdToPartition.containsKey(featureId)) {
					int partition = featureIdToPartition.get(featureId);
					layerWriters.get(partition).println(line);
				}
			}
		} finally {
			closeWriters(layerWriters);
		}

		return featureIdToPartition.size();
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

		// Validate parameters (defensive programming)
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
