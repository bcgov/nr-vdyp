package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
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

	@BeforeEach
	void setUp() {
		vdypProjectionService = new VdypProjectionService();
		parameters = new Parameters();
	}

	@Test
	void testPerformProjectionForChunk_EmptyRecords() throws Exception {
		List<BatchRecord> emptyRecords = new ArrayList<>();

		String result = vdypProjectionService.performProjectionForChunk(
				emptyRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());

		assertEquals("No records to process in chunk", result);
	}

	@Test
	void testPerformProjectionForChunk_WhitespacePartitionName() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, "   ", parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		assertTrue(exception.getMessage().contains("Partition name cannot be null or empty"));
	}

	@Test
	void testPerformProjectionForChunk_EmptyJobBaseDir() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, "");
		});

		assertTrue(exception.getMessage().contains("Job base directory cannot be null or empty"));
	}

	@Test
	void testPerformProjectionForChunk_CreatesOutputDirectory() throws Exception {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		} catch (Exception e) {
			// Expected to fail during projection
		}

		Path expectedOutputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(expectedOutputDir), "Output directory should be created");
	}

	@Test
	void testPerformProjectionForChunk_PartitionNameConversion() throws Exception {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, "partition5", parameters, JOB_EXECUTION_ID, tempDir.toString());
		} catch (Exception e) {
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
						batchRecords, partName, parameters, JOB_EXECUTION_ID, tempDir.toString());
			} catch (Exception e) {
				// Expected
			}

			Path expectedOutputDir = tempDir.resolve("output-" + partName);
			assertTrue(Files.exists(expectedOutputDir));
		}
	}

	@Test
	void testPerformProjectionForChunk_NullLayerData() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		batchRecord.setRawPolygonData("123456789,MAP1");
		batchRecord.setLayerHeader("FEATURE_ID,LAYER");
		batchRecord.setRawLayerData(null);
		batchRecords.add(batchRecord);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});
	}

	@Test
	void testPerformProjectionForChunk_MultipleRecordsCombination() {
		List<BatchRecord> batchRecords = createValidBatchRecords(3);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		Path expectedOutputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(expectedOutputDir));
	}

	@Test
	void testPerformProjectionForChunk_MultipleLayersPerRecord() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		batchRecord.setRawPolygonData("123456789,MAP1");
		batchRecord.setLayerHeader("FEATURE_ID,LAYER");
		batchRecord.setRawLayerData(List.of("123456789,P", "123456789,S", "123456789,V"));
		batchRecords.add(batchRecord);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});
	}

	@Test
	void testPerformProjectionForChunk_ErrorContextWithSmallChunk() {
		List<BatchRecord> batchRecords = createValidBatchRecords(2);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("2 records") || message.contains("chunk"));
	}

	@Test
	void testPerformProjectionForChunk_ErrorContextWithLargeChunk() {
		List<BatchRecord> batchRecords = createValidBatchRecords(10);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("10 records") || message.contains("and") && message.contains("more"));
	}

	@Test
	void testPerformProjectionForChunk_ErrorContextShowsFirst5FeatureIds() {
		List<BatchRecord> batchRecords = createValidBatchRecords(7);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertTrue(message.contains("and 2 more") || message.contains("7 records"));
	}

	@Test
	void testBuildBatchProjectionId_HCSV() {
		String projectionId = VdypProjectionService.buildBatchProjectionId(
				JOB_EXECUTION_ID, PARTITION_NAME, ProjectionRequestKind.HCSV);

		assertNotNull(projectionId);
		assertTrue(projectionId.contains("batch-1"));
		assertTrue(projectionId.contains("partition0"));
		assertTrue(projectionId.contains("projection-HCSV"));
		assertTrue(projectionId.matches(".*\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}$"));
	}

	@Test
	void testBuildBatchProjectionId_DCSV() {
		String projectionId = VdypProjectionService.buildBatchProjectionId(
				123L, "partition99", ProjectionRequestKind.DCSV);

		assertTrue(projectionId.contains("batch-123"));
		assertTrue(projectionId.contains("partition99"));
		assertTrue(projectionId.contains("projection-DCSV"));
	}

	@Test
	void testBuildBatchProjectionId_SCSV() {
		String projectionId = VdypProjectionService.buildBatchProjectionId(
				456L, "partition10", ProjectionRequestKind.SCSV);

		assertTrue(projectionId.contains("batch-456"));
		assertTrue(projectionId.contains("partition10"));
		assertTrue(projectionId.contains("projection-SCSV"));
	}

	@Test
	void testBuildBatchProjectionId_TimestampFormat() {
		String id1 = VdypProjectionService.buildBatchProjectionId(1L, "p0", ProjectionRequestKind.HCSV);
		String id2 = VdypProjectionService.buildBatchProjectionId(1L, "p0", ProjectionRequestKind.HCSV);

		// Both should have valid timestamp format
		assertTrue(id1.matches("batch-1-p0-projection-HCSV-\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
		assertTrue(id2.matches("batch-1-p0-projection-HCSV-\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
	}

	@Test
	void testPerformProjectionForChunk_NullHeaders() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader(null);
		batchRecord.setRawPolygonData("123456789,MAP1");
		batchRecord.setLayerHeader(null);
		batchRecord.setRawLayerData(List.of("123456789,P"));
		batchRecords.add(batchRecord);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});
	}

	@Test
	void testPerformProjectionForChunk_EmptyLayerList() {
		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		batchRecord.setRawPolygonData("123456789,MAP1");
		batchRecord.setLayerHeader("FEATURE_ID,LAYER");
		batchRecord.setRawLayerData(new ArrayList<>());
		batchRecords.add(batchRecord);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});
	}

	@Test
	void testPerformProjectionForChunk_MixedValidAndInvalidRecords() {
		List<BatchRecord> batchRecords = new ArrayList<>();

		// Valid record
		BatchRecord validRecord = new BatchRecord();
		validRecord.setFeatureId("123456789");
		validRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		validRecord.setRawPolygonData("123456789,MAP1");
		validRecord.setLayerHeader("FEATURE_ID,LAYER");
		validRecord.setRawLayerData(List.of("123456789,P"));
		batchRecords.add(validRecord);

		// Invalid record with null data
		BatchRecord invalidRecord = new BatchRecord();
		invalidRecord.setFeatureId("987654321");
		invalidRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		invalidRecord.setRawPolygonData(null);
		invalidRecord.setLayerHeader("FEATURE_ID,LAYER");
		invalidRecord.setRawLayerData(List.of("987654321,P"));
		batchRecords.add(invalidRecord);

		assertThrows(Exception.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});
	}

	@Test
	void testPerformProjectionForChunk_LargeFeatureIdSet() {
		List<BatchRecord> batchRecords = createValidBatchRecords(20);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertTrue(message.contains("20 records"));
		assertTrue(message.contains("and 15 more"));
	}

	@Test
	void testPerformProjectionForChunk_PartitionNameInErrorMessage() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, "partition777", parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		assertTrue(exception.getMessage().contains("partition777"));
	}

	@Test
	void testPerformProjectionForChunk_ExceptionTypeInErrorMessage() {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertTrue(message.contains("Exception type:") || message.contains("ProjectionRequestValidationException"));
	}

	@Test
	void testBuildBatchProjectionId_DifferentJobExecutionIds() {
		long[] jobIds = { 1L, 100L, 9999L, Long.MAX_VALUE };

		for (long jobId : jobIds) {
			String projectionId = VdypProjectionService.buildBatchProjectionId(
					jobId, PARTITION_NAME, ProjectionRequestKind.HCSV);

			assertTrue(projectionId.contains("batch-" + jobId));
		}
	}

	@Test
	void testPerformProjectionForChunk_VerifiesOutputDirectoryIsDirectory() throws Exception {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		try {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		} catch (Exception e) {
			// Expected
		}

		Path outputDir = tempDir.resolve("output-partition0");
		assertTrue(Files.exists(outputDir));
		assertTrue(Files.isDirectory(outputDir));
	}

	@Test
	void testPerformProjectionForChunk_WithExactly5Records() {
		List<BatchRecord> batchRecords = createValidBatchRecords(5);

		IOException exception = assertThrows(IOException.class, () -> {
			vdypProjectionService.performProjectionForChunk(
					batchRecords, PARTITION_NAME, parameters, JOB_EXECUTION_ID, tempDir.toString());
		});

		String message = exception.getMessage();
		assertTrue(message.contains("5 records"));
		assertFalse(message.contains("and 0 more"));
	}

	@Test
	void testStoreDebugLog_CreatesEmptyFile() throws Exception {
		String projectionId = "test-projection-123";
		List<BatchRecord> batchRecords = createValidBatchRecords(2);

		Method storeDebugLogMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

		String expectedFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = tempDir.resolve(expectedFileName);

		assertTrue(Files.exists(debugLogPath), "Debug log file should be created");
		assertTrue(Files.isRegularFile(debugLogPath), "Debug log should be a regular file");
		assertEquals(0, Files.size(debugLogPath), "Debug log file should be empty");
	}

	@Test
	void testStoreDebugLog_OverwritesExistingFile() throws Exception {
		String projectionId = "test-projection-456";
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		String debugLogFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
		Path debugLogPath = tempDir.resolve(debugLogFileName);
		Files.writeString(debugLogPath, "This is existing content that should be overwritten");

		assertTrue(Files.size(debugLogPath) > 0, "File should have content before test");

		Method storeDebugLogMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

		assertTrue(Files.exists(debugLogPath), "Debug log file should exist");
		assertEquals(0, Files.size(debugLogPath), "Debug log file should be empty after overwrite");
	}

	@Test
	void testStoreDebugLog_WithDifferentProjectionIds() throws Exception {
		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String[] projectionIds = {
				"batch-1-partition0-projection-HCSV-2025_10_02_14_06_43_4933",
				"batch-99-partition5-projection-DCSV-2025_12_31_23_59_59_9999",
				"simple-id"
		};

		Method storeDebugLogMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		for (String projectionId : projectionIds) {
			storeDebugLogMethod.invoke(vdypProjectionService, tempDir, projectionId, batchRecords);

			String expectedFileName = String.format("YieldTables_%s_DebugLog.txt", projectionId);
			Path debugLogPath = tempDir.resolve(expectedFileName);

			assertTrue(Files.exists(debugLogPath),
					"Debug log should be created for projection ID: " + projectionId);
			assertEquals(0, Files.size(debugLogPath), "File should be empty");
		}
	}

	@Test
	void testStoreDebugLog_IOExceptionHandling() throws Exception {
		String projectionId = "test-projection-123";
		List<BatchRecord> batchRecords = createValidBatchRecords(1);

		Path invalidParentDir = tempDir.resolve("nonexistent/parent/dir");

		assertFalse(Files.exists(invalidParentDir), "Parent directory should not exist");

		Method storeDebugLogMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeDebugLog", Path.class, String.class, List.class);
		storeDebugLogMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			storeDebugLogMethod.invoke(vdypProjectionService, invalidParentDir, projectionId, batchRecords);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException, "Expected IOException to be thrown");
		assertTrue(cause.getMessage().contains("Failed to create debug log placeholder"),
				"Exception message should contain 'Failed to create debug log placeholder'");
		assertTrue(cause.getCause() instanceof IOException, "Root cause should be an IOException");
	}

	@Test
	void testCreateOutputPartitionDir_NullJobBaseDir() throws Exception {
		Method createOutputPartitionDirMethod = VdypProjectionService.class.getDeclaredMethod(
				"createOutputPartitionDir", String.class, String.class);
		createOutputPartitionDirMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createOutputPartitionDirMethod.invoke(vdypProjectionService, "partition0", null);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException);
		assertTrue(cause.getMessage().contains("Job base directory cannot be null or empty"));
	}

	@Test
	void testCreateOutputPartitionDir_NullPartitionName() throws Exception {
		Method createOutputPartitionDirMethod = VdypProjectionService.class.getDeclaredMethod(
				"createOutputPartitionDir", String.class, String.class);
		createOutputPartitionDirMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createOutputPartitionDirMethod.invoke(vdypProjectionService, null, tempDir.toString());
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException);
		assertTrue(cause.getMessage().contains("Partition name cannot be null or empty"));
	}

	@Test
	void testCreateOutputPartitionDir_IOExceptionHandling() throws Exception {
		String partitionName = "partition0";
		String jobBaseDir = tempDir.toString();

		Path outputPartitionDir = tempDir.resolve("output-partition0");

		Files.createFile(outputPartitionDir);

		assertTrue(Files.exists(outputPartitionDir), "Output path should exist as a file");
		assertTrue(Files.isRegularFile(outputPartitionDir), "Output path should be a file, not directory");

		Method createOutputPartitionDirMethod = VdypProjectionService.class.getDeclaredMethod(
				"createOutputPartitionDir", String.class, String.class);
		createOutputPartitionDirMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createOutputPartitionDirMethod.invoke(vdypProjectionService, partitionName, jobBaseDir);
		});

		assertTrue(exception instanceof InvocationTargetException,
				"Expected InvocationTargetException, but got: " + exception.getClass().getSimpleName());
		Throwable cause = exception.getCause();
		assertNotNull(cause, "Exception cause should not be null");
		assertTrue(cause instanceof IOException,
				"Expected IOException, but got: " + (cause != null ? cause.getClass().getSimpleName() : "null"));
		assertTrue(cause.getMessage().contains("Failed to create output partition directory"),
				"Exception message should contain 'Failed to create output partition directory'");
		assertTrue(cause.getCause() instanceof IOException,
				"Root cause should be an IOException, but got: "
						+ (cause.getCause() != null ? cause.getCause().getClass().getSimpleName() : "null"));
	}

	@Test
	void testCreateCombinedInputStreamsFromChunk_EmptyRecords() throws Exception {
		Method createCombinedInputStreamsFromChunkMethod = VdypProjectionService.class.getDeclaredMethod(
				"createCombinedInputStreamsFromChunk", List.class);
		createCombinedInputStreamsFromChunkMethod.setAccessible(true);

		List<BatchRecord> emptyRecords = new ArrayList<>();

		Exception exception = assertThrows(Exception.class, () -> {
			createCombinedInputStreamsFromChunkMethod.invoke(vdypProjectionService, emptyRecords);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException);
		assertTrue(cause.getMessage().contains("Cannot create input streams from empty chunk"));
	}

	@Test
	void testCreateCombinedInputStreamsFromRawData_NoPolygonData() throws Exception {
		Method createCombinedInputStreamsFromRawDataMethod = VdypProjectionService.class
				.getDeclaredMethod("createCombinedInputStreamsFromRawData", List.class);
		createCombinedInputStreamsFromRawDataMethod.setAccessible(true);

		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader(null);
		batchRecord.setRawPolygonData(null);
		batchRecord.setLayerHeader("FEATURE_ID,LAYER");
		batchRecord.setRawLayerData(List.of("123456789,P"));
		batchRecords.add(batchRecord);

		Exception exception = assertThrows(Exception.class, () -> {
			createCombinedInputStreamsFromRawDataMethod.invoke(vdypProjectionService, batchRecords);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException);
		assertTrue(cause.getMessage().contains("Combined CSV data is empty or invalid"));
	}

	@Test
	void testCreateCombinedInputStreamsFromRawData_NoLayerData() throws Exception {
		Method createCombinedInputStreamsFromRawDataMethod = VdypProjectionService.class
				.getDeclaredMethod("createCombinedInputStreamsFromRawData", List.class);
		createCombinedInputStreamsFromRawDataMethod.setAccessible(true);

		List<BatchRecord> batchRecords = new ArrayList<>();
		BatchRecord batchRecord = new BatchRecord();
		batchRecord.setFeatureId("123456789");
		batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID");
		batchRecord.setRawPolygonData("123456789,MAP1");
		batchRecord.setLayerHeader(null);
		batchRecord.setRawLayerData(null);
		batchRecords.add(batchRecord);

		Exception exception = assertThrows(Exception.class, () -> {
			createCombinedInputStreamsFromRawDataMethod.invoke(vdypProjectionService,
					batchRecords);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IOException);
		assertTrue(cause.getMessage().contains("Combined CSV data is empty or invalid"));
	}

	@Test
	void testStoreYieldTable_NullYieldTable() throws Exception {
		Method storeYieldTableMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeYieldTable", ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable.class,
				Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		List<BatchRecord> batchRecords = createValidBatchRecords(1);
		String projectionId = "test-projection";

		assertDoesNotThrow(() -> {
			storeYieldTableMethod.invoke(vdypProjectionService, null, tempDir, projectionId, batchRecords);
		});
	}

	@Test
	void testStoreYieldTable_IOExceptionHandling() throws Exception {
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

		Method storeYieldTableMethod = VdypProjectionService.class.getDeclaredMethod(
				"storeYieldTable", YieldTable.class, Path.class, String.class, List.class);
		storeYieldTableMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			storeYieldTableMethod.invoke(vdypProjectionService, yieldTable, invalidParentDir, projectionId,
					batchRecords);
		});

		assertTrue(exception instanceof InvocationTargetException,
				"Expected InvocationTargetException, but got: " + exception.getClass().getSimpleName());
		Throwable cause = exception.getCause();
		assertNotNull(cause, "Exception cause should not be null");
		assertTrue(cause instanceof IOException,
				"Expected IOException, but got: " + (cause != null ? cause.getClass().getSimpleName() : "null"));
		assertTrue(cause.getMessage().contains("Failed to store yield table"),
				"Exception message should contain 'Failed to store yield table'");
		assertTrue(cause.getCause() instanceof IOException,
				"Root cause should be an IOException, but got: "
						+ (cause.getCause() != null ? cause.getCause().getClass().getSimpleName() : "null"));
	}

	private List<BatchRecord> createValidBatchRecords(int count) {
		List<BatchRecord> records = new ArrayList<>();
		String[] featureIds = { "100000000", "200000000", "300000000", "400000000", "500000000",
				"600000000", "700000000", "800000000", "900000000", "101000000",
				"111000000", "121000000", "131000000", "141000000", "151000000",
				"161000000", "171000000", "181000000", "191000000", "102000000" };

		for (int i = 0; i < count; i++) {
			BatchRecord batchRecord = new BatchRecord();
			batchRecord.setFeatureId(featureIds[i % featureIds.length]);
			batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT");
			batchRecord.setRawPolygonData(featureIds[i % featureIds.length] + ",MAP" + i + ",1234,DCR");
			batchRecord.setLayerHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE");
			batchRecord.setRawLayerData(List.of(featureIds[i % featureIds.length] + ",MAP" + i + ",1234,P"));
			records.add(batchRecord);
		}

		return records;
	}
}
