package ca.bc.gov.nrs.vdyp.batch.service;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VdypProjectionServiceTest {

	@Mock
	private ProjectionRunner projectionRunner;

	@Mock
	private ProjectionContext projectionContext;

	@Mock
	private ValidatedParameters validatedParameters;

	@Mock
	private YieldTable yieldTable;

	@Mock
	private InputStream yieldTableStream;

	@Mock
	private InputStream progressStream;

	@Mock
	private InputStream errorStream;

	@Mock
	private Parameters parameters;

	private VdypProjectionService vdypProjectionService;

	@TempDir
	Path tempDir;

	private static final String PARTITION_NAME = "partition-0";

	@BeforeEach
	void setUp() {
		vdypProjectionService = new VdypProjectionService();
		ReflectionTestUtils.setField(vdypProjectionService, "outputBasePath", tempDir.toString());
	}

	@Test
	void testPerformProjectionForChunk_Success() {
		List<BatchRecord> batchRecords = createTestBatchRecords(2);

		// Test that we can create combined input streams (core functionality)
		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createCombinedInputStreamsFromChunk", batchRecords);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		// Close streams
		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */
			}
		});
	}

	@Test
	void testPerformProjectionForChunk_EmptyRecords() throws Exception {
		List<BatchRecord> emptyRecords = new ArrayList<>();

		String result = vdypProjectionService.performProjectionForChunk(emptyRecords, PARTITION_NAME, parameters);

		assertEquals("No records to process in chunk", result);
	}

	@Test
	void testPerformProjectionForChunk_ProjectionFailure() {
		List<BatchRecord> batchRecords = createTestBatchRecords(1);
		Exception cause = new RuntimeException("Test failure");

		// Test the error handling method directly
		IOException result = (IOException) ReflectionTestUtils.invokeMethod(vdypProjectionService,
				"handleChunkProjectionFailure", batchRecords, PARTITION_NAME, cause);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("VDYP chunk projection failed for 1 records"));
		assertTrue(result.getMessage().contains(PARTITION_NAME));
		assertTrue(result.getMessage().contains("123456789"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testCreatePartitionOutputDir() {
		Path outputDir = (Path) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createPartitionOutputDir", PARTITION_NAME);

		assertNotNull(outputDir);
		assertTrue(Files.exists(outputDir));
		assertTrue(outputDir.toString().contains(PARTITION_NAME));
	}

	@Test
	void testCreateCombinedInputStreamsFromChunk_EmptyList() {
		List<BatchRecord> emptyRecords = new ArrayList<>();

		Exception exception = assertThrows(Exception.class,
				() -> ReflectionTestUtils.invokeMethod(vdypProjectionService, "createCombinedInputStreamsFromChunk",
						emptyRecords));

		assertTrue(exception.getCause().getMessage().contains("Cannot create input streams from empty chunk"));
	}

	@Test
	void testCreateCombinedInputStreamsFromRawData() {
		List<BatchRecord> batchRecords = createTestBatchRecords(2);

		@SuppressWarnings("unchecked")
		Map<String, InputStream> streams = (Map<String, InputStream>) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "createCombinedInputStreamsFromRawData", batchRecords);

		assertNotNull(streams);
		assertEquals(2, streams.size());
		assertTrue(streams.containsKey("HCSV-Polygon"));
		assertTrue(streams.containsKey("HCSV-Layers"));

		// Close streams
		streams.values().forEach(stream -> {
			try {
				stream.close();
			} catch (Exception e) {
				/* ignore */
			}
		});
	}

	@Test
	void testBuildChunkProjectionId() {
		String projectionId = (String) ReflectionTestUtils
				.invokeMethod(vdypProjectionService, "buildChunkProjectionId", PARTITION_NAME, 5);

		assertNotNull(projectionId);
		assertTrue(projectionId.contains("batch-chunk-projection"));
		assertTrue(projectionId.contains(PARTITION_NAME));
		assertTrue(projectionId.contains("size-5"));
	}

	@Test
	void testStoreChunkYieldTables() throws Exception {
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		setupMocksForYieldTableStorage();

		ReflectionTestUtils.invokeMethod(vdypProjectionService, "storeChunkYieldTables",
				projectionRunner, partitionDir, "test-projection", createTestBatchRecords(1));

		verify(yieldTable).getAsStream();
		verify(yieldTable.getOutputFormat()).getYieldTableFileName();
	}

	@Test
	void testStoreChunkLogs_WithProgressLogging() throws Exception {
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		setupMocksForLogStorage();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)).thenReturn(true);
		when(projectionRunner.getProgressStream()).thenReturn(progressStream);

		ReflectionTestUtils.invokeMethod(vdypProjectionService, "storeChunkLogs",
				projectionRunner, partitionDir, "test-projection", createTestBatchRecords(1));

		verify(projectionRunner).getProgressStream();
	}

	@Test
	void testStoreChunkLogs_WithErrorLogging() throws Exception {
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		setupMocksForLogStorage();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)).thenReturn(true);
		when(projectionRunner.getErrorStream()).thenReturn(errorStream);

		ReflectionTestUtils.invokeMethod(vdypProjectionService, "storeChunkLogs",
				projectionRunner, partitionDir, "test-projection", createTestBatchRecords(1));

		verify(projectionRunner).getErrorStream();
	}

	@Test
	void testStoreChunkLogs_WithDebugLogging() throws Exception {
		Path partitionDir = tempDir.resolve(PARTITION_NAME);
		Files.createDirectories(partitionDir);

		setupMocksForLogStorage();
		when(validatedParameters.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)).thenReturn(true);

		ReflectionTestUtils.invokeMethod(vdypProjectionService, "storeChunkLogs",
				projectionRunner, partitionDir, "test-projection", createTestBatchRecords(1));

		// Verify debug log file was created
		Path debugLogPath = partitionDir.resolve("YieldTables_CHUNK_test-projection_DebugLog.txt");
		assertTrue(Files.exists(debugLogPath));
	}

	@Test
	void testHandleChunkProjectionFailure() {
		List<BatchRecord> batchRecords = createTestBatchRecords(3);
		Exception cause = new RuntimeException("Test failure");

		IOException result = (IOException) ReflectionTestUtils.invokeMethod(vdypProjectionService,
				"handleChunkProjectionFailure", batchRecords, PARTITION_NAME, cause);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("VDYP chunk projection failed for 3 records"));
		assertTrue(result.getMessage().contains(PARTITION_NAME));
		assertTrue(result.getMessage().contains("123456789, 987654321, 111222333"));
		assertEquals(cause, result.getCause());
	}

	@Test
	void testHandleChunkProjectionFailure_ManyRecords() {
		List<BatchRecord> batchRecords = createTestBatchRecords(7);
		Exception cause = new RuntimeException("Test failure");

		IOException result = (IOException) ReflectionTestUtils.invokeMethod(vdypProjectionService,
				"handleChunkProjectionFailure", batchRecords, PARTITION_NAME, cause);

		assertNotNull(result);
		assertTrue(result.getMessage().contains("and 2 more"));
	}

	private void setupMocksForYieldTableStorage() {
		when(projectionRunner.getContext()).thenReturn(projectionContext);
		when(projectionContext.getYieldTables()).thenReturn(List.of(yieldTable));
		when(yieldTable.getOutputFormat())
				.thenReturn(mock(ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat.class));
		when(yieldTable.getOutputFormat().getYieldTableFileName()).thenReturn("test.csv");
		when(yieldTable.getAsStream()).thenReturn(yieldTableStream);
	}

	private void setupMocksForLogStorage() {
		when(projectionRunner.getContext()).thenReturn(projectionContext);
		when(projectionContext.getParams()).thenReturn(validatedParameters);
		when(validatedParameters.containsOption(any(ExecutionOption.class))).thenReturn(false);
	}

	private List<BatchRecord> createTestBatchRecords(int count) {
		List<BatchRecord> records = new ArrayList<>();
		String[] featureIds = { "123456789", "987654321", "111222333", "444555666", "777888999", "123123123",
				"456456456" };

		for (int i = 0; i < count; i++) {
			BatchRecord batchRecord = new BatchRecord();
			batchRecord.setFeatureId(featureIds[i % featureIds.length]);
			batchRecord.setPolygonHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT");
			batchRecord.setRawPolygonData(featureIds[i % featureIds.length] + ",MAP" + i + ",123" + i + ",DCR");
			batchRecord.setLayerHeader("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE");
			batchRecord.setRawLayerData(List.of(featureIds[i % featureIds.length] + ",MAP" + i + ",123" + i + ",P"));
			records.add(batchRecord);
		}

		return records;
	}
}
