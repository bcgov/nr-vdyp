package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchIOException;
import ca.bc.gov.nrs.vdyp.batch.exception.ResultAggregationException;

@ExtendWith(MockitoExtension.class)
class ResultAggregationServiceTest {

	@InjectMocks
	private ResultAggregationService resultAggregationService;

	@TempDir
	Path tempDir;

	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String JOB_TIMESTAMP = "2024_01_01_10_00_00_000";
	private static final String YIELD_TABLE_CONTENT = """
			TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
			1,123456789,FD,P,FD,100.0,50
			1,123456789,FD,P,FD,100.0,55
			""";

	private static final String ERROR_LOG_CONTENT = """
			2024-01-01 10:00:00 ERROR Processing failed for polygon 123
			2024-01-01 10:05:00 ERROR Another error occurred
			""";

	@BeforeEach
	void setUp() {
		// Test setup is handled by @TempDir and @InjectMocks
	}

	@Test
	void testAggregateResults_Success() throws IOException {
		setupPartitionDirectories();

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
		assertTrue(resultZip.getFileName().toString().endsWith(".zip"));

		verifyZipContent(resultZip);
	}

	@Test
	void testAggregateResults_BaseDirectoryNotExists() {
		Path nonExistentDir = tempDir.resolve("non-existent");

		IOException exception = assertThrows(
				IOException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, nonExistentDir.toString(),
						JOB_TIMESTAMP));

		assertTrue(exception.getMessage().contains("Base output directory does not exist") ||
				exception.getMessage().contains("does not exist"));
	}

	@Test
	void testAggregateResults_NoPartitionDirectories() throws IOException {
		// Create base directory but no partition directories
		Path baseDir = tempDir.resolve("empty-base");
		Files.createDirectories(baseDir);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, baseDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify it's an empty result ZIP with README
		verifyEmptyZipContent(resultZip);
	}

	@Test
	void testAggregateResults_MultiplePartitions() throws IOException {
		setupMultiplePartitionDirectories();

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		verifyMultiplePartitionZipContent(resultZip);
	}

	@Test
	void testAggregateResults_YieldTableMerging() throws IOException {
		// Setup two partitions with different yield tables
		Path partition1 = tempDir.resolve("output-partition-0");
		Path partition2 = tempDir.resolve("output-partition-1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		String yieldTable1 = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,111111111,FD,P,FD,100.0,40
				""";

		String yieldTable2 = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,222222222,CW,P,CW,100.0,45
				""";

		Files.writeString(partition1.resolve("YieldTable.csv"), yieldTable1);
		Files.writeString(partition2.resolve("YieldTable.csv"), yieldTable2);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		// Verify yield tables are merged with sequential table numbers
		verifyYieldTableMerging(resultZip);
	}

	@Test
	void testAggregateResults_LogAggregation() throws IOException {
		// Test aggregation of different log types
		Path partition1 = tempDir.resolve("output-partition-0");
		Path partition2 = tempDir.resolve("output-partition-1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		// Different log types in different partitions
		Files.writeString(partition1.resolve("error.log"), "Error 1");
		Files.writeString(partition1.resolve("progress.log"), "Progress 1");
		Files.writeString(partition2.resolve("error.log"), "Error 2");
		Files.writeString(partition2.resolve("debug.log"), "Debug info");

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		verifyLogAggregation(resultZip);
	}

	@Test
	void testAggregateResults_EmptyYieldTable() throws IOException {
		// Test with empty yield table file
		Path partitionDir = tempDir.resolve("output-partition-0");
		Files.createDirectories(partitionDir);

		Files.writeString(partitionDir.resolve("YieldTable.csv"), "");
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_InsufficientColumns() throws IOException {
		// Test yield table with insufficient columns
		Path partitionDir = tempDir.resolve("output-partition-0");
		Files.createDirectories(partitionDir);

		String invalidYieldTable = """
				TABLE_NUM,FEATURE_ID
				1,123
				""";

		Files.writeString(partitionDir.resolve("YieldTable.csv"), invalidYieldTable);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_MixedFileTypes() throws IOException {
		// Setup partition with various file types
		Path partitionDir = tempDir.resolve("output-partition-0");
		Files.createDirectories(partitionDir);

		// Create yield table
		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		// Create various log files
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("progress.log"), "Progress information");
		Files.writeString(partitionDir.resolve("debug.log"), "Debug information");

		// Create other result files
		Files.writeString(partitionDir.resolve("projection_results.csv"), "FEATURE_ID,RESULT\n123,data");

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify all file types are aggregated correctly
		verifyMixedFileTypesZipContent(resultZip);
	}

	private void setupPartitionDirectories() throws IOException {
		Path partitionDir = tempDir.resolve("output-partition-0");
		Files.createDirectories(partitionDir);

		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("other_results.csv"), "FEATURE_ID,DATA\n123456789,result_data");
	}

	private void setupMultiplePartitionDirectories() throws IOException {
		for (int i = 0; i < 3; i++) {
			Path partitionDir = tempDir.resolve("output-partition-" + i);
			Files.createDirectories(partitionDir);

			String yieldTableContent = String.format("""
					TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
					1,%d,FD,P,FD,100.0,%d
					""", 100000000 + i, 40 + i);

			Files.writeString(partitionDir.resolve("YieldTable.csv"), yieldTableContent);
			Files.writeString(partitionDir.resolve("error.log"), "Error from partition " + i);
			Files.writeString(partitionDir.resolve("result_" + i + ".csv"), "Data from partition " + i);
		}
	}

	private void verifyZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertFalse(entryNames.isEmpty());

		// Should contain aggregated yield table
		assertTrue(entryNames.contains("YieldTable.csv"));

		// Should contain aggregated error log
		assertTrue(entryNames.contains("ErrorLog.txt"));
	}

	private void verifyEmptyZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertEquals(1, entryNames.size());
		assertEquals("README.txt", entryNames.get(0));
	}

	private void verifyMultiplePartitionZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		// Should have merged yield table
		assertTrue(entryNames.contains("YieldTable.csv"));

		// Should have merged error log
		assertTrue(entryNames.contains("ErrorLog.txt"));
	}

	private void verifyMixedFileTypesZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		// Verify yield table aggregation
		assertTrue(entryNames.contains("YieldTable.csv"));

		// Verify log aggregation
		assertTrue(entryNames.contains("ErrorLog.txt"));
		assertTrue(entryNames.contains("ProgressLog.txt"));
		assertTrue(entryNames.contains("DebugLog.txt"));
	}

	private void verifyYieldTableMerging(Path zipPath) throws IOException {
		String yieldTableContent = getZipEntryContent(zipPath, "YieldTable.csv");

		assertNotNull(yieldTableContent);

		// Should contain both feature IDs with sequential table numbers
		assertTrue(yieldTableContent.contains("111111111"));
		assertTrue(yieldTableContent.contains("222222222"));

		// Check that table numbers are assigned sequentially
		String[] lines = yieldTableContent.split("\n");
		boolean foundTable1 = false;
		boolean foundTable2 = false;

		for (String line : lines) {
			if (line.contains("111111111") && line.startsWith("1,")) {
				foundTable1 = true;
			}
			if (line.contains("222222222") && line.startsWith("2,")) {
				foundTable2 = true;
			}
		}

		assertTrue(foundTable1, "Should find table 1 for first feature");
		assertTrue(foundTable2, "Should find table 2 for second feature");
	}

	private void verifyLogAggregation(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		// Should have error logs merged
		assertTrue(entryNames.contains("ErrorLog.txt"));

		// Should have progress logs merged
		assertTrue(entryNames.contains("ProgressLog.txt"));

		// Should have debug logs merged
		assertTrue(entryNames.contains("DebugLog.txt"));

		// Verify error log content includes both partitions
		String errorLogContent = getZipEntryContent(zipPath, "ErrorLog.txt");
		assertTrue(errorLogContent.contains("Error 1"));
		assertTrue(errorLogContent.contains("Error 2"));
	}

	private String getZipEntryContent(Path zipPath, String entryName) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(entryName)) {
					return new String(zis.readAllBytes());
				}
			}
		}
		return null;
	}

	@Test
	void testAggregateResults_NullJobTimestamp() {
		String tempDirPath = tempDir.toString();
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDirPath, null));
		assertTrue(exception.getMessage().contains("Job timestamp cannot be null"));
	}

	@Test
	void testAggregateResults_EmptyJobTimestamp() {
		String tempDirPath = tempDir.toString();
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDirPath, " "));
		assertTrue(exception.getMessage().contains("Job timestamp cannot be null"));
	}

	@Test
	void testAggregateResults_NullJobBaseDir() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, null, JOB_TIMESTAMP));

		assertTrue(exception.getMessage().contains("Job base directory cannot be null"));
	}

	@Test
	void testAggregateResults_EmptyJobBaseDir() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, "", JOB_TIMESTAMP));

		assertTrue(exception.getMessage().contains("Job base directory cannot be null"));
	}

	@Test
	void testAggregateResults_NonDirectoryPath() throws IOException {
		// Create a file instead of directory
		Path filePath = tempDir.resolve("not-a-directory.txt");
		Files.writeString(filePath, "test");

		IOException exception = assertThrows(
				IOException.class,
				() -> resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, filePath.toString(),
						JOB_TIMESTAMP));

		assertTrue(exception.getMessage().contains("not a directory"));
	}

	@Test
	void testValidateConsolidatedZip_NonExistentZip() {
		Path nonExistentZip = tempDir.resolve("non-existent.zip");

		boolean isValid = resultAggregationService.validateConsolidatedZip(nonExistentZip);

		assertFalse(isValid);
	}

	@Test
	void testValidateConsolidatedZip_EmptyZip() throws IOException {
		// Create empty ZIP file
		Path emptyZip = tempDir.resolve("empty.zip");
		Files.writeString(emptyZip, "");

		boolean isValid = resultAggregationService.validateConsolidatedZip(emptyZip);

		assertFalse(isValid);
	}

	@Test
	void testValidateConsolidatedZip_MissingYieldTable() throws IOException {
		// Create ZIP without YieldTable.csv
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);
		Files.writeString(partition.resolve("error.log"), "Error log only");

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		boolean isValid = resultAggregationService.validateConsolidatedZip(resultZip);

		assertFalse(isValid, "Should be invalid without YieldTable.csv");
	}

	@Test
	void testCleanupPartitionDirectories_Success() throws IOException {
		// Setup input and output partition directories
		Path inputPartition = tempDir.resolve("input-partition0");
		Path outputPartition = tempDir.resolve("output-partition0");
		Files.createDirectories(inputPartition);
		Files.createDirectories(outputPartition);

		// Add some files
		Files.writeString(inputPartition.resolve("test.csv"), "data");
		Files.writeString(outputPartition.resolve("result.csv"), "result");

		resultAggregationService.cleanupPartitionDirectories(tempDir);

		// Verify directories are deleted
		assertFalse(Files.exists(inputPartition));
		assertFalse(Files.exists(outputPartition));
	}

	@Test
	void testCleanupPartitionDirectories_NonExistentPath() {
		Path nonExistent = tempDir.resolve("non-existent");

		// Should not throw exception
		assertDoesNotThrow(() -> resultAggregationService.cleanupPartitionDirectories(nonExistent));
	}

	@Test
	void testCleanupPartitionDirectories_FilePath() throws IOException {
		// Create a file instead of directory
		Path filePath = tempDir.resolve("file.txt");
		Files.writeString(filePath, "content");

		// Should not throw exception, just log warning
		assertDoesNotThrow(() -> resultAggregationService.cleanupPartitionDirectories(filePath));
	}

	@Test
	void testCleanupPartitionDirectories_NestedStructure() throws IOException {
		// Create nested directory structure
		Path inputPartition = tempDir.resolve("input-partition0");
		Path nestedDir = inputPartition.resolve("nested");
		Files.createDirectories(nestedDir);
		Files.writeString(nestedDir.resolve("file.txt"), "data");

		resultAggregationService.cleanupPartitionDirectories(tempDir);

		assertFalse(Files.exists(inputPartition));
	}

	@Test
	void testCleanupPartitionDirectories_OnlyPartitionDirs() throws IOException {
		// Create partition directories and non-partition directories
		Path inputPartition = tempDir.resolve("input-partition0");
		Path outputPartition = tempDir.resolve("output-partition0");
		Path otherDir = tempDir.resolve("other-directory");
		Path otherFile = tempDir.resolve("file.txt");

		Files.createDirectories(inputPartition);
		Files.createDirectories(outputPartition);
		Files.createDirectories(otherDir);
		Files.writeString(otherFile, "content");

		resultAggregationService.cleanupPartitionDirectories(tempDir);

		// Only partition directories should be deleted
		assertFalse(Files.exists(inputPartition));
		assertFalse(Files.exists(outputPartition));
		assertTrue(Files.exists(otherDir), "Non-partition directory should not be deleted");
		assertTrue(Files.exists(otherFile), "Non-partition file should not be deleted");
	}

	@Test
	void testAggregateResults_WithHeaderOnlyYieldTable() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Create yield table with header only
		String headerOnly = "TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE\n";
		Files.writeString(partition.resolve("YieldTable.csv"), headerOnly);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_YieldTableWithSameFeatureIdDifferentLayers() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Same FEATURE_ID with different layers
		String yieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,123456789,FD,P,FD,100.0,50
				1,123456789,CW,S,CW,80.0,45
				""";
		Files.writeString(partition.resolve("YieldTable.csv"), yieldTable);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);

		// Should have different table numbers for different polygon/layer combinations
		assertTrue(yieldTableContent.contains("123456789"));
	}

	@Test
	void testAggregateResults_GeneralLogType() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Create a log file that doesn't match error/progress/debug patterns
		Files.writeString(partition.resolve("application.log"), "General application log");
		Files.writeString(partition.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resultZip))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertTrue(entryNames.contains("GeneralLog.txt"));
	}

	@Test
	void testFindPartitionDirectories_WithDirectMatchPattern() throws IOException {
		// Create directories with exact prefix match
		Path partition1 = tempDir.resolve("output-partition0");
		Path partition2 = tempDir.resolve("output-partition-10");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		Files.writeString(partition1.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);
		Files.writeString(partition2.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_WithNonExistentYieldTable() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Try to process but yield table doesn't exist
		Files.writeString(partition.resolve("error.log"), ERROR_LOG_CONTENT);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		boolean isValid = resultAggregationService.validateConsolidatedZip(resultZip);
		assertFalse(isValid, "Should be invalid without YieldTable.csv");
	}

	@Test
	void testAggregateResults_WithUnreadableYieldTable() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		Path yieldTablePath = partition.resolve("YieldTable.csv");
		Files.writeString(yieldTablePath, YIELD_TABLE_CONTENT);

		// Make file unreadable (on Windows this may not work as expected, but test
		// should pass)
		if (yieldTablePath.toFile().setReadable(false)) {
			Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
					JOB_TIMESTAMP);

			assertNotNull(resultZip);
			assertTrue(Files.exists(resultZip));

			// Restore permissions for cleanup
			yieldTablePath.toFile().setReadable(true);
		}
	}

	@Test
	void testAggregateResults_WithNullPartitionDirectory() throws IOException {
		// Create a partition with null directory scenario
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		Files.writeString(partition.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_WithEmptyLines() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Create yield table with empty and whitespace lines
		String yieldTableWithEmptyLines = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE

				1,123456789,FD,P,FD,100.0,50

				1,123456789,CW,S,CW,80.0,45
				""";
		Files.writeString(partition.resolve("YieldTable.csv"), yieldTableWithEmptyLines);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);
	}

	@Test
	void testAggregateResults_WithHeaderKeywordVariations() throws IOException {
		Path partition = tempDir.resolve("output-partition-0");
		Files.createDirectories(partition);

		// Test various header patterns
		String[] headerVariations = {
				"TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID",
				"FEATURE_ID,POLYGON_ID,LAYER_ID",
				"POLYGON_ID,LAYER_ID,SPECIES_CODE",
				"SPECIES_CODE,LAYER_ID,TABLE_NUM"
		};

		for (int i = 0; i < headerVariations.length; i++) {
			Path partitionDir = tempDir.resolve("output-partition-" + i);
			Files.createDirectories(partitionDir);

			String yieldTable = headerVariations[i] + "\n1,123,FD,P\n";
			Files.writeString(partitionDir.resolve("YieldTable.csv"), yieldTable);
		}

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testMergeLogs_WithPartialFailures() throws IOException {
		Path partition1 = tempDir.resolve("output-partition-0");
		Path partition2 = tempDir.resolve("output-partition-1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		// Create log files with some that will fail
		Files.writeString(partition1.resolve("error.log"), "Error 1");
		Files.writeString(partition2.resolve("error.log"), "Error 2");
		Files.writeString(partition1.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		// Create a directory instead of file (will cause copy failure)
		Path invalidLog = partition2.resolve("error.log.backup");
		Files.createDirectories(invalidLog);

		Path resultZip = resultAggregationService.aggregateResultsFromJobDir(JOB_EXECUTION_ID, tempDir.toString(),
				JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resultZip))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertTrue(entryNames.contains("ErrorLog.txt"));
	}

	@Test
	void testBatchIOException_MessageConstructor() {
		String message = "Test error message";
		BatchIOException exception = new BatchIOException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testBatchIOException_MessageAndCauseConstructor() {
		String message = "Test error message";
		IOException cause = new IOException("Original cause");
		BatchIOException exception = new BatchIOException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testBatchIOException_CauseConstructor() {
		IOException cause = new IOException("Original cause");
		BatchIOException exception = new BatchIOException(cause);

		assertEquals(cause, exception.getCause());
		assertTrue(exception.getMessage().contains("Original cause"));
	}

	@Test
	void testBatchIOException_HandleIOException_WithContext() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		IOException cause = new IOException("Original error");
		String errorDescription = "Failed to process file";
		String context = "test-file.csv";

		IOException result = BatchIOException.handleIOException(context, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(context));
		assertTrue(result.getMessage().contains("IOException"));
		assertTrue(result.getMessage().contains("Original error"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchIOException_HandleIOException_WithNullContext() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		IOException cause = new IOException("Original error");
		String errorDescription = "Failed to process file";

		IOException result = BatchIOException.handleIOException(null, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertFalse(result.getMessage().contains("null"));
		assertTrue(result.getMessage().contains("IOException"));
		assertTrue(result.getMessage().contains("Original error"));
	}

	@Test
	void testBatchIOException_HandleIOException_WithNullCauseMessage() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		IOException cause = new IOException((String) null);
		String errorDescription = "Failed to process file";
		String context = "test-file.csv";

		IOException result = BatchIOException.handleIOException(context, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains("No error message available"));
	}

	@Test
	void testBatchIOException_HandleDirectoryWalkFailure() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		Path directory = tempDir.resolve("test-directory");
		IOException cause = new IOException("Directory walk failed");
		String errorDescription = "Failed to walk directory";

		IOException result = BatchIOException.handleDirectoryWalkFailure(directory, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(directory.toString()));
		assertTrue(result.getMessage().contains("Directory walk failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchIOException_HandleFileReadFailure() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		Path filePath = tempDir.resolve("test-file.csv");
		IOException cause = new IOException("Read failed");
		String errorDescription = "Failed to read file";

		IOException result = BatchIOException.handleFileReadFailure(filePath, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(filePath.toString()));
		assertTrue(result.getMessage().contains("Read failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchIOException_HandleFileWriteFailure() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		Path filePath = tempDir.resolve("test-file.csv");
		IOException cause = new IOException("Write failed");
		String errorDescription = "Failed to write file";

		IOException result = BatchIOException.handleFileWriteFailure(filePath, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(filePath.toString()));
		assertTrue(result.getMessage().contains("Write failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchIOException_HandleFileCopyFailure() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		Path filePath = tempDir.resolve("test-file.csv");
		IOException cause = new IOException("Copy failed");
		String errorDescription = "Failed to copy file";

		IOException result = BatchIOException.handleFileCopyFailure(filePath, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(filePath.toString()));
		assertTrue(result.getMessage().contains("Copy failed"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testBatchIOException_HandleStreamFailure() {
		Logger logger = LoggerFactory.getLogger(ResultAggregationServiceTest.class);
		String streamName = "input-stream";
		IOException cause = new IOException("Stream error");
		String errorDescription = "Failed to process stream";

		IOException result = BatchIOException.handleStreamFailure(streamName, cause, errorDescription, logger);

		assertNotNull(result);
		assertTrue(result.getMessage().contains(errorDescription));
		assertTrue(result.getMessage().contains(streamName));
		assertTrue(result.getMessage().contains("Stream error"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testResultAggregationException_MessageConstructor() {
		String message = "Failed to aggregate results";
		ResultAggregationException exception = new ResultAggregationException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testResultAggregationException_MessageAndCauseConstructor() {
		String message = "Failed to aggregate results";
		Throwable cause = new IOException("Original IO error");
		ResultAggregationException exception = new ResultAggregationException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

	@Test
	void testResultAggregationException_CauseConstructor() {
		Throwable cause = new IOException("Original IO error");
		ResultAggregationException exception = new ResultAggregationException(cause);

		assertEquals(cause, exception.getCause());
		assertTrue(exception.getMessage().contains("Original IO error"));
	}

	@Test
	void testResultAggregationException_ExtendsRuntimeException() {
		ResultAggregationException exception = new ResultAggregationException("Test");

		assertTrue(exception instanceof RuntimeException);
		assertTrue(exception instanceof ca.bc.gov.nrs.vdyp.batch.exception.BatchException);
	}

	@Test
	void testResultAggregationException_WithDifferentCauseTypes() {
		IOException ioCause = new IOException("IO error during aggregation");
		ResultAggregationException exception1 = new ResultAggregationException("Aggregation failed", ioCause);
		assertEquals(ioCause, exception1.getCause());

		RuntimeException runtimeCause = new RuntimeException("Runtime error during aggregation");
		ResultAggregationException exception2 = new ResultAggregationException("Aggregation failed", runtimeCause);
		assertEquals(runtimeCause, exception2.getCause());

		IllegalStateException stateCause = new IllegalStateException("Invalid state during aggregation");
		ResultAggregationException exception3 = new ResultAggregationException("Aggregation failed", stateCause);
		assertEquals(stateCause, exception3.getCause());
	}

	@Test
	void testResultAggregationException_MessageContainsDetails() {
		String detailedMessage = "Failed to aggregate results from partition-5 with 100 records";
		ResultAggregationException exception = new ResultAggregationException(detailedMessage);

		assertTrue(exception.getMessage().contains("partition-5"));
		assertTrue(exception.getMessage().contains("100 records"));
	}

	@Test
	void testResultAggregationException_NestedCauses() {
		IOException rootCause = new IOException("Disk full");
		RuntimeException intermediateCause = new RuntimeException("Processing failed", rootCause);
		ResultAggregationException exception = new ResultAggregationException("Aggregation failed", intermediateCause);

		assertEquals(intermediateCause, exception.getCause());
		assertEquals(rootCause, exception.getCause().getCause());
	}

	@Test
	void testResultAggregationException_NullMessage() {
		ResultAggregationException exception = new ResultAggregationException((String) null);

		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testResultAggregationException_EmptyMessage() {
		String emptyMessage = "";
		ResultAggregationException exception = new ResultAggregationException(emptyMessage);

		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testResultAggregationException_ThrowAndCatch() {
		String message = "Test aggregation failure";

		assertThrows(ResultAggregationException.class, () -> {
			throw new ResultAggregationException(message);
		});

		try {
			throw new ResultAggregationException(message);
		} catch (ResultAggregationException e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	void testResultAggregationException_CauseWithNullMessage() {
		IOException cause = new IOException((String) null);
		ResultAggregationException exception = new ResultAggregationException(cause);

		assertEquals(cause, exception.getCause());
		assertNotNull(exception.getMessage());
	}

	@Test
	void testCollectYieldTablesFromPartition_IOException() throws Exception {
		Path partitionDir = tempDir.resolve("output-partition-0");
		Files.createDirectories(partitionDir);

		try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
			mockedFiles.when(() -> Files.walk(partitionDir))
					.thenThrow(new IOException("Mocked directory walk failure"));

			Map<String, List<Path>> yieldTablesByType = new HashMap<>();

			Method collectYieldTablesMethod = ResultAggregationService.class.getDeclaredMethod(
					"collectYieldTablesFromPartition", Path.class, Map.class);
			collectYieldTablesMethod.setAccessible(true);

			Exception exception = assertThrows(
					Exception.class,
					() -> collectYieldTablesMethod.invoke(resultAggregationService, partitionDir, yieldTablesByType),
					"Expected Exception when Files.walk fails");

			Throwable actualException = exception.getCause();
			assertNotNull(actualException, "Exception cause should not be null");

			String exceptionMessage = actualException.getMessage();
			assertNotNull(exceptionMessage, "Exception message should not be null");
			assertTrue(exceptionMessage.contains("Error walking directory tree for yield tables"),
					"Exception message should contain error description");
			assertTrue(exceptionMessage.contains(partitionDir.toString()),
					"Exception message should contain directory path");
			assertTrue(exceptionMessage.contains("Mocked directory walk failure"),
					"Exception message should contain cause message");
			assertTrue(actualException.getCause() instanceof IOException,
					"Cause should be the original IOException");

			assertTrue(yieldTablesByType.isEmpty(), "yieldTablesByType should remain empty");
		}
	}
}
