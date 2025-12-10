package ca.bc.gov.nrs.vdyp.batch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BatchInputPartitionerTest {

	@InjectMocks
	private BatchInputPartitioner batchInputPartitioner;

	@TempDir
	Path tempDir;

	private static final String TEST_JOB_GUID = "test-job-guid-12345";

	private static final String POLYGON_CSV_CONTENT = """
			FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
			123456789,082G055,1234,DCR
			987654321,082G055,5678,DCR
			111222333,082G055,9999,DCR
			444555666,082G055,1111,DCR
			""";

	private static final String LAYER_CSV_CONTENT = """
			FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
			123456789,082G055,1234,P
			987654321,082G055,5678,P
			111222333,082G055,9999,P
			444555666,082G055,1111,P
			123456789,082G055,1234,S
			987654321,082G055,5678,S
			""";

	@BeforeEach
	void setUp() {
		// Test setup is handled by @TempDir and @InjectMocks
	}

	@Test
	void testPartitionCsvFiles_Success() throws BatchPartitionException {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(4, totalFeatureIds);

		// Verify partition directories were created
		assertTrue(Files.exists(tempDir.resolve("input-partition0")));
		assertTrue(Files.exists(tempDir.resolve("input-partition1")));

		// Verify partition files were created
		assertTrue(Files.exists(tempDir.resolve("input-partition0").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition0").resolve("layers.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition1").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition1").resolve("layers.csv")));
	}

	@Test
	void testPartitionCsvFiles_SequentialChunking_TwoPartitions() throws BatchPartitionException, IOException {
		// Test data with 5 FEATURE_IDs and 2 partitions
		// Balanced distribution: chunkSize=2, remainder=1
		// Expected: partition0 gets 3 records (2+1), partition1 gets 2 records
		String polygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				15724968,082G055,1234,DCR
				15724970,082G055,5678,DCR
				15724973,082G055,9999,DCR
				15725009,082G055,1111,DCR
				15725037,082G055,2222,DCR
				""";

		String layerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				15724968,082G055,1234,P
				15724970,082G055,5678,P
				15724973,082G055,9999,P
				15725009,082G055,1111,P
				15725037,082G055,2222,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonCsv.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(5, totalFeatureIds);

		// Verify balanced distribution: partition0 gets 3 records (first partition gets +1 from remainder)
		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		String partition0Content = Files.readString(partition0Polygon);
		assertTrue(partition0Content.contains("15724968"), "Partition 0 should contain first FEATURE_ID");
		assertTrue(partition0Content.contains("15724970"), "Partition 0 should contain second FEATURE_ID");
		assertTrue(partition0Content.contains("15724973"), "Partition 0 should contain third FEATURE_ID");
		assertFalse(partition0Content.contains("15725009"), "Partition 0 should NOT contain fourth FEATURE_ID");

		// Partition 1 gets remaining 2 records
		Path partition1Polygon = tempDir.resolve("input-partition1").resolve("polygons.csv");
		String partition1Content = Files.readString(partition1Polygon);
		assertTrue(partition1Content.contains("15725009"), "Partition 1 should contain fourth FEATURE_ID");
		assertTrue(partition1Content.contains("15725037"), "Partition 1 should contain fifth FEATURE_ID");
		assertFalse(partition1Content.contains("15724973"), "Partition 1 should NOT contain third FEATURE_ID");

		// Verify layers follow the same partitioning
		Path partition0Layer = tempDir.resolve("input-partition0").resolve("layers.csv");
		String partition0LayerContent = Files.readString(partition0Layer);
		long partition0LayerCount = partition0LayerContent.lines().filter(line -> !line.startsWith("FEATURE_ID"))
				.count();
		assertEquals(3, partition0LayerCount, "Partition 0 should have 3 layer records");

		Path partition1Layer = tempDir.resolve("input-partition1").resolve("layers.csv");
		String partition1LayerContent = Files.readString(partition1Layer);
		long partition1LayerCount = partition1LayerContent.lines().filter(line -> !line.startsWith("FEATURE_ID"))
				.count();
		assertEquals(2, partition1LayerCount, "Partition 1 should have 2 layer records");
	}

	@Test
	void testPartitionCsvFiles_EmptyPolygonFile() {
		MockMultipartFile emptyPolygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", "".getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(emptyPolygonFile, layerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(exception.getMessage().contains("no valid FEATURE_IDs"));
	}

	@Test
	void testPartitionCsvFiles_EmptyLayerFile() throws BatchPartitionException {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile emptyLayerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "".getBytes());

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, emptyLayerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(4, totalFeatureIds);
	}

	@Test
	void testPartitionCsvFiles_HeaderOnlyFiles() {
		String headerOnlyPolygon = "FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT\n";
		String headerOnlyLayer = "FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE\n";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", headerOnlyPolygon.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", headerOnlyLayer.getBytes()
		);

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(exception.getMessage().contains("no valid FEATURE_IDs"));
	}

	@Test
	void testPartitionCsvFiles_SinglePartition() throws BatchPartitionException {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 1, tempDir, TEST_JOB_GUID);

		assertEquals(4, totalFeatureIds);

		// Verify single partition directory was created
		assertTrue(Files.exists(tempDir.resolve("input-partition0")));
		assertFalse(Files.exists(tempDir.resolve("input-partition1")));
	}

	@Test
	void testPartitionCsvFiles_NonExistentOutputDirectory() throws BatchPartitionException {
		Path nonExistentDir = tempDir.resolve("non-existent");

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, nonExistentDir, TEST_JOB_GUID);

		assertEquals(4, totalFeatureIds);
		// Directory should be created automatically
		assertTrue(Files.exists(nonExistentDir));
	}

	@Test
	void testPartitionCsvFiles_NoCommaInFeatureId() throws BatchPartitionException {
		String singleFieldPolygon = """
				FEATURE_ID
				123456789
				987654321
				""";

		String correspondingLayer = """
				FEATURE_ID
				123456789
				987654321
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", singleFieldPolygon.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", correspondingLayer.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(2, totalFeatureIds);
	}

	@Test
	void testPartitionCsvFiles_WithNonMatchingLayerFeatureIds() throws BatchPartitionException, IOException {
		String polygonCsvContent = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR
				""";

		String layerCsvContent = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				999999999,082G055,9999,P
				888888888,082G055,8888,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonCsvContent.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", layerCsvContent.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(1, totalFeatureIds); // Only one polygon feature ID processed

		// Layer file should have created partition files but with only headers
		Path partition0LayerFile = tempDir.resolve("input-partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0LayerFile));

		String layerContent = Files.readString(partition0LayerFile);
		// Should only contain header since no matching feature IDs
		assertEquals("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE", layerContent.trim());
	}

	@Test
	void testPartitionCsvFiles_WithEmptyLinesInFiles() throws BatchPartitionException, IOException {
		String polygonWithEmptyLines = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR

				987654321,082G055,5678,DCR

				111222333,082G055,9999,DCR
				""";

		String layerWithEmptyLines = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				123456789,082G055,1234,P

				987654321,082G055,5678,P

				111222333,082G055,9999,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonWithEmptyLines.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", layerWithEmptyLines.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		// Empty lines should be skipped
		assertEquals(3, totalFeatureIds);

		// Verify layer files were created and empty lines were skipped
		Path partition0LayerFile = tempDir.resolve("input-partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0LayerFile));

		String content = Files.readString(partition0LayerFile);
		// Should contain header and two valid lines (empty lines skipped)
		long lineCount = content.lines().count();
		assertEquals(3, lineCount); // header + 2 data lines
	}

	@Test
	void testCountTotalFeatureIds_IOException() {
		MockMultipartFile faultyPolygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		) {
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("Simulated I/O error while reading polygon file");
			}
		};

		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(faultyPolygonFile, layerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(
				exception.getMessage().contains("Failed to read polygon file while counting FEATURE_IDs"),
				"Exception message should indicate failure during counting"
		);
		assertNotNull(exception.getCause(), "Exception should have a cause");
		assertTrue(exception.getCause() instanceof IOException, "Cause should be IOException");
	}

	@Test
	void testPartitionPolygonFile_IOException() {
		MockMultipartFile faultyPolygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		) {
			private int callCount = 0;

			@Override
			public java.io.InputStream getInputStream() throws IOException {
				callCount++;
				// First call succeeds (for counting), second call fails (for partitioning)
				if (callCount > 1) {
					throw new IOException("Simulated I/O error during polygon file partitioning");
				}
				return super.getInputStream();
			}
		};

		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(faultyPolygonFile, layerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(
				exception.getMessage().contains("Failed to partition polygon file"),
				"Exception message should indicate failure during polygon partitioning"
		);
		assertNotNull(exception.getCause(), "Exception should have a cause");
		assertTrue(exception.getCause() instanceof IOException, "Cause should be IOException");
	}

	@Test
	void testPartitionLayerFile_IOException() {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);

		MockMultipartFile faultyLayerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		) {
			@Override
			public java.io.InputStream getInputStream() throws IOException {
				throw new IOException("Simulated I/O error while reading layer file");
			}
		};

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(polygonFile, faultyLayerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(
				exception.getMessage().contains("Failed to partition layer file"),
				"Exception message should indicate failure during layer partitioning"
		);
		assertNotNull(exception.getCause(), "Exception should have a cause");
		assertTrue(exception.getCause() instanceof IOException, "Cause should be IOException");
	}

	@Test
	void testPartitionCsvFiles_PolygonFileWithoutHeader() throws BatchPartitionException, IOException {
		String polygonNoHeader = """
				123456789,082G055,1234,DCR
				987654321,082G055,5678,DCR
				111222333,082G055,9999,DCR
				""";

		String layerWithHeader = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				123456789,082G055,1234,P
				987654321,082G055,5678,P
				111222333,082G055,9999,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonNoHeader.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", layerWithHeader.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(3, totalFeatureIds);

		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		assertTrue(Files.exists(partition0Polygon));

		String partition0Content = Files.readString(partition0Polygon);
		assertTrue(partition0Content.contains("123456789"), "First data line should be in partition 0");
	}

	@Test
	void testPartitionCsvFiles_LayerFileWithoutHeader() throws BatchPartitionException, IOException {
		String polygonWithHeader = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR
				987654321,082G055,5678,DCR
				111222333,082G055,9999,DCR
				""";

		String layerNoHeader = """
				123456789,082G055,1234,P
				987654321,082G055,5678,P
				111222333,082G055,9999,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonWithHeader.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", layerNoHeader.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(3, totalFeatureIds);

		Path partition0Layer = tempDir.resolve("input-partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0Layer));

		String partition0Content = Files.readString(partition0Layer);
		assertTrue(partition0Content.contains("123456789"), "First data line should be in partition 0");
	}

	@Test
	void testPartitionCsvFiles_BothFilesWithoutHeaders() throws BatchPartitionException, IOException {
		String polygonNoHeader = """
				123456789,082G055,1234,DCR
				987654321,082G055,5678,DCR
				""";

		String layerNoHeader = """
				123456789,082G055,1234,P
				987654321,082G055,5678,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", polygonNoHeader.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", layerNoHeader.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(2, totalFeatureIds);

		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		Path partition0Layer = tempDir.resolve("input-partition0").resolve("layers.csv");

		assertTrue(Files.exists(partition0Polygon));
		assertTrue(Files.exists(partition0Layer));

		String polygonContent = Files.readString(partition0Polygon);
		String layerContent = Files.readString(partition0Layer);

		assertTrue(polygonContent.contains("123456789"), "First polygon data line should be processed");
		assertTrue(layerContent.contains("123456789"), "First layer data line should be processed");
	}

	@Test
	void testCreatePartitionWriters_IOException() throws IOException {
		Path conflictingFile = tempDir.resolve("input-partition0");
		Files.createFile(conflictingFile);

		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		BatchPartitionException exception = assertThrows(
				BatchPartitionException.class,
				() -> batchInputPartitioner.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID)
		);

		assertTrue(
				exception.getMessage().contains("Failed to create partition writers"),
				"Exception message should indicate failure creating partition writers"
		);
		assertNotNull(exception.getCause(), "Exception should have a cause");
	}

	@Test
	void testCloseWriters_ExceptionHandling() throws BatchPartitionException, IOException {
		MockMultipartFile polygonFile = new MockMultipartFile(
				"polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes()
		);
		MockMultipartFile layerFile = new MockMultipartFile(
				"layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes()
		);

		int totalFeatureIds = batchInputPartitioner
				.partitionCsvFiles(polygonFile, layerFile, 2, tempDir, TEST_JOB_GUID);

		assertEquals(4, totalFeatureIds);

		assertTrue(Files.exists(tempDir.resolve("input-partition0").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition0").resolve("layers.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition1").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("input-partition1").resolve("layers.csv")));

		String content = Files.readString(tempDir.resolve("input-partition0").resolve("polygons.csv"));
		assertNotNull(content);
		assertFalse(content.isEmpty());
	}
}
