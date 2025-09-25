package ca.bc.gov.nrs.vdyp.batch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StreamingCsvPartitionerTest {

	@InjectMocks
	private StreamingCsvPartitioner streamingCsvPartitioner;

	@TempDir
	Path tempDir;

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
	void testPartitionCsvFiles_Success() throws IOException {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertNotNull(result);
		assertEquals(tempDir, result.getBaseOutputDir());
		assertEquals(2, result.getGridSize());
		assertEquals(4, result.getTotalFeatureIds());

		// Verify partition directories were created
		assertTrue(Files.exists(tempDir.resolve("partition0")));
		assertTrue(Files.exists(tempDir.resolve("partition1")));

		// Verify partition files were created
		assertTrue(Files.exists(tempDir.resolve("partition0").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("partition0").resolve("layers.csv")));
		assertTrue(Files.exists(tempDir.resolve("partition1").resolve("polygons.csv")));
		assertTrue(Files.exists(tempDir.resolve("partition1").resolve("layers.csv")));

		// Verify partition counts
		Map<Integer, Long> partitionCounts = result.getPartitionCounts();
		assertNotNull(partitionCounts);
		assertTrue(partitionCounts.size() > 0);
		
		// Total should match the number of unique feature IDs
		long totalCount = partitionCounts.values().stream().mapToLong(Long::longValue).sum();
		assertEquals(4, totalCount);
	}

	@Test
	void testPartitionCsvFiles_EmptyPolygonFile() {
		MockMultipartFile emptyPolygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", "".getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		IOException exception = assertThrows(
				IOException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(emptyPolygonFile, layerFile, 2, tempDir)
		);

		assertTrue(exception.getMessage().contains("Polygon CSV file is empty or has no header"));
	}

	@Test
	void testPartitionCsvFiles_EmptyLayerFile() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile emptyLayerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", "".getBytes());

		IOException exception = assertThrows(
				IOException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, emptyLayerFile, 2, tempDir)
		);

		assertTrue(exception.getMessage().contains("Layer CSV file is empty or has no header"));
	}

	@Test
	void testPartitionCsvFiles_HeaderOnlyFiles() throws IOException {
		String headerOnlyPolygon = "FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT\n";
		String headerOnlyLayer = "FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE\n";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", headerOnlyPolygon.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", headerOnlyLayer.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertNotNull(result);
		assertEquals(0, result.getTotalFeatureIds());
		assertTrue(result.getPartitionCounts().isEmpty());
	}

	@Test
	void testPartitionCsvFiles_InvalidFeatureIds() throws IOException {
		String invalidPolygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				invalid_id,082G055,1234,DCR
				,082G055,5678,DCR
				987654321,082G055,9999,DCR
				""";

		String validLayerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				987654321,082G055,9999,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", invalidPolygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", validLayerCsv.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertNotNull(result);
		// Only valid feature ID should be processed
		assertEquals(1, result.getTotalFeatureIds());
	}

	@Test
	void testPartitionCsvFiles_SinglePartition() throws IOException {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 1, tempDir);

		assertNotNull(result);
		assertEquals(1, result.getGridSize());
		assertEquals(4, result.getTotalFeatureIds());

		// Verify single partition directory was created
		assertTrue(Files.exists(tempDir.resolve("partition0")));
		assertFalse(Files.exists(tempDir.resolve("partition1")));

		// All feature IDs should be in partition 0
		Map<Integer, Long> partitionCounts = result.getPartitionCounts();
		assertEquals(1, partitionCounts.size());
		assertEquals(4L, partitionCounts.get(0).longValue());
	}

	@Test
	void testPartitionCsvFiles_NonExistentOutputDirectory() throws IOException {
		Path nonExistentDir = tempDir.resolve("non-existent");
		
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, nonExistentDir);

		assertNotNull(result);
		// Directory should be created automatically
		assertTrue(Files.exists(nonExistentDir));
		assertEquals(nonExistentDir, result.getBaseOutputDir());
	}

	@Test
	void testPartitionCsvFiles_NoCommaInFeatureId() throws IOException {
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

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", singleFieldPolygon.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", correspondingLayer.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertNotNull(result);
		assertEquals(2, result.getTotalFeatureIds());
	}

	@Test
	void testPartitionResult_GetPartitionDir() throws IOException {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 3, tempDir);

		// Test getPartitionDir method
		assertEquals(tempDir.resolve("partition0"), result.getPartitionDir(0));
		assertEquals(tempDir.resolve("partition1"), result.getPartitionDir(1));
		assertEquals(tempDir.resolve("partition2"), result.getPartitionDir(2));
	}

	@Test
	void testPartitionCsvFiles_LayerFileWithoutMatchingFeatureIds() throws IOException {
		String polygonCsvContent = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR
				""";

		String layerCsvContent = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				999999999,082G055,9999,P
				888888888,082G055,8888,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", polygonCsvContent.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsvContent.getBytes());

		StreamingCsvPartitioner.PartitionResult result = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertNotNull(result);
		assertEquals(1, result.getTotalFeatureIds()); // Only one polygon feature ID processed
		
		// Layer file should have created partition files but with only headers
		Path partition0LayerFile = tempDir.resolve("partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0LayerFile));
		
		String layerContent = Files.readString(partition0LayerFile);
		// Should only contain header since no matching feature IDs
		assertEquals("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE", layerContent.trim());
	}
}