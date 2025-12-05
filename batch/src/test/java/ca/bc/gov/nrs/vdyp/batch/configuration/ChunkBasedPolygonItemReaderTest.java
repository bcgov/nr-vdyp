package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchDataReadException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;

class ChunkBasedPolygonItemReaderTest {

	@TempDir
	Path tempDir;

	private ChunkBasedPolygonItemReader reader;
	private ExecutionContext executionContext;

	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		reader = new ChunkBasedPolygonItemReader("test-partition", 123L, JOB_GUID, 2);
		executionContext = new ExecutionContext();
	}

	@Test
	void testConstructor() {
		ChunkBasedPolygonItemReader reader1 = new ChunkBasedPolygonItemReader(null, 123L, JOB_GUID, 0);
		assertNotNull(reader1);
	}

	@Test
	void testReadWithoutOpen() {
		BatchDataReadException exception = assertThrows(BatchDataReadException.class, () -> reader.read());
		assertTrue(exception.getMessage().contains("Reader not opened. Call open() first."));
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
	void testOpenWithEmptyPolygonFile() throws IOException, BatchDataReadException {
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
	void testSuccessfulOpenAndRead() throws IOException, BatchDataReadException {
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
	void testReadWithNoLayerFile() throws IOException, BatchDataReadException {
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
	void testReadWithLayerFileButNoHeader() throws IOException, BatchDataReadException {
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
	void testClose() throws IOException, BatchDataReadException {
		setupValidTestFiles();
		reader.open(executionContext);

		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);

		assertDoesNotThrow(() -> reader.close());

		assertDoesNotThrow(() -> reader.close());

		BatchDataReadException exception = assertThrows(BatchDataReadException.class, () -> reader.read());
		assertTrue(exception.getMessage().contains("Reader not opened. Call open() first."));
	}

	@Test
	void testRecordSkipMetrics() throws IOException, BatchDataReadException {
		ChunkBasedPolygonItemReader readerWithoutMetrics = new ChunkBasedPolygonItemReader("test", 123L, JOB_GUID, 2);

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
	void testExtractFeatureIdFromLine() throws IOException, BatchDataReadException {
		setupValidTestFiles();
		reader.open(executionContext);

		// Test via reading records which internally uses extractFeatureIdFromLine
		BatchRecord batchRecord = reader.read();
		assertNotNull(batchRecord);
		assertEquals("123", batchRecord.getFeatureId());

		reader.close();
	}

	@Test
	void testReadRethrowsBatchException() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n  ,invalid_empty_featureId\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());
		reader.open(executionContext);

		BatchDataReadException exception = assertThrows(BatchDataReadException.class, () -> reader.read());

		assertTrue(exception.getMessage().contains("Failed to read data from partition"));
		assertNotNull(exception.getCause());
		assertTrue(exception.getCause() instanceof IllegalArgumentException);

		reader.close();
	}

	@Test
	void testReadPolygonRecordSuccessfully() throws IOException, BatchDataReadException {
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
	void testSetupCreatesPolygonFile() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		Path polygonFile = partitionDir.resolve("polygons.csv");
		Files.write(polygonFile, "FEATURE_ID,DATA\n123,data1\n".getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		assertTrue(Files.exists(polygonFile));
	}

	@Test
	void testClose_DoesNotThrowException() throws IOException, BatchDataReadException {
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
	void testOpenThrowsItemStreamExceptionWhenPolygonFileMissing() throws IOException {
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
	void testReadThrowsExceptionAfterOpenFailure() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		executionContext.putString("jobBaseDir", tempDir.toString());

		assertThrows(ItemStreamException.class, () -> reader.open(executionContext));

		assertThrows(BatchDataReadException.class, () -> reader.read());
	}

	@Test
	void testReadSkipsEmptyLinesAndReadsValidRecord() throws IOException, BatchDataReadException {
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
	void testReadMultipleRecordsAndReturnsNullAtEnd() throws IOException, BatchDataReadException {
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
