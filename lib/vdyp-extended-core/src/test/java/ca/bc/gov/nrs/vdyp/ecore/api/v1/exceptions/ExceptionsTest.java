package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

public class ExceptionsTest {

	@Test
	void PolygonExecutionExceptionTest() {

		var e1 = new PolygonExecutionException(new IllegalStateException("illegal"));
		assertEquals("illegal", e1.getCause().getMessage());

		var e2 = new PolygonExecutionException("validation error");
		assertEquals("validation error", e2.getMessage());

		var e3 = new PolygonExecutionException("validation error", new IllegalStateException("illegal"));
		assertEquals("illegal", e3.getCause().getMessage());
		assertEquals("validation error", e3.getMessage());

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e4_1 = new PolygonValidationException(validationMessages);
		assertEquals("validation failure", e4_1.getMessage());
		var e4 = new PolygonExecutionException(e4_1);
		assertEquals("validation failure", e4.getMessage());

		var e5 = new PolygonExecutionException("validation error", new AssertionError());
		assertEquals(null, e5.getCause().getMessage());
		assertEquals("validation error", e5.getMessage());

		var validationMessage = new ValidationMessage(ValidationMessageKind.GENERIC, "a validation failure");
		var e6_1 = new PolygonValidationException(validationMessage);
		var e6 = new PolygonExecutionException("during execution", e6_1);
		assertEquals("during execution: a validation failure", e6.getMessage());
	}

	@Test
	void LayerValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new LayerValidationException(validationMessages);
		assertEquals("validation failure", e1_1.getMessage());
	}

	@Test
	void ProjectionRequestValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new ProjectionRequestValidationException(validationMessages);
		assertEquals("validation failure", e1_1.getMessage());
	}

	@Test
	void StandYieldCalculationExceptionTest() {

		var e1 = new StandYieldCalculationException(new Exception("calculation exception"));
		assertEquals("Exception: calculation exception", e1.getMessage());
		assertEquals("Exception", e1.getCause().getClass().getSimpleName());
	}

	@Test
	void YieldTableExceptionTest() {

		var e1 = new YieldTableGenerationException("message", new Exception("cause"));
		assertEquals("message", e1.getMessage());
		assertEquals("Exception", e1.getCause().getClass().getSimpleName());

		var e2 = new YieldTableGenerationException("another message");
		assertEquals("another message", e2.getMessage());
		assertEquals(null, e2.getCause());
	}

	@Test
	void NotFoundExceptionTest() {

		var e1 = new NotFoundException();
		assertEquals(null, e1.getCause());
	}

	@Test
	void ExceptionsClassTest() {

		var e1 = new PolygonExecutionException("validation error");
		String message1 = Exceptions.getMessage(e1, "while performation operation f, ");
		assertTrue(message1.startsWith("while performation operation f, saw"));
		assertTrue(message1.endsWith("validation error"));

		var e2 = new PolygonExecutionException("validation error", new IllegalStateException("illegal"));
		String message2 = Exceptions.getMessage(e2, "while performation operation f, ");
		assertTrue(message2.startsWith("while performation operation f, saw"));
		assertTrue(message2.endsWith("illegal"));
	}
}
