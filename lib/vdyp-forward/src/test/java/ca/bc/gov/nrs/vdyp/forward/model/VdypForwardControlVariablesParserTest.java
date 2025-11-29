package ca.bc.gov.nrs.vdyp.forward.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.forward.parsers.ForwardControlVariableParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariable;

class VdypForwardControlVariablesParserTest {

	@Test
	void testNullInput() {
		try {
			var parser = new ForwardControlVariableParser();
			parser.parse(null);
			Assertions.fail();
		} catch (ValueParseException e) {
			assertThat(e, hasProperty("message", is("VdypControlVariableParser: supplied string is null")));
		}
	}

	@Test
	void testEmptyInput() {
		try {
			var parser = new ForwardControlVariableParser();
			parser.parse("   ");
			Assertions.fail();
		} catch (ValueParseException e) {
			assertThat(e, hasProperty("message", is("VdypControlVariableParser: supplied string \"   \" is empty")));
		}
	}

	@Test
	void testInvalidInput() {
		try {
			var parser = new ForwardControlVariableParser();
			parser.parse("a b c");
			Assertions.fail();
		} catch (ValueParseException e) {
			assertThat(e, hasProperty("message", is("\"a\" is not a valid Integer")));
		}
	}

	@Test
	void testValidInput() throws Exception {
		var parser = new ForwardControlVariableParser();

		var details = parser.parse("1 1 1 1 1 1");
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.GROW_TARGET_1)));
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.COMPAT_VAR_OUTPUT_2)));
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3)));
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.OUTPUT_FILES_4)));
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.ALLOW_COMPAT_VAR_CALCS_5)));
		assertThat(1, equalTo(details.getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6)));
	}

	@Test
	void testExtraInputIgnored() throws Exception {
		var parser = new ForwardControlVariableParser();

		var details = parser.parse("1 1 1 1 1 1 1 1 1 1 1");
		assertThat(1, equalTo(details.getControlVariable(10)));
		assertThrows(IllegalArgumentException.class, () -> details.getControlVariable(11));
	}
}
