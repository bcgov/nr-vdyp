package ca.bc.gov.nrs.vdyp.batch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BatchConfigurationExceptionTest {

	@Test
	void testConstructor_WithMessageOnly() {
		String message = "Configuration error occurred";

		BatchConfigurationException exception = new BatchConfigurationException(message);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	void testConstructor_WithMessageAndCause() {
		String message = "Configuration failed due to underlying error";
		Throwable cause = new IllegalArgumentException("Invalid configuration parameter");

		BatchConfigurationException exception = new BatchConfigurationException(message, cause);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(cause, exception.getCause());
		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	void testConstructor_WithNullMessage() {
		BatchConfigurationException exception = new BatchConfigurationException(null);

		assertNotNull(exception);
		assertNull(exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructor_WithNullMessageAndCause() {
		Throwable cause = new RuntimeException("Root cause");

		BatchConfigurationException exception = new BatchConfigurationException(null, cause);

		assertNotNull(exception);
		assertNull(exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void testConstructor_WithEmptyMessage() {
		String emptyMessage = "";

		BatchConfigurationException exception = new BatchConfigurationException(emptyMessage);

		assertNotNull(exception);
		assertEquals(emptyMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testConstructor_WithNullCause() {
		String message = "Error message";

		BatchConfigurationException exception = new BatchConfigurationException(message, null);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	void testExceptionChaining_WithMultipleLevels() {
		Throwable rootCause = new IllegalStateException("Root cause");
		Throwable intermediateCause = new RuntimeException("Intermediate cause", rootCause);
		String message = "Top level configuration error";

		BatchConfigurationException exception = new BatchConfigurationException(message, intermediateCause);

		assertNotNull(exception);
		assertEquals(message, exception.getMessage());
		assertSame(intermediateCause, exception.getCause());
		assertSame(rootCause, exception.getCause().getCause());
	}

	@Test
	void testIsRuntimeException() {
		BatchConfigurationException exception = new BatchConfigurationException("Test");

		assertTrue(exception instanceof RuntimeException);

		assertNotNull(exception);
	}
}
