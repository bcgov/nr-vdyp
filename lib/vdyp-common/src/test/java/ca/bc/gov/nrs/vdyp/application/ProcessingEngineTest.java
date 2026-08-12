package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.OVER225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U125TO175;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U175TO225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U75TO125;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.BASAL_AREA;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.LOREY_HEIGHT;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.QUAD_MEAN_DIAMETER;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.WHOLE_STEM_VOLUME;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.WHOLE_STEM_VOL;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.asFloat;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasMessage;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.unboxedArrayCloseTo;
import static org.easymock.EasyMock.capture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine.AgeTriplet;
import ca.bc.gov.nrs.vdyp.application.ProcessingEngine.SpeciesToApplyTo;
import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.PrimarySpeciesDetails;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingStateTestUtils;
import ca.bc.gov.nrs.vdyp.processing_state.SpeciesRankingDetails;
import ca.bc.gov.nrs.vdyp.processing_state.TestLayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.TestProcessingState;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.LessThan13Exception;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.sindex.exceptions.SpeciesErrorException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class ProcessingEngineTest {
	@Nested
	class CalculateMissingSiteCurve {
		@Test
		void testDistPresentAndMapEmpty() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.of(new Sp64Distribution(1, "BL", 100f));
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, present(is(SiteIndexEquation.SI_BA_NIGH)));
			control.verify();
		}

		@Test
		void testDistMissingAndMapEmpty() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.empty();
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, present(is(SiteIndexEquation.SI_BA_NIGH)));
			control.verify();
		}

		@Test
		void testDistMissingAndMapEmptyCurveUnknown() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.empty();
			final String speciesId = null;

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, notPresent());
			control.verify();
		}

		@Test
		void testDistPresentAndMapPresentWithCurve() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.of(new Sp64Distribution(1, "BL", 100f));
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("BL", region)).andStubReturn(SiteIndexEquation.SI_BL_CHEN);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, present(is(SiteIndexEquation.SI_BL_CHEN)));
			control.verify();
		}

		@Test
		void testDistMissingAndMapMapPresentWithCurve() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.empty();
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("B", region)).andStubReturn(SiteIndexEquation.SI_BA_DILUCCA);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, present(is(SiteIndexEquation.SI_BA_DILUCCA)));
			control.verify();
		}

		@Test
		void testDistPresentAndMapPresentWithoutCurveFallbackSucceeds() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.of(new Sp64Distribution(1, "BL", 100f));
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("BL", region)).andStubReturn(null);
			EasyMock.expect(siteCurveMap.get("B", region)).andStubReturn(SiteIndexEquation.SI_BA_DILUCCA);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, present(is(SiteIndexEquation.SI_BA_DILUCCA)));
			control.verify();
		}

		@Test
		void testDistPresentAndMapPresentWithoutCurveFallbackFails() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.of(new Sp64Distribution(1, "BL", 100f));
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("BL", region)).andStubReturn(null);
			EasyMock.expect(siteCurveMap.get("B", region)).andStubReturn(null);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, notPresent());
			control.verify();
		}

		@Test
		void testDistMissingAndMapMapPresentWithoutCurve() {
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			Region region = Region.COASTAL;
			Optional<Sp64Distribution> sp0Dist = Optional.empty();
			final String speciesId = "B";

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("B", region)).andStubReturn(null);

			control.replay();

			var result = ProcessingEngine.calculateMissingSiteCurve(siteCurveMap, region, sp0Dist, speciesId);
			assertThat(result, notPresent());
			control.verify();
		}

		@Test
		void testBankWithoutValue() {
			var controlMap = TestUtils.loadControlMap();
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Blah", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("");
				pb.controlMap(controlMap);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
					});
				});
			});
			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);
			control.replay();

			VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

			Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

			ProcessingEngine.calculateMissingSiteCurves(bank, siteCurveMap);

			assertArrayEquals(
					bank.siteCurveNumbers, new int[] { 0, SiteIndexEquation.SI_BA_NIGH.n() }, "siteCurveNumbers"

			);
			control.verify();
		}

		@Test
		void testBankWithValue() {
			var controlMap = TestUtils.loadControlMap();
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Blah", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("");
				pb.controlMap(controlMap);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
					});
				});
			});
			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);
			control.replay();

			VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

			Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);
			bank.siteCurveNumbers[1] = SiteIndexEquation.SI_BA_DILUCCA.n();

			ProcessingEngine.calculateMissingSiteCurves(bank, siteCurveMap);

			assertArrayEquals(
					bank.siteCurveNumbers, new int[] { 0, SiteIndexEquation.SI_BA_DILUCCA.n() }, "siteCurveNumbers"
			);
			control.verify();
		}

		@Test
		void testBankUseDistributionIfMapIsPresent() {
			var controlMap = TestUtils.loadControlMap();
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Blah", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("");
				pb.controlMap(controlMap);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.addSp64Distribution("BL", 100);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
					});
				});
			});
			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(false);
			EasyMock.expect(siteCurveMap.get("BL", Region.COASTAL)).andStubReturn(SiteIndexEquation.SI_BL_CHEN);
			control.replay();

			VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

			Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

			ProcessingEngine.calculateMissingSiteCurves(bank, siteCurveMap);

			assertArrayEquals(
					bank.siteCurveNumbers, new int[] { 0, SiteIndexEquation.SI_BL_CHEN.n() }, "siteCurveNumbers"

			);
			control.verify();
		}

	}

	@Test
	void testLps() {
		var controlMap = TestUtils.loadControlMap();
		final var control = EasyMock.createControl();
		MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
		LayerProcessingState<?> lps = control.createMock(LayerProcessingState.class);
		var polygon = VdypPolygon.build(pb -> {
			pb.polygonIdentifier("Blah", 2025);
			pb.percentAvailable(90f);
			pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
			pb.forestInventoryZone("");
			pb.controlMap(controlMap);
			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.speciesGroup("B");
					sb.addSp64Distribution("BL", 100);
					sb.addSite(ib -> {
						ib.yearsToBreastHeight(6f);
						ib.ageTotal(250f);
						ib.height(20f);
					});
				});
			});
		});

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

		EasyMock.expect(lps.getBank()).andStubReturn(bank);

		lps.setSiteCurveNumbers(new int[] { 0, SiteIndexEquation.SI_BA_NIGH.n() });
		EasyMock.expectLastCall().once();

		control.replay();

		ProcessingEngine.calculateMissingSiteCurves(lps, siteCurveMap);

		control.verify();
	}

	@Nested
	class CalculateCoverages {
		@Test
		void testOneSpeces() {
			var controlMap = TestUtils.loadControlMap();
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			LayerProcessingState<?> lps = control.createMock(LayerProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Blah", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("");
				pb.controlMap(controlMap);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.addSp64Distribution("BL", 100);
						sb.percentGenus(100);
						sb.baseArea(10);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
					});
				});
			});

			VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

			Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

			EasyMock.expect(lps.getBank()).andStubReturn(bank);
			EasyMock.expect(lps.getNSpecies()).andStubReturn(1);
			EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1 });

			control.replay();

			ProcessingEngine.calculateCoverages(lps);

			assertThat(lps.getBank().percentagesOfForestedLand[1], closeTo(100f));

			control.verify();
		}

		@Test
		void testTwoSpeces() {
			var controlMap = TestUtils.loadControlMap();
			final var control = EasyMock.createControl();
			MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap = control.createMock(MatrixMap2.class);
			LayerProcessingState<?> lps = control.createMock(LayerProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Blah", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("");
				pb.controlMap(controlMap);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.addSp64Distribution("BL", 70);
						sb.baseArea(60);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
						sb.percentGenus(70);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.addSp64Distribution("S", 30);
						sb.baseArea(40);
						sb.addSite(ib -> {
							ib.yearsToBreastHeight(6f);
							ib.ageTotal(250f);
							ib.height(20f);
						});
						sb.percentGenus(30);
					});
				});
			});

			VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);

			Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

			EasyMock.expect(siteCurveMap.isEmpty()).andStubReturn(true);

			EasyMock.expect(lps.getBank()).andStubReturn(bank);
			EasyMock.expect(lps.getNSpecies()).andStubReturn(2);
			EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2 });

			control.replay();

			ProcessingEngine.calculateCoverages(lps);

			// Percentage should be based on BA as a proportion of total
			assertThat(lps.getBank().percentagesOfForestedLand[1], closeTo(60f));
			assertThat(lps.getBank().percentagesOfForestedLand[2], closeTo(40f));

			control.verify();
		}
	}

	@Nested
	class EstimateSiteIndexAndAge {
		private static final int PRIMARY_SITE_CURVE_NUMBER = 99;
		private static final int SECONDARY_SITE_CURVE_NUMBER = 12;
		private static final int TERTIARY_SITE_CURVE_NUMBER = 11;
		private static final int UC_ALL_INDEX = UtilizationClass.ALL.ordinal();

		Map<String, Object> controlMap;
		ProcessingDebugSettings fds;

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ProcessingControlParser();
			controlMap = TestUtils.loadControlMap(parser, TestUtils.class, "VDYP.CTR");
			controlMap.put(ControlKey.VTROL.name(), new ProcessingControlVariables(new Integer[] {}));
			fds = new ProcessingDebugSettings(new Integer[25]);
		}

		@Nested
		class Simple {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Test
			void testPrimarySitePresent() throws Exception {
				var em = EasyMock.createControl();
				ProcessingState<?> ps = em.mock(ProcessingState.class);
				LayerProcessingState<?> lps = em.mock(LayerProcessingState.class);

				var layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 1970);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(13.825554f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("D");
						sb.percentGenus(6.479738f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(78.74126f);
						sb.addSite(ib -> {
							ib.ageTotal(45.0f);
							ib.height(24.3f);
							ib.siteCurveNumber(34);
							ib.siteIndex(28.7f);
							ib.yearsAtBreastHeight(39.6f);
							ib.yearsToBreastHeight(5.4f);
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(0.9534709f);
						sb.addSite(ib -> {
							// Empty
						});
					});

					lb.primaryGenus("H");

				});
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = new Bank(layer, bec, x -> true);

				EasyMock.expect(lps.getBank()).andStubReturn(bank);
				EasyMock.expect(lps.getPrimarySpeciesIndex()).andStubReturn(3);
				EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2, 3, 4 });
				EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(118);
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(13);
				EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(34);
				EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(59);

				em.replay();

				new ProcessingEngine(ps).estimateMissingSiteIndices(lps);

				assertThat(
						bank.siteIndices,
						VdypMatchers.unboxedArrayCloseTo(27.923136f, 26.326431f, Float.NaN, 28.7f, 27.923136f)
				);

				em.verify();

			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Test
			void testPrimarySiteMissing() throws Exception {
				var em = EasyMock.createControl();
				ProcessingState<?> ps = em.mock(ProcessingState.class);
				LayerProcessingState<?> lps = em.mock(LayerProcessingState.class);

				var layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 1970);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(0.89672107f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(11.230089f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("D");
						sb.percentGenus(65.21433f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(12.9306135f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(9.728239f);
						sb.addSite(ib -> {
							// Empty
						});
					});

				});
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = new Bank(layer, bec, x -> true);
				bank.siteIndices[2] = 13.4f;

				EasyMock.expect(lps.getBank()).andStubReturn(bank);
				EasyMock.expect(lps.getPrimarySpeciesIndex()).andStubReturn(3);
				EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2, 3, 4, 5 });
				EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(118);
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(11);
				EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(99);
				EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(99);
				EasyMock.expect(lps.getSiteCurveNumber(5)).andStubReturn(59);

				em.replay();

				new ProcessingEngine(ps).estimateMissingSiteIndices(lps);

				assertThat(
						bank.siteIndices,
						VdypMatchers.unboxedArrayCloseTo(11.424034f, 13.110651f, 13.4f, 15.2992f, Float.NaN, 11.424034f)
				);

				em.verify();

			}

			@Test
			void testPrimarySiteEstimateFailed() throws Exception {
				var em = EasyMock.createControl();
				ProcessingState<?> ps = em.mock(ProcessingState.class);
				LayerProcessingState<?> lps = em.mock(LayerProcessingState.class);

				var layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 1970);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(0.89672107f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(11.230089f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("D");
						sb.percentGenus(65.21433f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(12.9306135f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(9.728239f);
						sb.addSite(ib -> {
							// Empty
						});
					});

				});
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = new Bank(layer, bec, x -> true);

				EasyMock.expect(lps.getBank()).andStubReturn(bank);
				EasyMock.expect(lps.getPrimarySpeciesIndex()).andStubReturn(3);
				EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2, 3, 4, 5 });
				EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(118);
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(11);
				EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(99);
				EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(99);
				EasyMock.expect(lps.getSiteCurveNumber(5)).andStubReturn(59);

				em.replay();

				@SuppressWarnings({ "unchecked", "rawtypes" })
				Float result = new ProcessingEngine(ps).estimateMissingNonPrimarySiteIndices(lps, 2, null);

				assertThat(result, asFloat(notANumber()));

				assertThat(
						bank.siteIndices,
						VdypMatchers.unboxedArrayCloseTo(0.0f, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN)
				);

				em.verify();

			}

			@Nested
			class ConvertSite {
				IMocksControl em;
				@SuppressWarnings("rawtypes")
				ProcessingEngine unit;
				ProcessingState<?> ps;

				@BeforeEach
				void setup() {
					em = EasyMock.createControl();
					ps = em.createMock(ProcessingState.class);
					unit = EasyMock.partialMockBuilder(ProcessingEngine.class) //
							.addMockedMethod("convertSiteIndexBetweenCurves")//
							.withConstructor(ps)//
							.createMock(em);

				}

				@Test
				void testSuccess() throws Exception {

					unit.convertSiteIndexBetweenCurves(
							SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
					);
					EasyMock.expectLastCall().andStubReturn(64.0);

					em.replay();

					@SuppressWarnings("unchecked")
					Optional<Float> result = assertDoesNotThrow(
							() -> unit.convertSiteIndex(
									SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
							)
					);
					assertThat(result, present(closeTo(64.0f)));

					em.verify();
				}

				@Test
				void testNoAnswer() throws Exception {

					unit.convertSiteIndexBetweenCurves(
							SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
					);
					EasyMock.expectLastCall().andThrow(new NoAnswerException());

					em.replay();

					@SuppressWarnings("unchecked")
					Optional<Float> result = assertDoesNotThrow(
							() -> unit.convertSiteIndex(
									SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
							)
					);
					assertThat(result, notPresent());

					em.verify();
				}

				@Test
				void testCurveError() throws Exception {

					unit.convertSiteIndexBetweenCurves(
							SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
					);
					final CurveErrorException cause = new CurveErrorException();
					EasyMock.expectLastCall().andThrow(cause);

					em.replay();

					var ex = assertThrows(
							ProcessingException.class,
							() -> unit.convertSiteIndex(
									SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
							)
					);
					assertThat(ex, causedBy(sameInstance(cause)));

					em.verify();
				}

				@Test
				void testSpeciesError() throws Exception {

					unit.convertSiteIndexBetweenCurves(
							SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
					);
					final SpeciesErrorException cause = new SpeciesErrorException();
					EasyMock.expectLastCall().andThrow(cause);

					em.replay();

					var ex = assertThrows(
							ProcessingException.class,
							() -> unit.convertSiteIndex(
									SiteIndexEquation.SI_ACB_HUANG, 42.0, SiteIndexEquation.SI_ACT_THROWER
							)
					);
					assertThat(ex, causedBy(sameInstance(cause)));

					em.verify();
				}

			}
		}

		@Nested
		class Extended {

			@Test
			void testChoice1MovesSiteIndexFromSecondarySpeciesToPrimarySpecies() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				float sourceSiteIndex = 13.4f;
				// uses real expecterd value later
				float expected = Float.NaN; // current behavior is dropping this value but that is ok to fix when we
											// know
											// we have matched VDYP7

				fixture.bank.siteIndices[primarySlot] = Float.NaN;
				fixture.bank.siteIndices[secondarySlot] = sourceSiteIndex;
				fixture.bank.siteIndices[fixture.slot("H")] = 16.1f;

				setChoices(1);
				assertThrows(ProcessingException.class, () -> runEstimateExtended(fixture));

				assertThat(fixture.bank.siteIndices[primarySlot], is(expected));
			}

			@Test
			void testChoice15OnlyMovesSiteIndexWhenPrimarySpeciesAgeIsUnderThirty() throws Exception {
				var applies = createFixture();
				int appliesPrimarySlot = applies.slot("F");
				int appliesSecondarySlot = applies.slot("L");
				float appliesSourceSiteIndex = 13.4f;

				applies.bank.siteIndices[appliesPrimarySlot] = Float.NaN;
				applies.bank.siteIndices[appliesSecondarySlot] = appliesSourceSiteIndex;
				applies.bank.ageTotals[appliesPrimarySlot] = 25.0f;

				setChoices(15);

				assertThrows(ProcessingException.class, () -> runEstimateExtended(applies));

				var skipped = createFixture();
				int skippedPrimarySlot = skipped.slot("F");
				int skippedSecondarySlot = skipped.slot("L");
				float originalPrimarySiteIndex = 19.0f;

				skipped.bank.siteIndices[skippedPrimarySlot] = originalPrimarySiteIndex;
				skipped.bank.siteIndices[skippedSecondarySlot] = appliesSourceSiteIndex;
				skipped.bank.ageTotals[skippedPrimarySlot] = 30.0f;

				setChoices(15);
				runEstimateExtended(skipped);

				assertThat(skipped.bank.siteIndices[skippedPrimarySlot], closeTo(originalPrimarySiteIndex));
			}

			@Test
			void testChoice2MovesPrimarySiteIndexToAllMissingNonPrimarySpecies() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				float primarySiteIndex = 24.25f;
				float expectedSecondary = (float) SiteTool.convertSiteIndexBetweenCurves(
						SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(primarySlot)), primarySiteIndex,
						SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(secondarySlot))
				);

				fixture.bank.siteIndices[primarySlot] = primarySiteIndex;
				fixture.bank.siteIndices[secondarySlot] = Float.NaN;
				fixture.bank.siteIndices[fixture.slot("H")] = 17.8f;

				setChoices(2);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.siteIndices[secondarySlot], closeTo(expectedSecondary));
			}

			@Test
			void testChoice3FillsMissingAgeTripletValues() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				int tertiarySlot = fixture.slot("H");

				fixture.bank.ageTotals[primarySlot] = Float.NaN;
				fixture.bank.yearsAtBreastHeight[primarySlot] = 30.0f;
				fixture.bank.yearsToBreastHeight[primarySlot] = 7.0f;

				fixture.bank.ageTotals[secondarySlot] = 40.0f;
				fixture.bank.yearsAtBreastHeight[secondarySlot] = Float.NaN;
				fixture.bank.yearsToBreastHeight[secondarySlot] = 5.0f;

				fixture.bank.ageTotals[tertiarySlot] = 35.0f;
				fixture.bank.yearsAtBreastHeight[tertiarySlot] = 28.0f;
				fixture.bank.yearsToBreastHeight[tertiarySlot] = Float.NaN;

				setChoices(3);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.ageTotals[primarySlot], closeTo(37.0f));
				assertThat(fixture.bank.yearsAtBreastHeight[secondarySlot], closeTo(35.0f));
				assertThat(fixture.bank.yearsToBreastHeight[tertiarySlot], closeTo(7.0f));
			}

			@Test
			void testChoice4MovesTotalAgeFromSecondarySpeciesFirst() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");

				fixture.bank.ageTotals[primarySlot] = Float.NaN;
				fixture.bank.ageTotals[secondarySlot] = 48.0f;
				fixture.bank.ageTotals[fixture.slot("H")] = 62.0f;

				setChoices(4);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.ageTotals[primarySlot], closeTo(48.0f));
			}

			@Test
			void testChoice4FallsBackToAnotherSpeciesWhenSecondaryAgeIsMissing() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				int tertiarySlot = fixture.slot("H");

				fixture.bank.ageTotals[primarySlot] = Float.NaN;
				fixture.bank.ageTotals[secondarySlot] = Float.NaN;
				fixture.bank.ageTotals[tertiarySlot] = 63.0f;

				setChoices(4);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.ageTotals[primarySlot], closeTo(63.0f));
			}

			@Test
			void testChoice5EstimatesDominantHeightForPrimarySpeciesOnly() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				float expected = fixture.lps.getParent().estimators.estimateLeadHeightFromPrimaryHeight(
						fixture.bank.loreyHeights[primarySlot][UC_ALL_INDEX], fixture.bank.speciesNames[primarySlot],
						fixture.lps.getBecZone().getRegion(), fixture.bank.treesPerHectare[primarySlot][UC_ALL_INDEX]
				);

				fixture.bank.dominantHeights[primarySlot] = Float.NaN;
				fixture.bank.dominantHeights[secondarySlot] = Float.NaN;

				setChoices(5);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.dominantHeights[primarySlot], closeTo(expected));
				assertThat(Float.isNaN(fixture.bank.dominantHeights[secondarySlot]), is(true));
			}

			@Test
			void testChoice6EstimatesDominantHeightForNonPrimarySpeciesOnly() throws Exception {
				var fixture = createFixture();
				int primarySlot = fixture.slot("F");
				int secondarySlot = fixture.slot("L");
				float expected = fixture.lps.getParent().estimators.estimateLeadHeightFromPrimaryHeight(
						fixture.bank.loreyHeights[secondarySlot][UC_ALL_INDEX],
						fixture.bank.speciesNames[secondarySlot], fixture.lps.getBecZone().getRegion(),
						fixture.bank.treesPerHectare[secondarySlot][UC_ALL_INDEX]
				);

				fixture.bank.dominantHeights[primarySlot] = Float.NaN;
				fixture.bank.dominantHeights[secondarySlot] = Float.NaN;

				setChoices(6);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.dominantHeights[secondarySlot], closeTo(expected));
				assertThat(Float.isNaN(fixture.bank.dominantHeights[primarySlot]), is(true));
			}

			@Test
			void testChoice7EstimatesPrimarySiteIndexFromHeightAndTotalAge() throws Exception {
				assertChoiceEstimatesSiteIndexFromHeightAndAge(7, "F", SiteIndexAgeType.SI_AT_TOTAL);
			}

			@Test
			void testChoice8EstimatesPrimarySiteIndexFromHeightAndBreastHeightAge() throws Exception {
				assertChoiceEstimatesSiteIndexFromHeightAndAge(8, "F", SiteIndexAgeType.SI_AT_BREAST);
			}

			@Test
			void testChoice9EstimatesNonPrimarySiteIndexFromHeightAndTotalAge() throws Exception {
				assertChoiceEstimatesSiteIndexFromHeightAndAge(9, "L", SiteIndexAgeType.SI_AT_TOTAL);
			}

			@Test
			void testChoice10EstimatesNonPrimarySiteIndexFromHeightAndBreastHeightAge() throws Exception {
				assertChoiceEstimatesSiteIndexFromHeightAndAge(10, "L", SiteIndexAgeType.SI_AT_BREAST);
			}

			@Test
			void testChoice11EstimatesPrimarySpeciesAgesFromHeightAndSiteIndex() throws Exception {
				assertChoiceEstimatesAgesFromHeightAndSiteIndex(11, "F");
			}

			@Test
			void testChoice12EstimatesNonPrimarySpeciesAgesFromHeightAndSiteIndex() throws Exception {
				assertChoiceEstimatesAgesFromHeightAndSiteIndex(12, "L");
			}

			@Test
			void testChoice13EstimatesYearsToBreastHeightForPrimarySpecies() throws Exception {
				assertChoiceEstimatesYearsToBreastHeight(13, "F");
			}

			@Test
			void testChoice14EstimatesYearsToBreastHeightForNonPrimarySpecies() throws Exception {
				assertChoiceEstimatesYearsToBreastHeight(14, "L");
			}

			private void
					assertChoiceEstimatesSiteIndexFromHeightAndAge(int choice, String genus, SiteIndexAgeType ageType)
							throws Exception {
				var fixture = createFixture();
				int slot = fixture.slot(genus);
				SiteIndexEquation curve = SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(slot));
				float inputSiteIndex = 22.4f;
				float inputYtbh = yearsToBreastHeight(curve, inputSiteIndex);
				float age = ageType == SiteIndexAgeType.SI_AT_TOTAL ? 55.0f : 47.0f;
				float height = (float) SiteTool.ageAndSiteIndexToHeight(curve, age, ageType, inputSiteIndex, inputYtbh);
				float expectedSiteIndex = (float) SiteTool.heightAndAgeToSiteIndex(
						curve, age, ageType, height,
						ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEstimationType.SI_EST_DIRECT
				);
				float expectedYtbh = yearsToBreastHeight(curve, expectedSiteIndex);

				fixture.bank.siteIndices[slot] = Float.NaN;
				fixture.bank.dominantHeights[slot] = height;
				fixture.bank.yearsToBreastHeight[slot] = Float.NaN;

				if (ageType == SiteIndexAgeType.SI_AT_TOTAL) {
					fixture.bank.ageTotals[slot] = age;
					fixture.bank.yearsAtBreastHeight[slot] = age - expectedYtbh;
				} else {
					fixture.bank.ageTotals[slot] = Float.NaN;
					fixture.bank.yearsAtBreastHeight[slot] = age;
				}

				setChoices(choice);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.siteIndices[slot], closeTo(expectedSiteIndex));
				assertThat(fixture.bank.yearsToBreastHeight[slot], closeTo(expectedYtbh));
			}

			private void assertChoiceEstimatesAgesFromHeightAndSiteIndex(int choice, String genus) throws Exception {
				var fixture = createFixture();
				int slot = fixture.slot(genus);
				SiteIndexEquation curve = SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(slot));
				float siteIndex = 21.8f;
				float expectedYtbh = yearsToBreastHeight(curve, siteIndex);
				float inputTotalAge = 52.0f;
				float height = (float) SiteTool.ageAndSiteIndexToHeight(
						curve, inputTotalAge, SiteIndexAgeType.SI_AT_TOTAL, siteIndex, expectedYtbh
				);
				float expectedBreastHeightAge = (float) SiteTool
						.heightAndSiteIndexToAge(curve, height, SiteIndexAgeType.SI_AT_BREAST, siteIndex, expectedYtbh);
				float expectedTotalAge = (float) SiteTool
						.heightAndSiteIndexToAge(curve, height, SiteIndexAgeType.SI_AT_TOTAL, siteIndex, expectedYtbh);

				fixture.bank.siteIndices[slot] = siteIndex;
				fixture.bank.dominantHeights[slot] = height;
				fixture.bank.ageTotals[slot] = Float.NaN;
				fixture.bank.yearsAtBreastHeight[slot] = Float.NaN;
				fixture.bank.yearsToBreastHeight[slot] = expectedYtbh;

				setChoices(choice);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.ageTotals[slot], closeTo(expectedTotalAge));
				assertThat(fixture.bank.yearsAtBreastHeight[slot], closeTo(expectedBreastHeightAge));
			}

			private void assertChoiceEstimatesYearsToBreastHeight(int choice, String genus) throws Exception {
				var fixture = createFixture();
				int slot = fixture.slot(genus);
				SiteIndexEquation curve = SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(slot));
				float siteIndex = 20.6f;
				float expectedYtbh = yearsToBreastHeight(curve, siteIndex);

				fixture.bank.siteIndices[slot] = siteIndex;
				fixture.bank.yearsToBreastHeight[slot] = Float.NaN;

				setChoices(choice);
				runEstimateExtended(fixture);

				assertThat(fixture.bank.yearsToBreastHeight[slot], closeTo(expectedYtbh));
			}

			private LayerFixture createFixture() throws ProcessingException {
				var polygon = VdypPolygon.build(pb -> {
					pb.polygonIdentifier("EstimateFixture", 2025);
					pb.percentAvailable(90f);
					pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
					pb.forestInventoryZone("");
					pb.controlMap(controlMap);

					pb.addLayer(lb -> {
						lb.layerType(LayerType.PRIMARY);
						lb.primaryGenus("F");
						addSpecies(lb, "F", PRIMARY_SITE_CURVE_NUMBER, 45.0f, 19.5f, 24.0f, 50.0f, 7.0f, 43.0f);
						addSpecies(lb, "L", SECONDARY_SITE_CURVE_NUMBER, 35.0f, 18.2f, 22.0f, 48.0f, 6.0f, 42.0f);
						addSpecies(lb, "H", TERTIARY_SITE_CURVE_NUMBER, 20.0f, 17.3f, 20.5f, 46.0f, 5.5f, 40.5f);
					});
				});

				var fps = new TestProcessingState(controlMap, VdypApplicationIdentifier.VDYP_FORWARD);
				fps.setPolygon(polygon);

				TestLayerProcessingState lps = fps.getPrimaryLayerProcessingState();
				var bank = lps.getBank();

				lps.setSpeciesRankingDetails(
						new SpeciesRankingDetails(slotFor(bank, "F"), Optional.of(slotFor(bank, "L")), 0, 0, 0)
				);
				lps.setSiteCurveNumbers(bank.siteCurveNumbers);

				var slots = new HashMap<String, Integer>();
				for (int slot : lps.getIndices()) {
					slots.put(bank.speciesNames[slot], slot);
				}

				return new LayerFixture(lps, bank, slots);
			}

			private void addSpecies(
					VdypLayer.Builder layerBuilder, String genus, int siteCurveNumber, float percentGenus,
					float siteIndex, float dominantHeight, float totalAge, float yearsToBreastHeight,
					float yearsAtBreastHeight
			) {
				layerBuilder.addSpecies(sb -> {
					sb.speciesGroup(genus);
					sb.percentGenus(percentGenus);
					sb.baseArea(10.0f);
					sb.treesPerHectare(300.0f);
					sb.loreyHeight(0.0f, dominantHeight - 1.0f);
					sb.addSite(ib -> {
						ib.siteCurveNumber(siteCurveNumber);
						ib.siteIndex(siteIndex);
						ib.height(dominantHeight);
						ib.ageTotal(totalAge);
						ib.yearsToBreastHeight(yearsToBreastHeight);
						ib.yearsAtBreastHeight(yearsAtBreastHeight);
					});
				});
			}

			private void setChoices(int... choices) {
				fds = new ProcessingDebugSettings(new Integer[25]);
				int slot = 11;
				for (int choice : choices) {
					fds.setValue(slot++, choice);
				}
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			private void runEstimateExtended(LayerFixture fixture) throws ProcessingException {
				new ProcessingEngine(null).estimateMissingSiteIndicesAndAgesExtended(fixture.lps, fds);
			}

			private float yearsToBreastHeight(SiteIndexEquation curve, float siteIndex) throws Exception {
				return (float) SiteTool.yearsToBreastHeight(curve, siteIndex);
			}

			private int slotFor(Bank bank, String genus) {
				for (int slot : bank.getIndices()) {
					if (genus.equals(bank.speciesNames[slot])) {
						return slot;
					}
				}
				throw new IllegalArgumentException("No bank slot for genus " + genus);
			}

			private final class LayerFixture {
				private final TestLayerProcessingState lps;
				private final Bank bank;
				private final Map<String, Integer> slots;

				private LayerFixture(TestLayerProcessingState lps, Bank bank, Map<String, Integer> slots) {
					this.lps = lps;
					this.bank = bank;
					this.slots = slots;
				}

				private int slot(String genus) {
					return slots.get(genus);
				}
			}
		}

		@Nested
		class ExtendedChoices {

			IMocksControl em;
			LayerProcessingState<?> lps;
			VdypLayer layer;
			Bank bank;

			@SuppressWarnings("rawtypes")
			ProcessingEngine unit;
			ProcessingState<?> ps;

			@BeforeEach
			void setup() {
				em = EasyMock.createControl();
				ps = em.createMock(ProcessingState.class);
				unit = EasyMock.partialMockBuilder(ProcessingEngine.class) //
						.addMockedMethod("getSiteIndexEquationByIndex") //
						.addMockedMethod("yearsToBreastHeight") //
						.addMockedMethod("heightAndSiteIndexToAge") //
						.withConstructor(ps) //
						.createMock(em);
				lps = em.createMock(LayerProcessingState.class);

				layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 1970);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(0.89672107f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(11.230089f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("D");
						sb.percentGenus(65.21433f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(12.9306135f);
						sb.addSite(ib -> {
							// Empty
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(9.728239f);
						sb.addSite(ib -> {
							// Empty
						});
					});

				});

				EasyMock.expect(lps.getBank()).andStubReturn(bank);
				EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2, 3, 4, 5 });
				var bec = Utils.getBec("CWH", controlMap);
				bank = new Bank(layer, bec, x -> true);

			}

			void initArray(float[] array, float... values) {
				if (values.length != array.length) {
					throw new IllegalArgumentException("Array length missmatch");
				}
				System.arraycopy(array, 0, values, 0, 6);
			}

			@Nested
			class CalculateYearsToBreastHeightFromSiteIndex {
				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testSkipIfNonPrimarySiInvalid(float invalidValue)
						throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubReturn(2.3);
					em.replay();

					Arrays.fill(bank.siteIndices, invalidValue);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.siteIndices[3] = 13.4f;// Primary, should be ignored
					bank.siteIndices[4] = 42.0f; // Subsequent non valid value should not be ignored

					unit.calculateYearsToBreastHeightFromSiteIndex(lps, bank, 3, SpeciesToApplyTo.NONPRIMARY);

					// 4 changed, the others are not. 3 because it was primary, the others because SI was invalid
					assertThat(
							bank.yearsToBreastHeight,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, 2.3f, Float.NaN)
					);

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@Test
				void testSkipIfCalculatedYtbhZero() throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubReturn(0.0);
					em.replay();

					Arrays.fill(bank.siteIndices, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.siteIndices[3] = 13.4f;// Primary, should be ignored
					bank.siteIndices[1] = 42f;

					unit.calculateYearsToBreastHeightFromSiteIndex(lps, bank, 3, SpeciesToApplyTo.NONPRIMARY);

					// 1 should still be NaN instead of 0.0
					assertThat(
							bank.yearsToBreastHeight,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN)
					);

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testSkipIfExistingYtbh(float invalidValue) throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubReturn(2.3);
					em.replay();

					Arrays.fill(bank.siteIndices, 42f);
					Arrays.fill(bank.yearsToBreastHeight, 5.7f);

					bank.yearsToBreastHeight[3] = invalidValue;// Primary, should be ignored
					bank.yearsToBreastHeight[4] = invalidValue;// Should be filled in

					unit.calculateYearsToBreastHeightFromSiteIndex(lps, bank, 3, SpeciesToApplyTo.NONPRIMARY);

					// 3 should not change because it's primary, 4 should be changed to 2.3. others should remain 5.7
					assertThat(
							bank.yearsToBreastHeight, unboxedArrayCloseTo(5.7f, 5.7f, 5.7f, invalidValue, 2.3f, 5.7f)
					);

					em.verify();
				}

				@Test
				void testWrapCommonCalculatorException() throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					var cause = new CurveErrorException();
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubThrow(cause);
					em.replay();

					Arrays.fill(bank.siteIndices, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.siteIndices[3] = 13.4f;// Primary, should be ignored
					bank.siteIndices[1] = 42f;

					@SuppressWarnings("unchecked")
					var ex = assertThrows(
							ProcessingException.class,
							() -> unit.calculateYearsToBreastHeightFromSiteIndex(
									lps, bank, 3, SpeciesToApplyTo.NONPRIMARY
							)
					);

					assertThat(ex, causedBy(is(cause)));

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testFillInPrimary(float invalidValue) throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubReturn(2.3);
					em.replay();

					Arrays.fill(bank.siteIndices, 42f);
					Arrays.fill(bank.yearsToBreastHeight, invalidValue);

					unit.calculateYearsToBreastHeightFromSiteIndex(lps, bank, 3, SpeciesToApplyTo.PRIMARY);

					// 3 should change because it's primary,others should remain invalid
					assertThat(
							bank.yearsToBreastHeight,
							unboxedArrayCloseTo(
									invalidValue, invalidValue, invalidValue, 2.3f, invalidValue, invalidValue
							)
					);

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testIgnorePrimaryIfSet(float invalidValue) throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(unit.yearsToBreastHeight(SiteIndexEquation.SI_ACB_HUANG, 42.0)).andStubReturn(2.3);
					em.replay();

					Arrays.fill(bank.siteIndices, 42f);
					Arrays.fill(bank.yearsToBreastHeight, invalidValue);

					unit.calculateYearsToBreastHeightFromSiteIndex(lps, bank, 3, SpeciesToApplyTo.PRIMARY);
					bank.yearsToBreastHeight[3] = 5.7f;

					// 3 should remain 5.7 because it's set, others should remain invalid because they are non-primary
					assertThat(
							bank.yearsToBreastHeight,
							unboxedArrayCloseTo(
									invalidValue, invalidValue, invalidValue, 5.7f, invalidValue, invalidValue
							)
					);

					em.verify();
				}

			}

			@Nested
			class EstimateAgesFromHeightAndSiteIndex {
				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testSkipIfNonPrimarySiInvalid(float invalidValue)
						throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_BREAST, 42f, 1.5f
							)
					).andStubReturn(60.0);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_TOTAL, 42f, 1.5f
							)
					).andStubReturn(62.0);
					em.replay();

					Arrays.fill(bank.siteIndices, invalidValue);
					Arrays.fill(bank.dominantHeights, 25f);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, 1.5f);
					Arrays.fill(bank.ageTotals, Float.NaN);

					bank.siteIndices[3] = 13.4f;// Primary, should be ignored
					bank.siteIndices[4] = 42.0f; // Subsequent non valid value should not be ignored

					unit.estimateAgesFromHeightAndSiteIndex(lps, bank, 3, SpeciesToApplyTo.NONPRIMARY);

					// 4 changed, the others are not. 3 because it was primary, the others because SI was invalid
					assertThat(
							bank.yearsAtBreastHeight,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, 60f, Float.NaN)
					);
					assertThat(
							bank.ageTotals,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, 62f, Float.NaN)
					);

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@ValueSource(floats = { Float.NaN, 0.0f, -0.1f, -9f })
				void testSkipIfNonPrimaryHeightInvalid(float invalidValue)
						throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(4)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_BREAST, 42f, 1.5f
							)
					).andStubReturn(60.0);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_TOTAL, 42f, 1.5f
							)
					).andStubReturn(62.0);
					em.replay();

					Arrays.fill(bank.siteIndices, 42f);
					Arrays.fill(bank.dominantHeights, invalidValue);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, 1.5f);
					Arrays.fill(bank.ageTotals, Float.NaN);

					bank.dominantHeights[3] = 13.4f;// Primary, should be ignored
					bank.dominantHeights[4] = 25.0f; // Subsequent non valid value should not be ignored

					unit.estimateAgesFromHeightAndSiteIndex(lps, bank, 3, SpeciesToApplyTo.NONPRIMARY);

					// 4 changed, the others are not. 3 because it was primary, the others because height was invalid
					assertThat(
							bank.yearsAtBreastHeight,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, 60f, Float.NaN)
					);
					assertThat(
							bank.ageTotals,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, Float.NaN, 62f, Float.NaN)
					);

					em.verify();
				}

				@SuppressWarnings("unchecked")
				@Test
				void testPrimary() throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_BREAST, 42f, 1.5f
							)
					).andStubReturn(60.0);
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_TOTAL, 42f, 1.5f
							)
					).andStubReturn(62.0);
					em.replay();

					Arrays.fill(bank.siteIndices, 13f);
					Arrays.fill(bank.dominantHeights, 25f);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, 1.5f);
					Arrays.fill(bank.ageTotals, Float.NaN);

					bank.siteIndices[3] = 42.0f;// Primary, should not be ignored

					unit.estimateAgesFromHeightAndSiteIndex(lps, bank, 3, SpeciesToApplyTo.PRIMARY);

					// 3 changed, the others are not.
					assertThat(
							bank.yearsAtBreastHeight,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, 60f, Float.NaN, Float.NaN)
					);
					assertThat(
							bank.ageTotals,
							unboxedArrayCloseTo(Float.NaN, Float.NaN, Float.NaN, 62f, Float.NaN, Float.NaN)
					);

					em.verify();
				}

				@Test
				void testException() throws ProcessingException, CommonCalculatorException {

					EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(17);
					EasyMock.expect(unit.getSiteIndexEquationByIndex(17)).andStubReturn(SiteIndexEquation.SI_ACB_HUANG);
					var cause = new LessThan13Exception();
					EasyMock.expect(
							unit.heightAndSiteIndexToAge(
									SiteIndexEquation.SI_ACB_HUANG, 25f, SiteIndexAgeType.SI_AT_BREAST, 42f, 1.5f
							)
					).andStubThrow(cause);
					em.replay();

					Arrays.fill(bank.siteIndices, 13f);
					Arrays.fill(bank.dominantHeights, 25f);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, 1.5f);
					Arrays.fill(bank.ageTotals, Float.NaN);

					bank.siteIndices[3] = 42.0f;

					@SuppressWarnings("unchecked")
					var ex = assertThrows(
							ProcessingException.class,
							() -> unit.estimateAgesFromHeightAndSiteIndex(lps, bank, 3, SpeciesToApplyTo.PRIMARY)
					);

					assertThat(ex, causedBy(is(cause)));

					em.verify();
				}

			}

			@Nested
			class FillMissingAgeOfTriplet {
				static List<Arguments> correctAges() {
					return List.of(Arguments.of(12f, 7f, 5f), Arguments.of(12f, 6f, 6f), Arguments.of(12f, 5f, 7f));
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@CsvSource({ "12, 4, 5", "NaN, NaN, 5", "12, NaN, NaN", "NaN, 4, NaN", "NaN, NaN, NaN" })
				void testWrongNumberMissing(float total, float atBH, float toBH) {
					em.replay();
					Arrays.fill(bank.ageTotals, Float.NaN);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.ageTotals[1] = total;
					bank.yearsAtBreastHeight[1] = atBH;
					bank.yearsToBreastHeight[1] = toBH;

					unit.fillMissingAgeOfTriplet(lps, bank);

					assertThat(bank.ageTotals[1], is(total));
					assertThat(bank.yearsAtBreastHeight[1], is(atBH));
					assertThat(bank.yearsToBreastHeight[1], is(toBH));
					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@MethodSource("correctAges")
				void testTotalMissing(float total, float atBH, float toBH) {
					em.replay();
					Arrays.fill(bank.ageTotals, Float.NaN);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.yearsAtBreastHeight[1] = atBH;
					bank.yearsToBreastHeight[1] = toBH;

					unit.fillMissingAgeOfTriplet(lps, bank);

					assertThat(bank.ageTotals[1], is(total));
					assertThat(bank.yearsAtBreastHeight[1], is(atBH));
					assertThat(bank.yearsToBreastHeight[1], is(toBH));
					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@MethodSource("correctAges")
				void testToBreastHeightMissing(float total, float atBH, float toBH) {
					em.replay();
					Arrays.fill(bank.ageTotals, Float.NaN);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.ageTotals[1] = total;
					bank.yearsAtBreastHeight[1] = atBH;

					unit.fillMissingAgeOfTriplet(lps, bank);

					assertThat(bank.ageTotals[1], is(total));
					assertThat(bank.yearsAtBreastHeight[1], is(atBH));
					assertThat(bank.yearsToBreastHeight[1], is(toBH));
					em.verify();
				}

				@SuppressWarnings("unchecked")
				@ParameterizedTest
				@MethodSource("correctAges")
				void testAtBreastHeightMissing(float total, float atBH, float toBH) {
					em.replay();
					Arrays.fill(bank.ageTotals, Float.NaN);
					Arrays.fill(bank.yearsAtBreastHeight, Float.NaN);
					Arrays.fill(bank.yearsToBreastHeight, Float.NaN);

					bank.ageTotals[1] = total;
					bank.yearsToBreastHeight[1] = toBH;

					unit.fillMissingAgeOfTriplet(lps, bank);

					assertThat(bank.ageTotals[1], is(total));
					assertThat(bank.yearsAtBreastHeight[1], is(atBH));
					assertThat(bank.yearsToBreastHeight[1], is(toBH));
					em.verify();
				}
			}
		}
	}

	@Nested
	class CalculateDominantHeightAgeSiteIndex {
		MatrixMap2Impl<String, Region, Coefficients> hl1Coefficients;
		Map<String, Object> controlMap;

		IMocksControl em;
		ProcessingState<?> ps;
		LayerProcessingState<?> lps;

		@SuppressWarnings("rawtypes")
		ProcessingEngine unit;

		@BeforeEach
		void setup() throws IOException, ResourceParseException {
			em = EasyMock.createControl();
			ps = em.createMock(ProcessingState.class);
			unit = EasyMock.partialMockBuilder(ProcessingEngine.class) //
					.addMockedMethod("getSiteIndexEquationByIndex") //
					.addMockedMethod("yearsToBreastHeight") //
					.addMockedMethod("heightAndSiteIndexToAge") //
					.addMockedMethod("convertSiteIndexBetweenCurves") //
					.withConstructor(ps) //
					.createMock(em);
			lps = em.createMock(LayerProcessingState.class);

			var parser = new ProcessingControlParser();
			controlMap = TestUtils.loadControlMap(parser, TestUtils.class, "VDYP.CTR");

			hl1Coefficients = new MatrixMap2Impl<>(
					List.of("AC", "AT", "B", "C", "D", "E", "F", "H", "L", "MB", "PA", "PL", "PW", "PY", "S", "Y"),
					List.of(Region.COASTAL, Region.INTERIOR), (s, r) -> Coefficients.empty(3, 1)
			);
			hl1Coefficients
					.put("AC", Region.COASTAL, new Coefficients(new float[] { 1.0016f, 0.20508f, -0.0013743f }, 1));
			hl1Coefficients
					.put("AC", Region.INTERIOR, new Coefficients(new float[] { 1.00337f, 0.26975f, -0.0012802f }, 1));
			hl1Coefficients
					.put("AT", Region.COASTAL, new Coefficients(new float[] { 0.98946f, 0.15312f, -0.0013087f }, 1));
			hl1Coefficients
					.put("AT", Region.INTERIOR, new Coefficients(new float[] { 0.98946f, 0.15312f, -0.0013087f }, 1));
			hl1Coefficients
					.put("B", Region.COASTAL, new Coefficients(new float[] { 0.97183f, 0.18096f, -0.001858f }, 1));
			hl1Coefficients
					.put("B", Region.INTERIOR, new Coefficients(new float[] { 0.95942f, 0.31474f, -0.0011697f }, 1));
			hl1Coefficients
					.put("C", Region.COASTAL, new Coefficients(new float[] { 0.99412f, 0.1801f, -0.0021747f }, 1));
			hl1Coefficients
					.put("C", Region.INTERIOR, new Coefficients(new float[] { 1.01352f, 0.43532f, -9.144E-4f }, 1));
			hl1Coefficients
					.put("D", Region.COASTAL, new Coefficients(new float[] { 1.00358f, 0.09603f, -0.0038217f }, 1));
			hl1Coefficients
					.put("D", Region.INTERIOR, new Coefficients(new float[] { 1.00358f, 0.09603f, -0.0038217f }, 1));
			hl1Coefficients
					.put("E", Region.COASTAL, new Coefficients(new float[] { 1.01384f, 0.1352f, -0.0031488f }, 1));
			hl1Coefficients
					.put("E", Region.INTERIOR, new Coefficients(new float[] { 1.01384f, 0.1352f, -0.0031488f }, 1));
			hl1Coefficients
					.put("F", Region.COASTAL, new Coefficients(new float[] { 0.99518f, 0.18193f, -0.0017202f }, 1));
			hl1Coefficients
					.put("F", Region.INTERIOR, new Coefficients(new float[] { 0.9802f, 0.24054f, -0.0016229f }, 1));
			hl1Coefficients
					.put("H", Region.COASTAL, new Coefficients(new float[] { 0.96537f, 0.16792f, -0.0011664f }, 1));
			hl1Coefficients
					.put("H", Region.INTERIOR, new Coefficients(new float[] { 0.99065f, 0.29502f, -0.00125f }, 1));
			hl1Coefficients
					.put("L", Region.COASTAL, new Coefficients(new float[] { 0.99315f, 0.17747f, -0.0022617f }, 1));
			hl1Coefficients
					.put("L", Region.INTERIOR, new Coefficients(new float[] { 0.99315f, 0.17747f, -0.0022617f }, 1));
			hl1Coefficients
					.put("MB", Region.COASTAL, new Coefficients(new float[] { 0.99951f, 0.11329f, -0.0068948f }, 1));
			hl1Coefficients
					.put("MB", Region.INTERIOR, new Coefficients(new float[] { 0.99951f, 0.11329f, -0.0068948f }, 1));
			hl1Coefficients
					.put("PA", Region.COASTAL, new Coefficients(new float[] { 0.99952f, 0.20278f, -0.0020051f }, 1));
			hl1Coefficients
					.put("PA", Region.INTERIOR, new Coefficients(new float[] { 0.99952f, 0.20278f, -0.0020051f }, 1));
			hl1Coefficients
					.put("PL", Region.COASTAL, new Coefficients(new float[] { 0.95856f, 0.13349f, -9.818E-4f }, 1));
			hl1Coefficients
					.put("PL", Region.INTERIOR, new Coefficients(new float[] { 0.97312f, 0.17339f, -0.0010287f }, 1));
			hl1Coefficients
					.put("PW", Region.COASTAL, new Coefficients(new float[] { 0.98775f, 0.17671f, -0.0029754f }, 1));
			hl1Coefficients
					.put("PW", Region.INTERIOR, new Coefficients(new float[] { 0.98775f, 0.17671f, -0.0029754f }, 1));
			hl1Coefficients
					.put("PY", Region.COASTAL, new Coefficients(new float[] { 0.95955f, 0.17742f, -0.0019664f }, 1));
			hl1Coefficients
					.put("PY", Region.INTERIOR, new Coefficients(new float[] { 0.95955f, 0.17742f, -0.0019664f }, 1));
			hl1Coefficients
					.put("S", Region.COASTAL, new Coefficients(new float[] { 0.99424f, 0.12502f, -0.0020471f }, 1));
			hl1Coefficients
					.put("S", Region.INTERIOR, new Coefficients(new float[] { 0.97776f, 0.2563f, -0.001448f }, 1));
			hl1Coefficients
					.put("Y", Region.COASTAL, new Coefficients(new float[] { 0.97184f, 0.24781f, -0.0014371f }, 1));
			hl1Coefficients
					.put("Y", Region.INTERIOR, new Coefficients(new float[] { 0.97184f, 0.24781f, -0.0014371f }, 1));
		}

		@SuppressWarnings("unchecked")
		@Test
		void testAll() throws Exception {

			var bec = Utils.getBec("CWH", controlMap);
			Bank bank = ProcessingStateTestUtils.mockBank(bec, 1);

			ProcessingStateTestUtils.fill(bank.speciesNames, null, "C");

			ProcessingStateTestUtils.fill(bank.dominantHeights, 0f, Float.NaN);
			ProcessingStateTestUtils.fill(bank.loreyHeights[0], 6.4601994f, 22.9584f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[1], 6.4602f, 22.9584f);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[0], 4.444444f, 84.303024f, 16.28283f, 18.050505f, 16.71717f, 33.25252f);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[1], 4.444444f, 84.303024f, 16.28283f, 18.050505f, 16.71717f, 33.25252f);

			ProcessingStateTestUtils.fill(bank.ageTotals, 0f, 22f);
			ProcessingStateTestUtils.fill(bank.yearsAtBreastHeight, 0f, Float.NaN);
			ProcessingStateTestUtils.fill(bank.yearsToBreastHeight, 0f, 7.7f);

			ProcessingStateTestUtils.fill(bank.siteIndices, 34.0f, 34.0f);

			EasyMock.expect(lps.getBank()).andStubReturn(bank);
			EasyMock.expect(lps.getBecZone()).andStubReturn(bec);
			EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.empty());
			EasyMock.expect(lps.getSiteCurveNumber(1)).andStubReturn(122);
			EasyMock.expect(lps.getSiteCurveNumber(0)).andStubReturn(122);

			EasyMock.expect(lps.getNSpecies()).andStubReturn(1);
			EasyMock.expect(lps.getPrimarySpeciesIndex()).andStubReturn(1);
			EasyMock.expect(unit.getSiteIndexEquationByIndex(122)).andStubReturn(SiteIndexEquation.SI_CWC_NIGH);
			EasyMock.expect(
					unit.convertSiteIndexBetweenCurves(
							SiteIndexEquation.SI_CWC_NIGH, 34.0, SiteIndexEquation.SI_CWC_NIGH
					)
			).andStubThrow(new NoAnswerException());

			Capture<PrimarySpeciesDetails> detailsCapture = EasyMock.newCapture();

			lps.setPrimarySpeciesDetails(EasyMock.capture(detailsCapture));
			EasyMock.expectLastCall().once();

			em.replay();

			unit.calculateDominantHeightAgeSiteIndex(lps, hl1Coefficients);

			em.verify();

			var result = detailsCapture.getValue();
			assertThat("dominant height", result.primarySpeciesDominantHeight(), closeTo(22.95f));
			assertThat("total age", result.primarySpeciesTotalAge(), closeTo(22f));
			assertThat("at breast height", result.primarySpeciesAgeAtBreastHeight(), asFloat(notANumber()));
			assertThat("to breast height", result.primarySpeciesAgeToBreastHeight(), closeTo(7.7f));
			assertThat("site index", result.primarySpeciesSiteIndex(), closeTo(34f));
		}

		@Nested
		class DominantHeight {
			@Test
			void testSimple() throws ProcessingException {
				var bec = Utils.getBec("CDF", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				bank.dominantHeights[primarySpeciesIndex] = Float.NaN;
				bank.loreyHeights[primarySpeciesIndex][1] = 33.744f;
				bank.speciesNames[primarySpeciesIndex] = "D";
				bank.treesPerHectare[primarySpeciesIndex][1] = 290.61615f;

				float result = ProcessingEngine.calculatePrimarySpeciesDominantHeight(
						Region.COASTAL, hl1Coefficients, bank, primarySpeciesIndex
				);

				assertThat(result, closeTo(35.312016f));
			}

			@Test
			void testCoastal() throws ProcessingException {
				var bec = Utils.getBec("CDF", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				bank.dominantHeights[primarySpeciesIndex] = Float.NaN;
				bank.loreyHeights[primarySpeciesIndex][1] = 33.744f;
				bank.speciesNames[primarySpeciesIndex] = "F"; // hl1Coefficients differs between regions
				bank.treesPerHectare[primarySpeciesIndex][1] = 290.61615f;

				float result = ProcessingEngine.calculatePrimarySpeciesDominantHeight(
						Region.COASTAL, hl1Coefficients, bank, primarySpeciesIndex
				);

				assertThat(result, closeTo(35.657f));
			}

			@Test
			void testInterior() throws ProcessingException {
				var bec = Utils.getBec("IDF", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				bank.dominantHeights[primarySpeciesIndex] = Float.NaN;
				bank.loreyHeights[primarySpeciesIndex][1] = 33.744f;
				bank.speciesNames[primarySpeciesIndex] = "F";// hl1Coefficients differs between regions
				bank.treesPerHectare[primarySpeciesIndex][1] = 290.61615f;

				float result = ProcessingEngine.calculatePrimarySpeciesDominantHeight(
						Region.INTERIOR, hl1Coefficients, bank, primarySpeciesIndex
				);

				assertThat(result, closeTo(36.712f));
			}

			@Test
			void testDominantAlreadySet() throws ProcessingException {
				var bec = Utils.getBec("CDF", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				bank.dominantHeights[primarySpeciesIndex] = 42f;
				bank.loreyHeights[primarySpeciesIndex][1] = 33.744f;
				bank.speciesNames[primarySpeciesIndex] = "D";
				bank.treesPerHectare[primarySpeciesIndex][1] = 290.61615f;

				float result = ProcessingEngine.calculatePrimarySpeciesDominantHeight(
						Region.COASTAL, hl1Coefficients, bank, primarySpeciesIndex
				);

				assertThat(result, closeTo(42f));
			}

			@Test
			void testLoreyNotSet() {
				var bec = Utils.getBec("CDF", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				bank.dominantHeights[primarySpeciesIndex] = Float.NaN;
				bank.loreyHeights[primarySpeciesIndex][1] = Float.NaN;
				bank.speciesNames[primarySpeciesIndex] = "D";
				bank.treesPerHectare[primarySpeciesIndex][1] = 290.61615f;

				var ex = assertThrows(
						ProcessingException.class,
						() -> ProcessingEngine.calculatePrimarySpeciesDominantHeight(
								Region.COASTAL, hl1Coefficients, bank, primarySpeciesIndex
						)
				);

				assertThat(ex, hasMessage(containsString("primary species D")));
				assertThat(ex, hasProperty("errorNumber", present(is(2))));

			}

		}

		@Nested
		class Ages {
			@Test
			void testWithSecondaryAndYTBH() throws ProcessingException {
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				ProcessingStateTestUtils.fill(bank.ageTotals, 0f, 15f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils
						.fill(bank.yearsAtBreastHeight, 0f, 11f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils.fill(bank.yearsToBreastHeight, 0f, 4f, 7.7f, 1f, 4.6f, 5.4f);

				AgeTriplet result = ProcessingEngine
						.calculatePrimarySpeciesAges(bank, primarySpeciesIndex, Optional.of(4));
				assertThat("total", result.total(), closeTo(15f));
				assertThat("atBreastHeight", result.atBreastHeight(), closeTo(14f));
				assertThat("toBreastHeight", result.toBreastHeight(), closeTo(1f));
			}

			@Test
			void testWithSecondaryAndYABH() throws ProcessingException {
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				ProcessingStateTestUtils.fill(bank.ageTotals, 0f, 15f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils.fill(bank.yearsAtBreastHeight, 0f, 11f, 7.3f, 14f, 10.4f, 9.6f);
				ProcessingStateTestUtils
						.fill(bank.yearsToBreastHeight, 0f, 4f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);

				AgeTriplet result = ProcessingEngine
						.calculatePrimarySpeciesAges(bank, primarySpeciesIndex, Optional.of(4));
				assertThat("total", result.total(), closeTo(15f));
				assertThat("atBreastHeight", result.atBreastHeight(), closeTo(14f));
				assertThat("toBreastHeight", result.toBreastHeight(), closeTo(1f));
			}

			@Test
			void testWithSecondaryAndNoOtherAges() throws ProcessingException {
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				ProcessingStateTestUtils.fill(bank.ageTotals, 0f, 15f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils
						.fill(bank.yearsAtBreastHeight, 0f, 11f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils
						.fill(bank.yearsToBreastHeight, 0f, 4f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);

				AgeTriplet result = ProcessingEngine
						.calculatePrimarySpeciesAges(bank, primarySpeciesIndex, Optional.of(4));
				assertThat("total", result.total(), closeTo(15f));
				assertThat("atBreastHeight", result.atBreastHeight(), closeTo(11f));
				assertThat("toBreastHeight", result.toBreastHeight(), closeTo(4f));
			}

			@Test
			void testNoTotals() {
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);
				int primarySpeciesIndex = 3;

				ProcessingStateTestUtils
						.fill(bank.ageTotals, 0f, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils
						.fill(bank.yearsAtBreastHeight, 0f, 11f, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
				ProcessingStateTestUtils.fill(bank.yearsToBreastHeight, 0f, 4f, 7.7f, 1f, 4.6f, 5.4f);

				var ex = assertThrows(
						ProcessingException.class,
						() -> ProcessingEngine.calculatePrimarySpeciesAges(bank, primarySpeciesIndex, Optional.of(4))
				);

				assertThat(ex, hasProperty("errorNumber", present(is(5))));
			}

			@Test
			void testWithoutSecondary() throws ProcessingException {
				var bec = Utils.getBec("CWH", controlMap);
				Bank bank = ProcessingStateTestUtils.mockBank(bec, 1);
				int primarySpeciesIndex = 1;

				ProcessingStateTestUtils.fill(bank.ageTotals, 0f, 22f);
				ProcessingStateTestUtils.fill(bank.yearsAtBreastHeight, 0f, Float.NaN);
				ProcessingStateTestUtils.fill(bank.yearsToBreastHeight, 0f, 7.7f);

				AgeTriplet result = ProcessingEngine
						.calculatePrimarySpeciesAges(bank, primarySpeciesIndex, Optional.of(4));
				assertThat("total", result.total(), closeTo(22f));
				assertThat("atBreastHeight", result.atBreastHeight(), asFloat(notANumber()));
				assertThat("toBreastHeight", result.toBreastHeight(), closeTo(7.7f));
			}
		}

		@Nested
		class SiteIndex {

			@Test
			void testSuccess() throws Exception {

				float[] siteIndices = { Float.NaN, Float.NaN, 34f, Float.NaN, Float.NaN, Float.NaN };
				int primarySpeciesIndex = 3;

				EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
				EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.of(4));
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(2);
				EasyMock.expect(lps.getSiteCurveNumber(0)).andStubReturn(64);

				EasyMock.expect(unit.getSiteIndexEquationByIndex(64)).andStubReturn(SiteIndexEquation.SI_SW_HUANG_PLA);
				EasyMock.expect(unit.getSiteIndexEquationByIndex(2)).andStubReturn(SiteIndexEquation.SI_AT_HUANG);
				EasyMock.expect(
						unit.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_AT_HUANG, 34.0, SiteIndexEquation.SI_SW_HUANG_PLA
						)
				).andStubReturn(
						SiteTool.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_AT_HUANG, 34.0, SiteIndexEquation.SI_SW_HUANG_PLA
						)
				);

				em.replay();

				@SuppressWarnings("unchecked")
				var result = unit.calculatePrimarySpeciesSiteIndex(lps, siteIndices, primarySpeciesIndex);

				assertThat("result", result, closeTo(30.929f));

				em.verify();
			}

			@Test
			void testNoConversion() throws Exception {

				float[] siteIndices = { Float.NaN, Float.NaN, 34f, Float.NaN, Float.NaN, Float.NaN };
				int primarySpeciesIndex = 3;

				EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
				EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.of(4));
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(122);
				EasyMock.expect(lps.getSiteCurveNumber(0)).andStubReturn(13);

				EasyMock.expect(unit.getSiteIndexEquationByIndex(122)).andStubReturn(SiteIndexEquation.SI_CWC_NIGH);
				EasyMock.expect(unit.getSiteIndexEquationByIndex(13)).andStubReturn(SiteIndexEquation.SI_DR_NIGH);
				EasyMock.expect(
						unit.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_CWC_NIGH, 34.0, SiteIndexEquation.SI_DR_NIGH
						)
				).andStubThrow(new NoAnswerException());

				em.replay();

				@SuppressWarnings("unchecked")
				var result = unit.calculatePrimarySpeciesSiteIndex(lps, siteIndices, primarySpeciesIndex);

				assertThat("result", result, closeTo(34.0f));

				em.verify();
			}

			@Test
			void testSmallIndex() throws Exception {

				float[] siteIndices = { Float.NaN, Float.NaN, 34f, Float.NaN, Float.NaN, Float.NaN };
				int primarySpeciesIndex = 3;

				EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
				EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.of(4));
				EasyMock.expect(lps.getSiteCurveNumber(2)).andStubReturn(2);
				EasyMock.expect(lps.getSiteCurveNumber(0)).andStubReturn(64);

				EasyMock.expect(unit.getSiteIndexEquationByIndex(64)).andStubReturn(SiteIndexEquation.SI_SW_HUANG_PLA);
				EasyMock.expect(unit.getSiteIndexEquationByIndex(2)).andStubReturn(SiteIndexEquation.SI_AT_HUANG);
				EasyMock.expect(
						unit.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_AT_HUANG, 34.0, SiteIndexEquation.SI_SW_HUANG_PLA
						)
				).andStubReturn(1.2);

				em.replay();

				@SuppressWarnings("unchecked")
				var result = unit.calculatePrimarySpeciesSiteIndex(lps, siteIndices, primarySpeciesIndex);

				assertThat("result", result, closeTo(34.0f));

				em.verify();
			}

			@Test
			void testAlreadySet() throws Exception {

				float[] siteIndices = { Float.NaN, Float.NaN, 34f, 27f, Float.NaN, Float.NaN };
				int primarySpeciesIndex = 3;

				EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
				EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.of(4));
				EasyMock.expect(lps.getSiteCurveNumber(3)).andStubReturn(2);
				EasyMock.expect(lps.getSiteCurveNumber(0)).andStubReturn(64);

				EasyMock.expect(unit.getSiteIndexEquationByIndex(64)).andStubReturn(SiteIndexEquation.SI_SW_HUANG_PLA);
				EasyMock.expect(unit.getSiteIndexEquationByIndex(2)).andStubReturn(SiteIndexEquation.SI_AT_HUANG);
				EasyMock.expect(
						unit.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_AT_HUANG, 27.0, SiteIndexEquation.SI_SW_HUANG_PLA
						)
				).andStubReturn(
						SiteTool.convertSiteIndexBetweenCurves(
								SiteIndexEquation.SI_AT_HUANG, 27.0, SiteIndexEquation.SI_SW_HUANG_PLA
						)
				);

				em.replay();

				@SuppressWarnings("unchecked")
				var result = unit.calculatePrimarySpeciesSiteIndex(lps, siteIndices, primarySpeciesIndex);

				assertThat("result", result, closeTo(25.3446f));

				em.verify();
			}

			@Test
			void testNoIndices() {

				float[] siteIndices = { Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN };
				int primarySpeciesIndex = 3;

				EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
				EasyMock.expect(lps.getSecondarySpeciesIndex()).andStubReturn(Optional.of(4));

				em.replay();

				@SuppressWarnings("unchecked")
				var ex = assertThrows(
						ProcessingException.class,
						() -> unit.calculatePrimarySpeciesSiteIndex(lps, siteIndices, primarySpeciesIndex)
				);

				assertThat(ex, hasProperty("errorNumber", present(is(7))));

				em.verify();
			}

		}
	}

	@Nested
	class SetCompatibilityVariables {
		ProcessingResolvedControlMap controlMap;

		EstimationMethods emp;
		ComputationMethods cmp;

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ProcessingControlParser();
			controlMap = new ProcessingResolvedControlMapImpl(
					TestUtils.loadControlMap(parser, TestUtils.class, "VDYP.CTR")
			);
			emp = new EstimationMethods(controlMap);
			cmp = new ComputationMethods(emp, VdypApplicationIdentifier.VDYP_FORWARD);

		}

		@Test
		void test() throws ProcessingException {
			final var em = EasyMock.createControl();
			final BecDefinition bec = controlMap.getBecLookup().get("CWH").get();

			ProcessingState ps = em.createMock(ProcessingState.class);
			LayerProcessingState lps = em.createMock(LayerProcessingState.class);

			EasyMock.expect(ps.getComputers()).andStubReturn(cmp);
			EasyMock.expect(ps.getEstimators()).andStubReturn(emp);
			EasyMock.expect(ps.getControlMap()).andStubReturn(controlMap);

			EasyMock.expect(ps.getPrimaryLayerProcessingState()).andStubReturn(lps);
			EasyMock.expect(lps.getIndices()).andStubReturn(new int[] { 1, 2, 3, 4, 5 });
			EasyMock.expect(lps.getNSpecies()).andStubReturn(5);
			EasyMock.expect(lps.getBecZone()).andStubReturn(bec);

			EasyMock.expect(lps.getVolumeEquationGroups()).andStubReturn(new int[] { -9, 12, 20, 25, 37, 66 });
			EasyMock.expect(lps.getDecayEquationGroups()).andStubReturn(new int[] { -9, 7, 14, 19, 31, 54 });

			EasyMock.expect(lps.getPrimarySpeciesAgeAtBreastHeight()).andStubReturn(54f);
			EasyMock.expect(lps.getPrimarySpeciesIndex()).andStubReturn(3);

			Capture<MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>[]> capCvVolume = EasyMock
					.newCapture();
			Capture<MatrixMap2<UtilizationClass, LayerType, Float>[]> capCvBasalArea = EasyMock.newCapture();
			Capture<MatrixMap2<UtilizationClass, LayerType, Float>[]> capCvQuadraticMeanDiameter = EasyMock
					.newCapture();
			Capture<Map<UtilizationClassVariable, Float>[]> capCvPrimaryLayerSmall = EasyMock.newCapture();

			Bank bank = ProcessingStateTestUtils.mockBank(bec, 5);

			ProcessingStateTestUtils.fill(bank.speciesNames, null, "B", "C", "D", "H", "S");

			ProcessingStateTestUtils.fill(bank.ageTotals, 0f, Float.NaN, Float.NaN, 55f, Float.NaN, Float.NaN);
			ProcessingStateTestUtils
					.fill(bank.yearsAtBreastHeight, 0f, Float.NaN, Float.NaN, 54f, Float.NaN, Float.NaN);
			ProcessingStateTestUtils.fill(bank.yearsToBreastHeight, 0f, 5.0f, 7.5f, 1.0f, 4.5f, 5.2f);

			ProcessingStateTestUtils.fill(bank.loreyHeights[0], 7.016903f, 30.97237f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[1], 8.0272f, 36.7553f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[2], 6.4602f, 22.9584f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[3], 10.6033f, 33.744f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[4], 7.5464f, 22.7704f);
			ProcessingStateTestUtils.fill(bank.loreyHeights[5], 8.2003f, 32.0125f);

			ProcessingStateTestUtils
					.fill(bank.basalAreas[0], 0.015282828f, 45.386444f, 0.5363535f, 1.2914546f, 2.353737f, 41.2049f);
			ProcessingStateTestUtils
					.fill(bank.basalAreas[1], 0.0f, 0.40698987f, 0.005070707f, 0.013767676f, 0.023070706f, 0.36508077f);
			ProcessingStateTestUtils
					.fill(bank.basalAreas[2], 0.012555555f, 5.096949f, 0.12951516f, 0.3131616f, 0.5185757f, 4.1356964f);
			ProcessingStateTestUtils.fill(
					bank.basalAreas[3], 0.0015656565f, 29.598463f, 0.014262626f, 0.051797975f, 0.46197978f, 29.070423f
			);
			ProcessingStateTestUtils
					.fill(bank.basalAreas[4], 0.0f, 5.8687477f, 0.3650303f, 0.83281815f, 1.0865252f, 3.5843737f);
			ProcessingStateTestUtils.fill(
					bank.basalAreas[5], 0.0011616162f, 4.4152927f, 0.022474747f, 0.079909086f, 0.26358584f, 4.049323f
			);

			ProcessingStateTestUtils.fill(
					bank.quadMeanDiameters[0], 6.0632977f, 30.998875f, 10.21287f, 15.043846f, 20.077644f, 36.7306f
			);
			ProcessingStateTestUtils
					.fill(bank.quadMeanDiameters[1], 6.1f, 31.500628f, 9.1706505f, 13.66034f, 18.178656f, 42.070774f);
			ProcessingStateTestUtils.fill(
					bank.quadMeanDiameters[2], 5.997418f, 27.745249f, 10.063532f, 14.862596f, 19.873747f, 39.793953f
			);
			ProcessingStateTestUtils.fill(
					bank.quadMeanDiameters[3], 6.4799547f, 36.01055f, 10.470092f, 15.579478f, 20.52722f, 36.869785f
			);
			ProcessingStateTestUtils.fill(
					bank.quadMeanDiameters[4], Float.NaN, 20.989742f, 10.276456f, 15.108315f, 20.090958f, 31.892609f
			);
			ProcessingStateTestUtils.fill(
					bank.quadMeanDiameters[5], 6.3775334f, 32.9989f, 10.186823f, 15.028074f, 19.852716f, 37.923714f
			);

			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[0], 5.2828283f, 601.3737f, 65.47475f, 72.666664f, 74.35353f, 388.87875f);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[1], 0.0f, 5.222222f, 0.7676767f, 0.9393939f, 0.88888884f, 2.6262624f);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[2], 4.444444f, 84.303024f, 16.28283f, 18.050505f, 16.71717f, 33.25252f);
			ProcessingStateTestUtils.fill(
					bank.treesPerHectare[3], 0.47474745f, 290.61615f, 1.6565655f, 2.7171717f, 13.959595f, 272.2828f
			);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[4], 0.0f, 169.60605f, 44.010098f, 46.454544f, 34.272724f, 44.868683f);
			ProcessingStateTestUtils
					.fill(bank.treesPerHectare[5], 0.36363637f, 51.62626f, 2.7575758f, 4.50505f, 8.515151f, 35.848484f);

			ProcessingStateTestUtils
					.fill(bank.wholeStemVolumes[0], 0.06363636f, 627.24994f, 2.6241412f, 9.197676f, 22.62818f, 592.8f);
			ProcessingStateTestUtils.fill(
					bank.wholeStemVolumes[1], 0.0f, 6.272525f, 0.018686868f, 0.076464646f, 0.17656565f, 6.000808f
			);
			ProcessingStateTestUtils.fill(
					bank.wholeStemVolumes[2], 0.05121212f, 43.907677f, 0.60878783f, 1.9431312f, 3.861616f, 37.49414f
			);
			ProcessingStateTestUtils.fill(
					bank.wholeStemVolumes[3], 0.007878787f, 464.165f, 0.110202014f, 0.56585854f, 6.073636f, 457.4153f
			);
			ProcessingStateTestUtils
					.fill(bank.wholeStemVolumes[4], 0.0f, 56.45232f, 1.7560605f, 5.9258585f, 9.749595f, 39.020805f);
			ProcessingStateTestUtils.fill(
					bank.wholeStemVolumes[5], 0.004545454f, 56.45242f, 0.13040403f, 0.6863636f, 2.7667675f, 52.868885f
			);

			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[0], 0.0f, 598.18396f, 0.38727272f, 6.9943438f, 20.327776f, 570.47455f
			);
			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[1], 0.0f, 6.0193934f, 9.0909086E-4f, 0.05030303f, 0.15363635f,
					5.814545f
			);
			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[2], 0.0f, 39.838383f, 0.11272726f, 1.4245454f, 3.3496969f, 34.951412f
			);
			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[3], 0.0f, 448.56998f, 0.057676766f, 0.50989896f, 5.6983833f, 442.30402f
			);
			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[4], 0.0f, 50.332424f, 0.19444443f, 4.460101f, 8.661818f, 37.01606f
			);
			ProcessingStateTestUtils.fill(
					bank.closeUtilizationVolumes[5], 0.0f, 53.423737f, 0.02151515f, 0.549495f, 2.4642422f, 50.388485f
			);

			ProcessingStateTestUtils
					.fill(bank.cuVolumesMinusDecay[0], 0.0f, 586.02844f, 0.3832323f, 6.916161f, 20.089392f, 558.63965f);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecay[1], 0.0f, 5.9056563f, 9.0909086E-4f, 0.050202016f, 0.15292929f, 5.701616f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecay[2], 0.0f, 36.629692f, 0.110505044f, 1.3848485f, 3.2451513f, 31.889189f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecay[3], 0.0f, 440.93735f, 0.057171714f, 0.5057576f, 5.65404f, 434.72037f
			);
			ProcessingStateTestUtils
					.fill(bank.cuVolumesMinusDecay[4], 0.0f, 49.569794f, 0.19323231f, 4.428889f, 8.587777f, 36.359898f);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecay[5], 0.0f, 52.985954f, 0.02141414f, 0.5464646f, 2.4494948f, 49.968582f
			);

			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[0], 0.0f, 583.4573f, 0.38252524f, 6.901414f, 20.037878f,
					556.13544f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[1], 0.0f, 5.87505f, 9.0909086E-4f, 0.05010101f, 0.15272726f,
					5.671313f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[2], 0.0f, 35.64939f, 0.110101f, 1.3765656f, 3.2180805f,
					30.944645f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[3], 0.0f, 439.67856f, 0.057070702f, 0.50555557f, 5.651313f,
					433.46463f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[4], 0.0f, 49.34848f, 0.1930303f, 4.423131f, 8.568384f, 36.163937f
			);
			ProcessingStateTestUtils.fill(
					bank.cuVolumesMinusDecayAndWastage[5], 0.0f, 52.90575f, 0.02141414f, 0.54606056f, 2.4473736f,
					49.890903f
			);

			EasyMock.expect(lps.getBank()).andStubReturn(bank);

			// The call we are testing for
			lps.setCompatibilityVariableDetails(
					capture(capCvVolume), //
					capture(capCvBasalArea), //
					capture(capCvQuadraticMeanDiameter), //
					capture(capCvPrimaryLayerSmall) //
			);
			EasyMock.expectLastCall().once();

			var unit = new ProcessingEngine(ps);
			em.replay();

			unit.setCompatibilityVariables();

			em.verify();

			assertThat(
					"CvVolume[1]", capCvVolume.getValue()[1],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0063990355f), U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.00016450882f), OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[2]", capCvVolume.getValue()[2],
					allOf(
							mmHasEntry(is(-0.00024962425f), U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.00011026859f), U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.000006198883f), U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.000024557114f), OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[3]", capCvVolume.getValue()[3],
					allOf(
							mmHasEntry(is(0.00623063f), U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0010375977f), U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0001244545f), U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0000038146973f), OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[4]", capCvVolume.getValue()[4],
					allOf(
							mmHasEntry(is(-0.00013566017f), U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.00033128262f), U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.00021290779f), U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.0000059604645f), OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[5]", capCvVolume.getValue()[5],
					allOf(
							mmHasEntry(is(-8.8346004E-4F), U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.0002478361f), U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0008614063f), U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-0.0000052452087f), OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[1]", capCvVolume.getValue()[1],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-0.0048389435f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(1.3446808E-4f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[2]", capCvVolume.getValue()[2],
					allOf(
							mmHasEntry(is(0.01768279f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(0.0010006428f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(2.2292137E-4f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-1.9311905E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[3]", capCvVolume.getValue()[3],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(0.010708809f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(7.4100494E-4f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-2.4795532E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[4]", capCvVolume.getValue()[4],
					allOf(
							mmHasEntry(is(0.011499405f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-0.0010294914f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-9.3603134E-4f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-5.4836273E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[5]", capCvVolume.getValue()[5],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(0.010175705f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-0.0014338493f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY),
							mmHasEntry(is(-2.9087067E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[1]", capCvVolume.getValue()[1],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(
									is(0.035768032f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(-0.0016698837f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							)
					)
			);
			assertThat(
					"CvVolume[2]", capCvVolume.getValue()[2],
					allOf(
							mmHasEntry(
									is(-0.16244507f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(-0.0045113564f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(-0.0030164719f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(3.528595E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY
							)
					)
			);
			assertThat(
					"CvVolume[3]", capCvVolume.getValue()[3],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(
									is(4.1484833E-5f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							)
					)
			);
			assertThat(
					"CvVolume[4]", capCvVolume.getValue()[4],
					allOf(
							mmHasEntry(
									is(-0.13775301f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(0.005630493f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(0.0028266907f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							),
							mmHasEntry(
									is(3.7765503E-4f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
									LayerType.PRIMARY
							)
					)
			);
			assertThat(
					"CvVolume[5]", capCvVolume.getValue()[5],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
							mmHasEntry(
									is(5.378723E-4f), OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY
							)
					)
			);
			assertThat(
					"CvVolume[1]", capCvVolume.getValue()[1],
					allOf(
							mmHasEntry(is(0.0f), U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-2.5427341E-4f), U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(0.0023429394F), U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-7.033348E-5f), OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[2]", capCvVolume.getValue()[2],
					allOf(
							mmHasEntry(is(-2.9444695E-5f), U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(4.208088E-5f), U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-2.4318695E-5f), U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(9.536743E-7f), OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[3]", capCvVolume.getValue()[3],
					allOf(
							mmHasEntry(is(0.0013506413f), U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(4.787445E-4f), U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(3.9100647E-5f), U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-1.4305115E-6f), OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[4]", capCvVolume.getValue()[4],
					allOf(
							mmHasEntry(is(-7.891655E-5f), U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(2.7656555E-5f), U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(4.196167E-5f), U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-1.0967255E-5f), OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvVolume[5]", capCvVolume.getValue()[5],
					allOf(
							mmHasEntry(is(1.8835068E-5f), U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-1.0085106E-4f), U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(1.6236305E-4f), U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY),
							mmHasEntry(is(-7.390976E-6f), OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvBasalArea[1]", capCvBasalArea.getValue()[1],
					allOf(
							mmHasEntry(is(1.4913082E-4f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-5.034916E-5f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-7.482059E-5f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-2.397038E-5f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvBasalArea[2]", capCvBasalArea.getValue()[2],
					allOf(
							mmHasEntry(is(-2.193451E-5f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(5.4836273E-6f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(9.596348E-6f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(6.660819E-6f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvBasalArea[3]", capCvBasalArea.getValue()[3],
					allOf(
							mmHasEntry(is(9.918213E-5f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-1.5150756E-5f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-7.9244375E-5f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-4.341826E-6f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvBasalArea[4]", capCvBasalArea.getValue()[4],
					allOf(
							mmHasEntry(is(1.9073486E-4f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-8.2850456E-5f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-5.2928925E-5f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-5.531311E-5f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvBasalArea[5]", capCvBasalArea.getValue()[5],
					allOf(
							mmHasEntry(is(1.2397766E-4f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-3.7431717E-5f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-7.364154E-5f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-1.289323E-5f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvQuadraticMeanDiameter[1]", capCvQuadraticMeanDiameter.getValue()[1],
					allOf(
							mmHasEntry(is(0.007255554F), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-0.014289856f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-0.044784546F), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(0.0f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvQuadraticMeanDiameter[2]", capCvQuadraticMeanDiameter.getValue()[2],
					allOf(
							mmHasEntry(is(6.942749E-4f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-2.0217896E-4f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(6.008148E-4f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-1.2207031E-4f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvQuadraticMeanDiameter[3]", capCvQuadraticMeanDiameter.getValue()[3],
					allOf(
							mmHasEntry(is(3.6621094E-4F), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-0.008190155f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-0.0019168854f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-0.008535385F), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvQuadraticMeanDiameter[4]", capCvQuadraticMeanDiameter.getValue()[4],
					allOf(
							mmHasEntry(is(-0.0010547638f), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(-7.696152E-4f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-0.0012798309f), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(1.7547607E-4f), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvQuadraticMeanDiameter[5]", capCvQuadraticMeanDiameter.getValue()[5],
					allOf(
							mmHasEntry(is(-2.4032593E-4F), OVER225, LayerType.PRIMARY),
							mmHasEntry(is(9.880066E-4f), U125TO175, LayerType.PRIMARY),
							mmHasEntry(is(-0.005464554F), U175TO225, LayerType.PRIMARY),
							mmHasEntry(is(-7.9631805E-4F), U75TO125, LayerType.PRIMARY)
					)
			);
			assertThat(
					"CvPrimaryLayerSmall[1]", capCvPrimaryLayerSmall.getValue()[1],
					allOf(
							hasEntry(is(BASAL_AREA), closeTo(-2.1831444E-7f)),
							hasEntry(is(QUAD_MEAN_DIAMETER), is(0.0f)), hasEntry(is(LOREY_HEIGHT), is(0.0f)),
							hasEntry(is(WHOLE_STEM_VOLUME), is(0.0f))
					)
			);
			assertThat(
					"CvPrimaryLayerSmall[2]", capCvPrimaryLayerSmall.getValue()[2],
					allOf(
							hasEntry(is(BASAL_AREA), closeTo(-4.496146E-5f)),
							hasEntry(is(QUAD_MEAN_DIAMETER), closeTo(0.0023674965F)),
							hasEntry(is(LOREY_HEIGHT), closeTo(1.3113013E-6f)),
							hasEntry(is(WHOLE_STEM_VOLUME), closeTo(0.0010289619f))
					)
			);
			assertThat(
					"CvPrimaryLayerSmall[3]", capCvPrimaryLayerSmall.getValue()[3],
					allOf(
							hasEntry(is(BASAL_AREA), closeTo(4.9466034E-6f)),
							hasEntry(is(QUAD_MEAN_DIAMETER), is(0.0f)),
							hasEntry(is(LOREY_HEIGHT), closeTo(-1.5556934E-5f)),
							hasEntry(is(WHOLE_STEM_VOLUME), is(0.0f))
					)
			);
			assertThat(
					"CvPrimaryLayerSmall[4]", capCvPrimaryLayerSmall.getValue()[4],
					allOf(
							hasEntry(is(BASAL_AREA), is(0.0f)), hasEntry(is(QUAD_MEAN_DIAMETER), is(0.0f)),
							hasEntry(is(LOREY_HEIGHT), is(0.0f)), hasEntry(is(WHOLE_STEM_VOLUME), is(0.0f))
					)
			);
			assertThat(
					"CvPrimaryLayerSmall[5]", capCvPrimaryLayerSmall.getValue()[5],
					allOf(
							hasEntry(is(BASAL_AREA), closeTo(3.4208642E-6f)),
							hasEntry(is(QUAD_MEAN_DIAMETER), is(0.0f)),
							hasEntry(is(LOREY_HEIGHT), closeTo(-5.7758567E-5f)),
							hasEntry(is(WHOLE_STEM_VOLUME), is(0.0f))
					)
			);

		}
	}
}
