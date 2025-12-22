package ca.bc.gov.nrs.vdyp.batch.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

		// Calculate byte offset: header line + first data line
		// "FEATURE_ID,POLYGON_ID\n" = 22 bytes
		// "F001,P001\n" = 10 bytes
		// Total offset to start at F002 = 32 bytes
		long startByte = 32;

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, startByte, 2)) {
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

		assertThat(exception.getMessage(), containsString("Start byte must be non-negative"));
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

		// Calculate byte offset: "FEATURE_ID\n" = 11 bytes
		long startByte = 11;

		try (BatchRangeInputStream stream = BatchRangeInputStream.create(polygonFile, startByte, 1)) {
			String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(result, containsString("F001\n")); // Data with newline
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

}
