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
