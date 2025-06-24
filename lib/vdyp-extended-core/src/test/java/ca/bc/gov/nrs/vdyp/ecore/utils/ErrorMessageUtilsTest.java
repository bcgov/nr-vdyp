package ca.bc.gov.nrs.vdyp.ecore.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;

public class ErrorMessageUtilsTest {
	@Test
	void testErrorMessageCreation() {
		var polygon = new Polygon.Builder().build();
		String message = ErrorMessageUtils.BuildVDYPApplicationErrorMessage(
				VdypApplicationIdentifier.FIP_START, polygon, "running", new Exception("Testing Exception")
		);
		assertThat(message, containsString("running"));
		assertThat(message, containsString("Testing Exception"));
	}

	@Test
	void testErrorMessageCausedByCreation() {
		var polygon = new Polygon.Builder().build();
		var exception = new PolygonExecutionException(
				"Testing Exception",
				new RuntimeException(
						"Internal Exception", new RuntimeException("Internal Exception", new IllegalArgumentException())
				)
		);
		String message = ErrorMessageUtils
				.BuildVDYPApplicationErrorMessage(VdypApplicationIdentifier.FIP_START, polygon, "running", exception);
		assertThat(message, containsString("running"));
		assertThat(message, containsString("Testing Exception"));
		assertThat(message, containsString("Internal Exception"));
	}
}
