package ca.bc.gov.nrs.vdyp.io.parse.control;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.coe.DebugSettingsParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public class DebugSettingsParserTest {

	@Test
	void testEmpty() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("");

		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testSingleFirstSlot() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("1");

		int expectedSlot = 1;

		assertThat(result, debugFlag(expectedSlot, is(1)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i == expectedSlot)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testSingleSecondSlot() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("0 1");

		int expectedSlot = 2;

		assertThat(result, debugFlag(expectedSlot, is(1)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i == expectedSlot)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testFirstAndSecond() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("2 3");

		assertThat(result, debugFlag(1, is(2)));
		assertThat(result, debugFlag(2, is(3)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i <= 2)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testFirstAndSecondLeadingSpace() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse(" 2 3");

		assertThat(result, debugFlag(1, is(2)));
		assertThat(result, debugFlag(2, is(3)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i <= 2)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testFirstAndSecondLeadingZero() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("0203");

		assertThat(result, debugFlag(1, is(2)));
		assertThat(result, debugFlag(2, is(3)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i <= 2)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testLeadingZeroThen2Digit() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse("0210");

		assertThat(result, debugFlag(1, is(2)));
		assertThat(result, debugFlag(2, is(10)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i <= 2)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	@Test
	void testLeadingSpaceThen2Digit() throws ValueParseException {
		var parser = new DebugSettingsParser();
		var result = parser.parse(" 210");

		assertThat(result, debugFlag(1, is(2)));
		assertThat(result, debugFlag(2, is(10)));
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			if (i <= 2)
				continue;
			assertThat(result, debugFlag(i, is(0)));
		}
	}

	//" 0 0 0 0 0 0 0 050 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0"

	public static Matcher<DebugSettings> debugFlag(int i, Matcher<Integer> valueMatcher) {
		return (Matcher<DebugSettings>) new TypeSafeDiagnosingMatcher<DebugSettings>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("is a DebugSettings with flag ").appendValue(i).appendText(" that ");
				valueMatcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(DebugSettings item, Description mismatchDescription) {
				int value = item.getValue(i);
				if (!valueMatcher.matches(value)) {
					valueMatcher.describeMismatch(value, mismatchDescription);
					return false;
				}
				return true;
			}

		};
	}
}
