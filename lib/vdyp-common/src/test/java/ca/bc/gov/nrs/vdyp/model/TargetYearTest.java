package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TargetYearTest {

	@Test
	void testAbsolute() {
		var unit = TargetYear.absolute(2026);
		assertThat(unit, hasProperty("value", is(2026)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(2026));
		assertThat(unit.toString(), is("2026 CE"));
	}

	@Test
	void testAbsoluteZero() {
		var unit = TargetYear.absolute(0);
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(0));
		assertThat(unit.toString(), is("1 BCE"));
	}

	@Test
	void testAbsoluteNegative() {
		var unit = TargetYear.absolute(-1);
		assertThat(unit, hasProperty("value", is(-1)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(-1));
		assertThat(unit.toString(), is("2 BCE"));
	}

	@Test
	void testRelative() {
		var unit = TargetYear.relative(30);
		assertThat(unit, hasProperty("value", is(30)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(1995 + 30));
		assertThat(unit.toString(), is("+30"));
	}

	@Test
	void testRelativeZero() {
		var unit = TargetYear.relative(0);
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(1995));
		assertThat(unit.toString(), is("+0"));
	}

	@Test
	void testRelativeNegative() {
		var unit = TargetYear.relative(-30);
		assertThat(unit, hasProperty("value", is(-30)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit, hasProperty("specified", is(true)));
		assertThat(unit.relativeTo(1995), is(1965));
		assertThat(unit.toString(), is("-30"));
	}

	@Test
	void testNoGrowth() {
		var unit = TargetYear.NO_GROWTH;
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit, hasProperty("specified", is(false)));
		assertThat(unit.relativeTo(1995), is(1995));
		assertThat(unit.toString(), is("No growth"));
	}

	@Test
	void testPerPolygon() {
		var unit = TargetYear.PER_POLYGON;
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit, hasProperty("specified", is(false)));
		var ex = assertThrows(IllegalStateException.class, () -> unit.relativeTo(1995));
		assertThat(ex, hasMessage(containsString("growth")));
		assertThat(unit.toString(), is("Per polygon growth"));
	}

	static List<Arguments> normalControlVariableYears() {
		return List.of(
				Arguments.of(1, 1, true), Arguments.of(2, 2, true), Arguments.of(399, 399, true),
				Arguments.of(400, 400, true), Arguments.of(1920, 1920, false), Arguments.of(1921, 1921, false),
				Arguments.of(2026, 2026, false), Arguments.of(2399, 2399, false), Arguments.of(2400, 2400, false)
		);
	}

	static List<Arguments> badControlVariableYears() {
		return List.of(
				Arguments.of(-2, "negative"), Arguments.of(-3, "negative"), Arguments.of(-400, "negative"),
				Arguments.of(401, "between 400 and 1920"), Arguments.of(1919, "between 400 and 1920"),
				Arguments.of(2401, "later than 2400"), Arguments.of(3052, "later than 2400")
		);
	}

	@ParameterizedTest
	@MethodSource("normalControlVariableYears")
	void testFromControlVariable(int controlValue, int value, boolean relative) {
		var unit = TargetYear.fromControlVariable(controlValue);
		assertThat(unit, hasProperty("value", is(value)));
		assertThat(unit, hasProperty("relative", is(relative)));
		assertThat(unit, hasProperty("specified", is(true)));
	}

	@ParameterizedTest
	@MethodSource("badControlVariableYears")
	void testFromControlVariableError(int controlValue, String message) {
		var ex = assertThrows(IllegalArgumentException.class, () -> TargetYear.fromControlVariable(controlValue));
		assertThat(ex, hasMessage(containsString(message)));
	}

	@Test
	void testFromControlVariableNoGrowth() {
		var unit = TargetYear.fromControlVariable(0);
		assertThat(unit, Matchers.sameInstance(TargetYear.NO_GROWTH));
	}

	@Test
	void testFromControlVariablePerPolygon() {
		var unit = TargetYear.fromControlVariable(-1);
		assertThat(unit, Matchers.sameInstance(TargetYear.PER_POLYGON));
	}

}
