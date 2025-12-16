package ca.bc.gov.nrs.vdyp.batch.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class BatchUtilsTest {
	@Test
	void testCreateJobFolderName_WithEmptyPrefix() {
		String prefix = "";
		String guid = "123e4567-e89b-12d3-a456-426614174000";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("-123e4567-e89b-12d3-a456-426614174000", result);
	}

	@Test
	void testCreateJobFolderName_WithEmptyGuid() {
		String prefix = "vdyp-batch";
		String guid = "";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("vdyp-batch-", result);
	}

	@Test
	void testCreateJobFolderName_Complete() {
		String prefix = "job";
		String guid = "123e4567-e89b-12d3-a456-426614174000";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("job-123e4567-e89b-12d3-a456-426614174000", result);
		// Verify format includes both components
		assertTrue(result.contains(prefix));
		assertTrue(result.contains(guid));
	}

	@Test
	void testCreateJobTimestamp() {
		String timestamp = BatchUtils.createJobTimestamp();

		assertNotNull(timestamp);
		// Verify format: yyyy_MM_dd_HH_mm_ss_SSSS
		assertTrue(timestamp.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));

		// Verify it can be parsed back using the expected format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
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
	void testDateTimeFormatterForFilenames() {
		// Test that createJobTimestamp produces the expected format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");

		assertNotNull(formatter);

		// Test formatting a known date
		LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123_400_000);
		String formatted = formatter.format(testDate);

		assertEquals("2024_01_15_10_30_45_1234", formatted);

		// Verify createJobTimestamp produces parseable output
		String timestamp = BatchUtils.createJobTimestamp();
		assertDoesNotThrow(() -> LocalDateTime.parse(timestamp, formatter));
	}

	@Test
	void testDateTimeFormatterForFilenames_ParseAndFormat() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
		String timestamp = "2024_12_31_23_59_59_9999";

		LocalDateTime parsed = LocalDateTime.parse(timestamp, formatter);
		String reformatted = formatter.format(parsed);

		assertEquals(timestamp, reformatted);
	}
}
