package ca.bc.gov.nrs.vdyp.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service responsible for aggregating VDYP projection results from all
 * partitions into a single consolidated output ZIP file.
 *
 * This service implements the proper batch processing pattern:
 * 1.Collect intermediate results from all partitions
 * 2.Merge yield tables and logs by type
 * 3.Create single consolidated ZIP file
 * 4.Clean up intermediate files
 */
@Service
public class ResultAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(ResultAggregationService.class);
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
	private static final String PARTITION_PREFIX = "partition-";
	private static final String YIELD_TABLE_TYPE = "YieldTable";
	private static final String YIELD_TABLE_FILENAME = "YieldTable.csv";

	/**
	 * Aggregates all partition results into a single consolidated ZIP file with
	 * job-specific organization.
	 *
	 * @param jobExecutionId The job execution ID for result organization
	 * @param baseOutputPath Base output directory containing partition results
	 * @return Path to the consolidated ZIP file
	 * @throws IOException if aggregation fails
	 */
	public Path aggregateResults(Long jobExecutionId, String baseOutputPath) throws IOException {
		logger.info("Starting result aggregation for job execution: {}", jobExecutionId);

		Path baseDir = Paths.get(baseOutputPath);
		if (!Files.exists(baseDir)) {
			throw new IOException("Base output directory does not exist: " + baseOutputPath);
		}

		// Create job-specific directory structure
		String jobDirName = String.format("vdyp-output-%s", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
		Path jobSpecificDir = baseDir.resolve(jobDirName);
		Files.createDirectories(jobSpecificDir);

		logger.info("Created job-specific directory: {}", jobSpecificDir);

		// Collect all partition directories from base directory
		List<Path> partitionDirs = findPartitionDirectories(baseDir);
		logger.info("Found {} partition directories to aggregate", partitionDirs.size());

		if (partitionDirs.isEmpty()) {
			logger.warn("No partition directories found for aggregation");
			String finalZipFileName = String
					.format("vdyp-output-%s.zip", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
			Path finalZipPath = jobSpecificDir.resolve(finalZipFileName);
			return createEmptyResultZip(finalZipPath);
		}

		// Organize partition files in job-specific directory
		organizePartitionFiles(partitionDirs, jobSpecificDir);

		// Create final ZIP file in job-specific directory
		String finalZipFileName = String.format("vdyp-output-%s.zip", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
		Path finalZipPath = jobSpecificDir.resolve(finalZipFileName);

		// Get organized partition directories for aggregation
		List<Path> organizedPartitionDirs = findPartitionDirectories(jobSpecificDir);

		// Aggregate results
		try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(finalZipPath))) {
			aggregateYieldTables(organizedPartitionDirs, zipOut);
			aggregateLogs(organizedPartitionDirs, zipOut);

			logger.info("Successfully created consolidated ZIP file: {}", finalZipPath);
		}

		return finalZipPath;
	}

	/**
	 * Finds all partition directories in the base output directory.
	 */
	private List<Path> findPartitionDirectories(Path baseDir) throws IOException {
		List<Path> partitionDirs = new ArrayList<>();

		logger.info("Searching for partition directories in: {}", baseDir);

		// List all items in base directory for debugging
		try (Stream<Path> allItems = Files.list(baseDir)) {
			allItems.forEach(
					item -> logger.info("Found item: {} (isDirectory: {})", item.getFileName(),
							Files.isDirectory(item)));
		}

		try (Stream<Path> files = Files.list(baseDir)) {
			files.filter(Files::isDirectory).filter(dir -> {
				String dirName = dir.getFileName().toString();
				boolean matches = dirName.startsWith(PARTITION_PREFIX) || dirName.matches("partition\\d+");
				logger.info("Directory {} matches partition pattern: {}", dirName, matches);
				return matches;
			}).forEach(partitionDirs::add);
		}

		logger.info(
				"Found {} partition directories: {}", partitionDirs.size(),
				partitionDirs.stream().map(p -> p.getFileName().toString()).toList());

		partitionDirs.sort(Comparator.comparing(path -> path.getFileName().toString()));
		return partitionDirs;
	}

	/**
	 * Aggregates yield tables from all partitions, merging tables of the same type.
	 */
	private void aggregateYieldTables(List<Path> partitionDirs, ZipOutputStream zipOut) throws IOException {
		logger.info("Aggregating yield tables from {} partitions", partitionDirs.size());

		Map<String, List<Path>> yieldTablesByType = new HashMap<>();

		// Collect all yield tables by type
		for (Path partitionDir : partitionDirs) {
			collectYieldTablesFromPartition(partitionDir, yieldTablesByType);
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
	 * Collects yield table files from a partition directory.
	 */
	private void collectYieldTablesFromPartition(Path partitionDir, Map<String, List<Path>> yieldTablesByType)
			throws IOException {
		try (Stream<Path> files = Files.walk(partitionDir)) {
			files.filter(Files::isRegularFile).filter(file -> isYieldTableFile(file.getFileName().toString())).forEach(
					file -> yieldTablesByType.computeIfAbsent(YIELD_TABLE_TYPE, k -> new ArrayList<>()).add(file));
		}
	}

	/**
	 * Determines if a file is a yield table based on its name.
	 */
	private boolean isYieldTableFile(String fileName) {
		String lowerName = fileName.toLowerCase();
		// Check if it's explicitly a yield table file, but exclude log files
		return (lowerName.contains("yield") || lowerName.endsWith(".ytb")) && !isLogFile(fileName);
	}

	/**
	 * Merges multiple yield tables of the same type into a single file in the ZIP.
	 * Assigns TABLE_NUM based on polygon/layer combinations.
	 */
	private void mergeYieldTables(List<Path> tablePaths, ZipOutputStream zipOut) throws IOException {
		ZipEntry zipEntry = new ZipEntry(YIELD_TABLE_FILENAME);
		zipOut.putNextEntry(zipEntry);

		Map<String, Integer> polygonLayerTableNumbers = new HashMap<>();
		boolean isFirstFile = true;

		for (Path tablePath : tablePaths) {
			isFirstFile = processYieldTableFile(tablePath, zipOut, polygonLayerTableNumbers, isFirstFile);
		}

		zipOut.closeEntry();
		logger.debug(
				"Merged {} files into yield table: {} with {} unique polygon/layer combinations", tablePaths.size(),
				YIELD_TABLE_FILENAME, polygonLayerTableNumbers.size());
	}

	/**
	 * Processes a single yield table file and writes its content to the ZIP output
	 * stream.
	 */
	private boolean processYieldTableFile(
			Path tablePath, ZipOutputStream zipOut, Map<String, Integer> polygonLayerTableNumbers, boolean isFirstFile)
			throws IOException {
		try (Stream<String> lines = Files.lines(tablePath)) {
			Iterator<String> lineIterator = lines.iterator();

			if (lineIterator.hasNext()) {
				isFirstFile = processFirstLine(lineIterator.next(), zipOut, polygonLayerTableNumbers, isFirstFile);
			}

			processRemainingLines(lineIterator, zipOut, polygonLayerTableNumbers);
		}
		return false; // After processing first file, subsequent files are not first
	}

	/**
	 * Processes the first line of a yield table file (header or data line).
	 */
	private boolean processFirstLine(
			String firstLine, ZipOutputStream zipOut, Map<String, Integer> polygonLayerTableNumbers,
			boolean isFirstFile) throws IOException {
		if (isHeaderLine(firstLine)) {
			if (isFirstFile) {
				writeLineToZip(firstLine, zipOut);
			}
			return false; // Header processed, subsequent files are not first
		} else {
			// Not a header, this is a data line - process it
			processDataLine(firstLine, zipOut, polygonLayerTableNumbers);
			return isFirstFile; // Keep first file status for data-only files
		}
	}

	/**
	 * Processes the remaining data lines from a yield table file.
	 */
	private void processRemainingLines(
			Iterator<String> lineIterator, ZipOutputStream zipOut, Map<String, Integer> polygonLayerTableNumbers)
			throws IOException {
		while (lineIterator.hasNext()) {
			processDataLine(lineIterator.next(), zipOut, polygonLayerTableNumbers);
		}
	}

	/**
	 * Processes a single data line and writes it to the ZIP output stream.
	 */
	private void processDataLine(String line, ZipOutputStream zipOut, Map<String, Integer> polygonLayerTableNumbers)
			throws IOException {
		String processedLine = assignTableNumber(line, polygonLayerTableNumbers);
		if (processedLine != null) {
			writeLineToZip(processedLine, zipOut);
		}
	}

	/**
	 * Writes a line to the ZIP output stream with proper line separator.
	 */
	private void writeLineToZip(String line, ZipOutputStream zipOut) throws IOException {
		zipOut.write((line + System.lineSeparator()).getBytes());
	}

	/**
	 * Assigns TABLE_NUM based on polygon/layer combination. Each unique FEATURE_ID
	 * + LAYER_ID combination gets a unique TABLE_NUM.
	 *
	 * @param line                     The CSV line to process
	 * @param polygonLayerTableNumbers Map tracking TABLE_NUM for each polygon/layer
	 *                                 combination
	 * @return The processed line with correct TABLE_NUM, or null if line should be
	 *         skipped
	 */
	private String assignTableNumber(String line, Map<String, Integer> polygonLayerTableNumbers) {
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
		Integer tableNum = polygonLayerTableNumbers.get(polygonLayerKey);
		if (tableNum == null) {
			// New polygon/layer combination, assign next available table number
			tableNum = polygonLayerTableNumbers.isEmpty() ? 1
					: polygonLayerTableNumbers.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
			polygonLayerTableNumbers.put(polygonLayerKey, tableNum);
			logger.debug(
					"Assigned TABLE_NUM {} to polygon/layer combination: FEATURE_ID={}, LAYER_ID={}", tableNum,
					featureId, layerId);
		}

		// Replace TABLE_NUM (first column) with the assigned number
		columns[0] = String.valueOf(tableNum);

		// Rejoin the columns
		return String.join(",", columns);
	}

	/**
	 * Determines if a line is a header line.
	 */
	private boolean isHeaderLine(String line) {
		String upperLine = line.toUpperCase();
		return upperLine.contains("FEATURE") || upperLine.contains("POLYGON") || upperLine.contains("LAYER")
				|| upperLine.contains("SPECIES") || upperLine.startsWith("#") || upperLine.trim().isEmpty();
	}

	/**
	 * Aggregates log files from all partitions.
	 */
	private void aggregateLogs(List<Path> partitionDirs, ZipOutputStream zipOut) throws IOException {
		logger.info("Aggregating log files from {} partitions", partitionDirs.size());

		Map<String, List<Path>> logsByType = new HashMap<>();

		// Collect all log files by type
		for (Path partitionDir : partitionDirs) {
			try (Stream<Path> files = Files.walk(partitionDir)) {
				files.filter(Files::isRegularFile).filter(file -> isLogFile(file.getFileName().toString()))
						.forEach(file -> {
							String logType = extractLogType(file.getFileName().toString());
							logsByType.computeIfAbsent(logType, k -> new ArrayList<>()).add(file);
						});
			}
		}

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
		if (lowerName.contains("error"))
			return "Error";
		if (lowerName.contains("progress"))
			return "Progress";
		if (lowerName.contains("debug"))
			return "Debug";
		return "General";
	}

	/**
	 * Merges multiple log files of the same type into a single file in the ZIP.
	 */
	private void mergeLogs(String logType, List<Path> logPaths, ZipOutputStream zipOut) throws IOException {
		String mergedLogFileName = String.format("%sLog.txt", logType);

		ZipEntry zipEntry = new ZipEntry(mergedLogFileName);
		zipOut.putNextEntry(zipEntry);

		for (Path logPath : logPaths) {
			Files.copy(logPath, zipOut);
			zipOut.write("\n".getBytes());
		}

		zipOut.closeEntry();
		logger.debug("Merged {} log files into: {}", logPaths.size(), mergedLogFileName);
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
			zipOut.write(readme.getBytes());
			zipOut.closeEntry();
		}

		logger.info("Created empty result ZIP: {}", zipPath);
		return zipPath;
	}

	/**
	 * Organizes partition files into job-specific directory structure. Creates
	 * partition-0, partition-1, etc. subdirectories within the job directory.
	 */
	private void organizePartitionFiles(List<Path> partitionDirs, Path jobSpecificDir) throws IOException {
		logger.info(
				"Organizing partition files from {} partitions into job directory: {}", partitionDirs.size(),
				jobSpecificDir);

		// Log all partition directories found
		if (logger.isInfoEnabled()) {
			for (int i = 0; i < partitionDirs.size(); i++) {
				Path partitionDir = partitionDirs.get(i);
				logger.info(
						"Partition {} found: full path = {}, filename = {}", i, partitionDir,
						partitionDir.getFileName());
			}
		}

		for (int i = 0; i < partitionDirs.size(); i++) {
			Path sourcePartitionDir = partitionDirs.get(i);
			String originalDirName = sourcePartitionDir.getFileName().toString();

			// Create organized partition directory with sequential naming
			String organizedDirName = PARTITION_PREFIX + i;
			Path targetPartitionDir = jobSpecificDir.resolve(organizedDirName);
			Files.createDirectories(targetPartitionDir);

			logger.info("Organizing partition {}: source = {} -> target = {}", i, originalDirName, organizedDirName);

			// Copy all files from source partition directory to organized target
			try (Stream<Path> files = Files.walk(sourcePartitionDir)) {
				files.filter(Files::isRegularFile).forEach(file -> {
					try {
						Path targetFile = targetPartitionDir.resolve(file.getFileName());
						Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
						logger.debug("Organized file: {} -> {}", file.getFileName(), targetFile);
					} catch (IOException e) {
						logger.warn("Failed to organize file {}: {}", file, e.getMessage());
					}
				});
			}

			logger.info("Organized partition {} files to {}", i, targetPartitionDir);
		}

		// Clean up original partition directories after organizing
		cleanupIntermediateFiles(partitionDirs);

		logger.info("All partition files organized in job directory: {}", jobSpecificDir);
	}

	/**
	 * Cleans up intermediate partition directories and files.
	 */
	private void cleanupIntermediateFiles(List<Path> partitionDirs) {
		logger.info("Cleaning up {} intermediate partition directories", partitionDirs.size());

		for (Path partitionDir : partitionDirs) {
			try {
				// Only clean up if the directory still exists and is not within a job-specific
				// directory
				if (Files.exists(partitionDir) && !isWithinJobSpecificDirectory(partitionDir)) {
					deleteDirectoryRecursively(partitionDir);
					logger.debug("Cleaned up intermediate partition directory: {}", partitionDir);
				}
			} catch (IOException e) {
				logger.warn("Failed to clean up partition directory {}: {}", partitionDir, e.getMessage());
			}
		}
	}

	/**
	 * Checks if a directory is within a job-specific directory (should not be
	 * cleaned up).
	 */
	private boolean isWithinJobSpecificDirectory(Path partitionDir) {
		String parentDirName = partitionDir.getParent() != null ? partitionDir.getParent().getFileName().toString()
				: "";
		return parentDirName.startsWith("vdyp-output-");
	}

	/**
	 * Recursively deletes a directory and all its contents.
	 */
	private void deleteDirectoryRecursively(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
