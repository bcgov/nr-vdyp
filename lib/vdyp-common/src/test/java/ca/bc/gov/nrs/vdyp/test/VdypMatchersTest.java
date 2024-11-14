package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;

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

		var description = new StringDescription();
		unit.describeMismatch(item, description);
		assertTrue(result, "Expected match to pass but it failed with description: " + description.toString());
	}

	@Nested
	class deepEquals {

		@Nested
		class testVdypCompatibilityVariables {
			VdypCompatibilityVariables expected;
			Matcher<VdypCompatibilityVariables> unit;

			@BeforeEach
			void setup() {
				// Use a fixed seed so the random numbers are the same across runs.
				Random rand = new Random(42);

				expected = VdypCompatibilityVariables.build(ib -> {
					ib.polygonIdentifier("Test", 2024);
					ib.layerType(LayerType.PRIMARY);
					ib.genus("MB");
					ib.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
					ib.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
					ib.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
					ib.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
				});

				unit = VdypMatchers.deepEquals(expected);
			}

			@Test
			void testPass() {
				var actual = VdypCompatibilityVariables.build(ib -> {
					ib.copy(expected);
				});

				assertMatch(actual, unit);
			}

			@Test
			void testOneVolumeEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(ib -> {
					ib.copy(expected);
				});

				actual.getCvVolume().put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 20f
				);

				assertMismatch(
						actual, unit, matchesRegex(
								"CvVolume at \\[<U125TO175>, <CLOSE_UTIL_VOL>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneBaEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(ib -> {
					ib.copy(expected);
				});

				actual.getCvBasalArea().put(
						UtilizationClass.U125TO175, LayerType.PRIMARY, 20f
				);

				assertMismatch(
						actual, unit, matchesRegex(
								"CvBasalArea at \\[<U125TO175>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneDqEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(ib -> {
					ib.copy(expected);
				});

				actual.getCvQuadraticMeanDiameter().put(
						UtilizationClass.U125TO175, LayerType.PRIMARY, 20f
				);

				assertMismatch(
						actual, unit, matchesRegex(
								"CvQuadraticMeanDiameter at \\[<U125TO175>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneSmallEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(ib -> {
					ib.copy(expected);
				});

				actual.getCvPrimaryLayerSmall().put(
						UtilizationClassVariable.BASAL_AREA, 20f
				);

				assertMismatch(
						actual, unit, matchesRegex(
								"CvPrimaryLayerSmall at <BASAL_AREA> expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

		}

		@Nested
		class testVdypSite {
			VdypSite expected;
			Matcher<VdypSite> unit;

			@BeforeEach
			void setup() {
				expected = VdypSite.build(ib -> {
					ib.polygonIdentifier("Test", 2024);
					ib.layerType(LayerType.PRIMARY);
					ib.genus("MB");
					ib.ageTotal(40);
					ib.yearsToBreastHeight(5);
					ib.height(15);
					ib.siteCurveNumber(42);
					ib.siteIndex(4);
				});

				unit = VdypMatchers.deepEquals(expected);
			}

			@Test
			void testPass() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
				});

				assertMatch(actual, unit);
			}

			@Test
			void testPolyIdDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.polygonIdentifier("Different", 2025);
				});

				assertMismatch(
						actual, unit, equalTo(
								"PolygonIdentifier was <Different            2025> but expected <Test                 2024>"
						)
				);
			}

			@Test
			void testLayerTypeDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.layerType(LayerType.VETERAN);
				});

				assertMismatch(
						actual, unit, equalTo(
								"LayerType was <VETERAN> but expected <PRIMARY>"
						)
				);
			}

			@Test
			void testSpeciesGroupDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.genus("B");
				});

				assertMismatch(
						actual, unit, equalTo(
								"SpeciesGroup was \"B\" but expected \"MB\""
						)
				);
			}

			@Test
			void testHeightDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.height(16);
				});

				assertMismatch(
						actual, unit, equalTo(
								"Height was <Optional[16.0]> but expected <Optional[15.0]>"
						)
				);
			}

			@Test
			void testAgeTotalDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.ageTotal(45);
				});

				// Years at breast height is computed from AgeTotal and YearstToBreastHeight
				assertMismatch(
						actual, unit, equalTo(
								"AgeTotal was <Optional[45.0]> but expected <Optional[40.0]>, YearsAtBreastHeight was <Optional[40.0]> but expected <Optional[35.0]>"
						)
				);
			}

			@Test
			void testYearsToBreastHeightDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.yearsToBreastHeight(4);
				});

				// Years at breast height is computed from AgeTotal and YearstToBreastHeight
				assertMismatch(
						actual, unit, equalTo(
								"YearsToBreastHeight was <Optional[4.0]> but expected <Optional[5.0]>, YearsAtBreastHeight was <Optional[36.0]> but expected <Optional[35.0]>"
						)
				);
			}

			@Test
			void testSiteCurveNumberDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.siteCurveNumber(41);
				});

				assertMismatch(
						actual, unit, equalTo(
								"SiteCurveNumber was <Optional[41]> but expected <Optional[42]>"
						)
				);
			}

			@Test
			void testSiteIndexDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.siteIndex(3);
				});

				assertMismatch(
						actual, unit, equalTo(
								"SiteIndex was <Optional[3.0]> but expected <Optional[4.0]>"
						)
				);
			}

		}
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
