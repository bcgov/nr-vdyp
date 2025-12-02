package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BatchExceptionTest {

	@Test
	void testConstructor_WithFullContext() {
		String message = "Batch processing failed";
		Throwable cause = new RuntimeException("Root cause");
		Long jobExecutionId = 12345L;
		String jobGuid = "guid-001";
		String recordId = "FEATURE_001";
		boolean retryable = true;
		boolean skippable = true;

		BatchException exception = new BatchException(
				message, cause, jobGuid, jobExecutionId, recordId, retryable, skippable
		);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(cause, exception.getCause());
		assertEquals(recordId, exception.getFeatureId());
		assertTrue(exception.isRetryable());
		assertTrue(exception.isSkippable());
	}

	@Test
	void testConstructor_WithMessageOnly() {
		String message = "Simple error message";

		BatchException exception = new BatchException(message);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
		assertNull(exception.getFeatureId());
		assertFalse(exception.isRetryable());
		assertFalse(exception.isSkippable());
	}

	@Test
	void testConstructor_WithNullMessage() {
		BatchException exception = new BatchException(null);

		assertNotNull(exception);
		assertNull(exception.getMessage());
		assertNull(exception.getCause());
		assertNull(exception.getFeatureId());
		assertFalse(exception.isRetryable());
		assertFalse(exception.isSkippable());
	}

	@Test
	void testConstructor_WithEmptyMessage() {
		String emptyMessage = "";

		BatchException exception = new BatchException(emptyMessage);

		assertNotNull(exception);
		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
		assertNull(exception.getFeatureId());
	}

	@Test
	void testGetFeatureId_WhenSet() {
		String recordId = "FEATURE_123";
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, recordId, true, true);

		assertEquals(recordId, exception.getFeatureId());
	}

	@Test
	void testGetFeatureId_WhenNull() {
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, null, true, true);

		assertNull(exception.getFeatureId());
	}

	@Test
	void testIsRetryable_True() {
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, "FEATURE_001", true, false);

		assertTrue(exception.isRetryable());
	}

	@Test
	void testIsRetryable_False() {
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, "FEATURE_001", false, false);

		assertFalse(exception.isRetryable());
	}

	@Test
	void testIsSkippable_True() {
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, "FEATURE_001", false, true);

		assertTrue(exception.isSkippable());
	}

	@Test
	void testIsSkippable_False() {
		BatchException exception = new BatchException("Error", null, "guid-001", 1L, "FEATURE_001", false, false);

		assertFalse(exception.isSkippable());
	}

	@Test
	void testIsInstanceOfException() {
		BatchException exception = new BatchException("Test");

		assertTrue(exception instanceof Exception);
		assertNotNull(exception);
	}

	@Test
	void testConstructor_MessageOnly_DefaultBehavior() {
		BatchException exception = new BatchException("Test message");

		assertEquals("Test message", exception.getMessage());
		assertNull(exception.getCause());
		assertNull(exception.getFeatureId());
		assertFalse(exception.isRetryable());
		assertFalse(exception.isSkippable());
	}
}
