package ca.bc.gov.nrs.vdyp.back;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.compatibilityVariable;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
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
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class BackProcessingEngineTest {

	BackProcessingEngine engine;

	Map<String, Object> controlMap;

	BecLookup becLookup;

	@BeforeEach
	void setup() {
		engine = new BackProcessingEngine();

		controlMap = TestUtils.loadControlMap(); // TODO switch to appropriate control file for Back.

		becLookup = Utils.parsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get();

	}

	@Nested
	class Prepare {

		public BackProcessingState primaryOnlyWithSingleSpecies() throws ProcessingException {

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);

				pb.percentAvailable(80f);
				pb.biogeoclimaticZone(becLookup.get("IDF").get());
				pb.forestInventoryZone("");

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					final float ba = 5.0969491f;
					final float hl = 22.9584007f;
					final float dq = 31.5006275f;
					final float tph = BaseAreaTreeDensityDiameter.treesPerHectare(ba, dq);
					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
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

			var state = new BackProcessingState(controlMap);

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

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
				});
				pb.addLayer(lb -> {
					lb.layerType(LayerType.VETERAN);
					lb.baseAreaByUtilization(20f);

					lb.addSpecies(sb -> {
						sb.genus("F", controlMap);
						sb.baseArea(20f);
					});
				});
			});

			var state = new BackProcessingState(controlMap);

			state.setPolygon(polygon);

			return state;
		};

		@Nested
		class SetBav {

			@Test
			void testSetBavIfVeteranPresent() throws ProcessingException {

				var state = primaryAndVeteran();

				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, present(is(20f)));
			}

			@Test
			void testSetBavIfVeteranPresentAndTotalIsWrong() throws ProcessingException {
				var polygon = VdypPolygon.build(pb -> {
					pb.polygonIdentifier("Test", 2024);

					pb.percentAvailable(80f);
					pb.biogeoclimaticZone(becLookup.get("IDF").get());
					pb.forestInventoryZone("");

					pb.addLayer(lb -> {
						lb.layerType(LayerType.PRIMARY);
					});
					pb.addLayer(lb -> {
						lb.layerType(LayerType.VETERAN);
						lb.baseAreaByUtilization(42f); // Not the sum of the base areas of the species
						lb.addSpecies(sb -> {
							sb.genus("F", controlMap);
							sb.baseArea(20f);
						});
					});
				});

				var state = new BackProcessingState(controlMap);

				state.setPolygon(polygon);
				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, present(is(20f))); // Should be calculated from the species
			}

			@Test
			void testSetBavIfNoVeteranPresent() throws ProcessingException {

				var state = primaryOnlyWithSingleSpecies();

				engine.prepare(state);

				var result = state.getBaseAreaVeteran();

				assertThat(result, notPresent());
			}
		}

		@Nested
		class SetCv {

			@Test
			void testSingleSpecies() throws ProcessingException {

				var state = primaryOnlyWithSingleSpecies();

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

			}

		}

		@Nested
		class SetLimits {

			@Test
			void testSingleSpecies() throws ProcessingException {

				var state = primaryOnlyWithSingleSpecies();

				engine.prepare(state);

				var result = state.getLimits(1);

				assertThat(result, componentSizeLimits(39.9f, 75.8f, 0.792f, 2.155f, 0.0001f));

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
}
