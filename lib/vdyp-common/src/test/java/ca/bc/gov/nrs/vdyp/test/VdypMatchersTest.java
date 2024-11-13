package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;

class VdypMatchersTest {

	static <T> void assertMismatch(T item, Matcher<T> unit, Matcher<String> descriptionMatcher) {
		assertFalse(unit.matches(item), "expected match to fail but it passed");
		var description = new StringDescription();
		unit.describeMismatch(item, description);
		assertThat(
				"Mismatch description not as expected",
				description.toString(), descriptionMatcher
		);
	}

	static <T> void assertMatch(T item, Matcher<T> unit) {
		var result = unit.matches(item);
		if (result) {
			return;
		}

		var description = new StringDescription();
		unit.describeMismatch(item, description);
		assertFalse(result, "Expected match to pass but it failed with description: " + description.toString());
	}

	@Nested
	class mmEquals {

		MatrixMap2<Integer, Integer, String> expected;

		MatrixMap2<Integer, Integer, String> test;

		@BeforeEach
		void setup() {
			expected = new MatrixMap2Impl<>(
					List.of(1, 2, 3), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2)
			);
			test = new MatrixMap2Impl<>(List.of(1, 2, 3), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2));
		}

		@Test
		void testEqual() {
			var unit = VdypMatchers.mmEquals(expected, s -> Matchers.equalTo(s));
			assertMatch(test, unit);
		}

		@Test
		void testOneCellDifferent() {
			test.put(1, 4, "DIFFERENT");
			var unit = VdypMatchers.mmEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, equalTo("at [<1>, <4>] expected \"1:4\" but it was \"DIFFERENT\""));
		}

		@Test
		void testTwoCellsDifferent() {
			test.put(1, 4, "DIFFERENT");
			test.put(2, 5, "DIFFERENT");
			var unit = VdypMatchers.mmEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo(
							"at [<1>, <4>] expected \"1:4\" but it was \"DIFFERENT\" and there were 1 other mismatches"
					)
			);
		}

		@Test
		void testDifferentDimensionality() {
			MatrixMap<String> test3 = new MatrixMap3Impl<Integer, Integer, Integer, String>(
					List.of(1), List.of(2), List.of(3), (k1, k2, k3) -> "TEST"
			);
			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test3, unit, equalTo(
							"matrix map had 3 dimensions but expected 2"
					)
			);
		}

		@Test
		void testDimensionMissingValue() {
			test = new MatrixMap2Impl<>(List.of(1, 2), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2));

			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo(
							"matrix map had dimensions [[1, 2], [4, 5, 6]] but expected [[1, 2, 3], [4, 5, 6]]"
					)
			);
		}

		@Test
		void testDimensionExtraValue() {
			test = new MatrixMap2Impl<>(
					List.of(0, 1, 2, 3), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2)
			);

			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo(
							"matrix map had dimensions [[0, 1, 2, 3], [4, 5, 6]] but expected [[1, 2, 3], [4, 5, 6]]"
					)
			);
		}

	}

	@Nested
	class mapEquals {

		Map<Integer, String> expected;

		Map<Integer, String> test;

		@BeforeEach
		void setup() {
			expected = Utils.constMap(map -> {
				map.put(1, "Value 1");
				map.put(2, "Value 2");
			});
			test = new HashMap<>(expected);
		}

		@Test
		void testEqual() {
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMatch(test, unit);
		}

		@Test
		void testOneEntryDifferent() {
			test.put(1, "DIFFERENT");
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, matchesRegex("at <\\d> expected \"Value \\d\" but it was \"DIFFERENT\""));
		}

		@Test
		void testTwoEntriesDifferent() {
			test.put(1, "DIFFERENT");
			test.put(2, "DIFFERENT");
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, matchesRegex(
							"at <\\d> expected \"Value \\d\" but it was \"DIFFERENT\" and there were 1 other mismatches"
					)
			);
		}

		@Test
		void testMissingEntry() {
			test.remove(1);

			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo(
							"expected keys <[1, 2]> but were <[2]>"
					)
			);
		}

		@Test
		void testExtraEntry() {
			test.put(42, "EXTRA");

			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo(
							"expected keys <[1, 2]> but were <[1, 2, 42]>"
					)
			);
		}

		@Test
		void testBothEmpty() {
			expected = new HashMap<>();
			test = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMatch(
					test, unit
			);
		}

		@Test
		void testExpectedEmpty() {
			expected = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo("map size was <2>")
			);
		}

		@Test
		void testActualEmpty() {
			test = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit, equalTo("map was empty")
			);
		}

	}

}
