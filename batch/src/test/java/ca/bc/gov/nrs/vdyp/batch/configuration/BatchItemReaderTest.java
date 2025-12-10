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
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;

class BatchItemReaderTest {

	@TempDir
	Path tempDir;

	private BatchItemReader reader;
	private ExecutionContext executionContext;

	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		reader = new BatchItemReader("test-partition", 123L, JOB_GUID, 2);
		executionContext = new ExecutionContext();
	}

	@Test
	void testRead_BeforeOpen() {
		BatchDataReadException exception = assertThrows(BatchDataReadException.class, () -> reader.read());
		assertTrue(exception.getMessage().contains("Reader not opened. Call open() first."));
	}

	@Test
	void testOpen_WithEmptyJobBaseDir() {
		executionContext.putString("jobBaseDir", "");

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("jobBaseDir is empty"));
	}

	@Test
	void testOpen_WithNonExistentPartitionDir() {
		executionContext.putString("jobBaseDir", tempDir.toString());

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("Partition directory does not exist"));
	}

	@Test
	void testOpen_WithMissingPolygonFile() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);
		executionContext.putString("jobBaseDir", tempDir.toString());

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
		assertTrue(exception.getMessage().contains("Polygon file not found"));
	}

	@Test
	void testRead_WithEmptyPolygonFile() throws IOException, BatchDataReadException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);
		Files.createFile(partitionDir.resolve("polygons.csv")); // Empty file
		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchChunkMetadata chunkMetadata = reader.read();
		assertNull(chunkMetadata, "Empty polygon file should return null on read");

		reader.close();
	}

	@Test
	void testRead_WithMultipleChunks() throws IOException, BatchDataReadException {
		setupValidTestFiles();

		reader.open(executionContext);

		// Read first chunk (chunk size is 2, so should get 2 records)
		BatchChunkMetadata chunk1 = reader.read();
		assertNotNull(chunk1);
		assertEquals("test-partition", chunk1.getPartitionName());
		assertEquals(tempDir.toString(), chunk1.getJobBaseDir());
		assertEquals(0, chunk1.getStartIndex());
		assertEquals(2, chunk1.getRecordCount());

		// Read second chunk (remaining 1 record)
		BatchChunkMetadata chunk2 = reader.read();
		assertNotNull(chunk2);
		assertEquals("test-partition", chunk2.getPartitionName());
		assertEquals(2, chunk2.getStartIndex());
		assertEquals(1, chunk2.getRecordCount());

		// No more chunks
		BatchChunkMetadata chunk3 = reader.read();
		assertNull(chunk3);

		reader.close();
	}

	@Test
	void testRead_WithNoLayerFile() throws IOException, BatchDataReadException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file only
		String polygonContent = "FEATURE_ID,DATA\n123,data1\n456,data2\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchChunkMetadata chunkMetadata = reader.read();
		assertNotNull(chunkMetadata);
		assertEquals("test-partition", chunkMetadata.getPartitionName());
		assertEquals(0, chunkMetadata.getStartIndex());
		assertEquals(2, chunkMetadata.getRecordCount());

		reader.close();
	}

	@Test
	void testRead_WithLayerFileButNoHeader() throws IOException, BatchDataReadException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		// Create polygon file
		String polygonContent = "FEATURE_ID,DATA\n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		// Create empty layer file (no header)
		Files.createFile(partitionDir.resolve("layers.csv"));

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchChunkMetadata chunkMetadata = reader.read();
		assertNotNull(chunkMetadata);
		assertEquals(0, chunkMetadata.getStartIndex());
		assertEquals(1, chunkMetadata.getRecordCount());

		reader.close();
	}

	@Test
	void testClose_MultipleTimesDoesNotThrow() throws IOException, BatchDataReadException {
		setupValidTestFiles();
		reader.open(executionContext);

		BatchChunkMetadata chunkMetadata = reader.read();
		assertNotNull(chunkMetadata);

		assertDoesNotThrow(() -> reader.close());
		assertDoesNotThrow(() -> reader.close());

		BatchDataReadException exception = assertThrows(BatchDataReadException.class, () -> reader.read());
		assertTrue(exception.getMessage().contains("Reader not opened. Call open() first."));
	}

	@Test
	void testRead_WithEmptyLinesSkipped() throws IOException, BatchDataReadException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		String polygonContent = "FEATURE_ID,DATA\n\n   \n123,data1\n";
		Files.write(partitionDir.resolve("polygons.csv"), polygonContent.getBytes());

		executionContext.putString("jobBaseDir", tempDir.toString());

		reader.open(executionContext);

		BatchChunkMetadata chunkMetadata = reader.read();
		assertNotNull(chunkMetadata);
		assertEquals(1, chunkMetadata.getRecordCount()); // Empty lines are skipped

		reader.close();
	}

	@Test
	void testRead_AfterOpenFailure() throws IOException {
		Path partitionDir = tempDir.resolve("input-test-partition");
		Files.createDirectories(partitionDir);

		executionContext.putString("jobBaseDir", tempDir.toString());

		assertThrows(ItemStreamException.class, () -> reader.open(executionContext));

		assertThrows(BatchDataReadException.class, () -> reader.read());
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
