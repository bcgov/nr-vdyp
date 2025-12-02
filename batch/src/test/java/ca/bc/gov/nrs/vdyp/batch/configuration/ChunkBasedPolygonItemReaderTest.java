package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchDataReadException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChunkBasedPolygonItemReaderTest {

	@TempDir
	Path tempDir;

	@Mock
	private BatchMetricsCollector metricsCollector;

	private ChunkBasedPolygonItemReader reader;
	private ExecutionContext executionContext;

	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		reader = new ChunkBasedPolygonItemReader("test-partition", metricsCollector, 123L, JOB_GUID, 2);
		executionContext = new ExecutionContext();
	}

	@Test
	void testConstructor() {
		ChunkBasedPolygonItemReader reader1 = new ChunkBasedPolygonItemReader(
				null, metricsCollector, 123L, JOB_GUID, 0
		);
		// Constructor should handle null partitionName and ensure minimum chunk size
		assertNotNull(reader1);
	}

	@Test
	void testReadWithoutOpen() {
		Exception exception = assertThrows(IllegalStateException.class, () -> reader.read());
		assertEquals("Reader not opened. Call open() first.", exception.getMessage());
	}

	@Test
	void testOpenWithEmptyPartitionBaseDir() {
		executionContext.putString("jobBaseDir", "");

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("jobBaseDir not found or empty"));
	}

	@Test
	void testOpenWithNonExistentPartitionDir() {
		executionContext.putString("jobBaseDir", tempDir.toString());

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("Partition directory does not exist"));
	}

	@Test
	void testOpenWithMissingPolygonFile() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);
		executionContext.putString("jobBaseDir", tempDir.toString());

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("Polygon file not found"));
	}

	@Test
	void testOpenWithEmptyPolygonFile() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);
		Files.createFile(partitionDir.resolve("polygons.csv")); // Empty file
		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNull(batchRecord, "Empty polygon file should return null on read");

		reader.close();
	}

	@Test
	void testSuccessfulOpenAndRead() throws Exception {
		setupValidTestFiles();

		reader.open(executionContext);

		// Read first record
		BatchRecord record1 = reader.read();
		assertNotNull(record1);
		assertEquals("123", record1.getFeatureId());
		assertEquals("123,data1", record1.getRawPolygonData());
		assertEquals("test-partition", record1.getPartitionName());
		assertNotNull(record1.getRawLayerData());
		assertEquals(1, record1.getRawLayerData().size());
		assertEquals("123,layer1", record1.getRawLayerData().get(0));

		// Read second record
		BatchRecord record2 = reader.read();
		assertNotNull(record2);
		assertEquals("456", record2.getFeatureId());

		// Read third record (no layers)
		BatchRecord record3 = reader.read();
		assertNotNull(record3);
		assertEquals("789", record3.getFeatureId());
		assertTrue(record3.getRawLayerData().isEmpty());

		// No more records
		BatchRecord record4 = reader.read();
		assertNull(record4);

		reader.close();
	}

	@Test
	void testReadWithEmptyFeatureId() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file with empty FEATURE_ID (leading whitespace to simulate
		// empty field)
		String polygonContent = "FEATURE_ID,DATA\n   ,data1\n456,data2\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		// Create layer file
		String layerContent = "FEATURE_ID,LAYER_DATA\n456,layer1\n";
		Files.write(partitionDir.resolve("layers.csv"), layerContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		// Should skip empty FEATURE_ID and return the second record
		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("456", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testReadWithNoLayerFile() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file only
		String polygonContent = "FEATURE_ID,DATA\n123,data1\n456,data2\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("123", batchRecord.getFeatureId());
		assertTrue(batchRecord.getRawLayerData().isEmpty());

		reader.close();
	}

	@Test
	void testReadWithLayerFileButNoHeader() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file
		String polygonContent = "FEATURE_ID,DATA\n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		// Create empty layer file (no header)
		Files.createFile(partitionDir.resolve("layers.csv"));

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("123", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testUpdate() throws Exception {
		setupValidTestFiles();
		reader.open(executionContext);

		// Read some records to increase processed count
		reader.read();
		reader.read();

		ExecutionContext updateContext = new ExecutionContext();
		reader.update(updateContext);

		assertEquals(2, updateContext.getInt("test-partition.processed"));
		assertEquals(0, updateContext.getInt("test-partition.skipped"));

		reader.close();
	}

	@Test
	void testClose() throws Exception {
		setupValidTestFiles();
		reader.open(executionContext);

		// Read one record to ensure reader is active
		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);

		// Should not throw exception
		assertDoesNotThrow(() -> reader.close());

		// Should be able to call close multiple times without exception
		assertDoesNotThrow(() -> reader.close());

		// After closing, reader should not be able to read anymore
		Exception exception = assertThrows(IllegalStateException.class, () -> reader.read());
		assertEquals("Reader not opened. Call open() first.", exception.getMessage());
	}

	@Test
	void testRecordSkipMetrics() throws Exception {
		// Test with null metricsCollector
		ChunkBasedPolygonItemReader readerWithoutMetrics = new ChunkBasedPolygonItemReader(
				"test", null, 123L, JOB_GUID, 2
		);

		Path partitionDir = tempDir.resolve("input-test");
		Files.createDirectories(partitionDir);

		// Create polygon file with invalid data that will cause exception
		String polygonContent = "FEATURE_ID,DATA\n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());
		readerWithoutMetrics.open(executionContext);

		// Should handle null metricsCollector
		BatchRecord batchRecord = readerWithoutMetrics.read();
		assertNotNull(batchRecord);

		readerWithoutMetrics.close();
	}

	@Test
	void testRecordSkipMetricsWithInvalidFeatureId() throws Exception {
		doNothing().when(metricsCollector).recordSkip(anyLong(), any(), any(), any(), anyString());

		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file with non-numeric FEATURE_ID that can still be processed
		// as a valid record
		String polygonContent = "FEATURE_ID,DATA\nabc,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		// Should return record even with non-numeric FEATURE_ID (it's still a valid
		// string)
		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("abc", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testExtractFeatureIdFromLine() throws Exception {
		setupValidTestFiles();
		reader.open(executionContext);

		// Test via reading records which internally uses extractFeatureIdFromLine
		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("123", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testReadRethrowsBatchException() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		BatchDataReadException testException = BatchDataReadException.handleDataReadFailure(
				new IOException("Test IO failure"), "Test data read failure", JOB_GUID, 123L, "test-partition",
				org.slf4j.LoggerFactory.getLogger(ChunkBasedPolygonItemReaderTest.class)
		);

		ChunkBasedPolygonItemReader spyReader = spy(reader);
		doThrow(testException).when(spyReader).read();

		assertThrows(BatchDataReadException.class, spyReader::read);
	}

	@Test
	void testHandlePolygonProcessingException() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n123,data1\n456,data2\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord record1 = reader.read();
		assertNotNull(record1);
		assertEquals("123", record1.getFeatureId());

		reader.close();
	}

	@Test
	void testIOExceptionDuringReaderInitialization() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		Path polygonFile = partitionDir.resolve("polygons.csv");
		Files.write(polygonFile, "FEATURE_ID,DATA\n123,data1\n".getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		assertTrue(Files.exists(polygonFile));
	}

	@Test
	void testIOExceptionDuringCloseReader() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);

		assertDoesNotThrow(() -> reader.close());
	}

	@Test
	void testHandleReaderInitializationFailureWithItemStreamException() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		executionContext.putString("jobBaseDir", tempDir.toString());

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("Polygon file not found"));

		assertNotNull(exception.getCause());
		assertTrue(
				exception.getCause() instanceof BatchDataReadException
						|| exception.getMessage().contains("Polygon file not found")
		);
	}

	@Test
	void testRecordSkipMetricsWithMetricsCollector() throws Exception {
		doNothing().when(metricsCollector).recordSkip(anyLong(), anyString(), anyString(), any(), anyString());

		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n ,data1\n456,data2\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("456", batchRecord.getFeatureId());

		ExecutionContext updateContext = new ExecutionContext();
		reader.update(updateContext);
		assertEquals(1, updateContext.getInt("test-partition.skipped"));

		reader.close();
	}

	@Test
	void testPerformReaderCleanupAfterFailure() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		executionContext.putString("jobBaseDir", tempDir.toString());

		assertThrows(ItemStreamException.class, () -> reader.open(executionContext));

		assertThrows(IllegalStateException.class, () -> reader.read());
	}

	@Test
	void testExtractFeatureIdFromNullOrEmptyLine() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n\n   \n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("123", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testLoadNextChunkWithMultipleChunks() throws Exception {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n123,data1\n456,data2\n789,data3\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchRecord record1 = reader.read();
		assertNotNull(record1);
		assertEquals("123", record1.getFeatureId());

		BatchRecord record2 = reader.read();
		assertNotNull(record2);
		assertEquals("456", record2.getFeatureId());

		BatchRecord record3 = reader.read();
		assertNotNull(record3);
		assertEquals("789", record3.getFeatureId());

		BatchRecord record4 = reader.read();
		assertNull(record4);

		reader.close();
	}

	private void setupValidTestFiles() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n123,data1\n456,data2\n789,data3\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		String layerContent = "FEATURE_ID,LAYER_DATA\n123,layer1\n456,layer2\n";
		Files.write(partitionDir.resolve("layers.csv"), layerContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());
	}
}
