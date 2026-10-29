package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.StandYieldMessageKind;
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
		assertThat(e4_1.getMessage(), containsString("validation failure"));
		var e4 = new PolygonExecutionException(e4_1);
		assertThat(e4.getMessage(), containsString("validation failure"));

		var e5 = new PolygonExecutionException("validation error", new AssertionError());
		assertEquals(null, e5.getCause().getMessage());
		assertEquals("validation error", e5.getMessage());

		var e7 = new PolygonExecutionException(123L, "validation error");
		assertEquals("Polygon 123: validation error", e7.getMessage());

		var e8 = new PolygonExecutionException(123L, "validation error", new IllegalStateException("illegal"));
		assertEquals("Polygon 123: validation error", e8.getMessage());
		assertEquals("illegal", e8.getCause().getMessage());

		var e9 = new PolygonExecutionException(123L, new IllegalStateException("illegal"));
		assertEquals("Polygon 123: illegal", e9.getMessage());
	}

	@Test
	void PolygonValidationExceptionTest() {

		var e1 = new PolygonValidationException(
				456L, new ValidationMessage(ValidationMessageKind.GENERIC, "a failure")
		);
		assertThat(e1.getMessage(), allOf(startsWith("Polygon 456"), containsString("a failure")));
	}

	@Test
	void LayerValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new LayerValidationException(validationMessages);
		assertThat(e1_1.getMessage(), containsString("validation failure"));

		var e2 = new LayerValidationException(789L, "L1", validationMessages);
		assertThat(e2.getMessage(), allOf(startsWith("Polygon 789 Layer L1"), containsString("validation failure")));
	}

	@Test
	void ProjectionRequestValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new ProjectionRequestValidationException(validationMessages);
		assertThat(e1_1.getMessage(), containsString("validation failure"));
	}

	@Test
	void StandYieldCalculationExceptionTest() {

		var e1 = new StandYieldCalculationException(new Exception("calculation exception"));
		assertEquals("Exception: calculation exception", e1.getMessage());
		assertEquals("Exception", e1.getCause().getClass().getSimpleName());

		var e2 = new StandYieldCalculationException(123L, new Exception("calculation exception"));
		assertEquals("Polygon 123: Exception: calculation exception", e2.getMessage());

		var e3 = new StandYieldCalculationException(123L, StandYieldMessageKind.AGE_OUT_OF_RANGE, 0, 100);
		assertEquals("Polygon 123: stand total age is not in the range 0 to 100, inclusive", e3.getMessage());

		var e4 = new StandYieldCalculationException(123L, "L1", new Exception("calculation exception"));
		assertEquals("Polygon 123 Layer L1: Exception: calculation exception", e4.getMessage());

		var e5 = new StandYieldCalculationException(123L, "L1", StandYieldMessageKind.AGE_OUT_OF_RANGE, 0, 100);
		assertEquals("Polygon 123 Layer L1: stand total age is not in the range 0 to 100, inclusive", e5.getMessage());

		var e6 = new StandYieldCalculationException(123L, "L1", "PL", StandYieldMessageKind.AGE_OUT_OF_RANGE, 0, 100);
		assertEquals(
				"Polygon 123 Layer L1 Species PL: stand total age is not in the range 0 to 100, inclusive",
				e6.getMessage()
		);
	}

	@Test
	void YieldTableExceptionTest() {

		var e1 = new YieldTableGenerationException("message", new Exception("cause"));
		assertEquals("message", e1.getMessage());
		assertEquals("Exception", e1.getCause().getClass().getSimpleName());

		var e2 = new YieldTableGenerationException("another message");
		assertEquals("another message", e2.getMessage());
		assertEquals(null, e2.getCause());

		var e3 = new YieldTableGenerationException(123L, "message");
		assertEquals("Polygon 123: message", e3.getMessage());

		var e4 = new YieldTableGenerationException(123L, "message", new Exception("cause"));
		assertEquals("Polygon 123: message", e4.getMessage());
		assertEquals("Exception", e4.getCause().getClass().getSimpleName());

		var e5 = new YieldTableGenerationException(123L, new Exception("cause"));
		assertEquals("Polygon 123: cause", e5.getMessage());
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
