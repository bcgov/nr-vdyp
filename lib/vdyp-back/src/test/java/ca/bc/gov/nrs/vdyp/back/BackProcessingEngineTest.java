package ca.bc.gov.nrs.vdyp.back;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.compatibilityVariable;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecies;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.utilizationAllOnly;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackLayerProcessingState;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingStateTestUtils;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class BackProcessingEngineTest {

	BackProcessingEngine engine;

	Map<String, Object> rawControlMap;

	BecLookup becLookup;

	IMocksControl em;

	BackProcessingState state;

	ProcessingResolvedControlMapImpl controlMap;

	@BeforeEach
	void setup() {
		em = EasyMock.createControl();
		state = em.createMock(BackProcessingState.class);

		engine = new BackProcessingEngine(state);

		rawControlMap = TestUtils.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));

		becLookup = Utils.parsedControl(rawControlMap, ControlKey.BEC_DEF, BecLookup.class).get();

		controlMap = new ProcessingResolvedControlMapImpl(rawControlMap);
		var emp = new EstimationMethods(controlMap);
		var cmp = new ComputationMethods(emp, VdypApplicationIdentifier.VDYP_BACK);

		expect(state.getEstimators()).andStubReturn(emp);
		expect(state.getComputers()).andStubReturn(cmp);
	}

	@Nested
	class Prepare {

		public BackProcessingState primaryOnlyWithSingleSpecies() throws ProcessingException {

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);

				pb.percentAvailable(80f);
				pb.biogeoclimaticZone(becLookup.get("IDF").get());
				pb.forestInventoryZone("");
				pb.controlMap(rawControlMap);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					final float ba = 5.0969491f;
					final float hl = 22.9584007f;
					final float dq = 31.5006275f;
					final float tph = BaseAreaTreeDensityDiameter.treesPerHectare(ba, dq);
					lb.addSpecies(sb -> {
						sb.speciesGroup("F");
						sb.baseArea(ba);
						sb.loreyHeight(hl);
						sb.quadMeanDiameter(dq);
						sb.treesPerHectare(tph);
					});

					lb.baseAreaByUtilization(ba);
					lb.loreyHeightByUtilization(hl);
					lb.quadraticMeanDiameterByUtilization(dq);
					lb.treesPerHectareByUtilization(tph);
				});
			});

			state = new BackProcessingState(rawControlMap);

			state.setPolygon(polygon);

			@SuppressWarnings("unchecked")
			MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>[] cvVolume = new MatrixMap3[] { null,
					new MatrixMap3Impl<UtilizationClass, VolumeVariable, LayerType, Float>(
							List.of(UtilizationClass.values()), List.of(VolumeVariable.values()),
							List.of(LayerType.values()),
							(uc, vv, lt) -> 11f + vv.ordinal() * 2f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };

			@SuppressWarnings("unchecked")
			MatrixMap2<UtilizationClass, LayerType, Float>[] cvBa = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
							List.of(UtilizationClass.values()), List.of(LayerType.values()),
							(uc, lt) -> 13f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };

			@SuppressWarnings("unchecked")
			MatrixMap2<UtilizationClass, LayerType, Float>[] cvDq = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
							List.of(UtilizationClass.values()), List.of(LayerType.values()),
							(uc, lt) -> 17f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };
			@SuppressWarnings("unchecked")
			Map<UtilizationClassVariable, Float>[] cvSm = new EnumMap[] { null,
					new EnumMap<UtilizationClassVariable, Float>(UtilizationClassVariable.class) };

			for (var uc : UtilizationClassVariable.values()) {
				cvSm[1].put(uc, uc.ordinal() * 7f);
			}

			state.getPrimaryLayerProcessingState().setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm);

			return state;
		};

		public BackProcessingState primaryAndVeteran() throws ProcessingException {
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);

				pb.percentAvailable(80f);
				pb.biogeoclimaticZone(becLookup.get("IDF").get());
				pb.forestInventoryZone("");
				pb.controlMap(rawControlMap);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.baseAreaByUtilization(20f);

					lb.addSpecies(sb -> {
						sb.speciesGroup("F");
						sb.baseArea(20f);
					});
				});
			});

			state = new BackProcessingState(rawControlMap);

			state.setPolygon(polygon);

			return state;
		};

		@Nested
		class SetBav {

			@Test
			void testSetBavIfVeteranPresent() throws ProcessingException {

				em.replay();

				state = primaryAndVeteran();

				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, present(is(20f)));

				em.verify();

			}

			@Test
			void testSetBavIfVeteranPresentAndTotalIsWrong() throws ProcessingException {

				em.replay();

				var polygon = VdypPolygon.build(pb -> {
					pb.polygonIdentifier("Test", 2024);

					pb.percentAvailable(80f);
					pb.biogeoclimaticZone(becLookup.get("IDF").get());
					pb.forestInventoryZone("");
					pb.controlMap(rawControlMap);

					pb.addLayer(lb -> {
						lb.layerType(LayerType.PRIMARY);
					});
					pb.addLayer(lb -> {
						lb.layerType(LayerType.VETERAN);
						lb.baseAreaByUtilization(42f); // Not the sum of the base areas of the species
						lb.addSpecies(sb -> {
							sb.speciesGroup("F");
							sb.baseArea(20f);
						});
					});
				});

				state = new BackProcessingState(rawControlMap);

				state.setPolygon(polygon);
				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, present(is(20f))); // Should be calculated from the species

				em.verify();

			}

			@Test
			void testSetBavIfNoVeteranPresent() throws ProcessingException {

				em.replay();

				state = primaryOnlyWithSingleSpecies();

				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, notPresent());

				em.verify();

			}
		}

		@Nested
		class SetCv {

			@Test
			void testSingleSpecies() throws ProcessingException {

				em.replay();

				state = primaryOnlyWithSingleSpecies();

				engine.prepare(state);

				final int specIndex = 1; // Only one species

				assertThat(state, backCV("getCVBasalArea", specIndex, UtilizationClass.ALL, is(16f)));
				assertThat(state, backCV("getCVBasalArea", specIndex, UtilizationClass.U75TO125, is(19f)));
				assertThat(state, backCV("getCVBasalArea", specIndex, UtilizationClass.U125TO175, is(22f)));
				assertThat(state, backCV("getCVBasalArea", specIndex, UtilizationClass.U175TO225, is(25f)));
				assertThat(state, backCV("getCVBasalArea", specIndex, UtilizationClass.OVER225, is(28f)));

				assertThat(state, backCV("getCVQuadraticMeanDiameter", specIndex, UtilizationClass.ALL, is(20f)));
				assertThat(state, backCV("getCVQuadraticMeanDiameter", specIndex, UtilizationClass.U75TO125, is(23f)));
				assertThat(state, backCV("getCVQuadraticMeanDiameter", specIndex, UtilizationClass.U125TO175, is(26f)));
				assertThat(state, backCV("getCVQuadraticMeanDiameter", specIndex, UtilizationClass.U175TO225, is(29f)));
				assertThat(state, backCV("getCVQuadraticMeanDiameter", specIndex, UtilizationClass.OVER225, is(32f)));

				em.verify();

			}

		}

		@Nested
		class SetLimits {

			@Test
			void testSingleSpecies() throws ProcessingException {

				em.replay();

				state = primaryOnlyWithSingleSpecies();

				engine.prepare(state);

				var result = state.getLimits(1);

				assertThat(result, componentSizeLimits(39.9f, 75.8f, 0.792f, 2.155f, 0.0001f));

				em.verify();

			}
		}

	}

	static Matcher<ComponentSizeLimits> componentSizeLimits(
			float loreyHeightMaximum, float quadMeanDiameterMaximum, float minQuadMeanDiameterLoreyHeightRatio,
			float maxQuadMeanDiameterLoreyHeightRatio, float epsilon
	) {
		return new TypeSafeDiagnosingMatcher<ComponentSizeLimits>(ComponentSizeLimits.class) {

			@Override
			public void describeTo(Description description) {
				description.appendText("ComponentSizeLimits with");
				description.appendText(" ");
				description.appendText("loreyHeightMaximum = ").appendValue(loreyHeightMaximum);
				description.appendText(", ");
				description.appendText("quadMeanDiameterMaximum = ").appendValue(quadMeanDiameterMaximum);
				description.appendText(", ");
				description.appendText("minQuadMeanDiameterLoreyHeightRatio = ")
						.appendValue(minQuadMeanDiameterLoreyHeightRatio);
				description.appendText(", and ");
				description.appendText("maxQuadMeanDiameterLoreyHeightRatio = ")
						.appendValue(maxQuadMeanDiameterLoreyHeightRatio);

				description.appendText(" (all to within ±" + epsilon + ")");
			}

			static boolean matchFloatsSafely(float o1, float o2, float epsilon) {
				if (Float.isNaN(o1) && Float.isNaN(o2))
					return true;
				if (Float.isNaN(o1) || Float.isNaN(o2))
					return false;
				return FloatMath.abs(o1 - o2) <= epsilon;
			}

			@Override
			protected boolean matchesSafely(ComponentSizeLimits item, Description mismatchDescription) {
				boolean match = true;
				if (!matchFloatsSafely(item.loreyHeightMaximum(), loreyHeightMaximum, epsilon)) {
					match = false;
					mismatchDescription.appendText("loreyHeightMaximum was ").appendValue(item.loreyHeightMaximum());
				}
				if (!matchFloatsSafely(item.quadMeanDiameterMaximum(), quadMeanDiameterMaximum, epsilon)) {
					if (!match)
						mismatchDescription.appendText(", ");
					match = false;
					mismatchDescription.appendText("quadMeanDiameterMaximum was ")
							.appendValue(item.quadMeanDiameterMaximum());
				}
				if (!matchFloatsSafely(
						item.minQuadMeanDiameterLoreyHeightRatio(), minQuadMeanDiameterLoreyHeightRatio, epsilon
				)) {
					if (!match)
						mismatchDescription.appendText(", ");
					match = false;
					mismatchDescription.appendText("minQuadMeanDiameterLoreyHeightRatio was ")
							.appendValue(item.minQuadMeanDiameterLoreyHeightRatio());
				}
				if (!matchFloatsSafely(
						item.maxQuadMeanDiameterLoreyHeightRatio(), maxQuadMeanDiameterLoreyHeightRatio, epsilon
				)) {
					if (!match)
						mismatchDescription.appendText(", ");
					match = false;
					mismatchDescription.appendText("maxQuadMeanDiameterLoreyHeightRatio was ")
							.appendValue(item.maxQuadMeanDiameterLoreyHeightRatio());
				}
				return match;
			}

		};
	}

	static Matcher<BackProcessingState> backCV(String name, Object p1, Object p2, Matcher<Float> expected) {
		return compatibilityVariable(name, expected, BackProcessingState.class, p1, p2);
	}

	static Matcher<BackProcessingState> backCV(String name, Object p1, Matcher<Float> expected) {
		return compatibilityVariable(name, expected, BackProcessingState.class, p1);
	}

	@Nested
	class HeightFromSiteCurve {
		@Test
		void testYoung() throws ProcessingException {

			expect(state.getControlMap()).andStubReturn(new ProcessingResolvedControlMapImpl(rawControlMap));
			expect(state.getCurrentBecZone()).andStubReturn(becLookup.get("MS").get());

			em.replay();

			var height = engine.heightFromSiteCurve(45, 4.3f, 8.2f, 12.39f);
			assertThat(height, closeTo(1.90864f));

			em.verify();
		}

		@Test
		void testOld() throws ProcessingException {

			expect(state.getControlMap()).andStubReturn(new ProcessingResolvedControlMapImpl(rawControlMap));
			expect(state.getCurrentBecZone()).andStubReturn(becLookup.get("MS").get());

			em.replay();

			var height = engine.heightFromSiteCurve(45, 150f, 8.2f, 12.39f);
			assertThat(height, closeTo(20.43617f));

			em.verify();
		}
	}

	@Nested
	class ApplyBackupFactors {
		@Test
		void test() throws ProcessingException {

			expect(state.getControlMap()).andStubReturn(new ProcessingResolvedControlMapImpl(rawControlMap));
			expect(state.getCurrentBecZone()).andStubReturn(becLookup.get("MS").get());

			// HD_CNV
			expect(state.getConvergenceDominantHeight()).andStubReturn(Optional.of(9.17361069f));

			// BFH
			expect(state.getDominantHeightBackupFactor()).andStubReturn(Optional.of(1.00081587f));

			// BFHLI
			expect(state.getSpeciesLoreyHeightBackupFactor(1)).andStubReturn(1f);
			expect(state.getSpeciesLoreyHeightBackupFactor(2)).andStubReturn(1.05425811f);
			expect(state.getSpeciesLoreyHeightBackupFactor(3)).andStubReturn(1.65608633f);

			// HLI_CNV
			expect(state.getSpeciesConvergenceLoreyHeight(1)).andStubReturn(8.26395798f);
			expect(state.getSpeciesConvergenceLoreyHeight(2)).andStubReturn(7.30057955f);
			expect(state.getSpeciesConvergenceLoreyHeight(3)).andStubReturn(7.28091431f);

			// BFMAXHLI
			expect(state.getSpeciesLoreyHeightBackupFactorMaximum(1)).andStubReturn(12.8473997f);
			expect(state.getSpeciesLoreyHeightBackupFactorMaximum(2)).andStubReturn(13.8270998f);
			expect(state.getSpeciesLoreyHeightBackupFactorMaximum(3)).andStubReturn(16.6229f);

			// BA_CNV
			expect(state.getConvergenceBasalArea()).andStubReturn(Optional.of(10.6703072f));

			// BFB
			expect(state.getBasalAreaBackupFactor()).andStubReturn(Optional.of(1.40641582f));

			// DQ_CNV
			expect(state.getConvergenceQuadraticMeanDiameter()).andStubReturn(Optional.of(10.2024593f));

			// BFDQ
			expect(state.getQuadMeanDiameterBackupFactor()).andStubReturn(Optional.of(1.86042416f));

			// BFDQI
			expect(state.getSpeciesQuadMeanDiameterBackupFactor(1)).andStubReturn(1.00001431f);
			expect(state.getSpeciesQuadMeanDiameterBackupFactor(2)).andStubReturn(0.999987602f);
			expect(state.getSpeciesQuadMeanDiameterBackupFactor(3)).andStubReturn(1.00003755f);

			// DQI_CNV
			expect(state.getSpeciesConvergenceQuadraticMeanDiameter(1)).andStubReturn(9.88628769f);
			expect(state.getSpeciesConvergenceQuadraticMeanDiameter(2)).andStubReturn(10.3321753f);
			expect(state.getSpeciesConvergenceQuadraticMeanDiameter(3)).andStubReturn(9.93369484f);

			// BFMINDQI
			expect(state.getSpeciesQuadMeanDiameterBackupFactorMinimum(1)).andStubReturn(9.88628769f);
			expect(state.getSpeciesQuadMeanDiameterBackupFactorMinimum(2)).andStubReturn(10.3321753f);
			expect(state.getSpeciesQuadMeanDiameterBackupFactorMinimum(3)).andStubReturn(9.93369484f);

			// BFMINDQ
			expect(state.getQuadMeanDiameterBackupFactorMinimum()).andStubReturn(Optional.of(10.2024593f));

			// BAV
			expect(state.getBaseAreaVeteran()).andStubReturn(Optional.empty());

			// BACK7
			expect(state.getLimits(1)).andStubReturn(new ComponentSizeLimits(32.4f, 38.4f, 0.744f, 1.541f));
			expect(state.getLimits(2)).andStubReturn(new ComponentSizeLimits(32.0f, 40.7f, 0.757f, 1.705f));
			expect(state.getLimits(3)).andStubReturn(new ComponentSizeLimits(39.1f, 57.2f, 0.796f, 1.809f));

			BackLayerProcessingState layerState = em.createMock(BackLayerProcessingState.class);
			expect(layerState.getPrimarySpeciesIndex()).andStubReturn(2);
			expect(layerState.getIndices()).andStubReturn(new int[] { 1, 2, 3 });
			expect(layerState.getNSpecies()).andStubReturn(3);

			expect(state.getPrimaryLayerProcessingState()).andStubReturn(layerState);

			var polygon = VdypPolygon.build(pb -> {
				pb.controlMap(rawControlMap);
				pb.polygonIdentifier("092P037  72999905UNK 2011");
				pb.biogeoclimaticZone("MS");
				pb.forestInventoryZone("");
				pb.percentAvailable(61f);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.speciesIndex(3);
						sb.percentGenus(10);
					});
					lb.addSpecies(sb -> {
						sb.speciesIndex(12);
						sb.percentGenus(70);
						sb.addSite(ib -> {
							ib.siteCurveNumber(45);
							ib.yearsAtBreastHeight(77.3f);
							ib.yearsToBreastHeight(8.2f);
							ib.ageTotal(85f);
							ib.siteIndex(12.39f);
							ib.height(16f);
						});
					});
					lb.addSpecies(sb -> {
						sb.speciesIndex(15);
						sb.percentGenus(20);
					});
					lb.primaryGenus("PL");
					lb.empiricalRelationshipParameterIndex(118);
				});
			});
			var primaryLayer = polygon.getLayers().get(LayerType.PRIMARY);

			// Fill in Utilization

			primaryLayer.setLoreyHeightByUtilization(Utils.heightVector(6.11998129f, 14.2882891f));
			primaryLayer.setBaseAreaByUtilization(
					Utils.utilizationVector(
							0.0546229482f, 40.9836578f, 3.78800011f, 11.8987055f, 14.0205078f, 11.2764416f
					)
			);
			primaryLayer.setTreesPerHectareByUtilization(
					Utils.utilizationVector(20.5737705f, 1803.26233f, 466.262329f, 677.950806f, 464.049194f, 195f)
			);
			primaryLayer.setQuadraticMeanDiameterByUtilization(
					Utils.utilizationVector(5.81501532f, 17.0110435f, 10.1703596f, 14.9486017f, 19.613493f, 27.1346054f)
			);
			primaryLayer.setWholeStemVolumeByUtilization(
					Utils.utilizationVector(
							0.163934425f, 251.342957f, 15.7932787f, 65.5157318f, 88.3898392f, 81.6440964f
					)
			);
			primaryLayer.setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 204.092285f, 2.0988524f, 48.9055748f, 77.6209793f, 75.4668808f)
			);
			primaryLayer.setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 199.286377f, 2.05327868f, 47.9527855f, 76.0255737f, 73.2547531f)
			);
			primaryLayer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 197.866379f, 2.04245877f, 47.7080307f, 75.5704956f, 72.5454102f)
			);
			primaryLayer.getOrderedSpecies().get(0)
					.setLoreyHeightByUtilization(Utils.heightVector(5.72300005f, 12.8473997f));
			primaryLayer.getOrderedSpecies().get(0).setBaseAreaByUtilization(
					Utils.utilizationVector(
							0.028147541f, 4.09836054f, 1.34257376f, 1.134377f, 0.789475381f, 0.831934452f
					)
			);
			primaryLayer.getOrderedSpecies().get(0).setTreesPerHectareByUtilization(
					Utils.utilizationVector(
							11.0819674f, 284.674377f, 175.262299f, 68.0819626f, 26.8688507f, 14.4590158f
					)
			);
			primaryLayer.getOrderedSpecies().get(0).setQuadraticMeanDiameterByUtilization(
					Utils.utilizationVector(5.6867857f, 13.5389805f, 9.87597275f, 14.5652428f, 19.3419304f, 27.0663853f)
			);
			primaryLayer.getOrderedSpecies().get(0).setWholeStemVolumeByUtilization(
					Utils.utilizationVector(
							0.0836065561f, 22.3908195f, 5.31590176f, 6.03557348f, 4.96803236f, 6.07131147f
					)
			);
			primaryLayer.getOrderedSpecies().get(0).setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 14.2563934f, 0.357704908f, 4.15590143f, 4.18754101f, 5.55524588f)
			);
			primaryLayer.getOrderedSpecies().get(0).setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 13.4095078f, 0.339508206f, 3.94229484f, 3.96098328f, 5.16672134f)
			);
			primaryLayer.getOrderedSpecies().get(0).setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 13.1586876f, 0.334262282f, 3.87737703f, 3.88999987f, 5.05704927f)
			);
			primaryLayer.getOrderedSpecies().get(1)
					.setLoreyHeightByUtilization(Utils.heightVector(7.18720007f, 13.8270998f));
			primaryLayer.getOrderedSpecies().get(1).setBaseAreaByUtilization(
					Utils.utilizationVector(
							0.0106229503f, 28.6885567f, 1.96814752f, 9.33654118f, 11.2569828f, 6.12688494f
					)
			);
			primaryLayer.getOrderedSpecies().get(1).setTreesPerHectareByUtilization(
					Utils.utilizationVector(3.67213106f, 1250.29932f, 232.590164f, 528.704895f, 373.098358f, 115.91803f)
			);
			primaryLayer.getOrderedSpecies().get(1).setQuadraticMeanDiameterByUtilization(
					Utils.utilizationVector(
							6.06901979f, 17.0923748f, 10.3797817f, 14.9948254f, 19.5999241f, 25.9417591f
					)
			);
			primaryLayer.getOrderedSpecies().get(1).setWholeStemVolumeByUtilization(
					Utils.utilizationVector(
							0.0390163921f, 174.294434f, 8.95180321f, 52.6093445f, 71.0681992f, 41.665081f
					)
			);
			primaryLayer.getOrderedSpecies().get(1).setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 142.948029f, 1.6654098f, 39.9722939f, 62.7765579f, 38.5337677f)
			);
			primaryLayer.getOrderedSpecies().get(1).setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 140.00705f, 1.63885248f, 39.2903252f, 61.5590134f, 37.5188522f)
			);
			primaryLayer.getOrderedSpecies().get(1).setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 139.179657f, 1.63344252f, 39.124752f, 61.2242622f, 37.1972122f)
			);
			primaryLayer.getOrderedSpecies().get(2)
					.setLoreyHeightByUtilization(Utils.heightVector(6.1097002f, 16.6229f));
			primaryLayer.getOrderedSpecies().get(2).setBaseAreaByUtilization(
					Utils.utilizationVector(
							0.0158524588f, 8.19673729f, 0.47727865f, 1.42778683f, 1.97404909f, 4.31762266f
					)
			);
			primaryLayer.getOrderedSpecies().get(2).setTreesPerHectareByUtilization(
					Utils.utilizationVector(
							5.81967211f, 268.288696f, 58.4098358f, 81.1639328f, 64.0819626f, 64.6229477f
					)
			);
			primaryLayer.getOrderedSpecies().get(2).setQuadraticMeanDiameterByUtilization(
					Utils.utilizationVector(5.88917017f, 19.7230644f, 10.1999512f, 14.9659815f, 19.8046036f, 29.166481f)
			);
			primaryLayer.getOrderedSpecies().get(2).setWholeStemVolumeByUtilization(
					Utils.utilizationVector(
							0.0413114727f, 54.6577034f, 1.52557373f, 6.87081909f, 12.3536062f, 33.9077034f
					)
			);
			primaryLayer.getOrderedSpecies().get(2).setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 46.887867f, 0.0757376999f, 4.77737713f, 10.6568851f, 31.3778667f)
			);
			primaryLayer.getOrderedSpecies().get(2).setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 45.8698349f, 0.0749180317f, 4.72016382f, 10.5055733f, 30.5691795f)
			);
			primaryLayer.getOrderedSpecies().get(2).setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 45.5280304f, 0.0747540966f, 4.70590162f, 10.4562292f, 30.2911472f)
			);

			// Create bank

			var bank = new Bank(primaryLayer, polygon.getBiogeoclimaticZone(), x -> true);
			ProcessingStateTestUtils.fill(bank.percentagesOfForestedLand, 0f, 10f, 70f, 20f);

			expect(layerState.getPolygon()).andStubReturn(polygon);
			expect(layerState.getBank()).andStubReturn(bank);
			expect(state.getCurrentStartingYear()).andStubReturn(2011); // IYRFIRST

			em.replay();

			engine.applyBackupFactors(1995);

			em.verify();

			var primarySite = primaryLayer.getPrimarySpeciesRecord().get().getSite().get();
			assertThat(primarySite, hasProperty("height", present(closeTo(14.0952682f))));
			assertThat(primarySite, hasProperty("ageTotal", present(closeTo(69f))));
			assertThat(primarySite, hasProperty("yearsAtBreastHeight", present(closeTo(61.3f))));
			assertThat(primarySite, hasProperty("yearsToBreastHeight", present(closeTo(8.2f))));

			assertThat(
					primaryLayer,
					allOf(
							hasProperty("loreyHeightByUtilization", utilizationAllOnly(12.3255749f)),
							hasProperty("baseAreaByUtilization", utilizationAllOnly(33.975399f)),
							hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(14.7094793f)),
							hasProperty("treesPerHectareByUtilization", utilizationAllOnly(1999.30969f))
					)
			);
			assertThat(
					primaryLayer,
					hasSpecies(
							"B",
							allOf(
									hasProperty("loreyHeightByUtilization", utilizationAllOnly(11.6197081f)),
									hasProperty("baseAreaByUtilization", utilizationAllOnly(3.3975358f)),
									hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(12.2343445f)),
									hasProperty("treesPerHectareByUtilization", utilizationAllOnly(289.009918f))
							)
					)
			);
			assertThat(
					primaryLayer,
					hasSpecies(
							"PL",
							allOf(
									hasProperty("loreyHeightByUtilization", utilizationAllOnly(11.9535027f)),
									hasProperty("baseAreaByUtilization", utilizationAllOnly(23.7827759f)),
									hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(14.7683105f)),
									hasProperty("treesPerHectareByUtilization", utilizationAllOnly(1388.38855f))
							)
					)
			);
			assertThat(
					primaryLayer,
					hasSpecies(
							"S",
							allOf(
									hasProperty("loreyHeightByUtilization", utilizationAllOnly(13.9807625f)),
									hasProperty("baseAreaByUtilization", utilizationAllOnly(6.79508448f)),
									hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(16.39398f)),
									hasProperty("treesPerHectareByUtilization", utilizationAllOnly(321.911285f))
							)
					)
			);
		}

	}
}
