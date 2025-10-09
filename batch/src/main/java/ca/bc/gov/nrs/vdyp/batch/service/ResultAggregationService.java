package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchIOException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Service responsible for aggregating VDYP projection results from all partitions into a single consolidated output ZIP
 * file.
 */
@Service
public class ResultAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(ResultAggregationService.class);

	/**
	 * Aggregates all partition results into a single consolidated ZIP file within the job directory.
	 *
	 * @return Path to the consolidated ZIP file
	 */
	public Path aggregateResultsFromJobDir(Long jobExecutionId, String jobBaseDir, String jobTimestamp)
			throws IOException {
		logger.info(
				"Starting result aggregation for job execution: {} from job directory: {}", jobExecutionId, jobBaseDir
		);

		if (jobBaseDir == null || jobBaseDir.trim().isEmpty()) {
			throw new IllegalArgumentException("Job base directory cannot be null or empty");
		}

		if (jobTimestamp == null || jobTimestamp.trim().isEmpty()) {
			throw new IllegalArgumentException("Job timestamp cannot be null or empty");
		}

		Path jobBasePath = Paths.get(jobBaseDir);
		if (!Files.exists(jobBasePath)) {
			throw new IOException("Job base directory does not exist: " + jobBaseDir);
		}

		if (!Files.isDirectory(jobBasePath)) {
			throw new IOException("Job base path is not a directory: " + jobBaseDir);
		}

		logger.info("Using job base directory: {}", jobBasePath);

		// Collect all partition output directories from job-specific directory
		List<Path> partitionOutputDirs = findPartitionOutputDirectories(jobBasePath);
		logger.info("Found {} partition output directories to aggregate", partitionOutputDirs.size());

		// Create final ZIP file using same timestamp as job base directory
		String finalZipFileName = String.format("vdyp-output-%s.zip", jobTimestamp);
		Path finalZipPath = jobBasePath.resolve(finalZipFileName);

		if (partitionOutputDirs.isEmpty()) {
			logger.warn("No partition output directories found for aggregation");
			return createEmptyResultZip(finalZipPath);
		}

		// Aggregate results
		try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(finalZipPath))) {
			aggregateYieldTables(partitionOutputDirs, zipOut);
			aggregateLogs(partitionOutputDirs, zipOut);

			logger.info("Successfully created consolidated ZIP file: {}", finalZipPath);
		}

		return finalZipPath;
	}

	/**
	 * Finds all partition output directories in the job base directory.
	 */
	private List<Path> findPartitionOutputDirectories(Path jobBasePath) throws IOException {
		List<Path> partitionDirs = new ArrayList<>();

		logger.info("Searching for partition output directories in: {}", jobBasePath);

		// List all items in base directory for debugging
		try (Stream<Path> allItems = Files.list(jobBasePath)) {
			allItems.forEach(
					item -> logger
							.debug("Found item: {} (isDirectory: {})", item.getFileName(), Files.isDirectory(item))
			);
		}

		try (Stream<Path> files = Files.list(jobBasePath)) {
			files.filter(Files::isDirectory).filter(dir -> {
				String dirName = dir.getFileName().toString();
				boolean matches = dirName.startsWith(BatchConstants.Partition.OUTPUT_FOLDER_NAME_PREFIX)
						|| dirName.matches(BatchConstants.Partition.OUTPUT_FOLDER_NAME_PREFIX + "\\d+");
				logger.debug("Directory {} matches output partition pattern: {}", dirName, matches);
				return matches;
			}).forEach(partitionDirs::add);
		}

		logger.info(
				"Found {} partition directories: {}", partitionDirs.size(),
				partitionDirs.stream().map(p -> p.getFileName().toString()).toList()
		);

		// Sort partition directories by name to ensure consistent processing order.
		// This is critical for TABLE_NUM assignment in yield tables,
		// as TABLE_NUM is assigned sequentially based on the order in which
		// polygon/layer combinations are encountered during partition aggregation.
		partitionDirs.sort(Comparator.comparing(path -> path.getFileName().toString()));
		return partitionDirs;
	}

	/**
	 * Aggregates yield tables from all partitions, merging tables of the same type.
	 */
	private void aggregateYieldTables(List<Path> partitionOutputDirs, ZipOutputStream zipOut) throws IOException {
		logger.info("Aggregating yield tables from {} partitions", partitionOutputDirs.size());

		Map<String, List<Path>> yieldTablesByType = new HashMap<>();

		// Collect all yield tables by type
		for (Path partitionOutputDir : partitionOutputDirs) {
			collectYieldTablesFromPartition(partitionOutputDir, yieldTablesByType);
		}

		if (yieldTablesByType.isEmpty()) {
			logger.warn("No yield tables found in any partition directory");
		}

		// Merge and add to ZIP
		for (Map.Entry<String, List<Path>> entry : yieldTablesByType.entrySet()) {
			List<Path> tablePaths = entry.getValue();

			if (!tablePaths.isEmpty()) {
				mergeYieldTables(tablePaths, zipOut);
			}
		}

		logger.info("Aggregated {} different types of yield tables", yieldTablesByType.size());
	}

	/**
	 * Collects yield table files from a partition output directory.
	 */
	private void collectYieldTablesFromPartition(Path partitionOutputDir, Map<String, List<Path>> yieldTablesByType)
			throws IOException {
		if (!isValidPartitionDirectory(partitionOutputDir)) {
			return;
		}

		try (Stream<Path> files = Files.walk(partitionOutputDir)) {
			files.filter(Files::isRegularFile).filter(file -> isYieldTableFile(file.getFileName().toString())).forEach(
					file -> yieldTablesByType
							.computeIfAbsent(BatchConstants.File.YIELD_TABLE_TYPE, k -> new ArrayList<>()).add(file)
			);
		} catch (IOException e) {
			throw BatchIOException.handleDirectoryWalkFailure(
					partitionOutputDir, e, "Error walking directory tree for yield tables", logger
			);
		}
	}

	/**
	 * Determines if a file is a yield table based on its name.
	 */
	private boolean isYieldTableFile(String fileName) {
		String lowerName = fileName.toLowerCase();
		return (lowerName.contains("yield")) && !isLogFile(fileName);
	}

	/**
	 * Merges multiple yield tables of the same type into a single file in the ZIP. Assigns TABLE_NUM based on
	 * polygon/layer combinations.
	 */
	private void mergeYieldTables(List<Path> tablePaths, ZipOutputStream zipOut) throws IOException {
		ZipEntry zipEntry = new ZipEntry(BatchConstants.File.YIELD_TABLE_FILENAME);
		zipOut.putNextEntry(zipEntry);

		TableNumberAssigner tableNumberAssigner = new TableNumberAssigner();
		boolean isFirstFile = true;

		for (Path tablePath : tablePaths) {
			isFirstFile = processYieldTableFile(tablePath, zipOut, tableNumberAssigner, isFirstFile);
		}

		zipOut.closeEntry();
		logger.debug(
				"Merged {} files into yield table: {} with {} unique polygon/layer combinations", tablePaths.size(),
				BatchConstants.File.YIELD_TABLE_FILENAME, tableNumberAssigner.getUniqueCount()
		);
	}

	/**
	 * Processes a single yield table file and writes its content to the ZIP output stream.
	 */
	private boolean processYieldTableFile(
			Path tablePath, ZipOutputStream zipOut, TableNumberAssigner tableNumberAssigner, boolean isFirstFile
	) throws IOException {
		if (!Files.exists(tablePath)) {
			logger.warn("Yield table file does not exist: {}", tablePath);
			return isFirstFile;
		}

		if (!Files.isReadable(tablePath)) {
			logger.warn("Yield table file is not readable: {}", tablePath);
			return isFirstFile;
		}

		try (Stream<String> lines = Files.lines(tablePath, StandardCharsets.UTF_8)) {
			Iterator<String> lineIterator = lines.iterator();

			if (lineIterator.hasNext()) {
				isFirstFile = processFirstLine(lineIterator.next(), zipOut, tableNumberAssigner, isFirstFile);
			}

			processRemainingLines(lineIterator, zipOut, tableNumberAssigner);
		} catch (IOException e) {
			throw BatchIOException.handleFileReadFailure(tablePath, e, "Error reading yield table file", logger);
		}
		return false; // After processing first file, subsequent files are not first
	}

	/**
	 * Processes the first line of a yield table file (header or data line).
	 */
	private boolean processFirstLine(
			String firstLine, ZipOutputStream zipOut, TableNumberAssigner tableNumberAssigner, boolean isFirstFile
	) throws IOException {
		if (isHeaderLine(firstLine)) {
			if (isFirstFile) {
				writeLineToZip(firstLine, zipOut);
			}
			return false; // Header processed, subsequent files are not first
		} else {
			// Not a header, this is a data line - process it
			processDataLine(firstLine, zipOut, tableNumberAssigner);
			return isFirstFile; // Keep first file status for data-only files
		}
	}

	/**
	 * Processes the remaining data lines from a yield table file.
	 */
	private void processRemainingLines(
			Iterator<String> lineIterator, ZipOutputStream zipOut, TableNumberAssigner tableNumberAssigner
	) throws IOException {
		while (lineIterator.hasNext()) {
			processDataLine(lineIterator.next(), zipOut, tableNumberAssigner);
		}
	}

	/**
	 * Processes a single data line and writes it to the ZIP output stream.
	 */
	private void processDataLine(String line, ZipOutputStream zipOut, TableNumberAssigner tableNumberAssigner)
			throws IOException {
		String processedLine = tableNumberAssigner.assignTableNumber(line);
		if (processedLine != null) {
			writeLineToZip(processedLine, zipOut);
		}
	}

	/**
	 * Writes a line to the ZIP output stream with proper line separator.
	 */
	private void writeLineToZip(String line, ZipOutputStream zipOut) throws IOException {
		zipOut.write( (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
	}

	private class TableNumberAssigner {
		private final Map<String, Integer> polygonLayerTableNumbers = new HashMap<>();
		private int nextTableNum = 1;

		/**
		 * Assigns TABLE_NUM based on polygon/layer combination.
		 *
		 * @param line The CSV line to process
		 *
		 * @return The processed line with correct TABLE_NUM, or null if line should be skipped
		 */
		public String assignTableNumber(String line) {
			if (line == null || line.trim().isEmpty()) {
				return line;
			}

			// Split the line by comma
			String[] columns = line.split(",", -1);

			if (columns.length < 6) {
				// Not enough columns, return as-is
				return line;
			}

			// Extract FEATURE_ID (column 1) and LAYER_ID (column 5) based on CSV structure
			String featureId = columns.length > 1 ? columns[1].trim() : "";
			String layerId = columns.length > 5 ? columns[5].trim() : "";

			if (featureId.isEmpty()) {
				// No FEATURE_ID, skip this line
				logger.warn("Skipping line with missing FEATURE_ID: {}", line);
				return null;
			}

			// Create unique key for polygon/layer combination
			String polygonLayerKey = featureId + "_" + layerId;

			// Get or assign TABLE_NUM for this polygon/layer combination
			Integer tableNum = polygonLayerTableNumbers.computeIfAbsent(polygonLayerKey, key -> {
				// Check for overflow before assigning
				if (nextTableNum >= Integer.MAX_VALUE - 1) {
					logger.error("TABLE_NUM overflow detected. Current count: {}", polygonLayerTableNumbers.size());
					throw new IllegalStateException(
							"TABLE_NUM exceeded maximum value. Too many polygon/layer combinations."
					);
				}

				int assigned = nextTableNum++;
				logger.debug(
						"Assigned TABLE_NUM {} to polygon/layer combination: FEATURE_ID={}, LAYER_ID={}", assigned,
						featureId, layerId
				);
				return assigned;
			});

			// Replace TABLE_NUM (first column) with the assigned number
			columns[0] = String.valueOf(tableNum);

			// Rejoin the columns
			return String.join(",", columns);
		}

		/**
		 * Gets the number of unique polygon/layer combinations processed.
		 */
		public int getUniqueCount() {
			return polygonLayerTableNumbers.size();
		}
	}

	/**
	 * Determines if a line is a header line.
	 */
	private boolean isHeaderLine(String line) {
		if (line == null || line.trim().isEmpty()) {
			return true; // Treat empty lines as headers (skip them)
		}

		String upperLine = line.toUpperCase();
		// check if it starts with header keywords
		return upperLine.startsWith("TABLE") || upperLine.startsWith("FEATURE") || upperLine.startsWith("POLYGON")
				|| upperLine.contains("LAYER_ID") || upperLine.contains("SPECIES_CODE");
	}

	/**
	 * Validates if a partition directory is valid for processing.
	 */
	private boolean isValidPartitionDirectory(Path partitionDir) {
		if (partitionDir == null || !Files.exists(partitionDir)) {
			logger.warn("Partition directory does not exist or is null: {}", partitionDir);
			return false;
		}

		if (!Files.isDirectory(partitionDir)) {
			logger.warn("Partition path is not a directory: {}", partitionDir);
			return false;
		}

		return true;
	}

	/**
	 * Aggregates log files from all partitions.
	 */
	private void aggregateLogs(List<Path> partitionDirs, ZipOutputStream zipOut) throws IOException {
		logger.info("Aggregating log files from {} partitions", partitionDirs.size());

		Map<String, List<Path>> logsByType = new HashMap<>();

		// Filter valid directories first, then collect log files
		partitionDirs.stream().filter(this::isValidPartitionDirectory)
				.forEach(partitionDir -> collectLogFilesFromPartition(partitionDir, logsByType));

		// Merge and add to ZIP
		for (Map.Entry<String, List<Path>> entry : logsByType.entrySet()) {
			String logType = entry.getKey();
			List<Path> logPaths = entry.getValue();

			if (!logPaths.isEmpty()) {
				mergeLogs(logType, logPaths, zipOut);
			}
		}

		logger.info("Aggregated {} different types of log files", logsByType.size());
	}

	/**
	 * Collects log files from a single partition directory.
	 */
	private void collectLogFilesFromPartition(Path partitionDir, Map<String, List<Path>> logsByType) {
		try (Stream<Path> files = Files.walk(partitionDir)) {
			files.filter(Files::isRegularFile).filter(file -> isLogFile(file.getFileName().toString()))
					.forEach(file -> {
						String logType = extractLogType(file.getFileName().toString());
						logsByType.computeIfAbsent(logType, k -> new ArrayList<>()).add(file);
					});
		} catch (IOException e) {
			IOException wrappedException = BatchIOException
					.handleDirectoryWalkFailure(partitionDir, e, "Error walking directory tree for log files", logger);
			throw new BatchException("Failed to collect log files from partition", wrappedException);
		}
	}

	/**
	 * Determines if a file is a log file.
	 */
	private boolean isLogFile(String fileName) {
		String lowerName = fileName.toLowerCase();
		return lowerName.contains("log") || lowerName.contains("error") || lowerName.contains("progress")
				|| lowerName.contains("debug");
	}

	/**
	 * Extracts the log type from the filename.
	 */
	private String extractLogType(String fileName) {
		String lowerName = fileName.toLowerCase();
		if (lowerName.contains("error")) {
			return "Error";
		}
		if (lowerName.contains("progress")) {
			return "Progress";
		}
		if (lowerName.contains("debug")) {
			return "Debug";
		}

		return "General";
	}

	/**
	 * Merges multiple log files of the same type into a single file in the ZIP.
	 */
	private void mergeLogs(String logType, List<Path> logPaths, ZipOutputStream zipOut) throws IOException {
		String mergedLogFileName = String.format("%sLog.txt", logType);

		ZipEntry zipEntry = new ZipEntry(mergedLogFileName);
		zipOut.putNextEntry(zipEntry);

		int totalFiles = logPaths.size();
		int failedFiles = 0;

		for (Path logPath : logPaths) {
			try {
				Files.copy(logPath, zipOut);
				zipOut.write("\n".getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				failedFiles++;
				logger.warn("Failed to copy log file to ZIP: {} (error: {})", logPath, e.getMessage());
			}
		}

		zipOut.closeEntry();

		int successFiles = totalFiles - failedFiles;
		if (failedFiles > 0) {
			logger.warn(
					"Merged {} log files into: {} (success: {}, failed: {})", totalFiles, mergedLogFileName,
					successFiles, failedFiles
			);
		} else {
			logger.debug("Merged {} log files into: {}", totalFiles, mergedLogFileName);
		}
	}

	/**
	 * Creates an empty result ZIP file when no results are found.
	 */
	private Path createEmptyResultZip(Path zipPath) throws IOException {
		try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			ZipEntry readmeEntry = new ZipEntry("README.txt");
			zipOut.putNextEntry(readmeEntry);

			String readme = """
					VDYP Batch Processing Results
					No results were generated from this batch job.
					This may indicate that no polygons were successfully processed.
					""";
			zipOut.write(readme.getBytes(StandardCharsets.UTF_8));
			zipOut.closeEntry();
		}

		logger.info("Created empty result ZIP: {}", zipPath);
		return zipPath;
	}

	/**
	 * Cleans up interim partition directories after successful zip creation. Deletes input-partition and
	 * output-partition directories and their contents.
	 */
	public void cleanupPartitionDirectories(Path jobBasePath) throws IOException {
		logger.info("Starting cleanup of partition directories in: {}", jobBasePath);

		if (!Files.exists(jobBasePath) || !Files.isDirectory(jobBasePath)) {
			logger.warn("Job base directory does not exist or is not a directory: {}", jobBasePath);
			return;
		}

		int deletedDirs = 0;

		// Find and delete all input-partition and output-partition directories
		try (Stream<Path> files = Files.list(jobBasePath)) {
			List<Path> partitionDirs = files.filter(Files::isDirectory).filter(dir -> {
				String dirName = dir.getFileName().toString();
				return dirName.startsWith(BatchConstants.Partition.INPUT_FOLDER_NAME_PREFIX)
						|| dirName.startsWith(BatchConstants.Partition.OUTPUT_FOLDER_NAME_PREFIX);
			}).toList();

			for (Path partitionDir : partitionDirs) {
				try {
					deleteDirectoryRecursively(partitionDir);
					deletedDirs++;
					logger.debug("Deleted partition directory: {}", partitionDir.getFileName());
				} catch (IOException e) {
					logger.error("Failed to delete partition directory: {} - {}", partitionDir, e.getMessage(), e);
					// Continue with other directories even if one fails
				}
			}
		}

		logger.info("Cleanup completed. Deleted {} partition directories", deletedDirs);
	}

	/**
	 * Recursively deletes a directory and all its contents.
	 */
	private void deleteDirectoryRecursively(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return;
		}

		try (Stream<Path> walk = Files.walk(directory)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
					logger.trace("Deleted: {}", path);
				} catch (IOException e) {
					logger.warn("Failed to delete: {} - {}", path, e.getMessage());
				}
			});
		}
	}

	/**
	 * Validates the consolidated ZIP file to ensure it was created successfully and contains valid content before
	 * cleanup of interim files.
	 */
	public boolean validateConsolidatedZip(Path zipPath) {
		logger.info("Validating consolidated ZIP file: {}", zipPath);

		// Check if file exists
		if (!Files.exists(zipPath)) {
			logger.error("ZIP file does not exist: {}", zipPath);
			return false;
		}

		// Check if file is not empty
		long fileSize;
		try {
			fileSize = Files.size(zipPath);
			if (fileSize == 0) {
				logger.error("ZIP file is empty: {}", zipPath);
				return false;
			}
			logger.debug("ZIP file size: {} bytes", fileSize);
		} catch (IOException e) {
			logger.error("Failed to get ZIP file size: {} - {}", zipPath, e.getMessage(), e);
			return false;
		}

		// Validate ZIP structure and required content
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
			// Check if YieldTable.csv exists
			ZipEntry yieldTableEntry = zipFile.getEntry(BatchConstants.File.YIELD_TABLE_FILENAME);
			if (yieldTableEntry == null) {
				logger.error("ZIP file does not contain required file: {}", BatchConstants.File.YIELD_TABLE_FILENAME);
				return false;
			}

			logger.info(
					"ZIP file validation successful: {} (size: {} bytes, entries: {})", zipPath, fileSize,
					zipFile.size()
			);
			return true;

		} catch (ZipException e) {
			logger.error("ZIP file is corrupted or invalid: {} - {}", zipPath, e.getMessage(), e);
			return false;
		} catch (IOException e) {
			logger.error("Failed to validate ZIP file: {} - {}", zipPath, e.getMessage(), e);
			return false;
		}
	}
}
