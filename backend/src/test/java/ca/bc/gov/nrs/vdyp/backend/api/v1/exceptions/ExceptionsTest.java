package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;

public class ExceptionsTest {

	@Test
	void PolygonExecutionExceptionTest() {

		var e1 = new PolygonExecutionException(new IllegalStateException("illegal"));
		Assert.assertEquals("illegal", e1.getCause().getMessage());

		var e2 = new PolygonExecutionException("validation error");
		Assert.assertEquals("validation error", e2.getMessage());

		var e3 = new PolygonExecutionException("validation error", new IllegalStateException("illegal"));
		Assert.assertEquals("illegal", e3.getCause().getMessage());
		Assert.assertEquals("validation error", e3.getMessage());

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e4_1 = new PolygonValidationException(validationMessages);
		Assert.assertEquals("validation failure", e4_1.getMessage());
		var e4 = new PolygonExecutionException(e4_1);
		Assert.assertEquals("validation failure", e4.getMessage());

		var e5 = new PolygonExecutionException("validation error", new AssertionError());
		Assert.assertEquals(null, e5.getCause().getMessage());
		Assert.assertEquals("validation error", e5.getMessage());

		var validationMessage = new ValidationMessage(ValidationMessageKind.GENERIC, "a validation failure");
		var e6_1 = new PolygonValidationException(validationMessage);
		var e6 = new PolygonExecutionException("during execution", e6_1);
		Assert.assertEquals("during execution: a validation failure", e6.getMessage());
	}

	@Test
	void LayerValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new LayerValidationException(validationMessages);
		Assert.assertEquals("validation failure", e1_1.getMessage());
	}

	@Test
	void ProjectionRequestValidationExceptionTest() {

		var validationMessages = new ArrayList<ValidationMessage>();
		validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, "validation failure"));
		var e1_1 = new ProjectionRequestValidationException(validationMessages);
		Assert.assertEquals("validation failure", e1_1.getMessage());
	}

	@Test
	void StandYieldCalculationExceptionTest() {

		var e1 = new StandYieldCalculationException(12, new Exception("calculation exception"));
		Assert.assertEquals("StandYieldCalculation exception 12", e1.getMessage());
		Assert.assertEquals(12, e1.getErrorCode());
		Assert.assertEquals("Exception", e1.getCause().getClass().getSimpleName());

		var e2 = new StandYieldCalculationException(12);
		Assert.assertEquals("StandYieldCalculation exception 12", e2.getMessage());
		Assert.assertEquals(12, e2.getErrorCode());
		Assert.assertEquals(null, e2.getCause());
	}

	@Test
	void YieldTableExceptionTest() {

		var e1 = new YieldTableGenerationException("message", new Exception("cause"));
		Assert.assertEquals("message", e1.getMessage());
		Assert.assertEquals("Exception", e1.getCause().getClass().getSimpleName());

		var e2 = new YieldTableGenerationException("another message");
		Assert.assertEquals("another message", e2.getMessage());
		Assert.assertEquals(null, e2.getCause());
	}

	@Test
	void NotFoundExceptionTest() {

		var e1 = new NotFoundException();
		Assert.assertEquals(null, e1.getCause());
	}

	@Test
	void ExceptionsClassTest() {

		var e1 = new PolygonExecutionException("validation error");
		String message1 = Exceptions.getMessage(e1, "while performation operation f, ");
		Assert.assertTrue(message1.startsWith("while performation operation f, saw"));
		Assert.assertTrue(message1.endsWith("validation error"));

		var e2 = new PolygonExecutionException("validation error", new IllegalStateException("illegal"));
		String message2 = Exceptions.getMessage(e2, "while performation operation f, ");
		Assert.assertTrue(message2.startsWith("while performation operation f, saw"));
		Assert.assertTrue(message2.endsWith("illegal"));
	}
}
