package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * FEATURE_ID-based partitioner for VDYP batch processing.
 *
 * This partitioner implements the FEATURE_ID-based partitioning strategy that ensures each partition contains complete
 * polygon data (1 polygon + all associated layers).
 *
 * Partitioning Strategy: - Reads polygon CSV to extract unique FEATURE_IDs - Distributes FEATURE_ID ranges across
 * partitions using simple sizing (explicit partitionSize or default grid-size) - Each worker gets assigned specific
 * FEATURE_IDs and processes complete Polygon objects
 */
public class DynamicPartitioner implements Partitioner {

	private static final Logger logger = LoggerFactory.getLogger(DynamicPartitioner.class);

	private static final String FEATURE_ID_RANGE_START = "featureIdRangeStart";
	private static final String FEATURE_ID_RANGE_END = "featureIdRangeEnd";
	private static final String ASSIGNED_FEATURE_IDS = "assignedFeatureIds";
	private static final String PARTITION_NAME = "partitionName";
	private static final String PARTITION_0 = "partition0";

	private Resource polygonResource;

	public void setPolygonResource(Resource polygonResource) {
		this.polygonResource = polygonResource;
	}

	/**
	 * Creates partitions based on unique FEATURE_IDs from polygon CSV file. Each partition contains complete polygon
	 * data (polygon + all associated layers).
	 *
	 * @param gridSize Number of partitions to create
	 * @return Map of partition execution contexts with assigned FEATURE_IDs
	 */
	@Override
	@NonNull
	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> partitions = new HashMap<>();

		// Check if polygon resource is available
		if (polygonResource == null) {
			logger.warn(
					"[VDYP FEATURE_ID Partitioner] Warning: Polygon resource not set. Using default single partition."
			);
			// Create single empty partition
			ExecutionContext context = new ExecutionContext();
			context.putString(ASSIGNED_FEATURE_IDS, "");
			context.putString(PARTITION_NAME, PARTITION_0);
			partitions.put(PARTITION_0, context);
			return partitions;
		}

		// Extract unique FEATURE_IDs from polygon CSV
		List<Long> uniqueFeatureIds = extractUniqueFeatureIds();

		if (uniqueFeatureIds.isEmpty()) {
			logger.warn(
					"[VDYP FEATURE_ID Partitioner] Warning: No FEATURE_IDs found in polygon CSV file. Using single partition."
			);
			// Fallback: create single partition
			ExecutionContext context = new ExecutionContext();
			context.putString(ASSIGNED_FEATURE_IDS, "");
			context.putString(PARTITION_NAME, PARTITION_0);
			partitions.put(PARTITION_0, context);
			return partitions;
		}

		// SIMPLE PARTITION SIZING: Use provided partitionSize or default grid size
		int actualGridSize = gridSize;

		logger.info(
				"[VDYP FEATURE_ID Partitioner] Using partition size: {}, FEATURE_IDs: {}", actualGridSize,
				uniqueFeatureIds.size()
		);

		// Sort FEATURE_IDs for consistent partitioning
		Collections.sort(uniqueFeatureIds);

		// Divide FEATURE_IDs across partitions
		int totalFeatureIds = uniqueFeatureIds.size();
		int featureIdsPerPartition = Math.max(1, totalFeatureIds / actualGridSize);
		int remainder = totalFeatureIds % actualGridSize;

		int currentIndex = 0;

		for (int i = 0; i < actualGridSize && currentIndex < totalFeatureIds; i++) {
			ExecutionContext context = new ExecutionContext();

			// Calculate FEATURE_ID range for this partition
			int featureIdsInThisPartition = featureIdsPerPartition;

			// Add remainder to the last partitions
			if (i < remainder) {
				featureIdsInThisPartition++;
			}

			int endIndex = Math.min(currentIndex + featureIdsInThisPartition, totalFeatureIds);

			List<Long> assignedFeatureIds = uniqueFeatureIds.subList(currentIndex, endIndex);

			// Convert to comma-separated string for easy parsing
			String featureIdList = assignedFeatureIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b)
					.orElse("");

			// Set partition parameters - using FEATURE_ID assignments
			context.putString(ASSIGNED_FEATURE_IDS, featureIdList);
			context.putString(PARTITION_NAME, "partition" + i);

			// For backward compatibility, also set range bounds
			if (!assignedFeatureIds.isEmpty()) {
				context.putLong(FEATURE_ID_RANGE_START, assignedFeatureIds.get(0));
				context.putLong(FEATURE_ID_RANGE_END, assignedFeatureIds.get(assignedFeatureIds.size() - 1));
			}

			partitions.put("partition" + i, context);

			logger.info(
					"VDYP partition{} created: FEATURE_IDs [{}] ({} polygons)", i, featureIdList,
					assignedFeatureIds.size()
			);

			currentIndex = endIndex;
		}

		logger.info(
				"FEATURE_ID-based partitioner created {} partitions for {} unique FEATURE_IDs", partitions.size(),
				totalFeatureIds
		);

		return partitions;
	}

	/**
	 * Extract unique FEATURE_IDs from the polygon CSV file.
	 *
	 * @return Sorted list of unique FEATURE_IDs found in polygon CSV
	 */
	private List<Long> extractUniqueFeatureIds() {
		logger.info("[VDYP FEATURE_ID Partitioner] Extracting unique FEATURE_IDs from polygon CSV...");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(polygonResource.getInputStream()))) {
			// Validate header
			if (!validateHeader(reader)) {
				return new ArrayList<>();
			}

			// Extract FEATURE_IDs from data rows
			Set<Long> uniqueFeatureIds = extractFeatureIdsFromDataRows(reader);

			logger.info("[VDYP FEATURE_ID Partitioner] Extracted {} unique FEATURE_IDs", uniqueFeatureIds.size());

			return new ArrayList<>(uniqueFeatureIds);

		} catch (IOException e) {
			logger.error("[VDYP FEATURE_ID Partitioner] Error reading polygon CSV file", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Validates the CSV header and checks for FEATURE_ID column.
	 *
	 * @param reader BufferedReader for the CSV file
	 * @return true if header is valid, false otherwise
	 * @throws IOException if reading fails
	 */
	private boolean validateHeader(BufferedReader reader) throws IOException {
		String headerLine = reader.readLine();
		if (headerLine == null) {
			logger.error("[VDYP FEATURE_ID Partitioner] No header found in CSV file");
			return false;
		}

		logger.debug("[VDYP FEATURE_ID Partitioner] Header: {}", headerLine);

		if (!headerLine.toUpperCase().contains("FEATURE_ID")) {
			logger.error("[VDYP FEATURE_ID Partitioner] FEATURE_ID column not found in header: {}", headerLine);
			return false;
		}

		return true;
	}

	/**
	 * Extracts FEATURE_IDs from data rows in the CSV file.
	 *
	 * @param reader BufferedReader for the CSV file
	 * @return Set of unique FEATURE_IDs
	 * @throws IOException if reading fails
	 */
	private Set<Long> extractFeatureIdsFromDataRows(BufferedReader reader) throws IOException {
		Set<Long> uniqueFeatureIds = new HashSet<>();
		String line;
		int lineNumber = 1; // Start from 1 since header was already read

		while ( (line = reader.readLine()) != null) {
			lineNumber++;
			processDataLine(line, lineNumber, uniqueFeatureIds);
		}

		return uniqueFeatureIds;
	}

	/**
	 * Processes a single data line to extract FEATURE_ID.
	 *
	 * @param line             The CSV line to process
	 * @param lineNumber       Current line number for logging
	 * @param uniqueFeatureIds Set to add the extracted FEATURE_ID to
	 */
	private void processDataLine(String line, int lineNumber, Set<Long> uniqueFeatureIds) {
		if (line.trim().isEmpty()) {
			return;
		}

		try {
			String[] columns = line.split(",");
			if (columns.length > 0) {
				String featureIdStr = columns[0].trim();
				if (!featureIdStr.isEmpty()) {
					Long featureId = Long.parseLong(featureIdStr);
					uniqueFeatureIds.add(featureId);
					logger.debug("[VDYP FEATURE_ID Partitioner] Found FEATURE_ID: {}", featureId);
				}
			}
		} catch (NumberFormatException e) {
			logger.warn("[VDYP FEATURE_ID Partitioner] Invalid FEATURE_ID at line {}: {}", lineNumber, line);
		}
	}
}