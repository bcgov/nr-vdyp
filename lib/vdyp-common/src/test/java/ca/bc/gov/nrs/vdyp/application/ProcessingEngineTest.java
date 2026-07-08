package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Optional;

import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
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
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
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
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
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
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
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
			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("B", controlMap);
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
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
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
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("B", controlMap);
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
						sb.genus("S", controlMap);
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
}
