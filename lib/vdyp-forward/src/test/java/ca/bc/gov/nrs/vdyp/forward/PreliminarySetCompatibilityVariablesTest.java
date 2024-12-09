package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.OVER225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U125TO175;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U175TO225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U75TO125;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.BASAL_AREA;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.LOREY_HEIGHT;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.QUAD_MEAN_DIAMETER;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.WHOLE_STEM_VOL;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.CLOSE_UTIL_VOL;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY;
import static ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ForwardExecutionStep;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.TestProcessingState;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class PreliminarySetCompatibilityVariablesTest extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PreliminarySetCompatibilityVariablesTest.class);

	ProcessingState initialState;

	@BeforeEach
	void initialState() throws ProcessingException {
		/* the following ProcessingState definition was generated */

		/* the following Polygon definition was generated */

		var polygon = VdypPolygon.build(pb -> {
			pb.polygonIdentifier("01002 S000001 00", 1970);

			pb.biogeoclimaticZone(Utils.getBec("CWH", controlMap));
			pb.forestInventoryZone("A");

			pb.inventoryTypeGroup(Optional.of(37));
			pb.targetYear(Optional.of(1990));

			pb.mode(Optional.of(ca.bc.gov.nrs.vdyp.model.PolygonMode.START));
			pb.percentAvailable(99.000000f);

			pb.addLayer(lb -> {
				lb.layerType(ca.bc.gov.nrs.vdyp.model.LayerType.PRIMARY);

				lb.empiricalRelationshipParameterIndex(Optional.empty());

				lb.inventoryTypeGroup(Optional.of(37));

				lb.loreyHeight(Utils.heightVector(7.016600f, 30.972401f));
				lb.treesPerHectare(
						Utils.utilizationVector(
								5.292929f, 601.333313f, 65.474747f, 72.656563f, 74.343430f, 388.868683f
						) /* ALL does not match sum of bands */
				);
				lb.quadMeanDiameter(
						Utils.utilizationVector(
								6.063298f, 30.999918f, 10.212870f, 15.043846f, 20.077644f, 36.730598f
						) /* ALL does not match sum of bands */
				);
				lb.baseArea(
						Utils.utilizationVector(
								0.015283f, 45.386452f, 0.536364f, 1.291465f, 2.353737f, 41.204899f
						) /* ALL does not match sum of bands */
				);

				lb.wholeStemVolume(
						Utils.utilizationVector(
								0.063636f, 627.249939f, 2.624141f, 9.197676f, 22.628180f, 592.799988f
						) /* ALL does not match sum of bands */
				);
				lb.closeUtilizationVolumeByUtilization(
						Utils.utilizationVector(
								0.000000f, 598.184082f, 0.387273f, 6.994444f, 20.327675f, 570.474609f
						) /* ALL does not match sum of bands */
				);
				lb.closeUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(
								0.000000f, 586.028320f, 0.383232f, 6.916060f, 20.089291f, 558.639771f
						) /* ALL does not match sum of bands */
				);
				lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(
								0.000000f, 583.457458f, 0.382626f, 6.901413f, 20.037878f, 556.135437f
						) /* ALL does not match sum of bands */
				);
				lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.000000f, 0.365960f, 6.604445f, 19.146969f, 528.444153f)
				);

				lb.addSpecies(sb -> {
					sb.genus("B");
					sb.genus(3);

					sb.breakageGroup(5);
					sb.volumeGroup(12);
					sb.decayGroup(7);

					sb.percentGenus(0.008967f);

					sb.addSp64Distribution("B", 100.000000f);

					sb.loreyHeight(Utils.heightVector(8.027200f, 36.755299f));
					sb.treesPerHectare(
							Utils.utilizationVector(
									0.000000f, 5.212121f, 0.767677f, 0.939394f, 0.888889f, 2.626262f
							) /* ALL does not match sum of bands */
					);
					sb.quadMeanDiameter(
							Utils.utilizationVector(
									6.100000f, 31.531136f, 9.170650f, 13.660340f, 18.178658f, 42.070770f
							) /* ALL does not match sum of bands */
					);
					sb.baseArea(Utils.utilizationVector(0.000000f, 0.005071f, 0.013768f, 0.023071f, 0.365081f));

					sb.wholeStemVolume(Utils.utilizationVector(0.000000f, 0.018687f, 0.076465f, 0.176566f, 6.000808f));
					sb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(0.000000f, 0.000909f, 0.050303f, 0.153636f, 5.814545f)
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(
									0.000000f, 5.905555f, 0.000909f, 0.050202f, 0.152929f, 5.701616f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(0.000000f, 0.000909f, 0.050101f, 0.152727f, 5.671313f)
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(
									0.000000f, 5.573434f, 0.000909f, 0.047980f, 0.145960f, 5.378687f
							) /* ALL does not match sum of bands */
					);

				});
				lb.addSpecies(sb -> {
					sb.genus("C");
					sb.genus(4);

					sb.breakageGroup(6);
					sb.volumeGroup(20);
					sb.decayGroup(14);

					sb.percentGenus(0.112301f);

					sb.addSp64Distribution("C", 100.000000f);

					sb.loreyHeight(Utils.heightVector(6.460200f, 22.958401f));
					sb.treesPerHectare(
							Utils.utilizationVector(
									4.444444f, 84.303024f, 16.282829f, 18.050505f, 16.717171f, 33.252522f
							) /* ALL does not match sum of bands */
					);
					sb.quadMeanDiameter(
							Utils.utilizationVector(
									5.997417f, 27.745222f, 10.063532f, 14.862596f, 19.873747f, 39.793953f
							) /* ALL does not match sum of bands */
					);
					sb.baseArea(
							Utils.utilizationVector(
									0.012556f, 5.096939f, 0.129515f, 0.313162f, 0.518576f, 4.135696f
							) /* ALL does not match sum of bands */
					);

					sb.wholeStemVolume(
							Utils.utilizationVector(
									0.051212f, 43.907677f, 0.608788f, 1.943131f, 3.861616f, 37.494141f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(0.000000f, 0.112727f, 1.424545f, 3.349697f, 34.951412f)
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(0.000000f, 0.110505f, 1.384848f, 3.245151f, 31.889189f)
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(
									0.000000f, 35.649494f, 0.110101f, 1.376566f, 3.218081f, 30.944645f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(
									0.000000f, 33.246864f, 0.104545f, 1.304545f, 3.037980f, 28.799898f
							) /* ALL does not match sum of bands */
					);

				});
				lb.addSpecies(sb -> {
					sb.genus("D");
					sb.genus(5);

					sb.breakageGroup(12);
					sb.volumeGroup(25);
					sb.decayGroup(19);

					sb.percentGenus(0.652143f);

					sb.addSp64Distribution("D", 100.000000f);

					sb.loreyHeight(Utils.heightVector(10.603300f, 33.743999f));
					sb.treesPerHectare(
							Utils.utilizationVector(
									0.474747f, 290.606049f, 1.656566f, 2.717172f, 13.959595f, 272.282806f
							) /* ALL does not match sum of bands */
					);
					sb.quadMeanDiameter(
							Utils.utilizationVector(
									6.479955f, 36.011185f, 10.470093f, 15.579478f, 20.527220f, 36.869789f
							) /* ALL does not match sum of bands */
					);
					sb.baseArea(
							Utils.utilizationVector(
									0.001566f, 29.598473f, 0.014263f, 0.051798f, 0.461980f, 29.070423f
							) /* ALL does not match sum of bands */
					);

					sb.wholeStemVolume(
							Utils.utilizationVector(
									0.007879f, 464.164917f, 0.110202f, 0.565859f, 6.073636f, 457.415314f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(
									0.000000f, 448.570099f, 0.057677f, 0.509899f, 5.698383f, 442.304016f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(
									0.000000f, 440.937378f, 0.057172f, 0.505758f, 5.654040f, 434.720367f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(
									0.000000f, 439.678558f, 0.057071f, 0.505556f, 5.651313f, 433.464630f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(0.000000f, 0.054646f, 0.483131f, 5.392222f, 411.943604f)
					);

					sb.addSite(ib -> {
						ib.ageTotal(Optional.of(55.000000f));
						ib.height(Optional.of(35.299999f));
						ib.siteCurveNumber(Optional.of(13));
						ib.siteIndex(Optional.of(35.000000f));
						ib.yearsToBreastHeight(Optional.of(1.000000f));
					});

				});
				lb.addSpecies(sb -> {
					sb.genus("H");
					sb.genus(8);

					sb.breakageGroup(17);
					sb.volumeGroup(37);
					sb.decayGroup(31);

					sb.percentGenus(0.129306f);

					sb.addSp64Distribution("H", 100.000000f);

					sb.loreyHeight(Utils.heightVector(7.546400f, 22.770399f));
					sb.treesPerHectare(
							Utils.utilizationVector(
									0.000000f, 169.595947f, 44.010098f, 46.454544f, 34.272724f, 44.868683f
							) /* ALL does not match sum of bands */
					);
					sb.quadMeanDiameter(
							Utils.utilizationVector(
									Float.NaN, 20.990366f, 10.276456f, 15.108315f, 20.090958f, 31.892609f
							) /* ALL does not match sum of bands */
					);
					sb.baseArea(Utils.utilizationVector(0.000000f, 0.365030f, 0.832818f, 1.086525f, 3.584374f));

					sb.wholeStemVolume(Utils.utilizationVector(0.000000f, 1.756060f, 5.925858f, 9.749595f, 39.020805f));
					sb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(0.000000f, 0.194444f, 4.460101f, 8.661818f, 37.016060f)
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(
									0.000000f, 49.569897f, 0.193232f, 4.428889f, 8.587777f, 36.359898f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(0.000000f, 0.193030f, 4.423131f, 8.568384f, 36.163937f)
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(0.000000f, 0.185253f, 4.244747f, 8.221919f, 34.502422f)
					);

				});
				lb.addSpecies(sb -> {
					sb.genus("S");
					sb.genus(15);

					sb.breakageGroup(28);
					sb.volumeGroup(66);
					sb.decayGroup(54);

					sb.percentGenus(0.097282f);

					sb.addSp64Distribution("S", 100.000000f);

					sb.loreyHeight(Utils.heightVector(8.200300f, 32.012501f));
					sb.treesPerHectare(
							Utils.utilizationVector(
									0.363636f, 51.616158f, 2.757576f, 4.505050f, 8.515151f, 35.848484f
							) /* ALL does not match sum of bands */
					);
					sb.quadMeanDiameter(
							Utils.utilizationVector(
									6.377533f, 33.002167f, 10.186822f, 15.028074f, 19.852715f, 37.923717f
							) /* ALL does not match sum of bands */
					);
					sb.baseArea(
							Utils.utilizationVector(
									0.001162f, 4.415303f, 0.022475f, 0.079909f, 0.263586f, 4.049323f
							) /* ALL does not match sum of bands */
					);

					sb.wholeStemVolume(
							Utils.utilizationVector(
									0.004545f, 56.452423f, 0.130404f, 0.686364f, 2.766768f, 52.868885f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeByUtilization(
							Utils.utilizationVector(0.000000f, 0.021515f, 0.549495f, 2.464242f, 50.388485f)
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							Utils.utilizationVector(0.000000f, 0.021414f, 0.546465f, 2.449495f, 49.968582f)
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							Utils.utilizationVector(
									0.000000f, 52.905857f, 0.021414f, 0.546061f, 2.447374f, 49.890903f
							) /* ALL does not match sum of bands */
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							Utils.utilizationVector(
									0.000000f, 50.713131f, 0.020606f, 0.524141f, 2.348889f, 47.819595f
							) /* ALL does not match sum of bands */
					);

				});

				lb.primaryGenus(Optional.of("D"));
			});
		});

		/* End of generated Polygon definition */
		initialState = new TestProcessingState(controlMap);
		initialState.setPolygon(polygon);
		var primaryBank = initialState.getPrimaryLayerProcessingState().getBank();
		System.arraycopy(
				new float[] { 0.000000f, Float.NaN, Float.NaN, 55.000000f, Float.NaN, Float.NaN }, 0,
				primaryBank.ageTotals, 0, 6
		);
		System.arraycopy(
				new float[] { 0.000000f, 0.896721f, 11.230113f, 65.214325f, 12.930618f, 9.728219f }, 0,
				primaryBank.percentagesOfForestedLand, 0, 6
		);
		System.arraycopy(
				new float[][] { { 0.015283f, 45.386444f, 0.536354f, 1.291455f, 2.353737f, 41.204899f }, { 0.000000f,
						0.406990f, 0.005071f, 0.013768f, 0.023071f, 0.365081f }, { 0.012556f, 5.096949f, 0.129515f,
								0.313162f, 0.518576f, 4.135696f }, { 0.001566f, 29.598463f, 0.014263f, 0.051798f,
										0.461980f, 29.070423f }, { 0.000000f, 5.868748f, 0.365030f, 0.832818f,
												1.086525f, 3.584374f }, { 0.001162f, 4.415293f, 0.022475f, 0.079909f,
														0.263586f, 4.049323f } }, 0, primaryBank.basalAreas, 0, 6
		);
		System.arraycopy(new String[] { null, "B", "C", "D", "H", "S" }, 0, primaryBank.speciesNames, 0, 6);
		System.arraycopy(
				new float[] { 35.000000f, Float.NaN, Float.NaN, 35.000000f, Float.NaN, Float.NaN }, 0,
				primaryBank.siteIndices, 0, 6
		);
		System.arraycopy(
				new float[] { 0.000000f, Float.NaN, Float.NaN, 35.299999f, Float.NaN, Float.NaN }, 0,
				primaryBank.dominantHeights, 0, 6
		);
		System.arraycopy(
				new float[] { 0.000000f, Float.NaN, Float.NaN, 54.000000f, Float.NaN, Float.NaN }, 0,
				primaryBank.yearsAtBreastHeight, 0, 6
		);
		System.arraycopy(
				new float[] { 0.000000f, 5.000000f, 7.500000f, 1.000000f, 4.500000f, 5.200000f }, 0,
				primaryBank.yearsToBreastHeight, 0, 6
		);
		System.arraycopy(new int[] { 0, 118, 122, 13, 99, 59 }, 0, primaryBank.siteCurveNumbers, 0, 6);

		/* End of generated ProcessingState definition */
	}

	@Test
	/** SET_COMPATIBILITY_VARIABLES */
	void testSetCompatibilityVariables() throws ResourceParseException, IOException, ProcessingException {

		var reader = new ForwardDataStreamReader(controlMap);
		var polygon = reader.readNextPolygon().orElseThrow();

		ProcessingEngine fpe = new ProcessingEngine(controlMap) {

			@Override
			public void processPolygon(VdypPolygon polygon, ExecutionStep lastStepInclusive)
					throws ProcessingException {
				// TODO Auto-generated method stub

			}

			@Override
			protected ExecutionStep getFirstStep() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected ExecutionStep getLastStep() {
				// TODO Auto-generated method stub
				return null;
			}

		};
		fpe.fps = initialState;
		fpe.
		//fpe.processPolygon(polygon, ForwardProcessingEngine.ForwardExecutionStep.SET_COMPATIBILITY_VARIABLES);

		fpe.processPolygon(
				polygon, ProcessingEngine<ForwardExecutionStep>.ForwardExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX
		);
		TestUtils.writeModel(fpe.fps, System.out, 3, "initialState");
		// These values have been verified against the FORTRAN implementation, allowing for minor
		// platform-specific differences.

		ForwardLayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		assertThat(
				lps.getVolumeEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 12, 20, 25, 37, 66 })
		);
		assertThat(
				lps.getDecayEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 7, 14, 19, 31, 54 })
		);
		assertThat(
				lps.getBreakageEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 5, 6, 12, 17, 28 })
		);

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0063979626f));
		assertThat(lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00016450882f));

		assertThat(lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00024962425f));
		assertThat(lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00011026859f));
		assertThat(lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.000006198883f));
		assertThat(lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.000024557114f));

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0062299743f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0010375977f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0001244545f));
		assertThat(lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0000038146973f));

		assertThat(lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00013566017f));
		assertThat(lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.00033128262f));
		assertThat(lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.00021290779f));
		assertThat(lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0000059604645f));

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.000882864f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0002478361f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0008614063f));
		assertThat(lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0000052452087f));

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0048389435f));
		assertThat(lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(1.3446808E-4f));

		assertThat(lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.01768279f));
		assertThat(lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0010006428f));
		assertThat(lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(2.2292137E-4f));
		assertThat(lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-1.9311905E-5f));

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.010708809f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(7.4100494E-4f));
		assertThat(lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-2.4795532E-5f));

		assertThat(lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.011499405f));
		assertThat(lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0010294914f));
		assertThat(lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-9.3603134E-4f));
		assertThat(lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-5.4836273E-5f));

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.010175705f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0014338493f));
		assertThat(lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-2.9087067E-5f));

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.035768032f)
		);
		assertThat(
				lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0016698837f)
		);

		assertThat(
				lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.16244507f)
		);
		assertThat(
				lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0045113564f)
		);
		assertThat(
				lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0030164719f)
		);
		assertThat(
				lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(3.528595E-5f)
		);

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(4.1484833E-5f)
		);

		assertThat(
				lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.13775301f)
		);
		assertThat(
				lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.005630493f)
		);
		assertThat(
				lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0028266907f)
		);
		assertThat(
				lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(3.7765503E-4f)
		);

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(5.378723E-4f)
		);

		assertThat(lps.getCVVolume(1, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.5427341E-4f));
		assertThat(lps.getCVVolume(1, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0023424625f));
		assertThat(lps.getCVVolume(1, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.033348E-5f));

		assertThat(lps.getCVVolume(2, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.9444695E-5f));
		assertThat(lps.getCVVolume(2, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.208088E-5f));
		assertThat(lps.getCVVolume(2, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.4318695E-5f));
		assertThat(lps.getCVVolume(2, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(9.536743E-7f));

		assertThat(lps.getCVVolume(3, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0013506413f));
		assertThat(lps.getCVVolume(3, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.787445E-4f));
		assertThat(lps.getCVVolume(3, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(3.9100647E-5f));
		assertThat(lps.getCVVolume(3, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.4305115E-6f));

		assertThat(lps.getCVVolume(4, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.891655E-5f));
		assertThat(lps.getCVVolume(4, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(2.7656555E-5f));
		assertThat(lps.getCVVolume(4, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.196167E-5f));
		assertThat(lps.getCVVolume(4, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.0967255E-5f));

		assertThat(lps.getCVVolume(5, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(1.8835068E-5f));
		assertThat(lps.getCVVolume(5, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.0085106E-4f));
		assertThat(lps.getCVVolume(5, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(1.6236305E-4f));
		assertThat(lps.getCVVolume(5, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.390976E-6f));

		assertThat(lps.getCVBasalArea(1, OVER225, LayerType.PRIMARY), is(1.4913082E-4f));
		assertThat(lps.getCVBasalArea(1, U125TO175, LayerType.PRIMARY), is(-5.034916E-5f));
		assertThat(lps.getCVBasalArea(1, U175TO225, LayerType.PRIMARY), is(-7.482059E-5f));
		assertThat(lps.getCVBasalArea(1, U75TO125, LayerType.PRIMARY), is(-2.397038E-5f));

		assertThat(lps.getCVBasalArea(2, OVER225, LayerType.PRIMARY), is(-2.193451E-5f));
		assertThat(lps.getCVBasalArea(2, U125TO175, LayerType.PRIMARY), is(5.4836273E-6f));
		assertThat(lps.getCVBasalArea(2, U175TO225, LayerType.PRIMARY), is(9.596348E-6f));
		assertThat(lps.getCVBasalArea(2, U75TO125, LayerType.PRIMARY), is(6.660819E-6f));

		assertThat(lps.getCVBasalArea(3, OVER225, LayerType.PRIMARY), is(9.918213E-5f));
		assertThat(lps.getCVBasalArea(3, U125TO175, LayerType.PRIMARY), is(-1.5150756E-5f));
		assertThat(lps.getCVBasalArea(3, U175TO225, LayerType.PRIMARY), is(-7.9244375E-5f));
		assertThat(lps.getCVBasalArea(3, U75TO125, LayerType.PRIMARY), is(-4.341826E-6f));

		assertThat(lps.getCVBasalArea(4, OVER225, LayerType.PRIMARY), is(1.9073486E-4f));
		assertThat(lps.getCVBasalArea(4, U125TO175, LayerType.PRIMARY), is(-8.2850456E-5f));
		assertThat(lps.getCVBasalArea(4, U175TO225, LayerType.PRIMARY), is(-5.2928925E-5f));
		assertThat(lps.getCVBasalArea(4, U75TO125, LayerType.PRIMARY), is(-5.531311E-5f));

		assertThat(lps.getCVBasalArea(5, OVER225, LayerType.PRIMARY), is(1.2397766E-4f));
		assertThat(lps.getCVBasalArea(5, U125TO175, LayerType.PRIMARY), is(-3.7431717E-5f));
		assertThat(lps.getCVBasalArea(5, U175TO225, LayerType.PRIMARY), is(-7.364154E-5f));
		assertThat(lps.getCVBasalArea(5, U75TO125, LayerType.PRIMARY), is(-1.289323E-5f));

		assertThat(lps.getCVQuadraticMeanDiameter(1, OVER225, LayerType.PRIMARY), is(0.0072517395f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U125TO175, LayerType.PRIMARY), is(-0.014289856f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U175TO225, LayerType.PRIMARY), is(-0.04478264f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U75TO125, LayerType.PRIMARY), is(-0.020475388f));

		assertThat(lps.getCVQuadraticMeanDiameter(2, OVER225, LayerType.PRIMARY), is(6.942749E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U125TO175, LayerType.PRIMARY), is(-2.0217896E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U175TO225, LayerType.PRIMARY), is(6.008148E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U75TO125, LayerType.PRIMARY), is(-1.2207031E-4f));

		assertThat(lps.getCVQuadraticMeanDiameter(3, OVER225, LayerType.PRIMARY), is(3.7002563E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U125TO175, LayerType.PRIMARY), is(-0.008190155f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U175TO225, LayerType.PRIMARY), is(-0.0019168854f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U75TO125, LayerType.PRIMARY), is(-0.008534431f));

		assertThat(lps.getCVQuadraticMeanDiameter(4, OVER225, LayerType.PRIMARY), is(-0.0010547638f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U125TO175, LayerType.PRIMARY), is(-7.696152E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U175TO225, LayerType.PRIMARY), is(-0.0012798309f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U75TO125, LayerType.PRIMARY), is(1.7547607E-4f));

		assertThat(lps.getCVQuadraticMeanDiameter(5, OVER225, LayerType.PRIMARY), is(-2.3651123E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U125TO175, LayerType.PRIMARY), is(9.880066E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U175TO225, LayerType.PRIMARY), is(-0.005466461f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U75TO125, LayerType.PRIMARY), is(-7.972717E-4f));

		assertThat(lps.getCVSmall(1, BASAL_AREA), is(-2.1831444E-7f));
		assertThat(lps.getCVSmall(1, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(1, LOREY_HEIGHT), is(0.0f));
		assertThat(lps.getCVSmall(1, WHOLE_STEM_VOL), is(0.0f));

		assertThat(lps.getCVSmall(2, BASAL_AREA), is(-4.496146E-5f));
		assertThat(lps.getCVSmall(2, QUAD_MEAN_DIAMETER), is(0.0023670197f));
		assertThat(lps.getCVSmall(2, LOREY_HEIGHT), is(1.3113013E-6f));
		assertThat(lps.getCVSmall(2, WHOLE_STEM_VOL), is(0.0010289619f));

		assertThat(lps.getCVSmall(3, BASAL_AREA), is(4.9466034E-6f));
		assertThat(lps.getCVSmall(3, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(3, LOREY_HEIGHT), is(-1.5556934E-5f));
		assertThat(lps.getCVSmall(3, WHOLE_STEM_VOL), is(0.0f));

		assertThat(lps.getCVSmall(4, BASAL_AREA), is(0.0f));
		assertThat(lps.getCVSmall(4, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(4, LOREY_HEIGHT), is(0.0f));
		assertThat(lps.getCVSmall(4, WHOLE_STEM_VOL), is(0.0f));

		assertThat(lps.getCVSmall(5, BASAL_AREA), is(3.4208642E-6f));
		assertThat(lps.getCVSmall(5, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(5, LOREY_HEIGHT), is(-5.7758567E-5f));
		assertThat(lps.getCVSmall(5, WHOLE_STEM_VOL), is(0.0f));
	}
}
