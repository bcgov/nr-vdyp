package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.Change;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings.SpeciesDynamics;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class ForwardProcessingEngineTest {

	@Nested
	class ProcessPolygon {
		ForwardProcessingEngine fpe;
		Map<String, Object> controlMap;
		IMocksControl em;
		final Map<Integer, VdypPolygon> outputMap = new HashMap<>();

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ForwardControlParser();
			controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
			controlMap.put(
					ControlKey.VTROL.name(),
					new ForwardControlVariables(new Integer[] { -1, 1, 2, 3, 1, 1, 0, 0, 0, 0 })
			);
			em = EasyMock.createControl();
			VdypOutputWriter output = new VdypOutputWriter(
					controlMap, OutputStream.nullOutputStream(), OutputStream.nullOutputStream(),
					OutputStream.nullOutputStream()
			) {

				@Override
				public void writePolygonWithSpeciesAndUtilizationForYear(VdypPolygon polygon, int year)
						throws IOException {
					final var polyCopy = VdypPolygon.build(pb -> {
						pb.copy(polygon);
						pb.copyLayers(polygon, (lb, l) -> {
							lb.copySpecies(l, (sb, s) -> {
								sb.copySiteFrom(s, (ib, i) -> {
									// Do nothing
								});
							});
						});
					});
					outputMap.put(year, polyCopy);
				}

			};

			fpe = new ForwardProcessingEngine(controlMap, Optional.of(output));
		}

		@Test
		void testVdyp812() throws ProcessingException {
			final float percentAvailable = 83f;
			final float fraction = percentAvailable / 100;
			final var inputPoly = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("082L025       459", 1964);

				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.forestInventoryZone(" ");
				pb.inventoryTypeGroup(7);
				pb.mode(PolygonMode.START);
				pb.percentAvailable(percentAvailable);
				pb.targetYear(2123);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.empiricalRelationshipParameterIndex(60);
					lb.inventoryTypeGroup(7);
					lb.primaryGenus("F");

					lb.loreyHeightByUtilization(8.0215f, 19.6178f);

					lb.baseAreaByUtilization(
							0.027385544f, /* 24.936737f, */ 2.570687f, 4.922446f, 5.950603f, 11.493001f
					);
					lb.quadraticMeanDiameterByUtilization(
							6.037325f, 18.183805f, 10.097809f, 15.024368f, 19.883558f, 29.343294f
					);
					lb.treesPerHectareByUtilization(
							9.566265f, /* 960.241f, */ 321.0f, 277.6506f, 191.63857f, 169.95181f
					);

					lb.wholeStemVolumeByUtilization(
							0.10180723f, /* 181.18024f, */ 11.493495f, 30.97241f, 45.31157f, 93.40277f
					);
					lb.closeUtilizationVolumeByUtilization(
							0.0f, /* 146.82976f, */ 0.093855426f, 20.476025f, 39.108315f, 87.15169f
					);
					lb.closeUtilizationVolumeNetOfDecayByUtilization(
							0.0f, /* 142.9464f, */ 0.09301206f, 20.251808f, 38.45217f, 84.14952f
					);
					lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							0.0f, /* 141.65495f, */ 0.09277109f, 20.190603f, 38.26458f, 83.10699f
					);
					lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							0.0f, /* 137.93434f, */ 0.09060241f, 19.696987f, 37.2794f, 80.867226f
					);

					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);

						sb.breakageGroup(16);
						sb.decayGroup(27);
						sb.volumeGroup(33);

						sb.percentGenus(0.6f); // TODO Seems wrong should be 60f
						// sb.percentGenus(60f);

						sb.addSite(ib -> {
							ib.siteIndex(18.33f);
							ib.siteCurveNumber(96);

							ib.height(21f);

							ib.ageTotal(70f);
							ib.yearsAtBreastHeight(61.1f);
							ib.yearsToBreastHeight(9.4f);
						});

						sb.sp64DistributionSet(new Sp64DistributionSet(4, List.of())); // TODO this seems wrong, may
																						// need to fix parser

						sb.loreyHeight(7.7875f, 18.0594f);

						sb.baseArea(0.021361446f, /* 14.962049f, */ 2.1693978f, 3.157458f, 3.080265f, 6.554928f);
						sb.quadMeanDiameter(5.902309f, 17.314062f, 10.060733f, 15.029455f, 20.035698f, 30.987747f);
						sb.treesPerHectare(7.807229f, /* 635.482f, */ 272.89157f, 177.9759f, 97.69879f, 86.915665f);

						sb.wholeStemVolume(
								0.07903615f, /* 99.580246f, */ 9.413856f, 18.730844f, 21.876747f, 49.558796f
						);
						sb.closeUtilizationVolumeByUtilization(
								0.0f, /* 77.24904f, */ 0.06674699f, 12.073013f, 18.811207f, 46.298073f
						);
						sb.closeUtilizationVolumeNetOfDecayByUtilization(
								0.0f, /* 76.662056f, */ 0.066385545f, 12.01494f, 18.71253f, 45.868195f
						);
						sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
								0.0f, /* 76.4747f, */ 0.06626506f, 12.000121f, 18.680603f, 45.727592f
						);
						sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
								0.0f, /* 74.87422f, */ 0.06493976f, 11.758675f, 18.304459f, 44.746147f
						);
					});

					lb.addSpecies(sb -> {
						sb.genus("L", controlMap);

						sb.breakageGroup(20);
						sb.decayGroup(38);
						sb.volumeGroup(46);

						sb.percentGenus(0.4f); // TODO Seems wrong should be 40f
						// sb.percentGenus(40f);

						sb.sp64DistributionSet(new Sp64DistributionSet(4, List.of())); // TODO this seems wrong, may
																						// need to fix parser

						sb.loreyHeight(8.8518f, 21.9554f);

						sb.baseArea(0.0060240966f, /* 9.9747f, */ 0.4012892f, 1.764988f, 2.8703375f, 4.938072f);
						sb.quadMeanDiameter(6.603339f, 19.775358f, 10.304308f, 15.016189f, 19.724083f, 27.516943f);
						sb.treesPerHectare(1.7590363f, /* 324.75903f, */ 48.120483f, 99.66266f, 93.939766f, 83.03615f);

						sb.wholeStemVolume(
								0.022771085f, /* 81.59988f, */ 2.0796385f, 12.241567f, 23.434818f, 43.843975f
						);
						sb.closeUtilizationVolumeByUtilization(
								0.0f, /* 69.58085f, */ 0.027108436f, 8.403012f, 20.29711f, 40.85362f
						);
						sb.closeUtilizationVolumeNetOfDecayByUtilization(
								0.0f, /* 66.28422f, */ 0.026626507f, 8.236748f, 19.73964f, 38.281326f
						);
						sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
								0.0f, /* 65.180244f, */ 0.026506025f, 8.190482f, 19.583857f, 37.3794f
						);
						sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
								0.0f, /* 63.060005f, */ 0.025662651f, 7.9383135f, 18.974941f, 36.121086f
						);
					});
				});
			});

			em.replay();

			fpe.processPolygon(inputPoly);

			em.verify();

			// FIXME, these are wrong, testing that we have replicated the bug
			assertThat(
					outputMap.get(1964).getLayers().get(LayerType.PRIMARY),
					hasProperty(
							"treesPerHectareByUtilization",
							VdypMatchers.utilization(
									7.94f / fraction, 797.00f / fraction, 266.44f / fraction, 230.44f / fraction,
									159.06f / fraction, 141.06f / fraction
							)
					)
			);
			assertThat(
					outputMap.get(2023).getLayers().get(LayerType.PRIMARY),
					hasProperty(
							"treesPerHectareByUtilization",
							VdypMatchers.utilization(
									0.197f / fraction, 592.94f / fraction, 88.81f / fraction, 89.05f / fraction,
									113.55f / fraction, 301.54f / fraction
							)
					)
			);
		}
	}

	@Nested
	class GrowSpecies {
		IMocksControl em;
		ForwardProcessingEngine fpe;
		LayerProcessingState lps;
		Map<String, Object> controlMap;

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ForwardControlParser();
			controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
			controlMap.put(ControlKey.VTROL.name(), new ForwardControlVariables(new Integer[] {}));
			em = EasyMock.createControl();
			fpe = EasyMock.partialMockBuilder(ForwardProcessingEngine.class).addMockedMethod("growLoreyHeights")
					.addMockedMethod("growUsingPartialSpeciesDynamics").addMockedMethod("growUsingNoSpeciesDynamics")
					.addMockedMethod("growUsingFullSpeciesDynamics").withConstructor(controlMap, Optional.empty())
					.createMock(em);
			lps = em.createMock(LayerProcessingState.class);
		}

		@Test
		void testPartialSuccess() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(true);

			// Leads to no other methods being used.

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.PARTIAL
			);

			em.verify();
		}

		@Test
		void testPartialFailOneSpecies() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once();

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(EasyMock.eq(ba.rate(), 0.01f), EasyMock.eq(tph.factor(), 0.01f));
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.PARTIAL
			);

			em.verify();
		}

		@Test
		void testPartialFailTwoSpeciesNoGrowthDQ() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.0f); // No Growth
			var ba = Change.delta(45.386444f, 0.1f); // No Growth
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once(); // No Growth will result in failure

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(
					EasyMock.eq(0.002f, 0.001f), // No Growth
					EasyMock.eq(tph.factor(), 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.PARTIAL
			);

			em.verify();
		}

		@Test
		void testPartialFailTwoSpeciesNoGrowthBA() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.1f); // No Growth
			var ba = Change.delta(45.386444f, 0.0f); // No Growth
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once(); // No Growth will result in failure

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(
					EasyMock.eq(0.0f, 0.01f), // No Growth
					EasyMock.eq(tph.factor(), 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.PARTIAL
			);

			em.verify();
		}

		@Test
		void testDebugSpeciesDynamicsDisabled() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			// Go straight to No Species Dynamics
			fpe.growUsingNoSpeciesDynamics(EasyMock.eq(ba.rate(), 0.01f), EasyMock.eq(tph.factor(), 0.01f));
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 1, None
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.NONE
			);

			em.verify();
		}

		@Test
		void testDebug1SpeciesDynamicsFull() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			// Go straight to Full Species Dynamics
			fpe.growUsingFullSpeciesDynamics(
					EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.eq(30.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 0, full
			fpe.growSpecies(
					lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, SpeciesDynamics.FULL
			);

			em.verify();
		}

	}

	@Nested
	class EstimateSiteIndexAndAge {
		private static final int PRIMARY_SITE_CURVE_NUMBER = 99;
		private static final int SECONDARY_SITE_CURVE_NUMBER = 12;
		private static final int TERTIARY_SITE_CURVE_NUMBER = 11;
		private static final int UC_ALL_INDEX = UtilizationClass.ALL.ordinal();

		Map<String, Object> controlMap;
		ForwardDebugSettings fds;

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ForwardControlParser();
			controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
			controlMap.put(ControlKey.VTROL.name(), new ForwardControlVariables(new Integer[] {}));
			fds = new ForwardDebugSettings(new Integer[25]);
		}

		@Test
		void testChoice1MovesSiteIndexFromSecondarySpeciesToPrimarySpecies() throws Exception {
			var fixture = createFixture();
			int primarySlot = fixture.slot("F");
			int secondarySlot = fixture.slot("L");
			float sourceSiteIndex = 13.4f;
			// uses real expecterd value later
			float expected = Float.NaN; /// current behavior is dropping this value but that is ok to fix when we know
										/// we have matched VDYP7
			float realExpected = (float) SiteTool.convertSiteIndexBetweenCurves(
					SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(secondarySlot)), sourceSiteIndex,
					SiteIndexEquation.getByIndex(fixture.lps.getSiteCurveNumber(primarySlot))
			);

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
			float expected = (float) SiteTool.convertSiteIndexBetweenCurves(
					SiteIndexEquation.getByIndex(applies.lps.getSiteCurveNumber(appliesSecondarySlot)),
					appliesSourceSiteIndex,
					SiteIndexEquation.getByIndex(applies.lps.getSiteCurveNumber(appliesPrimarySlot))
			);

			applies.bank.siteIndices[appliesPrimarySlot] = Float.NaN;
			applies.bank.siteIndices[appliesSecondarySlot] = appliesSourceSiteIndex;
			applies.bank.ageTotals[appliesPrimarySlot] = 25.0f;

			setChoices(15);
			runEstimate(applies);

			assertThat(applies.bank.siteIndices[appliesPrimarySlot], closeTo(expected));

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
			float expected = fixture.lps.getFps().estimators.leadHeightFromPrimaryHeight(
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
			float expected = fixture.lps.getFps().estimators.leadHeightFromPrimaryHeight(
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
					ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType.SI_EST_DIRECT
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

		private LayerFixture createFixture() {
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("EstimateFixture", 2025);
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.forestInventoryZone("");
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.primaryGenus("F");
					addSpecies(lb, "F", PRIMARY_SITE_CURVE_NUMBER, 45.0f, 19.5f, 24.0f, 50.0f, 7.0f, 43.0f);
					addSpecies(lb, "L", SECONDARY_SITE_CURVE_NUMBER, 35.0f, 18.2f, 22.0f, 48.0f, 6.0f, 42.0f);
					addSpecies(lb, "H", TERTIARY_SITE_CURVE_NUMBER, 20.0f, 17.3f, 20.5f, 46.0f, 5.5f, 40.5f);
				});
			});

			var fps = new ForwardProcessingState(controlMap);
			fps.setPolygon(polygon);

			var lps = fps.getPrimaryLayerProcessingState();
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
				sb.genus(genus, controlMap);
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
			fds = new ForwardDebugSettings(new Integer[25]);
			int slot = 11;
			for (int choice : choices) {
				fds.setValue(slot++, choice);
			}
		}

		private void runEstimate(LayerFixture fixture) throws ProcessingException {
			ForwardProcessingEngine.estimateMissingSiteIndicesAndAgesExtended(fixture.lps, fds);
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
			private final LayerProcessingState lps;
			private final Bank bank;
			private final Map<String, Integer> slots;

			private LayerFixture(LayerProcessingState lps, Bank bank, Map<String, Integer> slots) {
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
