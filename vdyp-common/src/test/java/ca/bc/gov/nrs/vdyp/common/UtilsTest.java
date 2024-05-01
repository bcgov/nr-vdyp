package ca.bc.gov.nrs.vdyp.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class UtilsTest {

	@Test
	void testSingletonOrEmptyForNull() {
		var result = Utils.singletonOrEmpty(null);
		assertThat(result, Matchers.empty());
	}

	@Test
	void testSingletonOrEmptyForNonNull() {
		var result = Utils.singletonOrEmpty("X");
		assertThat(result, Matchers.contains("X"));
	}

	@Test
	void testExpectParsedControlMissing() {
		var ex = assertThrows(
				IllegalStateException.class, () -> Utils
						.expectParsedControl(Collections.emptyMap(), "NOT_PRESENT", Integer.class)
		);
		assertThat(ex, hasProperty("message", stringContainsInOrder("Expected control map to have", "NOT_PRESENT")));
	}

	@Test
	void testExpectParsedControlWrongType() {
		var ex = assertThrows(
				IllegalStateException.class, () -> Utils
						.expectParsedControl(Collections.singletonMap("WRONG_TYPE", 2d), "WRONG_TYPE", Integer.class)
		);
		assertThat(
				ex, hasProperty(
						"message", stringContainsInOrder(
								"Expected control map entry", "WRONG_TYPE", "to be", "Integer", "was", "Double"
						)
				)
		);
	}

	@Test
	void testExpectParsedControlStillString() {
		var ex = assertThrows(
				IllegalStateException.class, () -> Utils.expectParsedControl(
						Collections.singletonMap("WRONG_TYPE", "UNPARSED"), "WRONG_TYPE", Integer.class
				)
		);
		assertThat(
				ex, hasProperty(
						"message", stringContainsInOrder(
								"Expected control map entry", "WRONG_TYPE", "to be parsed but was still a String"
						)
				)
		);
	}

	@Test
	void testExpectParsedControlPresent() {
		var result = Utils.expectParsedControl(Collections.singletonMap("PRESENT", 2), "PRESENT", Integer.class);
		assertThat(result, is(2));
	}

	@Test
	void testExpectParsedControlPresentString() {
		var result = Utils.expectParsedControl(Collections.singletonMap("PRESENT", "X"), "PRESENT", String.class);
		assertThat(result, is("X"));
	}

	@Test
	void testCompareUsing() {
		var unit = Utils.compareUsing((String s) -> s.substring(1, 2)); // Compares Strings by their second character.

		var list = new ArrayList<>(List.of("12", "21", "33"));
		list.sort(unit);

		assertThat(list, Matchers.contains("21", "12", "33"));

	}

	@Test
	void testConstMap() {
		var unit = Utils.<String, String>constMap((x) -> {
			x.put("TEST", "VALUE");
		});

		assertThat(unit, hasEntry("TEST", "VALUE"));

		assertThrows(UnsupportedOperationException.class, () -> unit.put("TEST", "CHANGED"));
		assertThrows(UnsupportedOperationException.class, () -> unit.put("ANOTHER", "VALUE"));
	}

}
