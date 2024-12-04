package ca.bc.gov.nrs.vdyp.test;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.deepEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.processing_state.Bank;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.TestProcessingState;

public class TestUtilsTest {
	Random rand;

	@BeforeEach
	void setup() {
		// Setting the seed to a fixed value before each test should make the WriteState tests repeatable.
		rand = new Random(42);
	}

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
	class WriteProcessingState {
		@Test
		void testBank() throws IOException {
			var controlMap = TestUtils.loadControlMap();
			var expectedBec = Utils.getBec("CDF", controlMap);
			var expectedLayer = VdypLayer.build(lb -> {
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

			Bank expected = new Bank(expectedLayer, expectedBec, x -> true);

			// Change the bank values from those initially set to match the layer
			jitterBank(expected);

			expected.percentagesOfForestedLand[1] = 70;

			var buf = new StringBuffer();
			TestUtils.writeModel(expected, buf, 3, "result", "expectedLayer");

			System.out.print(buf.toString());

			Bank result = null;

			/* the following Bank definition was generated */

			var validSpecGroupIndices = List.of(0, 10);
			result = new Bank(
					expectedLayer, Utils.getBec("CDF", controlMap),
					spec -> validSpecGroupIndices.contains(spec.getGenusIndex())
			);
			System.arraycopy(new float[] { 0.000000f, 41.552448f }, 0, result.ageTotals, 0, 2);
			System.arraycopy(new float[] { 0.000000f, 70.000000f }, 0, result.percentagesOfForestedLand, 0, 2);
			System.arraycopy(
					new float[][] { { 1.073338f, 3.073815f, 0.181232f, 1.592524f, 1.446236f, 0.064980f },
							{ 1.052509f, 3.325452f, 0.180743f, 1.492788f, 1.646765f, 0.059891f } },
					0, result.basalAreas, 0, 2
			);
			System.arraycopy(new String[] { null, "MB" }, 0, result.speciesNames, 0, 2);
			System.arraycopy(new float[] { 0.000000f, 4.253897f }, 0, result.siteIndices, 0, 2);
			System.arraycopy(new float[] { 0.000000f, 14.915393f }, 0, result.dominantHeights, 0, 2);
			System.arraycopy(new float[] { 0.000000f, 38.343075f }, 0, result.yearsAtBreastHeight, 0, 2);
			System.arraycopy(new float[] { 0.000000f, 5.202045f }, 0, result.yearsToBreastHeight, 0, 2);
			System.arraycopy(new int[] { -1, 41 }, 0, result.siteCurveNumbers, 0, 2);

			/* End of generated Bank definition */

			assertThat(result, deepEquals(expected));
		}

		public void jitterBank(Bank expected) {
			TestUtils.jitterArray(expected.ageTotals, rand);
			TestUtils.jitterArray(expected.basalAreas, rand);
			TestUtils.jitterArray(expected.dominantHeights, rand);
			TestUtils.jitterArray(expected.siteCurveNumbers, rand);
			TestUtils.jitterArray(expected.siteIndices, rand);
			TestUtils.jitterArray(expected.yearsAtBreastHeight, rand);
			TestUtils.jitterArray(expected.yearsToBreastHeight, rand);
		}

		@Test
		void testProcessingState() throws IOException, ProcessingException {
			var controlMap = TestUtils.loadControlMap();

			var expectedPolygon = VdypPolygon.build(pb -> {

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
				pb.addLayer(lb -> {

					lb.layerType(LayerType.VETERAN);

					lb.empiricalRelationshipParameterIndex(21);
					lb.inventoryTypeGroup(34);
					lb.primaryGenus("S");

					lb.addSpecies(sb -> {
						sb.genus("S");
						sb.controlMap(controlMap);

						sb.percentGenus(90);

						sb.breakageGroup(12);
						sb.decayGroup(13);
						sb.volumeGroup(14);

						sb.addSp64Distribution("S", 100);

						sb.addCompatibilityVariables(cvb -> {
							cvb.cvVolume((k1, k2, k3) -> rand.nextFloat() * 10);
							cvb.cvBasalArea((k1, k2) -> rand.nextFloat() * 10);
							cvb.cvQuadraticMeanDiameter((k1, k2) -> rand.nextFloat() * 10);
							cvb.cvPrimaryLayerSmall(k1 -> rand.nextFloat() * 10);
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

			var expectedState = new TestProcessingState(controlMap);
			expectedState.setPolygon(expectedPolygon);

			jitterBank(expectedState.getPrimaryLayerProcessingState().getBank());
			jitterBank(expectedState.getVeteranLayerProcessingState().get().getBank());

			var buf = new StringBuffer();
			TestUtils.writeModel(expectedState, buf, 3, "result");

			System.out.print(buf.toString());

			ProcessingState result = null;

			{
				/* the following ProcessingState definition was generated */

				/* the following Polygon definition was generated */

				var polygon = VdypPolygon.build(pb -> {
					pb.polygonIdentifier("Test", 2024);

					pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
					pb.forestInventoryZone("Z");

					pb.inventoryTypeGroup(Optional.empty());
					pb.targetYear(Optional.empty());

					pb.mode(Optional.empty());
					pb.percentAvailable(90.000000f);

					pb.addLayer(lb -> {
						lb.layerType(ca.bc.gov.nrs.vdyp.model.LayerType.PRIMARY);

						lb.empiricalRelationshipParameterIndex(Optional.of(21));

						lb.inventoryTypeGroup(Optional.of(34));

						lb.loreyHeight(Utils.heightVector(3.637818f, 1.093304f));
						lb.treesPerHectare(
								Utils.utilizationVector(
										110.634865f, 543.441101f, 114.492920f, 82.724403f, 207.127731f, 139.096069f
								) /* ALL does not match sum of bands */
						);
						lb.quadMeanDiameter(
								Utils.utilizationVector(
										7.077106f, 21.115028f, 6.655489f, 0.913246f, 9.033722f, 4.512572f
								) /* ALL does not match sum of bands */
						);
						lb.baseArea(
								Utils.utilizationVector(
										1.366447f, 3.151621f, 0.095879f, 0.617439f, 1.884147f, 0.554157f
								) /* ALL does not match sum of bands */
						);

						lb.wholeStemVolume(
								Utils.utilizationVector(
										5.334631f, 19.966562f, 5.480312f, 6.987250f, 6.435294f, 1.063708f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeByUtilization(
								Utils.utilizationVector(
										2.618946f, 15.041424f, 2.638799f, 4.499437f, 5.583787f, 2.319401f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayByUtilization(
								Utils.utilizationVector(
										3.991433f, 6.302918f, 0.886892f, 0.752737f, 2.971749f, 1.691540f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
								Utils.utilizationVector(
										0.839070f, 6.456025f, 1.002645f, 3.303863f, 1.460645f, 0.688872f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
								Utils.utilizationVector(
										0.475964f, 6.769507f, 1.762282f, 0.822518f, 2.253841f, 1.930866f
								) /* ALL does not match sum of bands */
						);

						lb.addSpecies(sb -> {
							sb.genus("MB");
							sb.genus(10);

							sb.breakageGroup(12);
							sb.volumeGroup(14);
							sb.decayGroup(13);

							sb.percentGenus(90.000000f);

							sb.addSp64Distribution("MB", 100.000000f);

							sb.loreyHeight(Utils.heightVector(2.855202f, 1.605718f));
							sb.treesPerHectare(
									Utils.utilizationVector(
											125.306259f, 761.289429f, 230.860580f, 292.210693f, 24.196316f, 214.021866f
									) /* ALL does not match sum of bands */
							);
							sb.quadMeanDiameter(
									Utils.utilizationVector(
											3.165325f, 13.858646f, 3.579199f, 1.990815f, 8.177969f, 0.110663f
									) /* ALL does not match sum of bands */
							);
							sb.baseArea(
									Utils.utilizationVector(
											1.160050f, 3.346419f, 0.181329f, 1.505020f, 1.597234f, 0.062836f
									) /* ALL does not match sum of bands */
							);

							sb.wholeStemVolume(
									Utils.utilizationVector(
											3.731056f, 13.056453f, 3.364021f, 6.114161f, 2.041595f, 1.536676f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeByUtilization(
									Utils.utilizationVector(
											5.699161f, 13.450598f, 3.575303f, 4.922951f, 1.132478f, 3.819867f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayByUtilization(
									Utils.utilizationVector(
											1.892143f, 6.510414f, 1.845608f, 0.682118f, 1.801274f, 2.181414f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
									Utils.utilizationVector(
											1.738644f, 9.099073f, 2.595175f, 1.829268f, 2.783877f, 1.890754f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
									Utils.utilizationVector(
											2.852798f, 5.647935f, 1.407068f, 1.646782f, 2.481967f, 0.112118f
									) /* ALL does not match sum of bands */
							);

							sb.addCompatibilityVariables(cvb -> {

								MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume = new MatrixMap3Impl<>(
										UtilizationClass.UTIL_CLASSES,
										VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES, LayerType.ALL_USED,
										(uc, vv, lt) -> 0f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 1.510316f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 0.525790f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 8.338662f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 7.886145f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 4.603064f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 7.612512f
								);
								cvVolume.put(
										UtilizationClass.U75TO125,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 2.805719f
								);
								cvVolume.put(
										UtilizationClass.U75TO125,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 6.313797f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 1.959642f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 3.448284f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 1.792734f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 9.624868f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 8.656868f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 4.813280f
								);
								cvVolume.put(
										UtilizationClass.U125TO175,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 4.865906f
								);
								cvVolume.put(
										UtilizationClass.U125TO175,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 5.481640f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 4.209717f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 0.081496f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 6.328698f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 7.576355f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 6.998586f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 6.310371f
								);
								cvVolume.put(
										UtilizationClass.U175TO225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 3.153284f
								);
								cvVolume.put(
										UtilizationClass.U175TO225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 0.702512f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 5.716203f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 3.434496f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 3.710123f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 2.907161f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 8.718145f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 1.538193f
								);
								cvVolume.put(
										UtilizationClass.OVER225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 8.057309f
								);
								cvVolume.put(
										UtilizationClass.OVER225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 2.516958f
								);
								MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea = new MatrixMap2Impl<>(
										UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
								);
								cvBasalArea.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 6.162137f);
								cvBasalArea.put(UtilizationClass.U75TO125, LayerType.VETERAN, 3.839646f);
								cvBasalArea.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 3.743593f);
								cvBasalArea.put(UtilizationClass.U125TO175, LayerType.VETERAN, 1.149045f);
								cvBasalArea.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 6.972487f);
								cvBasalArea.put(UtilizationClass.U175TO225, LayerType.VETERAN, 1.467351f);
								cvBasalArea.put(UtilizationClass.OVER225, LayerType.PRIMARY, 9.086145f);
								cvBasalArea.put(UtilizationClass.OVER225, LayerType.VETERAN, 2.915670f);
								MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter = new MatrixMap2Impl<>(
										UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
								);

								cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 1.961470f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.VETERAN, 1.709174f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 8.091248f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.VETERAN, 2.848068f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 6.279333f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.VETERAN, 7.807972f);
								cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.PRIMARY, 4.633992f);
								cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.VETERAN, 6.571403f);

								Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall = new HashMap<>();

								cvPrimaryLayerSmall.put(UtilizationClassVariable.LOREY_HEIGHT, 3.055791f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.BASAL_AREA, 9.989216f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 5.399094f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.WHOLE_STEM_VOL, 7.960289f);

								cvb.cvVolume(cvVolume);
								cvb.cvBasalArea(cvBasalArea);
								cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);
								cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall);
							});

							sb.addSite(ib -> {
								ib.ageTotal(Optional.of(40.000000f));
								ib.height(Optional.of(15.000000f));
								ib.siteCurveNumber(Optional.of(42));
								ib.siteIndex(Optional.of(4.000000f));
								ib.yearsToBreastHeight(Optional.of(5.000000f));
							});

						});

						lb.primaryGenus(Optional.of("MB"));
					});
					pb.addLayer(lb -> {
						lb.layerType(ca.bc.gov.nrs.vdyp.model.LayerType.VETERAN);

						lb.empiricalRelationshipParameterIndex(Optional.of(21));

						lb.inventoryTypeGroup(Optional.of(34));

						lb.loreyHeight(Utils.heightVector(3.175555f, 13.881115f));
						lb.treesPerHectare(
								Utils.utilizationVector(
										196.516006f, 622.012695f, 79.691734f, 118.729874f, 141.539322f, 282.051727f
								) /* ALL does not match sum of bands */
						);
						lb.quadMeanDiameter(
								Utils.utilizationVector(
										6.705582f, 14.483641f, 0.364845f, 4.686717f, 4.838438f, 4.593642f
								) /* ALL does not match sum of bands */
						);
						lb.baseArea(
								Utils.utilizationVector(
										0.252516f, 2.816086f, 0.185386f, 0.994654f, 1.581418f, 0.054628f
								) /* ALL does not match sum of bands */
						);

						lb.wholeStemVolume(
								Utils.utilizationVector(
										0.953574f, 19.780567f, 2.692276f, 5.721595f, 4.523623f, 6.843073f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeByUtilization(
								Utils.utilizationVector(
										4.622794f, 15.538091f, 4.212268f, 5.364257f, 2.368544f, 3.593022f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayByUtilization(
								Utils.utilizationVector(
										1.419640f, 10.143337f, 4.880172f, 3.075758f, 0.385425f, 1.801982f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
								Utils.utilizationVector(
										3.100483f, 10.092585f, 1.466249f, 1.115289f, 3.914227f, 3.596821f
								) /* ALL does not match sum of bands */
						);
						lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
								Utils.utilizationVector(
										0.542688f, 6.009441f, 1.121508f, 2.761546f, 1.293929f, 0.832458f
								) /* ALL does not match sum of bands */
						);

						lb.addSpecies(sb -> {
							sb.genus("S");
							sb.genus(15);

							sb.breakageGroup(12);
							sb.volumeGroup(14);
							sb.decayGroup(13);

							sb.percentGenus(90.000000f);

							sb.addSp64Distribution("S", 100.000000f);

							sb.loreyHeight(Utils.heightVector(1.661621f, 16.040403f));
							sb.treesPerHectare(
									Utils.utilizationVector(
											241.617218f, 438.457031f, 87.019836f, 41.936874f, 281.616272f, 27.884029f
									) /* ALL does not match sum of bands */
							);
							sb.quadMeanDiameter(
									Utils.utilizationVector(
											3.965216f, 27.146305f, 7.451532f, 8.859586f, 1.708597f, 9.126590f
									) /* ALL does not match sum of bands */
							);
							sb.baseArea(
									Utils.utilizationVector(
											0.630295f, 2.920992f, 1.591866f, 0.885472f, 0.250752f, 0.192902f
									) /* ALL does not match sum of bands */
							);

							sb.wholeStemVolume(
									Utils.utilizationVector(
											2.081499f, 10.428626f, 0.189209f, 4.623718f, 3.838343f, 1.777356f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeByUtilization(
									Utils.utilizationVector(
											1.693190f, 19.573658f, 2.429848f, 5.804972f, 5.911832f, 5.427006f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayByUtilization(
									Utils.utilizationVector(
											4.492845f, 5.482807f, 1.719711f, 3.648051f, 0.070279f, 0.044766f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
									Utils.utilizationVector(
											3.238376f, 6.707272f, 0.068310f, 2.085977f, 1.212362f, 3.340622f
									) /* ALL does not match sum of bands */
							);
							sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
									Utils.utilizationVector(
											1.679569f, 7.687805f, 1.750385f, 1.273551f, 2.064738f, 2.599131f
									) /* ALL does not match sum of bands */
							);

							sb.addCompatibilityVariables(cvb -> {

								MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume = new MatrixMap3Impl<>(
										UtilizationClass.UTIL_CLASSES,
										VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES, LayerType.ALL_USED,
										(uc, vv, lt) -> 0f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 5.563822f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 4.803409f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 1.157501f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 3.106835f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 5.885086f
								);
								cvVolume.put(
										UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 0.761642f
								);
								cvVolume.put(
										UtilizationClass.U75TO125,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 8.951423f
								);
								cvVolume.put(
										UtilizationClass.U75TO125,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 8.886678f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 6.264751f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 6.610508f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 3.825809f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 7.061969f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 9.036251f
								);
								cvVolume.put(
										UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 5.905811f
								);
								cvVolume.put(
										UtilizationClass.U125TO175,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 8.336996f
								);
								cvVolume.put(
										UtilizationClass.U125TO175,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 1.468293f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 2.601936f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 0.075942f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 9.622362f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 6.406126f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 3.975361f
								);
								cvVolume.put(
										UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 8.291339f
								);
								cvVolume.put(
										UtilizationClass.U175TO225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 5.735347f
								);
								cvVolume.put(
										UtilizationClass.U175TO225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 4.349436f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.PRIMARY, 2.581649f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
										LayerType.VETERAN, 9.442972f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.PRIMARY, 8.217455f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
										LayerType.VETERAN, 8.913176f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.PRIMARY, 6.591835f
								);
								cvVolume.put(
										UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
										LayerType.VETERAN, 1.291338f
								);
								cvVolume.put(
										UtilizationClass.OVER225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.PRIMARY, 6.141067f
								);
								cvVolume.put(
										UtilizationClass.OVER225,
										UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
										LayerType.VETERAN, 6.888435f
								);
								MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea = new MatrixMap2Impl<>(
										UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
								);
								cvBasalArea.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 4.881973f);
								cvBasalArea.put(UtilizationClass.U75TO125, LayerType.VETERAN, 2.075868f);
								cvBasalArea.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 3.614706f);
								cvBasalArea.put(UtilizationClass.U125TO175, LayerType.VETERAN, 2.005780f);
								cvBasalArea.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 7.379816f);
								cvBasalArea.put(UtilizationClass.U175TO225, LayerType.VETERAN, 9.959743f);
								cvBasalArea.put(UtilizationClass.OVER225, LayerType.PRIMARY, 8.553615f);
								cvBasalArea.put(UtilizationClass.OVER225, LayerType.VETERAN, 9.691236f);
								MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter = new MatrixMap2Impl<>(
										UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
								);

								cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 3.397471f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.VETERAN, 5.050452f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 7.580298f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.VETERAN, 0.937272f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 7.260781f);
								cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.VETERAN, 5.087435f);
								cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.PRIMARY, 5.085622f);
								cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.VETERAN, 8.556303f);

								Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall = new HashMap<>();

								cvPrimaryLayerSmall.put(UtilizationClassVariable.LOREY_HEIGHT, 7.754099f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.BASAL_AREA, 7.710728f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 3.760872f);
								cvPrimaryLayerSmall.put(UtilizationClassVariable.WHOLE_STEM_VOL, 6.581034f);

								cvb.cvVolume(cvVolume);
								cvb.cvBasalArea(cvBasalArea);
								cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);
								cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall);
							});

						});

						lb.primaryGenus(Optional.of("S"));
					});
				});

				/* End of generated Polygon definition */
				var state = new TestProcessingState(controlMap);
				state.setPolygon(polygon);
				var primaryBank = state.getPrimaryLayerProcessingState().getBank();
				System.arraycopy(new float[] { 0.000000f, 37.029385f }, 0, primaryBank.ageTotals, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 0.000000f }, 0, primaryBank.percentagesOfForestedLand, 0, 2);
				System.arraycopy(
						new float[][] { { 1.205777f, 3.280845f, 0.169003f, 1.358052f, 1.693087f, 0.063084f },
								{ 1.212356f, 3.301440f, 0.192023f, 1.584733f, 1.700492f, 0.064628f } },
						0, primaryBank.basalAreas, 0, 2
				);
				System.arraycopy(new String[] { null, "MB" }, 0, primaryBank.speciesNames, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 3.794725f }, 0, primaryBank.siteIndices, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 14.770509f }, 0, primaryBank.dominantHeights, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 31.781885f }, 0, primaryBank.yearsAtBreastHeight, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 4.648443f }, 0, primaryBank.yearsToBreastHeight, 0, 2);
				System.arraycopy(new int[] { -1, 41 }, 0, primaryBank.siteCurveNumbers, 0, 2);
				var veteranBank = state.getVeteranLayerProcessingState().get().getBank();
				System.arraycopy(new float[] { 0.000000f, Float.NaN }, 0, veteranBank.ageTotals, 0, 2);
				System.arraycopy(new float[] { 0.000000f, 0.000000f }, 0, veteranBank.percentagesOfForestedLand, 0, 2);
				System.arraycopy(
						new float[][] { { 0.634654f, 3.064580f, 1.607465f, 0.930330f, 0.242154f, 0.211027f },
								{ 0.658044f, 3.157326f, 1.556144f, 0.908407f, 0.226441f, 0.190807f } },
						0, veteranBank.basalAreas, 0, 2
				);
				System.arraycopy(new String[] { null, "S" }, 0, veteranBank.speciesNames, 0, 2);
				System.arraycopy(new float[] { 0.000000f, Float.NaN }, 0, veteranBank.siteIndices, 0, 2);
				System.arraycopy(new float[] { 0.000000f, Float.NaN }, 0, veteranBank.dominantHeights, 0, 2);
				System.arraycopy(new float[] { 0.000000f, Float.NaN }, 0, veteranBank.yearsAtBreastHeight, 0, 2);
				System.arraycopy(new float[] { 0.000000f, Float.NaN }, 0, veteranBank.yearsToBreastHeight, 0, 2);
				System.arraycopy(new int[] { -1, -10 }, 0, veteranBank.siteCurveNumbers, 0, 2);

				/* End of generated ProcessingState definition */
			}

			assertThat(result, deepEquals((ProcessingState) expectedState));

		}

	}

	@Nested
	class WriteModel {

		@Test
		void polygon() throws IOException {
			Random rand = new Random(42);
			var controlMap = TestUtils.loadControlMap();

			var poly = VdypPolygon.build(pb -> {

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

			var buf = new StringBuffer();
			TestUtils.writeModel(poly, buf, 3, "result");

			System.out.print(buf.toString());

			VdypPolygon result = get(controlMap);

			assertThat(result, deepEquals(poly));
		}

		private VdypPolygon get(Map<String, Object> controlMap) {
			VdypPolygon result = null;

			/* the following Polygon definition was generated */

			result = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);

				pb.biogeoclimaticZone(Utils.getBec("CDF", controlMap));
				pb.forestInventoryZone("Z");

				pb.inventoryTypeGroup(Optional.empty());
				pb.targetYear(Optional.empty());

				pb.mode(Optional.empty());
				pb.percentAvailable(90.000000f);

				pb.addLayer(lb -> {
					lb.layerType(ca.bc.gov.nrs.vdyp.model.LayerType.PRIMARY);

					lb.empiricalRelationshipParameterIndex(Optional.of(21));

					lb.inventoryTypeGroup(Optional.of(34));

					lb.loreyHeight(Utils.heightVector(3.637818f, 1.093304f));
					lb.treesPerHectare(
							Utils.utilizationVector(
									110.634865f, 543.441101f, 114.492920f, 82.724403f, 207.127731f, 139.096069f
							) /* ALL does not match sum of bands */
					);
					lb.quadMeanDiameter(
							Utils.utilizationVector(
									7.077106f, 21.115028f, 6.655489f, 0.913246f, 9.033722f, 4.512572f
							) /* ALL does not match sum of bands */
					);
					lb.baseArea(
							Utils.utilizationVector(
									1.366447f, 3.151621f, 0.095879f, 0.617439f, 1.884147f, 0.554157f
							) /* ALL does not match sum of bands */
					);

					lb.wholeStemVolume(
							Utils.utilizationVector(
									5.334631f, 19.966562f, 5.480312f, 6.987250f, 6.435294f, 1.063708f
							) /* ALL does not match sum of bands */
					);
					lb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(
									2.618946f, 15.041424f, 2.638799f, 4.499437f, 5.583787f, 2.319401f
							) /* ALL does not match sum of bands */
					);
					lb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(
									3.991433f, 6.302918f, 0.886892f, 0.752737f, 2.971749f, 1.691540f
							) /* ALL does not match sum of bands */
					);
					lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(
									0.839070f, 6.456025f, 1.002645f, 3.303863f, 1.460645f, 0.688872f
							) /* ALL does not match sum of bands */
					);
					lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(
									0.475964f, 6.769507f, 1.762282f, 0.822518f, 2.253841f, 1.930866f
							) /* ALL does not match sum of bands */
					);

					lb.addSpecies(sb -> {
						sb.genus("MB");
						sb.genus(10);

						sb.breakageGroup(12);
						sb.volumeGroup(14);
						sb.decayGroup(13);

						sb.percentGenus(90.000000f);

						sb.addSp64Distribution("MB", 100.000000f);

						sb.loreyHeight(Utils.heightVector(2.855202f, 1.605718f));
						sb.treesPerHectare(
								Utils.utilizationVector(
										125.306259f, 761.289429f, 230.860580f, 292.210693f, 24.196316f, 214.021866f
								) /* ALL does not match sum of bands */
						);
						sb.quadMeanDiameter(
								Utils.utilizationVector(
										3.165325f, 13.858646f, 3.579199f, 1.990815f, 8.177969f, 0.110663f
								) /* ALL does not match sum of bands */
						);
						sb.baseArea(
								Utils.utilizationVector(
										1.160050f, 3.346419f, 0.181329f, 1.505020f, 1.597234f, 0.062836f
								) /* ALL does not match sum of bands */
						);

						sb.wholeStemVolume(
								Utils.utilizationVector(
										3.731056f, 13.056453f, 3.364021f, 6.114161f, 2.041595f, 1.536676f
								) /* ALL does not match sum of bands */
						);
						sb.closeUtilizationVolumeByUtilization(
								Utils.utilizationVector(
										5.699161f, 13.450598f, 3.575303f, 4.922951f, 1.132478f, 3.819867f
								) /* ALL does not match sum of bands */
						);
						sb.closeUtilizationVolumeNetOfDecayByUtilization(
								Utils.utilizationVector(
										1.892143f, 6.510414f, 1.845608f, 0.682118f, 1.801274f, 2.181414f
								) /* ALL does not match sum of bands */
						);
						sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
								Utils.utilizationVector(
										1.738644f, 9.099073f, 2.595175f, 1.829268f, 2.783877f, 1.890754f
								) /* ALL does not match sum of bands */
						);
						sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
								Utils.utilizationVector(
										2.852798f, 5.647935f, 1.407068f, 1.646782f, 2.481967f, 0.112118f
								) /* ALL does not match sum of bands */
						);

						sb.addCompatibilityVariables(cvb -> {

							MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float> cvVolume = new MatrixMap3Impl<>(
									UtilizationClass.UTIL_CLASSES,
									VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES, LayerType.ALL_USED,
									(uc, vv, lt) -> 0f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.PRIMARY, 7.275637f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.VETERAN, 0.546652f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.PRIMARY, 6.832234f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.VETERAN, 0.479393f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.PRIMARY, 3.087194f
							);
							cvVolume.put(
									UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.VETERAN, 9.420735f
							);
							cvVolume.put(
									UtilizationClass.U75TO125,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY,
									2.770784f
							);
							cvVolume.put(
									UtilizationClass.U75TO125,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.VETERAN,
									7.077106f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.PRIMARY, 6.655489f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.VETERAN, 0.913246f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.PRIMARY, 9.033722f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.VETERAN, 4.512572f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.PRIMARY, 3.687829f
							);
							cvVolume.put(
									UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.VETERAN, 3.816431f
							);
							cvVolume.put(
									UtilizationClass.U125TO175,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY,
									2.757480f
							);
							cvVolume.put(
									UtilizationClass.U125TO175,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.VETERAN,
									6.904258f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.PRIMARY, 4.636536f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.VETERAN, 7.620902f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.PRIMARY, 7.829018f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.VETERAN, 9.981786f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.PRIMARY, 9.193277f
							);
							cvVolume.put(
									UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.VETERAN, 1.519582f
							);
							cvVolume.put(
									UtilizationClass.U175TO225,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY,
									4.364910f
							);
							cvVolume.put(
									UtilizationClass.U175TO225,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.VETERAN,
									4.397998f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.PRIMARY, 7.499061f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL,
									LayerType.VETERAN, 9.306312f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.PRIMARY, 3.865668f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL,
									LayerType.VETERAN, 7.982866f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.PRIMARY, 1.773785f
							);
							cvVolume.put(
									UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY,
									LayerType.VETERAN, 1.505474f
							);
							cvVolume.put(
									UtilizationClass.OVER225,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY,
									5.943499f
							);
							cvVolume.put(
									UtilizationClass.OVER225,
									UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.VETERAN,
									3.383079f
							);
							MatrixMap2<UtilizationClass, LayerType, Float> cvBasalArea = new MatrixMap2Impl<>(
									UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
							);
							cvBasalArea.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 2.097675f);
							cvBasalArea.put(UtilizationClass.U75TO125, LayerType.VETERAN, 2.506613f);
							cvBasalArea.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 8.259658f);
							cvBasalArea.put(UtilizationClass.U125TO175, LayerType.VETERAN, 3.651612f);
							cvBasalArea.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 1.722179f);
							cvBasalArea.put(UtilizationClass.U175TO225, LayerType.VETERAN, 1.586545f);
							cvBasalArea.put(UtilizationClass.OVER225, LayerType.PRIMARY, 5.874274f);
							cvBasalArea.put(UtilizationClass.OVER225, LayerType.VETERAN, 2.741725f);
							MatrixMap2<UtilizationClass, LayerType, Float> cvQuadraticMeanDiameter = new MatrixMap2Impl<>(
									UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (uc, lt) -> 0f
							);

							cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 7.512804f);
							cvQuadraticMeanDiameter.put(UtilizationClass.U75TO125, LayerType.VETERAN, 6.436222f);
							cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 5.710403f);
							cvQuadraticMeanDiameter.put(UtilizationClass.U125TO175, LayerType.VETERAN, 0.802859f);
							cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 5.800248f);
							cvQuadraticMeanDiameter.put(UtilizationClass.U175TO225, LayerType.VETERAN, 0.906644f);
							cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.PRIMARY, 7.525099f);
							cvQuadraticMeanDiameter.put(UtilizationClass.OVER225, LayerType.VETERAN, 7.986169f);

							Map<UtilizationClassVariable, Float> cvPrimaryLayerSmall = new HashMap<>();

							cvPrimaryLayerSmall.put(UtilizationClassVariable.LOREY_HEIGHT, 0.314182f);
							cvPrimaryLayerSmall.put(UtilizationClassVariable.BASAL_AREA, 3.165325f);
							cvPrimaryLayerSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 3.579199f);
							cvPrimaryLayerSmall.put(UtilizationClassVariable.WHOLE_STEM_VOL, 1.990815f);

							cvb.cvVolume(cvVolume);
							cvb.cvBasalArea(cvBasalArea);
							cvb.cvQuadraticMeanDiameter(cvQuadraticMeanDiameter);
							cvb.cvPrimaryLayerSmall(cvPrimaryLayerSmall);
						});

						sb.addSite(ib -> {
							ib.ageTotal(Optional.of(40.000000f));
							ib.height(Optional.of(15.000000f));
							ib.siteCurveNumber(Optional.of(42));
							ib.siteIndex(Optional.of(4.000000f));
							ib.yearsToBreastHeight(Optional.of(5.000000f));
						});

					});

					lb.primaryGenus(Optional.of("MB"));
				});
			});

			/* End of generated polygon Definition */
			return result;
		}
	}
}
