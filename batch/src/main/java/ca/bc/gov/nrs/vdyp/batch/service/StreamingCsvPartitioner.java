package ca.bc.gov.nrs.vdyp.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.web.multipart.MultipartFile;
import ca.bc.gov.nrs.vdyp.batch.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming CSV partitioner that partitions CSV files by FEATURE_ID without
 * parsing the entire CSV into objects.
 */
@Component
public class StreamingCsvPartitioner {

	private static final Logger logger = LoggerFactory.getLogger(StreamingCsvPartitioner.class);

	/**
	 * Partition CSV files by FEATURE_ID using line-based processing.
	 * Creates separate CSV files for each partition containing only the data
	 * for that partition's assigned FEATURE_IDs.
	 */
	public PartitionResult partitionCsvFiles(MultipartFile polygonFile, MultipartFile layerFile,
			int gridSize, Path baseOutputDir) throws IOException {

		logger.info("Starting streaming CSV partitioning with grid size: {}", gridSize);
		if (logger.isInfoEnabled()) {
			logger.info("Processing files: polygon={} ({} bytes), layer={} ({} bytes)",
					Utils.sanitizeForLogging(polygonFile.getOriginalFilename()), polygonFile.getSize(),
					Utils.sanitizeForLogging(layerFile.getOriginalFilename()), layerFile.getSize());
		}

		if (!Files.exists(baseOutputDir)) {
			Files.createDirectories(baseOutputDir);
		}

		// Step 1: Scan and partition polygon CSV
		Map<Long, Integer> featureIdToPartition = new HashMap<>();
		String polygonHeader = null;

		try (BufferedReader polygonReader = new BufferedReader(new InputStreamReader(polygonFile.getInputStream()))) {
			polygonHeader = polygonReader.readLine(); // Read header
			if (polygonHeader == null) {
				throw new IOException("Polygon CSV file is empty or has no header");
			}

			Map<Integer, PrintWriter> polygonWriters = createPartitionWriters(baseOutputDir, "polygons.csv",
					polygonHeader, gridSize);

			String line;
			int partitionIndex = 0;
			while ((line = polygonReader.readLine()) != null) {
				Long featureId = extractFeatureId(line);
				if (featureId != null) {
					// Determine partition for this FEATURE_ID
					int partition = partitionIndex % gridSize;
					featureIdToPartition.put(featureId, partition);

					// Write to the appropriate partition file
					polygonWriters.get(partition).println(line);

					partitionIndex++;
				}
			}

			closeWriters(polygonWriters);
		}

		logger.info("Processed and partitioned {} FEATURE_IDs", featureIdToPartition.size());

		// Step 2: Process layer CSV
		String layerHeader = null;
		Map<Integer, PrintWriter> layerWriters = null;

		try (BufferedReader layerReader = new BufferedReader(new InputStreamReader(layerFile.getInputStream()))) {
			layerHeader = layerReader.readLine(); // Read header
			if (layerHeader == null) {
				throw new IOException("Layer CSV file is empty or has no header");
			}

			layerWriters = createPartitionWriters(baseOutputDir, "layers.csv", layerHeader, gridSize);

			String line;
			while ((line = layerReader.readLine()) != null) {
				Long featureId = extractFeatureId(line);
				if (featureId != null && featureIdToPartition.containsKey(featureId)) {
					int partition = featureIdToPartition.get(featureId);
					layerWriters.get(partition).println(line);
				}
			}

			closeWriters(layerWriters);
		}

		// Step 3: Calculate partition statistics
		Map<Integer, Long> partitionCounts = new ConcurrentHashMap<>();
		for (Map.Entry<Long, Integer> entry : featureIdToPartition.entrySet()) {
			partitionCounts.merge(entry.getValue(), 1L, Long::sum);
		}

		logger.info("Partitioning completed. Partition distribution: {}", partitionCounts);

		return new PartitionResult(baseOutputDir, gridSize, partitionCounts, featureIdToPartition.size());
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
	private Map<Integer, PrintWriter> createPartitionWriters(Path baseDir, String filename,
			String header, int gridSize) throws IOException {

		Map<Integer, PrintWriter> writers = new HashMap<>();

		for (int i = 0; i < gridSize; i++) {
			Path partitionDir = baseDir.resolve("partition" + i);
			Files.createDirectories(partitionDir);

			Path csvFile = partitionDir.resolve(filename);
			PrintWriter writer = new PrintWriter(new FileWriter(csvFile.toFile()));
			writer.println(header); // Write header first
			writers.put(i, writer);
		}

		return writers;
	}

	/**
	 * Close all writers safely.
	 */
	private void closeWriters(Map<Integer, PrintWriter> writers) {
		if (writers != null) {
			for (PrintWriter writer : writers.values()) {
				try {
					writer.close();
				} catch (Exception e) {
					logger.warn("Error closing writer", e);
				}
			}
		}
	}

	/**
	 * Result object containing partitioning information.
	 */
	public static class PartitionResult {
		private final Path baseOutputDir;
		private final int gridSize;
		private final Map<Integer, Long> partitionCounts;
		private final int totalFeatureIds;

		public PartitionResult(Path baseOutputDir, int gridSize, Map<Integer, Long> partitionCounts,
				int totalFeatureIds) {
			this.baseOutputDir = baseOutputDir;
			this.gridSize = gridSize;
			this.partitionCounts = partitionCounts;
			this.totalFeatureIds = totalFeatureIds;
		}

		public Path getBaseOutputDir() {
			return baseOutputDir;
		}

		public int getGridSize() {
			return gridSize;
		}

		public Map<Integer, Long> getPartitionCounts() {
			return partitionCounts;
		}

		public int getTotalFeatureIds() {
			return totalFeatureIds;
		}

		public Path getPartitionDir(int partitionIndex) {
			return baseOutputDir.resolve("partition" + partitionIndex);
		}
	}

}
