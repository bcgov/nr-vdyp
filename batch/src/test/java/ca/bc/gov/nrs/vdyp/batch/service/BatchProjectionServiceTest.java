package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchProjectionException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultStorageException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;

class BatchProjectionServiceTest {

	private BatchProjectionService batchProjectionService;
	private Parameters parameters;

	@TempDir
	Path tempDir;

	private static final String PARTITION_NAME = "partition0";
	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		batchProjectionService = new BatchProjectionService();
		parameters = new Parameters();
	}

	@Test
	void testPerformProjectionForChunk_WithMissingPartitionFiles() {
		// Create chunk metadata pointing to non-existent partition files
		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata(PARTITION_NAME, tempDir.toString(), 0, 1);

		BatchResultStorageException exception = assertThrows(BatchResultStorageException.class, () -> {
			batchProjectionService.performProjectionForChunk(chunkMetadata, parameters, JOB_EXECUTION_ID, JOB_GUID);
		});

		assertNotNull(exception.getMessage());
	}

	@Test
	void testPerformProjectionForChunk_WithInvalidPolygonData() throws IOException {
		// Create partition structure with invalid polygon data
		createPartitionStructure(PARTITION_NAME, List.of("123456789,MAP1"), List.of("123456789,P"));

		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata(PARTITION_NAME, tempDir.toString(), 0, 1);

		BatchProjectionException exception = assertThrows(BatchProjectionException.class, () -> {
			batchProjectionService.performProjectionForChunk(chunkMetadata, parameters, JOB_EXECUTION_ID, JOB_GUID);
		});

		assertNotNull(exception.getMessage());
		assertTrue(exception.getMessage().contains("partition") || exception.getMessage().contains(PARTITION_NAME));

		// Verify output directory was created
		Path expectedOutputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(expectedOutputDir), "Output directory should be created");
		assertTrue(Files.isDirectory(expectedOutputDir), "Output path should be a directory");
	}

	@Test
	void testPerformProjectionForChunk_WithMultipleRecordsInChunk() throws IOException {
		createPartitionStructure(
				PARTITION_NAME, List.of("100000000,MAP1", "200000000,MAP2", "300000000,MAP3"),
				List.of("100000000,P", "200000000,P", "300000000,P")
		);

		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata(PARTITION_NAME, tempDir.toString(), 0, 3);

		assertThrows(BatchProjectionException.class, () -> {
			batchProjectionService.performProjectionForChunk(chunkMetadata, parameters, JOB_EXECUTION_ID, JOB_GUID);
		});
	}

	@Test
	void testPerformProjectionForChunk_WithChunkOffset() throws IOException {
		// Create partition with 10 records
		createPartitionStructure(PARTITION_NAME, generatePolygonLines(10), generateLayerLines(10));

		// Process chunk: startIndex=5, recordCount=3 (records 5, 6, 7)
		BatchChunkMetadata chunkMetadata = new BatchChunkMetadata(PARTITION_NAME, tempDir.toString(), 5, 3);

		assertThrows(BatchProjectionException.class, () -> {
			batchProjectionService.performProjectionForChunk(chunkMetadata, parameters, JOB_EXECUTION_ID, JOB_GUID);
		});
	}

	/**
	 * Creates a minimal partition directory structure with polygon and layer CSV files.
	 */
	private void createPartitionStructure(String partitionName, List<String> polygonLines, List<String> layerLines)
			throws IOException {
		String inputPartitionFolderName = BatchUtils.buildInputPartitionFolderName(partitionName);
		Path partitionDir = tempDir.resolve(inputPartitionFolderName);
		Files.createDirectories(partitionDir);

		Path polygonFile = partitionDir.resolve(BatchConstants.Partition.INPUT_POLYGON_FILE_NAME);
		Path layerFile = partitionDir.resolve(BatchConstants.Partition.INPUT_LAYER_FILE_NAME);

		Files.write(polygonFile, polygonLines);
		Files.write(layerFile, layerLines);
	}

	private List<String> generatePolygonLines(int count) {
		String[] featureIds = { "100000000", "200000000", "300000000", "400000000", "500000000", "600000000",
				"700000000", "800000000", "900000000", "101000000" };

		return java.util.stream.IntStream.range(0, count).mapToObj(i -> featureIds[i % featureIds.length] + ",MAP" + i)
				.toList();
	}

	private List<String> generateLayerLines(int count) {
		String[] featureIds = { "100000000", "200000000", "300000000", "400000000", "500000000", "600000000",
				"700000000", "800000000", "900000000", "101000000" };

		return java.util.stream.IntStream.range(0, count).mapToObj(i -> featureIds[i % featureIds.length] + ",P")
				.toList();
	}

	@Test
	void testStoreYieldTable_WithNullYieldTable() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Path outputDir = tempDir.resolve("output");
		Files.createDirectories(outputDir);

		Method method = BatchProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, int.class);
		method.setAccessible(true);

		method.invoke(batchProjectionService, null, outputDir, "projection1", 10);

		Path unexpectedFile = outputDir.resolve("YieldTables_projection1_test_yield_table.txt");
		assertTrue(!Files.exists(unexpectedFile), "No file should be created for null YieldTable");
	}

	@Test
	void testStoreYieldTable_WithNullStream() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Path outputDir = tempDir.resolve("output");
		Files.createDirectories(outputDir);

		YieldTable mockYieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);

		when(mockYieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("test_yield_table.txt");
		when(mockYieldTable.getAsStream()).thenReturn(null);

		Method method = BatchProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, int.class);
		method.setAccessible(true);

		method.invoke(batchProjectionService, mockYieldTable, outputDir, "projection1", 10);

		Path unexpectedFile = outputDir.resolve("YieldTables_projection1_test_yield_table.txt");
		assertTrue(!Files.exists(unexpectedFile), "No file should be created for null stream");
	}

	@Test
	void testStoreYieldTable_WithValidNonEmptyStream() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Path outputDir = tempDir.resolve("output");
		Files.createDirectories(outputDir);

		YieldTable mockYieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		byte[] testData = "test yield table data".getBytes();
		InputStream testStream = new ByteArrayInputStream(testData);

		when(mockYieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("test_yield_table.txt");
		when(mockYieldTable.getAsStream()).thenReturn(testStream);

		Method method = BatchProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, int.class);
		method.setAccessible(true);

		method.invoke(batchProjectionService, mockYieldTable, outputDir, "projection1", 10);

		Path expectedFile = outputDir.resolve("YieldTables_projection1_test_yield_table.txt");
		assertTrue(Files.exists(expectedFile));
		assertTrue(Files.size(expectedFile) > 0);
	}

	@Test
	void testStoreYieldTable_WithEmptyStream() throws IOException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Path outputDir = tempDir.resolve("output");
		Files.createDirectories(outputDir);

		YieldTable mockYieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

		when(mockYieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("empty_table.txt");
		when(mockYieldTable.getAsStream()).thenReturn(emptyStream);

		Method method = BatchProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, int.class);
		method.setAccessible(true);

		method.invoke(batchProjectionService, mockYieldTable, outputDir, "projection1", 5);

		Path expectedFile = outputDir.resolve("YieldTables_projection1_empty_table.txt");
		assertTrue(Files.exists(expectedFile));
		assertEquals(0L, Files.size(expectedFile));
	}

	@Test
	void testStoreDebugLog() throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Path outputDir = tempDir.resolve("output");
		Files.createDirectories(outputDir);

		Method method = BatchProjectionService.class
				.getDeclaredMethod("storeDebugLog", Path.class, String.class, int.class);
		method.setAccessible(true);

		method.invoke(batchProjectionService, outputDir, "projection1", 10);

		Path debugLogFile = outputDir.resolve("YieldTables_projection1_DebugLog.txt");
		assertTrue(Files.exists(debugLogFile));
		assertEquals(0L, Files.size(debugLogFile));
	}
}
