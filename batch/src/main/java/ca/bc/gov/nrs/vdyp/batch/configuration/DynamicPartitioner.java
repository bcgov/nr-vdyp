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
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic partitioner for VDYP batch processing that divides CSV file processing by record position ranges.
 *
 * This partitioner determines the total record count and creates partitions based on sequential record positions.
 */
public class DynamicPartitioner implements Partitioner {

	private static final Logger logger = LoggerFactory.getLogger(DynamicPartitioner.class);

	private static final String START_LINE = "startLine";
	private static final String END_LINE = "endLine";
	private static final String PARTITION_NAME = "partitionName";
	private static final String PARTITION_0 = "partition0";

	// Input resource will be set dynamically during execution
	private Resource inputResource;

	public void setInputResource(Resource inputResource) {
		this.inputResource = inputResource;
	}

	@Override
	@NonNull
	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> partitions = new HashMap<>();

		// Check if input resource is available
		if (inputResource == null) {
			logger.warn("[VDYP Partitioner] Warning: Input resource not set. Using default single partition.");
			// Create single empty partition
			ExecutionContext context = new ExecutionContext();
			context.putLong(START_LINE, 2);
			context.putLong(END_LINE, 2);
			context.putString(PARTITION_NAME, PARTITION_0);
			partitions.put(PARTITION_0, context);
			return partitions;
		}

		// Calculate total record count by reading the actual CSV file
		long totalRecords = calculateTotalRecords();

		if (totalRecords <= 0) {
			logger.warn("[VDYP Partitioner] Warning: No records found in CSV file. Using single partition.");
			// Fallback: create single partition
			ExecutionContext context = new ExecutionContext();
			context.putLong(START_LINE, 2); // Skip header (line 1)
			context.putLong(END_LINE, 2);
			context.putString(PARTITION_NAME, PARTITION_0);
			partitions.put(PARTITION_0, context);
			return partitions;
		}

		// Divide records by position, not by ID values
		long recordsPerPartition = totalRecords / gridSize;
		long remainder = totalRecords % gridSize;

		long currentStartLine = 2; // Start after header (line 1)

		for (int i = 0; i < gridSize; i++) {
			ExecutionContext context = new ExecutionContext();

			// Calculate line range for this partition
			long recordsInThisPartition = recordsPerPartition;

			// Add remainder to the last partition
			if (i == gridSize - 1) {
				recordsInThisPartition += remainder;
			}

			long currentEndLine = currentStartLine + recordsInThisPartition - 1;

			// Set partition parameters - using line-based ranges
			context.putLong(START_LINE, currentStartLine);
			context.putLong(END_LINE, currentEndLine);
			context.putString(PARTITION_NAME, "partition" + i);

			partitions.put("partition" + i, context);

			logger.info(
					"VDYP partition{} created: lines {}-{} ({} records)", i, currentStartLine, currentEndLine,
					recordsInThisPartition
			);

			currentStartLine = currentEndLine + 1;
		}

		logger.info(
				"VDYP total partitions: {}, covering {} records (lines 2-{})", gridSize, totalRecords,
				currentStartLine - 1
		);

		return partitions;
	}

	/**
	 * Calculate total record count by reading the VDYP CSV file and counting data lines.
	 *
	 * This method counts the number of data records (excluding header) for position-based partitioning of VDYP data.
	 *
	 * @return Total number of data records
	 */
	private long calculateTotalRecords() {
		logger.info("[VDYP Partitioner] Calculating total records from VDYP CSV file...");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputResource.getInputStream()))) {
			String line;
			long recordCount = 0;
			int lineNumber = 0;

			// Skip header line
			String headerLine = reader.readLine();
			if (headerLine != null) {
				lineNumber = 1;
				logger.info("[VDYP Partitioner] Header: {}", headerLine);
			}

			// Count data records
			while ( (line = reader.readLine()) != null) {
				lineNumber++;

				if (!line.trim().isEmpty()) {
					recordCount++;
				}
			}

			logger.info("[VDYP Partitioner] CSV Analysis Complete:");
			logger.info("  - Total lines in file: {}", lineNumber);
			logger.info("  - VDYP data records found: {}", recordCount);
			logger.info("  - Using position-based partitioning for efficient parallel VDYP processing");

			return recordCount;

		} catch (IOException e) {
			logger.error("[VDYP Partitioner] Error reading CSV file: {}", e.getMessage(), e);
			return 0;
		}
	}
}