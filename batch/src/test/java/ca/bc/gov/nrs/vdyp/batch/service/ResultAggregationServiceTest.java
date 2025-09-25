package ca.bc.gov.nrs.vdyp.batch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ResultAggregationServiceTest {

	@InjectMocks
	private ResultAggregationService resultAggregationService;

	@TempDir
	Path tempDir;

	private static final Long JOB_EXECUTION_ID = 1L;
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

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

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
				() -> resultAggregationService.aggregateResults(JOB_EXECUTION_ID, nonExistentDir.toString()));

		assertTrue(exception.getMessage().contains("Base output directory does not exist"));
	}

	@Test
	void testAggregateResults_NoPartitionDirectories() throws IOException {
		// Create base directory but no partition directories
		Path baseDir = tempDir.resolve("empty-base");
		Files.createDirectories(baseDir);

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, baseDir.toString());

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify it's an empty result ZIP with README
		verifyEmptyZipContent(resultZip);
	}

	@Test
	void testAggregateResults_MultiplePartitions() throws IOException {
		setupMultiplePartitionDirectories();

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		verifyMultiplePartitionZipContent(resultZip);
	}

	@Test
	void testAggregateResults_YieldTableMerging() throws IOException {
		// Setup two partitions with different yield tables
		Path partition1 = tempDir.resolve("partition-0");
		Path partition2 = tempDir.resolve("partition-1");
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

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		// Verify yield tables are merged with sequential table numbers
		verifyYieldTableMerging(resultZip);
	}

	@Test
	void testAggregateResults_LogAggregation() throws IOException {
		// Test aggregation of different log types
		Path partition1 = tempDir.resolve("partition-0");
		Path partition2 = tempDir.resolve("partition-1");
		Files.createDirectories(partition1);
		Files.createDirectories(partition2);

		// Different log types in different partitions
		Files.writeString(partition1.resolve("error.log"), "Error 1");
		Files.writeString(partition1.resolve("progress.log"), "Progress 1");
		Files.writeString(partition2.resolve("error.log"), "Error 2");
		Files.writeString(partition2.resolve("debug.log"), "Debug info");

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		verifyLogAggregation(resultZip);
	}

	@Test
	void testAggregateResults_EmptyYieldTable() throws IOException {
		// Test with empty yield table file
		Path partitionDir = tempDir.resolve("partition-0");
		Files.createDirectories(partitionDir);

		Files.writeString(partitionDir.resolve("YieldTable.csv"), "");
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_InsufficientColumns() throws IOException {
		// Test yield table with insufficient columns
		Path partitionDir = tempDir.resolve("partition-0");
		Files.createDirectories(partitionDir);

		String invalidYieldTable = """
				TABLE_NUM,FEATURE_ID
				1,123
				""";

		Files.writeString(partitionDir.resolve("YieldTable.csv"), invalidYieldTable);

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));
	}

	@Test
	void testAggregateResults_MixedFileTypes() throws IOException {
		// Setup partition with various file types
		Path partitionDir = tempDir.resolve("partition-0");
		Files.createDirectories(partitionDir);

		// Create yield table
		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);

		// Create various log files
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("progress.log"), "Progress information");
		Files.writeString(partitionDir.resolve("debug.log"), "Debug information");

		// Create other result files
		Files.writeString(partitionDir.resolve("projection_results.csv"), "FEATURE_ID,RESULT\n123,data");

		Path resultZip = resultAggregationService.aggregateResults(JOB_EXECUTION_ID, tempDir.toString());

		assertNotNull(resultZip);
		assertTrue(Files.exists(resultZip));

		// Verify all file types are aggregated correctly
		verifyMixedFileTypesZipContent(resultZip);
	}

	private void setupPartitionDirectories() throws IOException {
		Path partitionDir = tempDir.resolve("partition-0");
		Files.createDirectories(partitionDir);

		Files.writeString(partitionDir.resolve("YieldTable.csv"), YIELD_TABLE_CONTENT);
		Files.writeString(partitionDir.resolve("error.log"), ERROR_LOG_CONTENT);
		Files.writeString(partitionDir.resolve("other_results.csv"), "FEATURE_ID,DATA\n123456789,result_data");
	}

	private void setupMultiplePartitionDirectories() throws IOException {
		for (int i = 0; i < 3; i++) {
			Path partitionDir = tempDir.resolve("partition-" + i);
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
}
