package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TargetYearTest {

	@Test
	void testAbsolute() {
		var unit = TargetYear.absolute(2026);
		assertThat(unit, hasProperty("value", is(2026)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit.relativeTo(1995), is(2026));
		assertThat(unit.toString(), is("2026 CE"));
	}

	@Test
	void testAbsoluteZero() {
		var unit = TargetYear.absolute(0);
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit.relativeTo(1995), is(0));
		assertThat(unit.toString(), is("1 BCE"));
	}

	@Test
	void testAbsoluteNegative() {
		var unit = TargetYear.absolute(-1);
		assertThat(unit, hasProperty("value", is(-1)));
		assertThat(unit, hasProperty("relative", is(false)));
		assertThat(unit.relativeTo(1995), is(-1));
		assertThat(unit.toString(), is("2 BCE"));
	}

	@Test
	void testRelative() {
		var unit = TargetYear.relative(30);
		assertThat(unit, hasProperty("value", is(30)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit.relativeTo(1995), is(1995 + 30));
		assertThat(unit.toString(), is("+30"));
	}

	@Test
	void testRelativeZero() {
		var unit = TargetYear.relative(0);
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit.relativeTo(1995), is(1995));
		assertThat(unit.toString(), is("+0"));
	}

	@Test
	void testRelativeNegative() {
		var unit = TargetYear.relative(-30);
		assertThat(unit, hasProperty("value", is(-30)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit.relativeTo(1995), is(1965));
		assertThat(unit.toString(), is("-30"));
	}

	@Test
	void testNoGrowth() {
		var unit = TargetYear.NO_GROWTH;
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		assertThat(unit.relativeTo(1995), is(1995));
		assertThat(unit.toString(), is("No growth"));
	}

	@Test
	void testPerPolygon() {
		var unit = TargetYear.PER_POLYGON;
		assertThat(unit, hasProperty("value", is(0)));
		assertThat(unit, hasProperty("relative", is(true)));
		var ex = assertThrows(IllegalStateException.class, () -> unit.relativeTo(1995));
		assertThat(ex, hasMessage(containsString("growth")));
		assertThat(unit.toString(), is("Per polygon growth"));
	}

}
