package ca.bc.gov.nrs.vdyp.batch.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchRangeInputStreamTest {

	@TempDir
	Path tempDir;

	@Test
	void testCreate_WithValidRange() throws IOException {
		// Create test polygon file
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = """
				FEATURE_ID,POLYGON_ID
				F001,P001
				F002,P002
				F003,P003
				""";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		// Read records 1-2 (startIndex=1, recordCount=2)
		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 1, 2)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F002,P002"));
			assertThat(result, containsString("F003,P003"));
		}
	}

	@Test
	void testCreate_WithNegativeStartIndex() {
		Path polygonFile = tempDir.resolve("polygon.csv");

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class, () -> BatchRangeInputStream.create(polygonFile, -1, 10)
		);

		assertThat(exception.getMessage(), containsString("Start index must be non-negative"));
		assertThat(exception.getMessage(), containsString("-1"));
	}

	@Test
	void testCreate_WithZeroRecordCount() {
		Path polygonFile = tempDir.resolve("polygon.csv");

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class, () -> BatchRangeInputStream.create(polygonFile, 0, 0)
		);

		assertThat(exception.getMessage(), containsString("Record count must be positive"));
		assertThat(exception.getMessage(), containsString("0"));
	}

	@Test
	void testCreate_WithTrailingNewline() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001\n\n")); // Data + trailing newline
		}
	}

	@Test
	void testCreateForLayers_WithValidRange() throws IOException {
		// Create test polygon file
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = """
				FEATURE_ID,POLYGON_ID
				F001,P001
				F002,P002
				""";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create test layer file
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = """
				FEATURE_ID,LAYER_ID
				F001,L001
				F001,L002
				F002,L003
				F003,L004
				""";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		// Read layers for polygon records 0-1 (F001, F002)
		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, 2)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001,L001"));
			assertThat(result, containsString("F001,L002"));
			assertThat(result, containsString("F002,L003"));
		}
	}

	@Test
	void testCreateForLayers_WithNegativeStartIndex() {
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> BatchRangeInputStream.createForLayers(polygonFile, layerFile, -1, 10)
		);

		assertThat(exception.getMessage(), containsString("Start index must be non-negative"));
	}

	@Test
	void testCreateForLayers_WithNegativeRecordCount() {
		Path polygonFile = tempDir.resolve("polygon.csv");
		Path layerFile = tempDir.resolve("layer.csv");

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, -5)
		);

		assertThat(exception.getMessage(), containsString("Record count must be positive"));
	}

	@Test
	void testCreateForLayers_WithEmptyFeatureIds() throws IOException {
		// Create polygon file with only headers
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = "FEATURE_ID,POLYGON_ID\n";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create layer file
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = "FEATURE_ID,LAYER_ID\nF001,L001\n";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		// Read with startIndex beyond available records
		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 10, 5)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, equalTo("\n")); // Only trailing newline
		}
	}

	@Test
	void testCreateForLayers_WithSortedOptimization() throws IOException {
		// Create polygon file
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = "FEATURE_ID\nF001\nF002\n";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create sorted layer file
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = """
				FEATURE_ID,LAYER_ID
				F001,L001
				F001,L002
				F002,L003
				F003,L004
				F004,L005
				""";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, 2)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001,L001"));
			assertThat(result, containsString("F002,L003"));
		}
	}

	@Test
	void testRead_SingleByte() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1)) {
			int firstByte = stream.read();

			assertThat(firstByte, greaterThanOrEqualTo(0)); // Valid byte read
		}
	}

	@Test
	void testRead_AfterClose() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1);
		stream.close();

		IOException exception = assertThrows(IOException.class, stream::read);

		assertThat(exception.getMessage(), is("Stream closed"));
	}

	@Test
	void testReadArray_WithOffsetAndLength() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1)) {
			byte[] buffer = new byte[10];
			int bytesRead = stream.read(buffer, 0, 10);

			assertThat(bytesRead, greaterThanOrEqualTo(0));
		}
	}

	@Test
	void testReadArray_AfterClose() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1);
		stream.close();

		byte[] buffer = new byte[10];
		IOException exception = assertThrows(IOException.class, () -> stream.read(buffer, 0, 10));

		assertThat(exception.getMessage(), is("Stream closed"));
	}

	@Test
	void testClose_MultipleTimes() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1);
		stream.close();
		stream.close(); // Second close should be safe

		// Verify closed state is maintained
		IOException exception = assertThrows(IOException.class, stream::read);
		assertThat(exception.getMessage(), is("Stream closed"));
	}

	@Test
	void testAvailable() throws IOException {
		Path polygonFile = tempDir.resolve("polygon.csv");
		String content = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, content.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, 0, 1)) {
			int available = stream.available();

			assertThat(available, greaterThanOrEqualTo(0));
		}
	}

	@Test
	void testExtractFeatureIdsFromPolygonRange_WithNullFeatureId() throws IOException {
		// Create polygon file with a record that has null FEATURE_ID
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = """
				FEATURE_ID

				F001
				""";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create layer file
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = "FEATURE_ID,LAYER_ID\nF001,L001\n";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, 2)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001,L001"));
		}
	}

	@Test
	void testReadMatchingLayerRecords_WithNullFeatureId() throws IOException {
		// Create polygon file
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create layer file with null FEATURE_ID line
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = """
				FEATURE_ID,LAYER_ID

				F001,L001
				""";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, 1)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001,L001"));
		}
	}

	@Test
	void testReadMatchingLayerRecords_WithNonMatchingFeatureId() throws IOException {
		// Create polygon file
		Path polygonFile = tempDir.resolve("polygon.csv");
		String polygonContent = "FEATURE_ID\nF001\n";
		Files.write(polygonFile, polygonContent.getBytes(StandardCharsets.UTF_8));

		// Create layer file with consecutive non-matching records
		Path layerFile = tempDir.resolve("layer.csv");
		String layerContent = """
				FEATURE_ID,LAYER_ID
				F001,L001
				F001,L002
				F999,L999
				F001,L003
				""";
		Files.write(layerFile, layerContent.getBytes(StandardCharsets.UTF_8));

		try (BatchRangeInputStream stream = BatchRangeInputStream.createForLayers(polygonFile, layerFile, 0, 1)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001,L001"));
			assertThat(result, containsString("F001,L002"));
		}
	}
}
