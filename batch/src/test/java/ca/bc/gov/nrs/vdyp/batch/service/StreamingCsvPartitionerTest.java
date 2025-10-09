package ca.bc.gov.nrs.vdyp.batch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

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

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

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
	void testPartitionCsvFiles_SequentialChunking_TwoPartitions() throws IOException {
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

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", polygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

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
		long partition0LayerCount = partition0LayerContent.lines().filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition0LayerCount, "Partition 0 should have 3 layer records");

		Path partition1Layer = tempDir.resolve("input-partition1").resolve("layers.csv");
		String partition1LayerContent = Files.readString(partition1Layer);
		long partition1LayerCount = partition1LayerContent.lines().filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(2, partition1LayerCount, "Partition 1 should have 2 layer records");
	}

	@Test
	void testPartitionCsvFiles_SequentialChunking_ThreePartitions() throws IOException {
		// Test data with 10 FEATURE_IDs and 3 partitions
		// Balanced distribution: chunkSize=3, remainder=1
		// Expected: partition0 gets 4 (3+1), partition1 gets 3, partition2 gets 3
		String polygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				15724968,082G055,1234,DCR
				15724970,082G055,5678,DCR
				15724973,082G055,9999,DCR
				15725009,082G055,1111,DCR
				15725037,082G055,2222,DCR
				15725102,082G055,3333,DCR
				15725195,082G055,4444,DCR
				15725199,082G055,5555,DCR
				15725218,082G055,6666,DCR
				15725219,082G055,7777,DCR
				""";

		String layerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				15724968,082G055,1234,P
				15724970,082G055,5678,P
				15724973,082G055,9999,P
				15725009,082G055,1111,P
				15725037,082G055,2222,P
				15725102,082G055,3333,P
				15725195,082G055,4444,P
				15725199,082G055,5555,P
				15725218,082G055,6666,P
				15725219,082G055,7777,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", polygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 3, tempDir);

		assertEquals(10, totalFeatureIds);

		// Verify balanced distribution: partition0 gets 4 records (first partition gets +1 from remainder)
		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		String partition0Content = Files.readString(partition0Polygon);
		assertTrue(partition0Content.contains("15724968"), "Partition 0 should contain FEATURE_ID 15724968");
		assertTrue(partition0Content.contains("15724970"), "Partition 0 should contain FEATURE_ID 15724970");
		assertTrue(partition0Content.contains("15724973"), "Partition 0 should contain FEATURE_ID 15724973");
		assertTrue(partition0Content.contains("15725009"), "Partition 0 should contain FEATURE_ID 15725009");
		assertFalse(partition0Content.contains("15725037"), "Partition 0 should NOT contain FEATURE_ID 15725037");

		// Partition 1 gets next 3 records
		Path partition1Polygon = tempDir.resolve("input-partition1").resolve("polygons.csv");
		String partition1Content = Files.readString(partition1Polygon);
		assertTrue(partition1Content.contains("15725037"), "Partition 1 should contain FEATURE_ID 15725037");
		assertTrue(partition1Content.contains("15725102"), "Partition 1 should contain FEATURE_ID 15725102");
		assertTrue(partition1Content.contains("15725195"), "Partition 1 should contain FEATURE_ID 15725195");
		assertFalse(partition1Content.contains("15725009"), "Partition 1 should NOT contain FEATURE_ID 15725009");
		assertFalse(partition1Content.contains("15725199"), "Partition 1 should NOT contain FEATURE_ID 15725199");

		// Partition 2 gets last 3 records
		Path partition2Polygon = tempDir.resolve("input-partition2").resolve("polygons.csv");
		String partition2Content = Files.readString(partition2Polygon);
		assertTrue(partition2Content.contains("15725199"), "Partition 2 should contain FEATURE_ID 15725199");
		assertTrue(partition2Content.contains("15725218"), "Partition 2 should contain FEATURE_ID 15725218");
		assertTrue(partition2Content.contains("15725219"), "Partition 2 should contain FEATURE_ID 15725219");
		assertFalse(partition2Content.contains("15725195"), "Partition 2 should NOT contain FEATURE_ID 15725195");

		// Verify layers follow the same partitioning
		Path partition0Layer = tempDir.resolve("input-partition0").resolve("layers.csv");
		long partition0LayerCount = Files.readString(partition0Layer).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition0LayerCount, "Partition 0 should have 4 layer records");

		Path partition1Layer = tempDir.resolve("input-partition1").resolve("layers.csv");
		long partition1LayerCount = Files.readString(partition1Layer).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition1LayerCount, "Partition 1 should have 3 layer records");

		Path partition2Layer = tempDir.resolve("input-partition2").resolve("layers.csv");
		long partition2LayerCount = Files.readString(partition2Layer).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition2LayerCount, "Partition 2 should have 3 layer records");
	}

	@Test
	void testPartitionCsvFiles_BalancedDistribution_Remainder2() throws IOException {
		// Test data with 11 FEATURE_IDs and 3 partitions
		// Balanced distribution: chunkSize=3, remainder=2
		// Expected: partition0 gets 4 (3+1), partition1 gets 4 (3+1), partition2 gets 3
		String polygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				100001,082G055,1234,DCR
				100002,082G055,5678,DCR
				100003,082G055,9999,DCR
				100004,082G055,1111,DCR
				100005,082G055,2222,DCR
				100006,082G055,3333,DCR
				100007,082G055,4444,DCR
				100008,082G055,5555,DCR
				100009,082G055,6666,DCR
				100010,082G055,7777,DCR
				100011,082G055,8888,DCR
				""";

		String layerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				100001,082G055,1234,P
				100002,082G055,5678,P
				100003,082G055,9999,P
				100004,082G055,1111,P
				100005,082G055,2222,P
				100006,082G055,3333,P
				100007,082G055,4444,P
				100008,082G055,5555,P
				100009,082G055,6666,P
				100010,082G055,7777,P
				100011,082G055,8888,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", polygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 3, tempDir);

		assertEquals(11, totalFeatureIds);

		// Verify balanced distribution with remainder=2
		// Partition 0 gets 4 records (first partition with +1)
		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		String partition0Content = Files.readString(partition0Polygon);
		long partition0Count = partition0Content.lines().filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition0Count, "Partition 0 should have 4 records");
		assertTrue(partition0Content.contains("100001"));
		assertTrue(partition0Content.contains("100004"));
		assertFalse(partition0Content.contains("100005"));

		// Partition 1 gets 4 records (second partition with +1)
		Path partition1Polygon = tempDir.resolve("input-partition1").resolve("polygons.csv");
		String partition1Content = Files.readString(partition1Polygon);
		long partition1Count = partition1Content.lines().filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition1Count, "Partition 1 should have 4 records");
		assertTrue(partition1Content.contains("100005"));
		assertTrue(partition1Content.contains("100008"));
		assertFalse(partition1Content.contains("100009"));

		// Partition 2 gets 3 records (no +1 bonus)
		Path partition2Polygon = tempDir.resolve("input-partition2").resolve("polygons.csv");
		String partition2Content = Files.readString(partition2Polygon);
		long partition2Count = partition2Content.lines().filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition2Count, "Partition 2 should have 3 records");
		assertTrue(partition2Content.contains("100009"));
		assertTrue(partition2Content.contains("100011"));
		assertFalse(partition2Content.contains("100008"));
	}

	@Test
	void testPartitionCsvFiles_BalancedDistribution_Remainder3() throws IOException {
		// Test data with 15 FEATURE_IDs and 4 partitions
		// Balanced distribution: chunkSize=3, remainder=3
		// Expected: partition0 gets 4, partition1 gets 4, partition2 gets 4, partition3 gets 3
		String polygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				200001,082G055,1234,DCR
				200002,082G055,5678,DCR
				200003,082G055,9999,DCR
				200004,082G055,1111,DCR
				200005,082G055,2222,DCR
				200006,082G055,3333,DCR
				200007,082G055,4444,DCR
				200008,082G055,5555,DCR
				200009,082G055,6666,DCR
				200010,082G055,7777,DCR
				200011,082G055,8888,DCR
				200012,082G055,9999,DCR
				200013,082G055,1010,DCR
				200014,082G055,1111,DCR
				200015,082G055,1212,DCR
				""";

		String layerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				200001,082G055,1234,P
				200002,082G055,5678,P
				200003,082G055,9999,P
				200004,082G055,1111,P
				200005,082G055,2222,P
				200006,082G055,3333,P
				200007,082G055,4444,P
				200008,082G055,5555,P
				200009,082G055,6666,P
				200010,082G055,7777,P
				200011,082G055,8888,P
				200012,082G055,9999,P
				200013,082G055,1010,P
				200014,082G055,1111,P
				200015,082G055,1212,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", polygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 4, tempDir);

		assertEquals(15, totalFeatureIds);

		// Verify balanced distribution with remainder=3
		Path partition0Polygon = tempDir.resolve("input-partition0").resolve("polygons.csv");
		long partition0Count = Files.readString(partition0Polygon).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition0Count, "Partition 0 should have 4 records");

		Path partition1Polygon = tempDir.resolve("input-partition1").resolve("polygons.csv");
		long partition1Count = Files.readString(partition1Polygon).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition1Count, "Partition 1 should have 4 records");

		Path partition2Polygon = tempDir.resolve("input-partition2").resolve("polygons.csv");
		long partition2Count = Files.readString(partition2Polygon).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition2Count, "Partition 2 should have 4 records");

		Path partition3Polygon = tempDir.resolve("input-partition3").resolve("polygons.csv");
		long partition3Count = Files.readString(partition3Polygon).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition3Count, "Partition 3 should have 3 records");

		// Verify layers also balanced
		Path partition0Layer = tempDir.resolve("input-partition0").resolve("layers.csv");
		long partition0LayerCount = Files.readString(partition0Layer).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(4, partition0LayerCount, "Partition 0 should have 4 layer records");

		Path partition3Layer = tempDir.resolve("input-partition3").resolve("layers.csv");
		long partition3LayerCount = Files.readString(partition3Layer).lines()
				.filter(line -> !line.startsWith("FEATURE_ID")).count();
		assertEquals(3, partition3LayerCount, "Partition 3 should have 3 layer records");
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

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertEquals(0, totalFeatureIds);
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

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		// Only valid feature ID should be processed
		assertEquals(1, totalFeatureIds);
	}

	@Test
	void testPartitionCsvFiles_SinglePartition() throws IOException {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 1, tempDir);

		assertEquals(4, totalFeatureIds);

		// Verify single partition directory was created
		assertTrue(Files.exists(tempDir.resolve("input-partition0")));
		assertFalse(Files.exists(tempDir.resolve("input-partition1")));
	}

	@Test
	void testPartitionCsvFiles_NonExistentOutputDirectory() throws IOException {
		Path nonExistentDir = tempDir.resolve("non-existent");

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, nonExistentDir);

		assertEquals(4, totalFeatureIds);
		// Directory should be created automatically
		assertTrue(Files.exists(nonExistentDir));
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

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertEquals(2, totalFeatureIds);
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

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		assertEquals(1, totalFeatureIds); // Only one polygon feature ID processed

		// Layer file should have created partition files but with only headers
		Path partition0LayerFile = tempDir.resolve("input-partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0LayerFile));

		String layerContent = Files.readString(partition0LayerFile);
		// Should only contain header since no matching feature IDs
		assertEquals("FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE", layerContent.trim());
	}

	@Test
	void testPartitionCsvFiles_NullPolygonFile() {
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(null, layerFile, 2, tempDir)
		);

		assertTrue(exception.getMessage().contains("Polygon file cannot be null"));
	}

	@Test
	void testPartitionCsvFiles_NullLayerFile() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, null, 2, tempDir)
		);

		assertTrue(exception.getMessage().contains("Layer file cannot be null"));
	}

	@Test
	void testPartitionCsvFiles_NullPartitionSize() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, layerFile, null, tempDir)
		);

		assertTrue(exception.getMessage().contains("Partition size cannot be null"));
	}

	@Test
	void testPartitionCsvFiles_ZeroPartitionSize() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, layerFile, 0, tempDir)
		);

		assertTrue(exception.getMessage().contains("Partition size must be positive"));
	}

	@Test
	void testPartitionCsvFiles_NegativePartitionSize() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv", POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", LAYER_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, layerFile, -1, tempDir)
		);

		assertTrue(exception.getMessage().contains("Partition size must be positive"));
	}

	@Test
	void testPartitionCsvFiles_NullJobBaseDir() {
		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				POLYGON_CSV_CONTENT.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				LAYER_CSV_CONTENT.getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> streamingCsvPartitioner.partitionCsvFiles(polygonFile, layerFile, 2, null));

		assertTrue(exception.getMessage().contains("Job base directory cannot be null"));
	}

	@Test
	void testExtractFeatureId_NullCsvLine() throws Exception {
		Method extractFeatureIdMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"extractFeatureId", String.class);
		extractFeatureIdMethod.setAccessible(true);

		Long result = (Long) extractFeatureIdMethod.invoke(streamingCsvPartitioner, (String) null);

		assertNull(result, "Null CSV line should return null");
	}

	@Test
	void testExtractFeatureId_WhitespaceCsvLine() throws Exception {
		Method extractFeatureIdMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"extractFeatureId", String.class);
		extractFeatureIdMethod.setAccessible(true);

		Long result = (Long) extractFeatureIdMethod.invoke(streamingCsvPartitioner, "   ");

		assertNull(result, "Whitespace-only CSV line should return null");
	}

	@Test
	void testExtractFeatureId_TabsAndSpaces() throws Exception {
		Method extractFeatureIdMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"extractFeatureId", String.class);
		extractFeatureIdMethod.setAccessible(true);

		Long result = (Long) extractFeatureIdMethod.invoke(streamingCsvPartitioner, "\t  \t");

		assertNull(result, "Tabs and spaces only should return null");
	}

	@Test
	void testCreatePartitionWriters_NullBaseDir() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, null, "test.csv", "HEADER", 2);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Base directory cannot be null"));
	}

	@Test
	void testCreatePartitionWriters_EmptyFilename() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, tempDir, "", "HEADER", 2);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Filename cannot be null or blank"));
	}

	@Test
	void testCreatePartitionWriters_NullHeader() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, tempDir, "test.csv", null, 2);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Header cannot be null"));
	}

	@Test
	void testCreatePartitionWriters_NullPartitionSize() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, tempDir, "test.csv", "HEADER", null);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Partition size must be positive"));
	}

	@Test
	void testCreatePartitionWriters_ZeroPartitionSize() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, tempDir, "test.csv", "HEADER", 0);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Partition size must be positive"));
		assertTrue(cause.getMessage().contains("got: 0"));
	}

	@Test
	void testCreatePartitionWriters_NegativePartitionSize() throws Exception {
		Method createPartitionWritersMethod = StreamingCsvPartitioner.class.getDeclaredMethod(
				"createPartitionWriters", Path.class, String.class, String.class, Integer.class);
		createPartitionWritersMethod.setAccessible(true);

		Exception exception = assertThrows(Exception.class, () -> {
			createPartitionWritersMethod.invoke(streamingCsvPartitioner, tempDir, "test.csv", "HEADER", -5);
		});

		Throwable cause = exception.getCause();
		assertTrue(cause instanceof IllegalArgumentException);
		assertTrue(cause.getMessage().contains("Partition size must be positive"));
		assertTrue(cause.getMessage().contains("got: -5"));
	}

	@Test
	void testPartitionCsvFiles_WithEmptyLinesInPolygonFile() throws IOException {
		String polygonWithEmptyLines = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR

				987654321,082G055,5678,DCR

				111222333,082G055,9999,DCR
				""";

		String layerCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				123456789,082G055,1234,P
				987654321,082G055,5678,P
				111222333,082G055,9999,P
				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				polygonWithEmptyLines.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv", layerCsv.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		// Empty lines should be skipped
		assertEquals(3, totalFeatureIds);
	}

	@Test
	void testPartitionCsvFiles_WithEmptyLinesInLayerFile() throws IOException {
		String polygonCsv = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,ORG_UNIT
				123456789,082G055,1234,DCR
				987654321,082G055,5678,DCR
				""";

		String layerWithEmptyLines = """
				FEATURE_ID,MAP_ID,POLYGON_NUMBER,LAYER_LEVEL_CODE
				123456789,082G055,1234,P

				987654321,082G055,5678,P

				""";

		MockMultipartFile polygonFile = new MockMultipartFile("polygonFile", "polygon.csv", "text/csv",
				polygonCsv.getBytes());
		MockMultipartFile layerFile = new MockMultipartFile("layerFile", "layer.csv", "text/csv",
				layerWithEmptyLines.getBytes());

		int totalFeatureIds = streamingCsvPartitioner.partitionCsvFiles(
				polygonFile, layerFile, 2, tempDir);

		// Should process successfully, empty lines in layer file are skipped
		assertEquals(2, totalFeatureIds);

		// Verify layer files were created
		Path partition0LayerFile = tempDir.resolve("input-partition0").resolve("layers.csv");
		assertTrue(Files.exists(partition0LayerFile));

		String content = Files.readString(partition0LayerFile);
		// Should contain header and one valid line (empty lines skipped)
		long lineCount = content.lines().count();
		assertEquals(2, lineCount); // header + 1 data line
	}
}
