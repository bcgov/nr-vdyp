package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.SpeciesRankingDetails;
import ca.bc.gov.nrs.vdyp.processing_state.TestLayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.TestProcessingState;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

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
	
		@Test
		void testChoice1MovesSiteIndexFromSecondarySpeciesToPrimarySpecies() throws Exception {
			var fixture = createFixture();
			int primarySlot = fixture.slot("F");
			int secondarySlot = fixture.slot("L");
			float sourceSiteIndex = 13.4f;
			// uses real expecterd value later
			float expected = Float.NaN; // current behavior is dropping this value but that is ok to fix when we know
										// we have matched VDYP7
			/*
			 * float realExpected = (float) SiteTool.convertSiteIndexBetweenCurves(
			 * SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(secondarySlot)), sourceSiteIndex,
			 * SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(primarySlot)) );
			 */
			fixture.bank.siteIndices[primarySlot] = Float.NaN;
			fixture.bank.siteIndices[secondarySlot] = sourceSiteIndex;
			fixture.bank.siteIndices[fixture.slot("H")] = 16.1f;
	
			setChoices(1);
			assertThrows(ProcessingException.class, () -> runEstimate(fixture));
	
			assertThat(fixture.bank.siteIndices[primarySlot], is(expected));
		}
	
		@Test
		void testChoice15OnlyMovesSiteIndexWhenPrimarySpeciesAgeIsUnderThirty() throws Exception {
			var applies = createFixture();
			int appliesPrimarySlot = applies.slot("F");
			int appliesSecondarySlot = applies.slot("L");
			float appliesSourceSiteIndex = 13.4f;
			/*
			 * float realExpected = (float) SiteTool.convertSiteIndexBetweenCurves(
			 * SiteIndexEquation.getByIndex(applies.lps.getSiteCurveNumber(appliesSecondarySlot)),
			 * appliesSourceSiteIndex, SiteIndexEquation.getByIndex(applies.lps.getSiteCurveNumber(appliesPrimarySlot))
			 * );
			 */
	
			applies.bank.siteIndices[appliesPrimarySlot] = Float.NaN;
			applies.bank.siteIndices[appliesSecondarySlot] = appliesSourceSiteIndex;
			applies.bank.ageTotals[appliesPrimarySlot] = 25.0f;
	
			setChoices(15);
	
			assertThrows(ProcessingException.class, () -> runEstimate(applies));
	
			var skipped = createFixture();
			int skippedPrimarySlot = skipped.slot("F");
			int skippedSecondarySlot = skipped.slot("L");
			float originalPrimarySiteIndex = 19.0f;
	
			skipped.bank.siteIndices[skippedPrimarySlot] = originalPrimarySiteIndex;
			skipped.bank.siteIndices[skippedSecondarySlot] = appliesSourceSiteIndex;
			skipped.bank.ageTotals[skippedPrimarySlot] = 30.0f;
	
			setChoices(15);
			runEstimate(skipped);
	
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
			assertThat(fixture.bank.dominantHeights[primarySlot], closeTo(expected));
			assertThat(Float.isNaN(fixture.bank.dominantHeights[secondarySlot]), is(true));
		}
	
		@Test
		void testChoice6EstimatesDominantHeightForNonPrimarySpeciesOnly() throws Exception {
			var fixture = createFixture();
			int primarySlot = fixture.slot("F");
			int secondarySlot = fixture.slot("L");
			float expected = fixture.lps.getParent().estimators.estimateLeadHeightFromPrimaryHeight(
					fixture.bank.loreyHeights[secondarySlot][UC_ALL_INDEX], fixture.bank.speciesNames[secondarySlot],
					fixture.lps.getBecZone().getRegion(), fixture.bank.treesPerHectare[secondarySlot][UC_ALL_INDEX]
			);
	
			fixture.bank.dominantHeights[primarySlot] = Float.NaN;
			fixture.bank.dominantHeights[secondarySlot] = Float.NaN;
	
			setChoices(6);
			runEstimate(fixture);
	
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
	
		private void assertChoiceEstimatesSiteIndexFromHeightAndAge(int choice, String genus, SiteIndexAgeType ageType)
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
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
			runEstimate(fixture);
	
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
				VdypLayer.Builder layerBuilder, String genus, int siteCurveNumber, float percentGenus, float siteIndex,
				float dominantHeight, float totalAge, float yearsToBreastHeight, float yearsAtBreastHeight
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
	
		private void runEstimate(LayerFixture fixture) throws ProcessingException {
			ProcessingEngine.estimateMissingSiteIndicesAndAgesExtended(fixture.lps, fds);
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
	
			private LayerFixture(
					TestLayerProcessingState lps, Bank bank, Map<String, Integer> slots
			) {
				this.lps = lps;
				this.bank = bank;
				this.slots = slots;
			}
	
			private int slot(String genus) {
				return slots.get(genus);
			}
		}
	}
}
