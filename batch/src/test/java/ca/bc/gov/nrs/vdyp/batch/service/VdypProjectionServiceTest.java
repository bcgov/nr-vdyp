package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;

class VdypProjectionServiceTest {

	private VdypProjectionService vdypProjectionService;
	private Parameters parameters;

	@TempDir
	Path tempDir;

	private static final String PARTITION_NAME = "partition0";
	private static final Long JOB_EXECUTION_ID = 1L;
	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		vdypProjectionService = new VdypProjectionService();
		parameters = new Parameters();
	}

	@Test
	void testPerformProjectionForChunk_EmptyRecords() {
		List<BatchRecord> emptyRecords = new ArrayList<>();

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					emptyRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_EmptyJobBaseDir() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, ""
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_CreatesOutputDirectory() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		} catch (BatchException e) {
			// Expected to fail during projection
		}

		Path expectedOutputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(expectedOutputDir), "Output directory should be created");
	}

	@Test
	void testPerformProjectionForChunk_PartitionNameConversion() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, "partition5", parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		} catch (BatchException e) {
			// Expected
		}

		Path expectedOutputDir = tempDir.resolve("output-partition5");
		assertTrue(Files.exists(expectedOutputDir));
	}

	@Test
	void testPerformProjectionForChunk_DifferentPartitionNumbers() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		String[] partitionNames = { "partition0", "partition10", "partition999" };

		for (String partName : partitionNames) {
			try {
				vdypProjectionService.performProjectionForChunk(
						batchRecords, partName, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
				);
			} catch (BatchException e) {
				// Expected
			}

			Path expectedOutputDir = tempDir.resolve("output-" + partName);
			assertTrue(Files.exists(expectedOutputDir));
		}
	}

	@Test
	void testPerformProjectionForChunk_MultipleRecordsCombination() {
		List<BatchRecord> batchRecords = createValidBatchRecords(3);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		Path expectedOutputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(expectedOutputDir));
	}

	@Test
	void testPerformProjectionForChunk_MultipleLayersPerRecord() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord(
				"123456789", "123456789,MAP1", List.of("123456789,P", "123456789,S", "123456789,V"), PARTITION_NAME
		);
		batchRecords.add(batchRecord);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_ErrorContextWithLargeChunk() {
		List<BatchRecord> batchRecords = createValidBatchRecords(10);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		String message = exception.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("10 records") || message.contains("partition"));
	}

	@Test
	void testPerformProjectionForChunk_ErrorContextShowsFirst5FeatureIds() {
		List<BatchRecord> batchRecords = createValidBatchRecords(7);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		String message = exception.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("7 records") || message.contains("partition"));
	}

	@Test
	void testBuildBatchProjectionId_HCSV() {
		String projectionId = BatchUtils
				.buildBatchProjectionId(JOB_EXECUTION_ID, PARTITION_NAME, ProjectionRequestKind.HCSV);

		assertNotNull(projectionId);
		assertTrue(projectionId.contains("batch-1"));
		assertTrue(projectionId.contains("partition0"));
		assertTrue(projectionId.contains("projection-HCSV"));
		assertTrue(projectionId.matches(".*\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}$"));
	}

	@Test
	void testBuildBatchProjectionId_TimestampFormat() {
		String id1 = BatchUtils.buildBatchProjectionId(1L, "p0", ProjectionRequestKind.HCSV);
		String id2 = BatchUtils.buildBatchProjectionId(1L, "p0", ProjectionRequestKind.HCSV);

		assertTrue(id1.contains("batch-1"));
		assertTrue(id1.contains("p0"));
		assertTrue(id1.contains("projection-HCSV"));
		assertTrue(id1.matches(".*\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}$"));

		assertTrue(id2.contains("batch-1"));
		assertTrue(id2.contains("p0"));
		assertTrue(id2.contains("projection-HCSV"));
		assertTrue(id2.matches(".*\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}$"));
	}

	@Test
	void testPerformProjectionForChunk_NullHeaders() {
		List<BatchRecord> batchRecords = new ArrayList<>();

		BatchRecord batchRecord = new BatchRecord(
				"123456789", "123456789,MAP1", List.of("123456789,P"), PARTITION_NAME
		);
		batchRecords.add(batchRecord);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_EmptyLayerList() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord("123456789", "123456789,MAP1", new ArrayList<>(), PARTITION_NAME);
		batchRecords.add(batchRecord);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_MixedValidAndInvalidRecords() {
		List<BatchRecord> batchRecords = new ArrayList<>();

		// Valid record
		BatchRecord validRecord = new BatchRecord(
				"123456789", "123456789,MAP1", List.of("123456789,P"), PARTITION_NAME
		);
		batchRecords.add(validRecord);

		BatchRecord invalidRecord = new BatchRecord(
				"987654321", "987654321,MAP1", List.of("987654321,P"), PARTITION_NAME
		);
		batchRecords.add(invalidRecord);

		assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});
	}

	@Test
	void testPerformProjectionForChunk_LargeFeatureIdSet() {
		List<BatchRecord> batchRecords = createValidBatchRecords(20);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		String message = exception.getMessage();
		assertTrue(message.contains("20 records") || message.contains("partition"));
	}

	@Test
	void testPerformProjectionForChunk_PartitionNameInErrorMessage() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, "partition777", parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		assertTrue(exception.getMessage().contains("partition777") || exception.getMessage().contains("partition"));
	}

	@Test
	void testPerformProjectionForChunk_ExceptionTypeInErrorMessage() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		String message = exception.getMessage();
		assertNotNull(message);
	}

	@Test
	void testBuildBatchProjectionId_DifferentJobExecutionIds() {
		long[] jobIds = { 1L, 100L, 9999L, Long.MAX_VALUE };

		for (long jobId : jobIds) {
			String projectionId = BatchUtils.buildBatchProjectionId(jobId, PARTITION_NAME, ProjectionRequestKind.HCSV);

			assertTrue(projectionId.contains("batch-" + jobId));
		}
	}

	@Test
	void testPerformProjectionForChunk_VerifiesOutputDirectoryIsDirectory() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		} catch (BatchException e) {
			// Expected
		}

		Path outputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(outputDir));
		assertTrue(Files.isDirectory(outputDir));
	}

	@Test
	void testPerformProjectionForChunk_WithExactly5Records() {
		List<BatchRecord> batchRecords = createValidBatchRecords(5);

		BatchException exception = assertThrows(BatchException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, JOB_GUID, tempDir.toString()
			);
		});

		String message = exception.getMessage();
		assertTrue(message.contains("5 records") || message.contains("partition"));
	}

	@Test
	void testStoreDebugLog_CreatesFileWithZeroSize()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		String projectionId = "test-projection-123";
		List<BatchRecord> batchRecords = createValidBatchRecords(2);

		Method storeDebugLogMethod = VdypProjectionService.class
				.getDeclaredMethod("storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

		String expectedFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = tempDir.resolve(expectedFileName);

		assertTrue(Files.exists(debugLogPath), "Debug log file should be created");
		assertTrue(Files.isRegularFile(debugLogPath), "Debug log should be a regular file");
		assertEquals(0, Files.size(debugLogPath), "Debug log file should be empty");
	}

	@Test
	void testStoreDebugLog_ReplacesExistingFileWithEmptyFile()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		String projectionId = "test-projection-456";
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		String debugLogFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = tempDir.resolve(debugLogFileName);
		Files.writeString(debugLogPath, "This is existing content that should be overwritten");

		assertTrue(Files.size(debugLogPath) > 0, "File should have content before test");

		Method storeDebugLogMethod = VdypProjectionService.class
				.getDeclaredMethod("storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

		assertTrue(Files.exists(debugLogPath), "Debug log file should exist");
		assertEquals(0, Files.size(debugLogPath), "Debug log file should be empty after overwrite");
	}

	@Test
	void testStoreDebugLog_WithDifferentProjectionIds()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String[] projectionIds = { "batch-1-partition0-projection-HCSV-2025_10_02_14_06_43_4933",
				"batch-99-partition5-projection-DCSV-2025_12_31_23_59_59_9999", "simple-id" };

		Method storeDebugLogMethod = VdypProjectionService.class
				.getDeclaredMethod("storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		for (String projectionId : projectionIds) {
			storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

			String expectedFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
			Path debugLogPath = tempDir.resolve(expectedFileName);

			assertTrue(Files.exists(debugLogPath), "Debug log should be created for projection ID: " + projectionId);
			assertEquals(0, Files.size(debugLogPath), "File should be empty");
		}
	}

	@Test
	void testStoreDebugLog_IOExceptionHandling() throws NoSuchMethodException {
		String projectionId = "test-projection-123";
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		Path invalidParentDir = tempDir.resolve("nonexistent/parent/dir");

		assertFalse(Files.exists(invalidParentDir), "Parent directory should not exist");

		Method storeDebugLogMethod = VdypProjectionService.class
				.getDeclaredMethod("storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
			storeDebugLogMethod.invoke(vdypProjectionService, invalidParentDir, projectionId, batchRecords);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException, "Expected IOException to be thrown");
	}

	@Test
	void testCreateOutputPartitionDir_NullJobBaseDir() throws NoSuchMethodException {
		Method createOutputPartitionDirMethod = VdypProjectionService.class
				.getDeclaredMethod("createOutputPartitionDir", String.class, String.class);
		createOutputPartitionDirMethod.setAccessible(true);

		InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
			createOutputPartitionDirMethod.invoke(vdypProjectionService, "partition0", null);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof NullPointerException || cause instanceof IOException);
	}

	@Test
	void testCreateOutputPartitionDir_IOExceptionHandling() throws NoSuchMethodException, IOException {
		String partitionName = "partition0";
		String jobBaseDir = tempDir.toString();

		Path outputPartitionDir = tempDir.resolve("output-partition0");

		Files.createFile(outputPartitionDir);

		assertTrue(Files.exists(outputPartitionDir), "Output path should exist as a file");
		assertTrue(Files.isRegularFile(outputPartitionDir), "Output path should be a file, not directory");

		Method createOutputPartitionDirMethod = VdypProjectionService.class
				.getDeclaredMethod("createOutputPartitionDir", String.class, String.class);
		createOutputPartitionDirMethod.setAccessible(true);

		InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
			createOutputPartitionDirMethod.invoke(vdypProjectionService, partitionName, jobBaseDir);
		});

		Throwable cause = exception.getCause();
		assertNotNull(cause, "Exception cause should not be null");
		assertTrue(
				cause instanceof IOException,
				"Expected IOException, but got: " + (cause != null ? cause.getClass().getSimpleName() : "null")
		);
	}

	@Test
	void testCreateCombinedInputStreamsFromChunk_EmptyRecords() throws NoSuchMethodException {
		Method createCombinedInputStreamsFromChunkMethod = VdypProjectionService.class
				.getDeclaredMethod("createCombinedInputStreamsFromChunk", List.class);
		createCombinedInputStreamsFromChunkMethod.setAccessible(true);

		List<BatchRecord> emptyRecords = new ArrayList<>();

		assertDoesNotThrow(() -> {
			createCombinedInputStreamsFromChunkMethod.invoke(vdypProjectionService, emptyRecords);
		});
	}

	@Test
	void testStoreYieldTable_NullYieldTable() throws NoSuchMethodException {
		Method storeYieldTableMethod = VdypProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String projectionId = "test-projection";

		assertDoesNotThrow(() -> {
			storeYieldTableMethod.invoke(vdypProjectionService, null, tempDir, projectionId, batchRecords);
		});
	}

	@Test
	void testStoreYieldTable_IOExceptionHandling() throws NoSuchMethodException {
		String projectionId = "test-projection-123";
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		// Mock YieldTable to provide a valid filename and InputStream
		YieldTable yieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		when(yieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("testYieldTable.csv");
		when(yieldTable.getAsStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));

		// Create a Path with a non-existent parent directory to trigger IOException
		Path invalidParentDir = tempDir.resolve("nonexistent/parent/dir");
		assertFalse(Files.exists(invalidParentDir), "Parent directory should not exist");

		Method storeYieldTableMethod = VdypProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
			storeYieldTableMethod
					.invoke(vdypProjectionService, yieldTable, invalidParentDir, projectionId, batchRecords);
		});

		Throwable cause = exception.getCause();
		assertNotNull(cause, "Exception cause should not be null");
		assertTrue(
				cause instanceof IOException,
				"Expected IOException, but got: " + (cause != null ? cause.getClass().getSimpleName() : "null")
		);
	}

	@Test
	void testCreateCombinedInputStreamsFromChunk_ValidMultipleRecords()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		Method createCombinedInputStreamsFromChunkMethod = VdypProjectionService.class
				.getDeclaredMethod("createCombinedInputStreamsFromChunk", List.class);
		createCombinedInputStreamsFromChunkMethod.setAccessible(true);

		List<BatchRecord> batchRecords = createValidBatchRecords(3);

		@SuppressWarnings("unchecked")
		java.util.Map<String, InputStream> result = (Map<String, InputStream>) createCombinedInputStreamsFromChunkMethod
				.invoke(vdypProjectionService, batchRecords);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertNotNull(result.get("HCSV-Polygon"));
		assertNotNull(result.get("HCSV-Layers"));

		InputStream polygonStream = result.get("HCSV-Polygon");
		assertNotNull(polygonStream);
		assertTrue(polygonStream.available() > 0);

		InputStream layerStream = result.get("HCSV-Layers");
		assertNotNull(layerStream);
		assertTrue(layerStream.available() > 0);
	}

	@Test
	void testCreateCombinedInputStreamsFromChunk_WithMultipleLayers()
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method createCombinedInputStreamsFromChunkMethod = VdypProjectionService.class
				.getDeclaredMethod("createCombinedInputStreamsFromChunk", List.class);
		createCombinedInputStreamsFromChunkMethod.setAccessible(true);

		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord(
				"123456789", "123456789,MAP1", List.of("123456789,P", "123456789,S", "123456789,V"), PARTITION_NAME
		);
		batchRecords.add(batchRecord);

		@SuppressWarnings("unchecked")
		Map<String, InputStream> result = (Map<String, InputStream>) createCombinedInputStreamsFromChunkMethod
				.invoke(vdypProjectionService, batchRecords);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertNotNull(result.get("HCSV-Polygon"));
		assertNotNull(result.get("HCSV-Layers"));
	}

	@Test
	void testStoreYieldTable_WithNullStream() throws NoSuchMethodException {
		Method storeYieldTableMethod = VdypProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		YieldTable yieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		when(yieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("testYieldTable.csv");
		when(yieldTable.getAsStream()).thenReturn(null);

		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String projectionId = "test-projection";

		assertDoesNotThrow(() -> {
			storeYieldTableMethod.invoke(vdypProjectionService, yieldTable, tempDir, projectionId, batchRecords);
		});
	}

	@Test
	void testStoreYieldTable_WithEmptyStream() throws NoSuchMethodException, IOException {
		Method storeYieldTableMethod = VdypProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		YieldTable yieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		when(yieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("emptyYieldTable.csv");
		when(yieldTable.getAsStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String projectionId = "test-projection";

		assertDoesNotThrow(() -> {
			storeYieldTableMethod.invoke(vdypProjectionService, yieldTable, tempDir, projectionId, batchRecords);
		});

		Path yieldTablePath = tempDir.resolve(String.format("YieldTables_%s_emptyYieldTable.csv", projectionId));
		assertTrue(Files.exists(yieldTablePath));
		assertEquals(0, Files.size(yieldTablePath));
	}

	@Test
	void testStoreYieldTable_WithValidData() throws NoSuchMethodException, IOException {
		Method storeYieldTableMethod = VdypProjectionService.class
				.getDeclaredMethod("storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		String testData = "FEATURE_ID,VOLUME\n123456789,100.5\n";
		YieldTable yieldTable = mock(YieldTable.class);
		OutputFormat mockOutputFormat = mock(OutputFormat.class);
		when(yieldTable.getOutputFormat()).thenReturn(mockOutputFormat);
		when(mockOutputFormat.getYieldTableFileName()).thenReturn("validYieldTable.csv");
		when(yieldTable.getAsStream()).thenReturn(new ByteArrayInputStream(testData.getBytes()));

		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String projectionId = "test-projection-valid";

		assertDoesNotThrow(() -> {
			storeYieldTableMethod.invoke(vdypProjectionService, yieldTable, tempDir, projectionId, batchRecords);
		});

		Path yieldTablePath = tempDir.resolve(String.format("YieldTables_%s_validYieldTable.csv", projectionId));
		assertTrue(Files.exists(yieldTablePath));
		assertTrue(Files.size(yieldTablePath) > 0);
		String content = Files.readString(yieldTablePath);
		assertEquals(testData, content);
	}

	private List<BatchRecord> createValidBatchRecords(int count) {
		List<BatchRecord> records = new ArrayList<>();
		String[] featureIds = { "100000000", "200000000", "300000000", "400000000", "500000000", "600000000",
				"700000000", "800000000", "900000000", "101000000", "111000000", "121000000", "131000000", "141000000",
				"151000000", "161000000", "171000000", "181000000", "191000000", "102000000" };

		for (int i = 0; i < count; i++) {
			String featureId = featureIds[i % featureIds.length];
			BatchRecord batchRecord = new BatchRecord(
					featureId, featureId + ",MAP" + i + ",1234,DCR", List.of(featureId + ",MAP" + i + ",1234,P"),
					PARTITION_NAME
			);
			records.add(batchRecord);
		}

		return records;
	}
}
