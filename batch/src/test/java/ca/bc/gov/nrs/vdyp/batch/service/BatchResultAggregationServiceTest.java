package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultAggregationException;

@ExtendWith(MockitoExtension.class)
class BatchResultAggregationServiceTest {

	@InjectMocks
	private BatchResultAggregationService resultAggregationService;

	@TempDir
	Path tempDir;

	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";
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
	void testAggregateResults_Success() throws BatchResultAggregationException, IOException {
		setupPartitionDirectories();

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
		assertTrue(resultZip.getFileName().toString().endsWith(".zip"));

		verifyZipContent(resultZip);
	}

	@Test
	void testAggregateResults_BaseDirectoryNotExists_ThrowsException() {
		Path nonExistentDir = tempDir.resolve("non-existent");
		String nonExistentPath = nonExistentDir.toString();

		BatchResultAggregationException exception = assertThrows(
				BatchResultAggregationException.class, () -> aggregateResultsFromJobDir(nonExistentPath)
		);

		assertTrue(
				exception.getMessage().contains("Job base directory does not exist")
						|| exception.getMessage().contains("does not exist")
		);
	}

	@Test
	void testAggregateResults_NoPartitionDirectories() throws BatchResultAggregationException, IOException {
		// Create base directory but no partition directories
		Path baseDir = tempDir.resolve("empty-base");
		Files.createDirectories(baseDir);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, baseDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify it's an empty result ZIP with README
		verifyEmptyZipContent(resultZip);
	}

	@Test
	void testAggregateResults_YieldTableMerging() throws BatchResultAggregationException, IOException {
		// Setup two partitions with different yield tables
		Path partition1 = tempDir.resolve("output-partition0");
		Path partition2 = tempDir.resolve("output-partition1");
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

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		// Verify yield tables are merged with sequential table numbers
		verifyYieldTableMerging(resultZip);
	}

	@Test
	void testAggregateResults_LogAggregation() throws BatchResultAggregationException, IOException {
		// Test aggregation of different log types
		Path partition1 = tempDir.resolve("output-partition0");
		Path partition2 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		// Different log types in different partitions
		Files.writeString(partition1.resolve("error.log"), "Error 1");
		Files.writeString(partition1.resolve("progress.log"), "Progress 1");
		Files.writeString(partition2.resolve("error.log"), "Error 2");
		Files.writeString(partition2.resolve("debug.log"), "Debug info");

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		verifyLogAggregation(resultZip);
	}

	@Test
	void testAggregateResults_InsufficientColumns() throws BatchResultAggregationException, IOException {
		// Test yield table with insufficient columns
		Path partitionDir = tempDir.resolve("output-partition0");
		Files.createDirectories(partitionDir);

		String invalidYieldTable = """
				TABLE_NUM,FEATURE_ID
				1,123
				""";

		Files.writeString(partitionDir.resolve("YieldTable.csv"), invalidYieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_MixedFileTypes() throws BatchResultAggregationException, IOException {
		// Setup partition with various file types
		Path partitionDir = tempDir.resolve("output-partition0");
		Files.createDirectories(partitionDir);

		// Create yield table
		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		// Create various log files
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("progress.log"), "Progress information");
		Files.writeString(partitionDir.resolve("debug.log"), "Debug information");

		// Create other result files
		Files.writeString(partitionDir.resolve("projection_results.csv"), "FEATURE_ID,RESULT\n123,data");

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify all file types are aggregated correctly
		verifyMixedFileTypesZipContent(resultZip);
	}

	private void setupPartitionDirectories() throws IOException {
		Path partitionDir = tempDir.resolve("output-partition0");
		Files.createDirectories(partitionDir);

		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("other_results.csv"), "FEATURE_ID,DATA\n123456789,result_data");
	}

	private void verifyZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ( (entry = zis.getNextEntry()) != null) {
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
			while ( (entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertEquals(1, entryNames.size());
		assertEquals("README.txt", entryNames.get(0));
	}

	private void verifyMixedFileTypesZipContent(Path zipPath) throws IOException {
		List<String> entryNames = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ( (entry = zis.getNextEntry()) != null) {
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
			while ( (entry = zis.getNextEntry()) != null) {
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
			while ( (entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(entryName)) {
					return new String(zis.readAllBytes());
				}
			}
		}
		return null;
	}

	@Test
	void testAggregateResults_NonDirectoryPath_ThrowsException() throws IOException {
		// Create a file instead of directory
		Path filePath = tempDir.resolve("not-a-directory.txt");
		Files.writeString(filePath, "test");
		String filePathString = filePath.toString();

		BatchResultAggregationException exception = assertThrows(
				BatchResultAggregationException.class, () -> aggregateResultsFromJobDir(filePathString)
		);

		assertTrue(exception.getMessage().contains("not a directory"));
	}

	private Path aggregateResultsFromJobDir(String directoryPath) throws BatchResultAggregationException {
		return resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, directoryPath, JOB_TIMESTAMP);
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
	void testValidateConsolidatedZip_MissingYieldTable() throws BatchResultAggregationException, IOException {
		// Create ZIP without YieldTable.csv
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);
		Files.writeString(partition.resolve("error.log"), "Error log only");

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

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
	void testAggregateResults_YieldTableWithSameFeatureIdDifferentLayers()
			throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		// Same FEATURE_ID with different layers
		String yieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,123456789,FD,P,FD,100.0,50
				1,123456789,CW,S,CW,80.0,45
				""";
		Files.writeString(partition.resolve("YieldTable.csv"), yieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);

		// Should have different table numbers for different polygon/layer combinations
		assertTrue(yieldTableContent.contains("123456789"));
	}

	@Test
	void testAggregateResults_GeneralLogType() throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		// Create a log file that doesn't match error/progress/debug patterns
		Files.writeString(partition.resolve("application.log"), "General application log");
		Files.writeString(partition.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resultZip))) {
			ZipEntry entry;
			while ( (entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertTrue(entryNames.contains("GeneralLog.txt"));
	}

	@Test
	void testFindPartitionDirectories_WithDirectMatchPattern() throws BatchResultAggregationException, IOException {
		// Create directories with exact prefix match
		Path partition1 = tempDir.resolve("output-partition0");
		Path partition2 = tempDir.resolve("output-partition10");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		Files.writeString(partition1.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);
		Files.writeString(partition2.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_WithNonExistentYieldTable() throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		// Try to process but yield table doesn't exist
		Files.writeString(partition.resolve("error.log"), ERROR_LOG_CONTENT);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		boolean isValid = resultAggregationService.validateConsolidatedZip(resultZip);
		assertFalse(isValid, "Should be invalid without YieldTable.csv");
	}

	@Test
	void testAggregateResults_WithUnreadableYieldTable() throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		Path yieldTablePath = partition.resolve("YieldTable.csv");
		Files.writeString(yieldTablePath, YIELD_TABLE_CONTENT);

		// Make file unreadable (on Windows this may not work as expected, but test
		// should pass)
		if (yieldTablePath.toFile().setReadable(false)) {
			Path resultZip = resultAggregationService
					.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

			assertNotNull(resultZip);
			assertTrue(Files.exists(resultZip));

			// Restore permissions for cleanup
			yieldTablePath.toFile().setReadable(true);
		}
	}

	@Test
	void testAggregateResults_WithEmptyLines() throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		// Create yield table with empty and whitespace lines
		String yieldTableWithEmptyLines = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE

				1,123456789,FD,P,FD,100.0,50

				1,123456789,CW,S,CW,80.0,45
				""";
		Files.writeString(partition.resolve("YieldTable.csv"), yieldTableWithEmptyLines);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);
	}

	@Test
	void testAggregateResults_WithHeaderKeywordVariations() throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		// Test various header patterns
		String[] headerVariations = { "TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID", "FEATURE_ID,POLYGON_ID,LAYER_ID",
				"POLYGON_ID,LAYER_ID,SPECIES_CODE", "SPECIES_CODE,LAYER_ID,TABLE_NUM" };

		for (int i = 0; i < headerVariations.length; i++) {
			Path partitionDir = tempDir.resolve("output-partition" + i);
			Files.createDirectories(partitionDir);

			String yieldTable = headerVariations[i] + "\n1,123,FD,P\n";
			Files.writeString(partitionDir.resolve("YieldTable.csv"), yieldTable);
		}

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testMergeLogs_WithPartialFailures() throws BatchResultAggregationException, IOException {
		Path partition1 = tempDir.resolve("output-partition0");
		Path partition2 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		// Create log files with some that will fail
		Files.writeString(partition1.resolve("error.log"), "Error 1");
		Files.writeString(partition2.resolve("error.log"), "Error 2");
		Files.writeString(partition1.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		// Create a directory instead of file (will cause copy failure)
		Path invalidLog = partition2.resolve("error.log.backup");
		Files.createDirectories(invalidLog);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(resultZip))) {
			ZipEntry entry;
			while ( (entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
			}
		}

		assertTrue(entryNames.contains("ErrorLog.txt"));
	}

	@Test
	void testAggregateResults_FirstPartitionEmptyYieldTable_HeaderRecovery()
			throws BatchResultAggregationException, IOException {
		// Scenario: First partition has empty yield table (projection failed)
		// Second partition has valid yield table with header
		// Expected: Header should be recovered from second partition

		Path partition0 = tempDir.resolve("output-partition0");
		Path partition1 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition0);
		Files.createDirectories(partition1);

		// First partition: empty yield table (simulates projection failure for all records in chunk)
		Files.writeString(partition0.resolve("YieldTable.csv"), "");

		// Second partition: valid yield table with header and data
		String validYieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,222222222,FD,P,FD,100.0,50
				""";
		Files.writeString(partition1.resolve("YieldTable.csv"), validYieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify that the final YieldTable.csv has header
		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent, "YieldTable.csv should exist in ZIP");

		// Should contain header from second partition
		assertTrue(yieldTableContent.contains("TABLE_NUM"), "Should contain TABLE_NUM header");
		assertTrue(yieldTableContent.contains("FEATURE_ID"), "Should contain FEATURE_ID header");
		assertTrue(yieldTableContent.contains("222222222"), "Should contain data from second partition");
	}

	@Test
	void testAggregateResults_AllPartitionsEmptyYieldTable_NoHeaderRecovery()
			throws BatchResultAggregationException, IOException {
		// Scenario: All partitions have empty yield tables (all projections failed)
		// Expected: Final YieldTable.csv will have no header (no valid header to recover)

		Path partition0 = tempDir.resolve("output-partition0");
		Path partition1 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition0);
		Files.createDirectories(partition1);

		// Both partitions: empty yield tables
		Files.writeString(partition0.resolve("YieldTable.csv"), "");
		Files.writeString(partition1.resolve("YieldTable.csv"), "");

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify that the final YieldTable.csv exists but may be empty or have no header
		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent, "YieldTable.csv should exist in ZIP even if empty");
	}

	@Test
	void testAggregateResults_FirstPartitionNoHeader_SecondPartitionWithHeader()
			throws BatchResultAggregationException, IOException {
		// Scenario: First partition has data but no header (unusual case)
		// Second partition has header and data
		// Expected: Header from second partition should be recovered

		Path partition0 = tempDir.resolve("output-partition0");
		Path partition1 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition0);
		Files.createDirectories(partition1);

		// First partition: data only, no header
		String dataOnly = "1,111111111,FD,P,FD,100.0,40\n";
		Files.writeString(partition0.resolve("YieldTable.csv"), dataOnly);

		// Second partition: header and data
		String validYieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,222222222,CW,P,CW,90.0,45
				""";
		Files.writeString(partition1.resolve("YieldTable.csv"), validYieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify that the final YieldTable.csv has header
		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);

		// Should have data from first partition (no header written initially)
		assertTrue(yieldTableContent.contains("111111111"), "Should contain data from first partition");

		// Should have header from second partition
		assertTrue(yieldTableContent.contains("TABLE_NUM"), "Should contain header");

		// Should have data from second partition
		assertTrue(yieldTableContent.contains("222222222"), "Should contain data from second partition");
	}

	@Test
	void testAggregateResults_SmallInvalidFile_NotUsedForHeaderRecovery()
			throws BatchResultAggregationException, IOException {
		// Scenario: First partition has small empty file (< 1 KB)
		// Second partition has large valid file with header
		// Expected: Small file should be ignored, header recovered from larger file

		Path partition0 = tempDir.resolve("output-partition0");
		Path partition1 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition0);
		Files.createDirectories(partition1);

		// First partition: very small file (below MIN_FILE_SIZE threshold of 1 KB)
		Files.writeString(partition0.resolve("YieldTable.csv"), "small");

		// Second partition: large valid file with header
		String validYieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,333333333,FD,P,FD,100.0,50
				1,333333333,CW,S,CW,80.0,45
				""";
		Files.writeString(partition1.resolve("YieldTable.csv"), validYieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent);

		// Should contain header from valid partition
		assertTrue(yieldTableContent.contains("TABLE_NUM"), "Should contain header");
		assertTrue(yieldTableContent.contains("333333333"), "Should contain data");
	}

	@Test
	void testProcessYieldTableResult_NoValidHeaderFound() throws BatchResultAggregationException, IOException {
		Path partition0 = tempDir.resolve("output-partition0");
		Path partition1 = tempDir.resolve("output-partition1");
		Files.createDirectories(partition0);
		Files.createDirectories(partition1);

		String dataOnly1 = "1,111111111,FD,P,FD,100.0,40\n";
		String dataOnly2 = "2,222222222,CW,P,CW,90.0,45\n";
		Files.writeString(partition0.resolve("YieldTable.csv"), dataOnly1);
		Files.writeString(partition1.resolve("YieldTable.csv"), dataOnly2);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		String yieldTableContent = getZipEntryContent(resultZip, "YieldTable.csv");
		assertNotNull(yieldTableContent, "YieldTable.csv should exist");

		assertTrue(yieldTableContent.contains("111111111"), "Should contain first partition data");
		assertTrue(yieldTableContent.contains("222222222"), "Should contain second partition data");
	}

	@Test
	void testValidateConsolidatedZip_ZipException() throws IOException {
		Path invalidZip = tempDir.resolve("invalid.zip");
		Files.writeString(invalidZip, "This is not a valid ZIP file");

		boolean isValid = resultAggregationService.validateConsolidatedZip(invalidZip);

		assertFalse(isValid, "Should return false for corrupted ZIP file");
	}

	@Test
	void testExtractPartitionNumber_InvalidNumber() throws BatchResultAggregationException, IOException {
		Path partitionInvalid = tempDir.resolve("output-partitionABC");
		Path partitionValid = tempDir.resolve("output-partition0");
		Files.createDirectories(partitionInvalid);
		Files.createDirectories(partitionValid);

		Files.writeString(partitionValid.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testSearchForValidHeaderInPartitions_InvalidPartitionDirectory()
			throws BatchResultAggregationException, IOException {
		Path partition0 = tempDir.resolve("output-partition0");
		Files.createDirectories(partition0);

		Files.writeString(partition0.resolve("YieldTable.csv"), "");

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testSearchForValidHeaderInPartitions_FileSizeCheckIOException()
			throws BatchResultAggregationException, IOException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		String validYieldTable = """
				TABLE_NUM,FEATURE_ID,SPECIES_1,LAYER_ID,GENUS,SP0_PERCENTAGE,TOTAL_AGE
				1,123456789,FD,P,FD,100.0,50
				""";
		Files.writeString(partition.resolve("YieldTable.csv"), validYieldTable);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testExtractHeaderFromFile_InvalidHeaderLine() throws IOException, BatchResultAggregationException {
		Path partition = tempDir.resolve("output-partition0");
		Files.createDirectories(partition);

		String dataOnly = "1,123456789,FD,P,FD,100.0,50\n";
		Files.writeString(partition.resolve("YieldTable.csv"), dataOnly);

		Path resultZip = resultAggregationService
				.aggregateResultsFromJobDir(JOB_EXECUTION_ID, JOB_GUID, tempDir.toString(), JOB_TIMESTAMP);

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}
}
