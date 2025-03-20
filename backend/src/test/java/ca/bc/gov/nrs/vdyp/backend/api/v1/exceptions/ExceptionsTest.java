package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class ExceptionsTest {

	@Test
	void ProjectionExecutionExceptionTest() {

		var e1 = new ProjectionInternalExecutionException(new IllegalStateException("illegal"));
		Assert.assertEquals("illegal", e1.getCause().getMessage());

		var e2 = new ProjectionInternalExecutionException("validation error");
		Assert.assertEquals("validation error", e2.getMessage());

		var e3 = new ProjectionInternalExecutionException("validation error", new IllegalStateException("illegal"));
		Assert.assertEquals("illegal", e3.getCause().getMessage());
		Assert.assertEquals("validation error", e3.getMessage());
	}

	@Test
	void NotFoundExceptionTest() {

		var e1 = new NotFoundException();
		Assert.assertEquals(null, e1.getCause());
	}

	@Test
	void ExceptionsClassTest() {

		var e1 = new ProjectionInternalExecutionException("validation error");
		String message1 = Exceptions.getMessage(e1, "while performation operation f, ");
		Assert.assertTrue(message1.startsWith("while performation operation f, saw"));
		Assert.assertTrue(message1.endsWith("validation error"));

		var e2 = new ProjectionInternalExecutionException("validation error", new IllegalStateException("illegal"));
		String message2 = Exceptions.getMessage(e2, "while performation operation f, ");
		Assert.assertTrue(message2.startsWith("while performation operation f, saw"));
		Assert.assertTrue(message2.endsWith("illegal"));
	}
}
