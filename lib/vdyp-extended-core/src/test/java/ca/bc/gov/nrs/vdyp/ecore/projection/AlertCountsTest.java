package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.AlertCounts.AlertType;

class AlertCountsTest {

	@ParameterizedTest
	@CsvSource({ "FATAL_ERROR, ERROR", "ERROR, ERROR", "WARNING, WARNING", "INFORMATION, INFO", "STATUS, INFO" })
	void testAlertTypeOfSeverity(MessageSeverityCode severity, AlertType expected) {
		assertThat(AlertType.of(severity), is(expected));
	}

	@Test
	void testPlusAddsEachCount() {
		var left = new AlertCounts(1, 2, 3, 4, 5, 6);
		var right = new AlertCounts(10, 20, 30, 40, 50, 60);

		assertThat(left.plus(right), is(new AlertCounts(11, 22, 33, 44, 55, 66)));
	}

	@Test
	void testPlusNullReturnsSameCounts() {
		var counts = new AlertCounts(1, 2, 3, 4, 5, 6);

		assertThat(counts.plus(null), sameInstance(counts));
	}
}
