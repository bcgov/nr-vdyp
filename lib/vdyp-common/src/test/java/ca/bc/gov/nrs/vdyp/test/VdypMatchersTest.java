package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;

class VdypMatchersTest {

	static <T> void assertMismatch(T item, Matcher<T> unit, Matcher<String> descriptionMatcher) {
		assertFalse(unit.matches(item), "expected match to fail but it passed");
		var description = new StringDescription();
		unit.describeMismatch(item, description);
		assertThat("Mismatch description not as expected", description.toString(), descriptionMatcher);
	}

	static <T> void assertMatch(T item, Matcher<T> unit) {
		var result = unit.matches(item);

		var description = new StringDescription();
		unit.describeMismatch(item, description);
		assertTrue(result, "Expected match to pass but it failed with description: " + description.toString());
	}

	Random rand = new Random(42);

	UtilizationVector mockUtilVector(float multiplier) {
		return Utils.utilizationVector(
				rand.nextFloat() * multiplier, rand.nextFloat() * multiplier, rand.nextFloat() * multiplier,
				rand.nextFloat() * multiplier, rand.nextFloat() * multiplier
		);
	}

	UtilizationVector mockHeightVector() {
		return Utils.heightVector(rand.nextFloat() * 5, rand.nextFloat() * 20);
	}

	@Nested
	class deepEquals {
		@Nested
		class testVdypPolygon {
			VdypPolygon expected;
			Matcher<VdypPolygon> unit;
			Map<String, Object> controlMap;

			@BeforeEach
			void setup() {
				Random rand = new Random(42);
				controlMap = TestUtils.loadControlMap();

				expected = VdypPolygon.build(pb -> {

					pb.polygonIdentifier("Test", 2024);
					pb.percentAvailable(90f);
					pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
					pb.forestInventoryZone("Z");

					pb.addLayer(lb -> {

						lb.layerType(LayerType.PRIMARY);

						lb.empiricalRelationshipParameterIndex(21);
						lb.inventoryTypeGroup(34);
						lb.primaryGenus("MB");

						lb.addSpecies(sb -> {
							sb.genus("MB");
							sb.controlMap(controlMap);

							sb.percentGenus(90);

							sb.breakageGroup(12);
							sb.decayGroup(13);
							sb.volumeGroup(14);

							sb.addSp64Distribution("MB", 100);

							sb.addCompatibilityVariables(cvb -> {
								cvb.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
								cvb.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
								cvb.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
								cvb.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
							});

							sb.addSite(ib -> {
								ib.ageTotal(40);
								ib.yearsToBreastHeight(5);
								ib.height(15);
								ib.siteCurveNumber(42);
								ib.siteIndex(4);
							});

							sb.loreyHeight(mockHeightVector());

							sb.baseArea(mockUtilVector(2));
							sb.quadMeanDiameter(mockUtilVector(10));
							sb.treesPerHectare(mockUtilVector(300));

							sb.wholeStemVolume(mockUtilVector(7));
							sb.closeUtilizationVolumeByUtilization(mockUtilVector(6));
							sb.closeUtilizationVolumeNetOfDecayByUtilization(mockUtilVector(5));
							sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(mockUtilVector(4));
							sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(mockUtilVector(3));
						});

						lb.loreyHeight(mockHeightVector());

						lb.baseArea(mockUtilVector(2));
						lb.quadMeanDiameter(mockUtilVector(10));
						lb.treesPerHectare(mockUtilVector(300));

						lb.wholeStemVolume(mockUtilVector(7));
						lb.closeUtilizationVolumeByUtilization(mockUtilVector(6));
						lb.closeUtilizationVolumeNetOfDecayByUtilization(mockUtilVector(5));
						lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(mockUtilVector(4));
						lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(mockUtilVector(3));
					});
				});

				unit = VdypMatchers.deepEquals(expected);

			}

			@Test
			void testPass() {
				var actual = VdypPolygon.build(pb -> {
					pb.copy(expected);
					pb.copyLayers(expected, (lb, expectedLayer) -> {
						lb.copy(expectedLayer);
						lb.copySpecies(expectedLayer, (sb, expectedSpec) -> {
							sb.copy(expectedSpec);
							sb.copySiteFrom(expectedSpec, (ib, i) -> {
							});
							sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
							});
						});
					});
				});

				assertMatch(actual, unit);
			}

			// Changing the key properties also causes mismatches on the children that share those key properties so use
			// startsWith

			@Test
			void testPolyIdDifferent() {
				var actual = VdypPolygon.build(pb -> {
					pb.copy(expected);

					pb.polygonIdentifier("Different", 2025);

					pb.copyLayers(expected, (lb, expectedLayer) -> {
						lb.copy(expectedLayer);
						lb.copySpecies(expectedLayer, (sb, expectedSpec) -> {
							sb.copy(expectedSpec);
							sb.copySiteFrom(expectedSpec, (ib, i) -> {
							});
							sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
							});
						});
					});
				});

				assertMismatch(
						actual, unit,
						startsWith(
								"PolygonIdentifier was <Different            2025> but expected <Test                 2024>"
						)
				);
			}

			@Test
			void testBecDifferent() {
				var actual = VdypPolygon.build(pb -> {
					pb.copy(expected);

					pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));

					pb.copyLayers(expected, (lb, expectedLayer) -> {
						lb.copy(expectedLayer);
						lb.copySpecies(expectedLayer, (sb, expectedSpec) -> {
							sb.copy(expectedSpec);
							sb.copySiteFrom(expectedSpec, (ib, i) -> {
							});
							sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
							});
						});
					});
				});

				assertMismatch(
						actual, unit,
						startsWith(
								"BiogeoclimaticZone was <IDF (Interior DougFir)> but expected <CDF (Coastal Dougfir)>"
						)
				);
			}

			@Test
			void testFizDifferent() {
				var actual = VdypPolygon.build(pb -> {
					pb.copy(expected);

					pb.forestInventoryZone("A");

					pb.copyLayers(expected, (lb, expectedLayer) -> {
						lb.copy(expectedLayer);
						lb.copySpecies(expectedLayer, (sb, expectedSpec) -> {
							sb.copy(expectedSpec);
							sb.copySiteFrom(expectedSpec, (ib, i) -> {
							});
							sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
							});
						});
					});
				});

				assertMismatch(actual, unit, startsWith("ForestInventoryZone was \"A\" but expected \"Z\""));
			}

			@Test
			void testLayerDifferent() {
				var actual = VdypPolygon.build(pb -> {
					pb.copy(expected);

					pb.copyLayers(expected, (lb, expectedLayer) -> {
						lb.copy(expectedLayer);
						lb.layerType(LayerType.VETERAN);
						lb.copySpecies(expectedLayer, (sb, expectedSpec) -> {
							sb.copy(expectedSpec);
							sb.copySiteFrom(expectedSpec, (ib, i) -> {
							});
							sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
							});
						});
					});
				});

				assertMismatch(actual, unit, startsWith("Layers was <[VETERAN]> but expected <[PRIMARY]>"));
			}

		}

		@Nested
		class testVdypLayer {
			VdypLayer expected;
			Matcher<VdypLayer> unit;

			@BeforeEach
			void setup() {

				Random rand = new Random(42);
				var controlMap = TestUtils.loadControlMap();

				// The numbers don't add up, we are just using them to test comparison

				expected = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);

					lb.empiricalRelationshipParameterIndex(21);
					lb.inventoryTypeGroup(34);
					lb.primaryGenus("MB");

					lb.addSpecies(sb -> {
						sb.genus("MB");
						sb.controlMap(controlMap);

						sb.percentGenus(90);

						sb.breakageGroup(12);
						sb.decayGroup(13);
						sb.volumeGroup(14);

						sb.addSp64Distribution("MB", 100);

						sb.addCompatibilityVariables(cvb -> {
							cvb.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
							cvb.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
							cvb.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
							cvb.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
						});

						sb.addSite(ib -> {
							ib.ageTotal(40);
							ib.yearsToBreastHeight(5);
							ib.height(15);
							ib.siteCurveNumber(42);
							ib.siteIndex(4);
						});

						sb.loreyHeight(mockHeightVector());

						sb.baseArea(mockUtilVector(2));
						sb.quadMeanDiameter(mockUtilVector(10));
						sb.treesPerHectare(mockUtilVector(300));

						sb.wholeStemVolume(mockUtilVector(7));
						sb.closeUtilizationVolumeByUtilization(mockUtilVector(6));
						sb.closeUtilizationVolumeNetOfDecayByUtilization(mockUtilVector(5));
						sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(mockUtilVector(4));
						sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(mockUtilVector(3));
					});

					lb.loreyHeight(mockHeightVector());

					lb.baseArea(mockUtilVector(2));
					lb.quadMeanDiameter(mockUtilVector(10));
					lb.treesPerHectare(mockUtilVector(300));

					lb.wholeStemVolume(mockUtilVector(7));
					lb.closeUtilizationVolumeByUtilization(mockUtilVector(6));
					lb.closeUtilizationVolumeNetOfDecayByUtilization(mockUtilVector(5));
					lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(mockUtilVector(4));
					lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(mockUtilVector(3));
				});

				unit = VdypMatchers.deepEquals(expected);

			}

			@Test
			void testPass() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);
						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
				});

				assertMatch(actual, unit);
			}

			// Changing the key properties also causes mismatches on the children that share those key properties so use
			// startsWith

			@Test
			void testPolyIdDifferent() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);
						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
					lb.polygonIdentifier("Different", 2025);
				});
				assertMismatch(
						actual, unit,
						startsWith(
								"PolygonIdentifier was <Different            2025> but expected <Test                 2024>"
						)
				);
			}

			@Test
			void testLayerTypeDifferent() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);
						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
					lb.layerType(LayerType.VETERAN);
				});
				assertMismatch(actual, unit, startsWith("LayerType was <VETERAN> but expected <PRIMARY>"));
			}

			@Test
			void testEmpericalIndexDifferent() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);
						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
					lb.empiricalRelationshipParameterIndex(23);
				});
				assertMismatch(
						actual, unit,
						startsWith("EmpiricalRelationshipParameterIndex was <Optional[23]> but expected <Optional[21]>")
				);
			}

			@Test
			void testInventoryTypeGroup() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);
						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
					lb.inventoryTypeGroup(65);
				});
				assertMismatch(
						actual, unit, startsWith("InventoryTypeGroup was <Optional[65]> but expected <Optional[34]>")
				);
			}

			@Test
			void testSpeciesDifferent() {
				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);

						if (expectedSpec.getGenus().equals("MB")) {
							sb.decayGroup(3);
						}

						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
				});
				assertMismatch(
						actual, unit,
						startsWith("mismatch in species group \"MB\": DecayGroup was <3> but expected <13>")
				);
			}

			@ParameterizedTest
			@EnumSource(UtilizationClassVariable.class)
			void testUtilizationDifferent(UtilizationClassVariable ucv) throws Exception {

				var actual = VdypLayer.build(lb -> {
					lb.copy(expected);
					lb.copySpecies(expected, (sb, expectedSpec) -> {
						sb.copy(expectedSpec);

						sb.copySiteFrom(expectedSpec, (ib, i) -> {
						});
						sb.copyCompatibilityVariablesFrom(expectedSpec, (ib, cv) -> {
						});
					});
				});

				// Make a change to one entry (SMALL) in the utilization vector for the specified field

				ucv.get(actual).scalarInPlace(UtilizationClass.SMALL, x -> x + 1);

				String name = ucv.getShortName();
				String type = "a utilization vector";
				String floatPattern = "(\\d+\\.\\d+)";
				String expectedVectorPattern = "\\[Small: " + floatPattern + ", All: " + floatPattern + ", 7.5cm: "
						+ floatPattern + ", 12.5cm: " + floatPattern + ", 17.5cm: " + floatPattern + ", 22.5cm: "
						+ floatPattern + "\\]";

				String actualVectorPattern = "\\[Small: \\[\\[" + floatPattern
						+ "\\]\\], All: \\2, 7.5cm: \\3, 12.5cm: \\4, 17.5cm: \\5, 22.5cm: \\6\\]";

				if (UtilizationClassVariable.LOREY_HEIGHT == ucv) {
					type = "a lorey height vector";
					expectedVectorPattern = "\\[Small: " + floatPattern + ", All: " + floatPattern + "\\]";
					actualVectorPattern = "\\[Small: \\[\\[" + floatPattern + "\\]\\], All: \\2\\]";
				}

				assertMismatch(
						actual, unit,
						matchesRegex(
								"^" + name + " expected " + type + " \"" + expectedVectorPattern + "\" but was "
										+ actualVectorPattern + "$"
						)
				);
			}
		}

		@Nested
		class testVdypSpecies {
			VdypSpecies expected;
			Matcher<VdypSpecies> unit;

			@BeforeEach
			void setup() {
				Random rand = new Random(42);
				var controlMap = TestUtils.loadControlMap();

				// The numbers don't add up, we are just using them to test comparison

				expected = VdypSpecies.build(sb -> {
					sb.polygonIdentifier("Test", 2024);
					sb.layerType(LayerType.PRIMARY);
					sb.genus("MB");
					sb.controlMap(controlMap);

					sb.percentGenus(90);

					sb.breakageGroup(12);
					sb.decayGroup(13);
					sb.volumeGroup(14);

					sb.addSp64Distribution("MB", 100);

					sb.addCompatibilityVariables(cvb -> {
						cvb.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
						cvb.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
						cvb.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
						cvb.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
					});

					sb.addSite(ib -> {
						ib.ageTotal(40);
						ib.yearsToBreastHeight(5);
						ib.height(15);
						ib.siteCurveNumber(42);
						ib.siteIndex(4);
					});

					sb.loreyHeight(mockHeightVector());

					sb.baseArea(mockUtilVector(2));
					sb.quadMeanDiameter(mockUtilVector(10));
					sb.treesPerHectare(mockUtilVector(300));

					sb.wholeStemVolume(mockUtilVector(7));
					sb.closeUtilizationVolumeByUtilization(mockUtilVector(6));
					sb.closeUtilizationVolumeNetOfDecayByUtilization(mockUtilVector(5));
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(mockUtilVector(4));
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(mockUtilVector(3));
				});

				unit = VdypMatchers.deepEquals(expected);
			}

			@Test
			void testPass() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (ib, cv) -> {
					});
				});

				assertMatch(actual, unit);
			}

			// Changing the key properties also causes mismatches on the children that share those key properties so use
			// startsWith

			@Test
			void testPolyIdDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.polygonIdentifier("Different", 2025);
				});

				assertMismatch(
						actual, unit,
						startsWith(
								"PolygonIdentifier was <Different            2025> but expected <Test                 2024>"
						)
				);
			}

			@Test
			void testLayerDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.layerType(LayerType.VETERAN);
				});

				assertMismatch(actual, unit, startsWith("LayerType was <VETERAN> but expected <PRIMARY>"));
			}

			@Test
			void testSpeciesGroupDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.genus("S");
				});

				assertMismatch(actual, unit, startsWith("Genus was \"S\" but expected \"MB\""));
			}

			@Test
			void testPercentDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.percentGenus(89);
				});

				assertMismatch(actual, unit, equalTo("PercentGenus was <89.0F> but expected <90.0F>"));
			}

			@Test
			void testBreakageGroupDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.breakageGroup(22);
				});

				assertMismatch(actual, unit, equalTo("BreakageGroup was <22> but expected <12>"));
			}

			@Test
			void testDecayGroupDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.decayGroup(23);
				});

				assertMismatch(actual, unit, equalTo("DecayGroup was <23> but expected <13>"));
			}

			@Test
			void testVolumeGroupDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});

					sb.volumeGroup(24);
				});

				assertMismatch(actual, unit, equalTo("VolumeGroup was <24> but expected <14>"));
			}

			@Test
			void testSp64DistributionDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});
					sb.sp64DistributionList(List.of());
					sb.addSp64Distribution("S", 70);
					sb.addSp64Distribution("F", 30);
				});

				assertMismatch(
						actual, unit,
						equalTo("Sp64DistributionSet was <[S[1]:70.0, F[2]:30.0]> but expected <[MB[1]:100.0]>")
				);
			}

			@Test
			void testCompatibilityVariablesDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
						cvb.cvBasalArea((k1, k2) -> cv.getCvBasalArea(k1, k2) + 1);
					});
				});

				assertMismatch(
						actual, unit,
						matchesRegex(
								"mismatch in Compatibility Variables \"MB\": CvBasalArea at \\[<U75TO125>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<\\d\\.\\d+F>\\) and there were \\d+ other mismatches"
						)
				);
			}

			@Test
			void testSiteDifferent() {
				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
						ib.height(22);
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});
				});

				assertMismatch(
						actual, unit,
						equalTo("mismatch in site \"MB\": Height was <Optional[22.0]> but expected <Optional[15.0]>")
				);
			}

			@ParameterizedTest
			@EnumSource(UtilizationClassVariable.class)
			void testUtilizationDifferent(UtilizationClassVariable ucv) throws Exception {

				var actual = VdypSpecies.build(sb -> {
					sb.copy(expected);
					sb.copySiteFrom(expected, (ib, i) -> {
					});
					sb.copyCompatibilityVariablesFrom(expected, (cvb, cv) -> {
					});
				});

				// Make a change to one entry (SMALL) in the utilization vector for the specified field

				ucv.get(actual).scalarInPlace(UtilizationClass.SMALL, x -> x + 1);

				String name = ucv.getShortName();
				String type = "a utilization vector";
				String floatPattern = "(\\d+\\.\\d+)";
				String expectedVectorPattern = "\\[Small: " + floatPattern + ", All: " + floatPattern + ", 7.5cm: "
						+ floatPattern + ", 12.5cm: " + floatPattern + ", 17.5cm: " + floatPattern + ", 22.5cm: "
						+ floatPattern + "\\]";

				String actualVectorPattern = "\\[Small: \\[\\[" + floatPattern
						+ "\\]\\], All: \\2, 7.5cm: \\3, 12.5cm: \\4, 17.5cm: \\5, 22.5cm: \\6\\]";

				if (UtilizationClassVariable.LOREY_HEIGHT == ucv) {
					type = "a lorey height vector";
					expectedVectorPattern = "\\[Small: " + floatPattern + ", All: " + floatPattern + "\\]";
					actualVectorPattern = "\\[Small: \\[\\[" + floatPattern + "\\]\\], All: \\2\\]";
				}

				assertMismatch(
						actual, unit,
						matchesRegex(
								"^" + name + " expected " + type + " \"" + expectedVectorPattern + "\" but was "
										+ actualVectorPattern + "$"
						)
				);
			}

		}

		@Nested
		class testVdypCompatibilityVariables {
			VdypCompatibilityVariables expected;
			Matcher<VdypCompatibilityVariables> unit;

			@BeforeEach
			void setup() {
				// Use a fixed seed so the random numbers are the same across runs.
				Random rand = new Random(42);

				expected = VdypCompatibilityVariables.build(cvb -> {
					cvb.polygonIdentifier("Test", 2024);
					cvb.layerType(LayerType.PRIMARY);
					cvb.genus("MB");
					cvb.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
					cvb.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
					cvb.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
					cvb.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
				});

				unit = VdypMatchers.deepEquals(expected);
			}

			@Test
			void testPass() {
				var actual = VdypCompatibilityVariables.build(cvb -> {
					cvb.copy(expected);
				});

				assertMatch(actual, unit);
			}

			@Test
			void testOneVolumeEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(cvb -> {
					cvb.copy(expected);
				});

				actual.getCvVolume().put(
						UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 20f
				);

				assertMismatch(
						actual, unit,
						matchesRegex(
								"CvVolume at \\[<U125TO175>, <CLOSE_UTIL_VOL>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneBaEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(cvb -> {
					cvb.copy(expected);
				});

				actual.getCvBasalArea().put(UtilizationClass.U125TO175, LayerType.PRIMARY, 20f);

				assertMismatch(
						actual, unit,
						matchesRegex(
								"CvBasalArea at \\[<U125TO175>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneDqEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(cvb -> {
					cvb.copy(expected);
				});

				actual.getCvQuadraticMeanDiameter().put(UtilizationClass.U125TO175, LayerType.PRIMARY, 20f);

				assertMismatch(
						actual, unit,
						matchesRegex(
								"CvQuadraticMeanDiameter at \\[<U125TO175>, <PRIMARY>\\] expected <\\d\\.\\d+F> but it was a java.lang.Float \\(<20.0F>\\)"
						)
				);
			}

			@Test
			void testOneSmallEntryDifferent() {
				var actual = VdypCompatibilityVariables.build(cvb -> {
					cvb.copy(expected);
				});

				actual.getCvPrimaryLayerSmall().put(UtilizationClassVariable.BASAL_AREA, 20f);

				assertMismatch(
						actual, unit,
						matchesRegex(
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
						actual, unit,
						equalTo(
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

				assertMismatch(actual, unit, equalTo("LayerType was <VETERAN> but expected <PRIMARY>"));
			}

			@Test
			void testSpeciesGroupDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.genus("B");
				});

				assertMismatch(actual, unit, equalTo("SpeciesGroup was \"B\" but expected \"MB\""));
			}

			@Test
			void testHeightDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.height(16);
				});

				assertMismatch(actual, unit, equalTo("Height was <Optional[16.0]> but expected <Optional[15.0]>"));
			}

			@Test
			void testAgeTotalDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.ageTotal(45);
				});

				// Years at breast height is computed from AgeTotal and YearstToBreastHeight
				assertMismatch(
						actual, unit,
						equalTo(
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
						actual, unit,
						equalTo(
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

				assertMismatch(actual, unit, equalTo("SiteCurveNumber was <Optional[41]> but expected <Optional[42]>"));
			}

			@Test
			void testSiteIndexDifferent() {
				var actual = VdypSite.build(ib -> {
					ib.copy(expected);
					ib.siteIndex(3);
				});

				assertMismatch(actual, unit, equalTo("SiteIndex was <Optional[3.0]> but expected <Optional[4.0]>"));
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
					test, unit,
					equalTo("at [<1>, <4>] expected \"1:4\" but it was \"DIFFERENT\" and there were 1 other mismatches")
			);
		}

		@Test
		void testDifferentDimensionality() {
			MatrixMap<String> test3 = new MatrixMap3Impl<Integer, Integer, Integer, String>(
					List.of(1), List.of(2), List.of(3), (k1, k2, k3) -> "TEST"
			);
			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(test3, unit, equalTo("matrix map had 3 dimensions but expected 2"));
		}

		@Test
		void testDimensionMissingValue() {
			test = new MatrixMap2Impl<>(List.of(1, 2), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2));

			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit,
					equalTo("matrix map had dimensions [[1, 2], [4, 5, 6]] but expected [[1, 2, 3], [4, 5, 6]]")
			);
		}

		@Test
		void testDimensionExtraValue() {
			test = new MatrixMap2Impl<>(
					List.of(0, 1, 2, 3), List.of(4, 5, 6), (k1, k2) -> String.format("%d:%d", k1, k2)
			);

			var unit = VdypMatchers.mmEquals((MatrixMap<String>) expected, s -> Matchers.equalTo(s));
			assertMismatch(
					test, unit,
					equalTo("matrix map had dimensions [[0, 1, 2, 3], [4, 5, 6]] but expected [[1, 2, 3], [4, 5, 6]]")
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
					test, unit,
					matchesRegex(
							"at <\\d> expected \"Value \\d\" but it was \"DIFFERENT\" and there were 1 other mismatches"
					)
			);
		}

		@Test
		void testMissingEntry() {
			test.remove(1);

			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, equalTo("expected keys <[1, 2]> but were <[2]>"));
		}

		@Test
		void testExtraEntry() {
			test.put(42, "EXTRA");

			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, equalTo("expected keys <[1, 2]> but were <[1, 2, 42]>"));
		}

		@Test
		void testBothEmpty() {
			expected = new HashMap<>();
			test = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMatch(test, unit);
		}

		@Test
		void testExpectedEmpty() {
			expected = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, equalTo("map size was <2>"));
		}

		@Test
		void testActualEmpty() {
			test = new HashMap<>();
			var unit = VdypMatchers.mapEquals(expected, s -> Matchers.equalTo(s));
			assertMismatch(test, unit, equalTo("map was empty"));
		}

	}

}
