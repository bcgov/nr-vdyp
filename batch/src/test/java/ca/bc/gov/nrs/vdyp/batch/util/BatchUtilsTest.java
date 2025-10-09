package ca.bc.gov.nrs.vdyp.batch.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class BatchUtilsTest {
	@Test
	void testCreateJobFolderName_WithEmptyPrefix() {
		String prefix = "";
		String timestamp = "2024_01_15_10_30_45_1234";

		String result = BatchUtils.createJobFolderName(prefix, timestamp);

		assertEquals("-2024_01_15_10_30_45_1234", result);
	}

	@Test
	void testCreateJobFolderName_WithEmptyTimestamp() {
		String prefix = "vdyp-batch";
		String timestamp = "";

		String result = BatchUtils.createJobFolderName(prefix, timestamp);

		assertEquals("vdyp-batch-", result);
	}

	@Test
	void testCreateJobTimestamp() {
		String timestamp = BatchUtils.createJobTimestamp();

		assertNotNull(timestamp);
		// Verify format: yyyy_MM_dd_HH_mm_ss_SSSS
		assertTrue(timestamp.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));

		// Verify it can be parsed back
		DateTimeFormatter formatter = BatchUtils.dateTimeFormatterForFilenames;
		assertDoesNotThrow(() -> LocalDateTime.parse(timestamp, formatter));
	}

	@Test
	void testCreateJobTimestamp_UniqueValues() {
		String timestamp1 = BatchUtils.createJobTimestamp();
		String timestamp2 = BatchUtils.createJobTimestamp();

		assertNotNull(timestamp1);
		assertNotNull(timestamp2);

		assertTrue(timestamp1.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
		assertTrue(timestamp2.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
	}

	@Test
	void testSanitizeForLogging_Null() {
		String result = BatchUtils.sanitizeForLogging(null);

		assertEquals("null", result);
	}

	@Test
	void testSanitizeForLogging_LongFilename() {
		// Create a filename longer than 100 characters
		String filename = "a".repeat(150);

		String result = BatchUtils.sanitizeForLogging(filename);

		assertEquals(100, result.length());
		assertTrue(result.endsWith("..."));
		assertEquals("a".repeat(97) + "...", result);
	}

	@Test
	void testSanitizeForLogging_ExactlyAtLimit() {
		String filename = "a".repeat(100);

		String result = BatchUtils.sanitizeForLogging(filename);

		assertEquals(100, result.length());
		assertFalse(result.endsWith("..."));
		assertEquals(filename, result);
	}

	@Test
	void testSanitizeForLogging_JustOverLimit() {
		String filename = "a".repeat(101);

		String result = BatchUtils.sanitizeForLogging(filename);

		assertEquals(100, result.length());
		assertTrue(result.endsWith("..."));
		assertEquals("a".repeat(97) + "...", result);
	}

	@Test
	void testSanitizeForLogging_MixedControlAndValid() {
		String filename = "test\u0000\u001ffile.csv";

		String result = BatchUtils.sanitizeForLogging(filename);

		assertEquals("testfile.csv", result);
	}

	@Test
	void testSanitizeForLogging_LongFilenameWithControlChars() {
		// Create a long filename with control characters
		String filename = "test\u0000" + "a".repeat(150) + "\u001f.csv";

		String result = BatchUtils.sanitizeForLogging(filename);

		assertEquals(100, result.length());
		assertTrue(result.endsWith("..."));
		assertTrue(result.startsWith("test"));
	}

	@Test
	void testDateTimeFormatterForFilenames() {
		DateTimeFormatter formatter = BatchUtils.dateTimeFormatterForFilenames;

		assertNotNull(formatter);

		// Test formatting a known date
		LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123_400_000);
		String formatted = formatter.format(testDate);

		assertEquals("2024_01_15_10_30_45_1234", formatted);
	}

	@Test
	void testDateTimeFormatterForFilenames_ParseAndFormat() {
		DateTimeFormatter formatter = BatchUtils.dateTimeFormatterForFilenames;
		String timestamp = "2024_12_31_23_59_59_9999";

		LocalDateTime parsed = LocalDateTime.parse(timestamp, formatter);
		String reformatted = formatter.format(parsed);

		assertEquals(timestamp, reformatted);
	}
}
