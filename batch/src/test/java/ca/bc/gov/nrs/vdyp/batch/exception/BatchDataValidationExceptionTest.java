package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BatchDataValidationExceptionTest {

	@Test
	void testConstructor_WithMessageOnly() {
		String message = "Data validation error occurred";

		BatchDataValidationException exception = new BatchDataValidationException(message);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	void testConstructor_WithMessageAndCause() {
		String message = "Validation failed due to underlying error";
		Throwable cause = new IllegalArgumentException("Invalid data parameter");

		BatchDataValidationException exception = new BatchDataValidationException(message, cause);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(cause, exception.getCause());
		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	void testConstructor_WithNullMessage() {
		BatchDataValidationException exception = new BatchDataValidationException(null);

		assertNotNull(exception);
		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructor_WithNullMessageAndCause() {
		Throwable cause = new RuntimeException("Root cause");

		BatchDataValidationException exception = new BatchDataValidationException(null, cause);

		assertNotNull(exception);
		assertNull(exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void testConstructor_WithEmptyMessage() {
		String emptyMessage = "";

		BatchDataValidationException exception = new BatchDataValidationException(emptyMessage);

		assertNotNull(exception);
		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructor_WithNullCause() {
		String message = "Error message";

		BatchDataValidationException exception = new BatchDataValidationException(message, null);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testExceptionChaining_WithMultipleLevels() {
		Throwable rootCause = new IllegalStateException("Root cause");
		Throwable intermediateCause = new RuntimeException("Intermediate cause", rootCause);
		String message = "Top level data validation error";

		BatchDataValidationException exception = new BatchDataValidationException(message, intermediateCause);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(intermediateCause, exception.getCause());
		assertSame(rootCause, exception.getCause().getCause());
	}

	@Test
	void testIsRuntimeException() {
		BatchDataValidationException exception = new BatchDataValidationException("Test");

		assertTrue(exception instanceof RuntimeException);
		assertNotNull(exception);
	}

	@Test
	void testConstructor_WithDataValidationContext() {
		String message = "Invalid FEATURE_ID format: ABC-123 does not match expected pattern";
		Throwable cause = new NumberFormatException("For input string: \"ABC\"");

		BatchDataValidationException exception = new BatchDataValidationException(message, cause);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains("FEATURE_ID"));
		assertTrue(exception.getCause() instanceof NumberFormatException);
	}
}
