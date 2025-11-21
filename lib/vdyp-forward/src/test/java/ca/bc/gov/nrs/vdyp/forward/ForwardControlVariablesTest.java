package ca.bc.gov.nrs.vdyp.forward;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariable;

class ForwardControlVariablesTest {

	@Test
	void testNull() throws ValueParseException {
		ForwardControlVariables fcv = new ForwardControlVariables(null);

		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.GROW_TARGET_1), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_OUTPUT_2), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.OUTPUT_FILES_4), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.ALLOW_COMPAT_VAR_CALCS_5), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6), Matchers.equalTo(0));
	}

	@Test
	void testEmpty() throws ValueParseException {
		ForwardControlVariables fcv = new ForwardControlVariables(new Integer[0]);

		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.GROW_TARGET_1), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_OUTPUT_2), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.OUTPUT_FILES_4), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.ALLOW_COMPAT_VAR_CALCS_5), Matchers.equalTo(0));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6), Matchers.equalTo(0));
	}

	@Test
	void testSimple() throws ValueParseException {
		ForwardControlVariables fcv = new ForwardControlVariables(new Integer[] { 1, 1, 1, 1, 1, 1 });

		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.GROW_TARGET_1), Matchers.equalTo(1));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_OUTPUT_2), Matchers.equalTo(1));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3), Matchers.equalTo(1));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.OUTPUT_FILES_4), Matchers.equalTo(1));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.ALLOW_COMPAT_VAR_CALCS_5), Matchers.equalTo(1));
		MatcherAssert.assertThat(fcv.getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6), Matchers.equalTo(1));
	}

	@Test
	void testYearValues() throws ValueParseException {
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { -2, 0, 0, 0, 0, 0 }));
		new ForwardControlVariables(new Integer[] { -1, 0, 0, 0, 0, 0 });
		new ForwardControlVariables(new Integer[] { 0, 0, 0, 0, 0, 0 });
		new ForwardControlVariables(new Integer[] { 1, 0, 0, 0, 0, 0 });
		new ForwardControlVariables(new Integer[] { 400, 0, 0, 0, 0, 0 });
		assertThrows(
				ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 401, 0, 0, 0, 0, 0 })
		);
		assertThrows(
				ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 1919, 0, 0, 0, 0, 0 })
		);
		new ForwardControlVariables(new Integer[] { 1920, 0, 0, 0, 0, 0 });
		new ForwardControlVariables(new Integer[] { 2400, 0, 0, 0, 0, 0 });
		assertThrows(
				ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 2401, 0, 0, 0, 0, 0 })
		);
	}

	@Test
	void testInvalidInput2To5() {
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 0, 3, 3, 4, 2, 2 }));
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 0, 0, 3, 4, 2, 2 }));
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 0, 0, 0, 4, 2, 2 }));
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 0, 0, 0, 0, 2, 2 }));
		assertThrows(ValueParseException.class, () -> new ForwardControlVariables(new Integer[] { 0, 0, 0, 0, 0, 2 }));
	}
}
