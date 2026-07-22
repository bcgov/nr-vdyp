package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecies;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasUtilization;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasUtilizationHeight;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.EnumMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.test.ProcessingTestUtils;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ComputationMethodsTest {

	@Nested
	class ComputeLoreyHeightWithSmallClass {
		IMocksControl em;

		@BeforeEach
		void setup() {
			em = EasyMock.createControl();
		}

		@AfterEach
		void teardown() {
			em.verify();
		}

		@Test
		void testBasic() {

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			// [-1:0.02031, 0:26.16433, 1:1.62086, 2:5.61891, 3:9.09629, 4:9.82827]
			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0.02031f, 1.62086f, 5.61891f, 9.09629f, 9.82827f));
			// [-1:7.62, 0:984.85, 1:199.51, 2:317.67, 3:298.06, 4:169.62]
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(7.62f, 199.51f, 317.67f, 298.06f, 169.62f));
			// [-1:6.1122, 0:15.3942]
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(6.1122f, 15.3942f));

			em.replay();

			float result = ComputationMethods.computeLoreyHeightWithSmallClass(entity);

			assertThat(result, closeTo(15.3941442526608f, 0.000001f));
		}

		@Test
		void testNoSmall() {

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 1.62086f, 5.61891f, 9.09629f, 9.82827f));
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 199.51f, 317.67f, 298.06f, 169.62f));
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, 15.3942f));

			em.replay();

			float result = ComputationMethods.computeLoreyHeightWithSmallClass(entity);

			assertThat(result, closeTo(15.3942f, 0.000001f));
		}

		@Test
		void testOnlySmall() {

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0.02031f, 0f, 0f, 0f, 0f));
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(7.62f, 0f, 0f, 0f, 0f));
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(6.1122f, 0f));

			em.replay();

			float result = ComputationMethods.computeLoreyHeightWithSmallClass(entity);

			assertThat(result, closeTo(6.1122f, 0.000001f));
		}

		@Test
		void testNoTrees() {

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 0f));
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 0f));
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, 0f));

			em.replay();

			float result = ComputationMethods.computeLoreyHeightWithSmallClass(entity);

			assertThat(result, equalTo(0f));
		}

		@Test
		void testSkinnyTrees() {

			// Logically we should never have infinitely skinny trees but check that we handle the situation gracefully

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 0f));
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 100f));
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, 10f));

			em.replay();

			float result = Assertions
					.assertDoesNotThrow(() -> ComputationMethods.computeLoreyHeightWithSmallClass(entity));

			assertThat(result, equalTo(0f));
		}

		@Test
		void testFlatTrees() {

			// Logically we should never have infinitely flat trees but check that we handle the situation gracefully

			VdypUtilizationHolder entity = em.createMock(VdypUtilizationHolder.class);

			EasyMock.expect(entity.getBaseAreaByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 10f));
			EasyMock.expect(entity.getTreesPerHectareByUtilization())
					.andStubReturn(Utils.utilizationVector(0f, 0f, 0f, 0f, 100f));
			EasyMock.expect(entity.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, 0f));

			em.replay();

			float result = Assertions
					.assertDoesNotThrow(() -> ComputationMethods.computeLoreyHeightWithSmallClass(entity));

			assertThat(result, equalTo(0f));
		}
	}

	@Nested
	class ComputeUtilizationComponentsPrimary {

		// These tests have to set up and check an enormous amount of state but there's no good way to isolate this
		// method without doing this.

		// This test is based on FipStartTest.testProcessPrimary()
		@Test
		void testNoCompatibilityVariables() throws ProcessingException {
			var control = TestUtils.resolveControlMap(TestUtils.loadControlMap());
			var emp = new EstimationMethods(control);
			var cmp = new ComputationMethods(emp, VdypApplicationIdentifier.FIP_START);

			var bec = control.getBecLookup().get("CWH").get();
			var volumeComputeMode = VolumeComputeMode.BY_UTIL;
			var compatibilityVariableMode = CompatibilityVariableMode.NONE;

			VdypLayer vdypLayer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2023);
				lb.layerType(LayerType.PRIMARY);
				lb.empiricalRelationshipParameterIndex(1);
				lb.inventoryTypeGroup(37);
				lb.primaryGenus("D");
				lb.controlMap(control.getControlMap());

				lb.addSpecies(sb -> {
					sb.speciesGroup("B");
					sb.percentGenus(0.890327f);
					sb.breakageGroup(3);
					sb.decayGroup(7);
					sb.volumeGroup(12);
					sb.addSp64Distribution("B", 100);
				});

				lb.addSpecies(sb -> {
					sb.speciesGroup("C");
					sb.percentGenus(11.401065f);
					sb.breakageGroup(6);
					sb.decayGroup(14);
					sb.volumeGroup(20);
					sb.addSp64Distribution("C", 100);
				});

				lb.addSpecies(sb -> {
					sb.speciesGroup("S");
					sb.percentGenus(9.18016f);
					sb.breakageGroup(28);
					sb.decayGroup(54);
					sb.volumeGroup(66);
					sb.addSp64Distribution("S", 100);
				});
				lb.addSpecies(sb -> {
					sb.speciesGroup("D");
					sb.percentGenus(66.19865f);
					sb.breakageGroup(12);
					sb.decayGroup(19);
					sb.volumeGroup(25);
					sb.addSp64Distribution("D", 100);
					sb.addSite(ib -> {
						ib.ageTotal(55f);
						ib.height(35.3f);
						ib.siteIndex(5f);
						ib.yearsAtBreastHeight(54f);
						ib.yearsToBreastHeight(1f);
					});
				});
				lb.addSpecies(sb -> {
					sb.speciesGroup("H");
					sb.percentGenus(12.329803f);
					sb.breakageGroup(17);
					sb.decayGroup(31);
					sb.volumeGroup(37);
					sb.addSp64Distribution("H", 100);
				});

			});

			vdypLayer.setBaseAreaByUtilization(Utils.utilizationVector(0.015377278f, 44.62498f, 0, 0, 0, 0));
			vdypLayer.setLoreyHeightByUtilization(Utils.heightVector(7.1445975f, 31.33066f));
			vdypLayer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.05059f, 30.259758f, 0, 0, 0, 0));
			vdypLayer.setTreesPerHectareByUtilization(Utils.utilizationVector(5.348034f, 620.5221f, 0, 0, 0, 0));
			vdypLayer.setWholeStemVolumeByUtilization(Utils.utilizationVector(0.06668958f, 635.66547f, 0, 0, 0, 0));

			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("B");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(0, 0.39730823f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(8.394591f, 38.600353f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.135865f, 31.66131f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(0, 5.0463796f, 0, 0, 0, 0));
				spec.setWholeStemVolumeByUtilization(Utils.utilizationVector(0, 6.3566556f, 0, 0, 0, 0));
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("C");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(0.013167085f, 5.0877233f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(6.615288f, 22.800163f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(5.9906764f, 26.472792f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(4.6714106f, 92.4345f, 0, 0, 0, 0));
				spec.setWholeStemVolumeByUtilization(Utils.utilizationVector(0.055698473f, 44.49658f, 0, 0, 0, 0));
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("S");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(5.7540054E-4f, 4.0966444f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(8.634716f, 34.688877f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.418026f, 34.53727f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(0.17785966f, 43.72828f, 0, 0, 0, 0));
				spec.setWholeStemVolumeByUtilization(Utils.utilizationVector(0.0024040062f, 57.209877f, 0, 0, 0, 0));
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("D");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(0.0016347925f, 29.541132f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(10.883329f, 33.53744f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.4600954f, 33.92449f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(0.49876377f, 326.82144f, 0, 0, 0, 0));
				spec.setWholeStemVolumeByUtilization(Utils.utilizationVector(0.008587101f, 470.39246f, 0, 0, 0, 0));
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("H");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(0f, 5.5021725f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(7.937317f, 24.345116f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.035055f, 21.433807f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(0f, 152.49152f, 0, 0, 0, 0));
				spec.setWholeStemVolumeByUtilization(Utils.utilizationVector(0f, 57.2099f, 0, 0, 0, 0));
			}

			cmp.computeUtilizationComponentsPrimary(bec, vdypLayer, volumeComputeMode, compatibilityVariableMode);

			assertThat(
					vdypLayer,
					hasUtilization(
							"baseAreaByUtilization", 0.015377278f, 44.624985f, 0.51319396f, 1.2678735f, 2.527925f,
							40.31599f
					)
			);
			assertThat(
					vdypLayer,
					hasUtilization(
							"quadraticMeanDiameterByUtilization", 6.05059f, 30.25976f, 10.208051f, 15.05496f,
							20.117634f, 35.51094f
					)
			);
			assertThat(
					vdypLayer,
					hasUtilization(
							"treesPerHectareByUtilization", 5.348034f, 620.5221f, 62.705555f, 71.224075f, 79.528076f,
							407.06442f
					)
			);
			assertThat(vdypLayer, hasUtilizationHeight(7.1445975f, 31.330658f));
			assertThat(
					vdypLayer,
					hasUtilization(
							"wholeStemVolumeByUtilization", 0.06668958f, 635.66547f, 2.6686912f, 9.683515f, 26.551117f,
							596.7621f
					)
			);

			assertThat(
					vdypLayer,
					hasSpecies(
							"B",
							hasUtilization(
									"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 5.994646f, 8.924726E-4f,
									0.0489161f, 0.14988856f, 5.794949f
							)
					)
			);
			assertThat(
					vdypLayer,
					hasSpecies(
							"C",
							hasUtilization(
									"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 36.962585f, 0.14629345f,
									1.7235806f, 3.7292817f, 31.36343f
							)
					)
			);
			assertThat(
					vdypLayer,
					hasSpecies(
							"S",
							hasUtilization(
									"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 53.98474f, 0.017718026f,
									0.41906428f, 2.039742f, 51.508217f
							)
					)
			);
			assertThat(
					vdypLayer,
					hasSpecies(
							"D",
							hasUtilization(
									"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 447.2099f, 0.093552716f,
									0.9728819f, 9.409065f, 436.7344f
							)
					)
			);
			assertThat(
					vdypLayer,
					hasSpecies(
							"H",
							hasUtilization(
									"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 50.59974f, 0.17554875f,
									4.1930995f, 8.415215f, 37.815876f
							)
					)
			);

			assertThat(
					vdypLayer,
					hasUtilization(
							"closeUtilizationVolumeByUtilization", 0, 606.03973f, 0.43906277f, 7.4471726f, 24.021961f,
							574.13153f
					)
			);

			assertThat(
					vdypLayer,
					hasUtilization(
							"closeUtilizationVolumeByUtilization", 0.0f, 606.03973f, 0.43906277f, 7.4471726f,
							24.021961f, 574.13153f
					)
			);

			assertThat(
					vdypLayer,
					hasUtilization(
							"closeUtilizationVolumeNetOfDecayByUtilization", 0.0f, 594.7516f, 0.43400544f, 7.3575425f,
							23.74319f, 563.21686f
					)
			);
			assertThat(
					vdypLayer,
					hasUtilization(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization", 0.0f, 592.3625f, 0.43337047f,
							7.3413553f, 23.687904f, 560.89984f
					)
			);
			assertThat(
					vdypLayer,
					hasUtilization(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", 0, 563.22406f, 0.4141465f,
							7.0206203f, 22.621572f, 533.1677f
					)
			);

		}

		// This test is based on GrowAllStepsTest.testStandardPath()

		@Test
		void testAllCompatibilityVariables() throws ProcessingException {
			var control = TestUtils.resolveControlMap(ProcessingTestUtils.loadControlMap());
			var emp = new EstimationMethods(control);
			var cmp = new ComputationMethods(emp, VdypApplicationIdentifier.VDYP_FORWARD);

			var bec = control.getBecLookup().get("CWH").get();
			var volumeComputeMode = VolumeComputeMode.BY_UTIL_WITH_WHOLE_STEM_BY_SPEC;
			var compatibilityVariableMode = CompatibilityVariableMode.ALL;

			VdypLayer vdypLayer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("01002 S000001 00", 1970);
				lb.layerType(LayerType.PRIMARY);
				lb.empiricalRelationshipParameterIndex(1);
				lb.inventoryTypeGroup(37);
				lb.primaryGenus("D");
				lb.controlMap(control.getControlMap());

				lb.addSpecies(sb -> {
					sb.speciesGroup("B");
					sb.percentGenus(0.890327f);
					sb.breakageGroup(3);
					sb.decayGroup(7);
					sb.volumeGroup(12);
					sb.addSp64Distribution("B", 100);
				});

				lb.addSpecies(sb -> {
					sb.speciesGroup("C");
					sb.percentGenus(11.401065f);
					sb.breakageGroup(6);
					sb.decayGroup(14);
					sb.volumeGroup(20);
					sb.addSp64Distribution("C", 100);
				});

				lb.addSpecies(sb -> {
					sb.speciesGroup("S");
					sb.percentGenus(9.18016f);
					sb.breakageGroup(28);
					sb.decayGroup(54);
					sb.volumeGroup(66);
					sb.addSp64Distribution("S", 100);
				});
				lb.addSpecies(sb -> {
					sb.speciesGroup("D");
					sb.percentGenus(66.19865f);
					sb.breakageGroup(12);
					sb.decayGroup(19);
					sb.volumeGroup(25);
					sb.addSp64Distribution("D", 100);
					sb.addSite(ib -> {
						ib.ageTotal(55f);
						ib.height(35.3f);
						ib.siteIndex(5f);
						ib.yearsAtBreastHeight(54f);
						ib.yearsToBreastHeight(1f);
					});
				});
				lb.addSpecies(sb -> {
					sb.speciesGroup("H");
					sb.percentGenus(12.329803f);
					sb.breakageGroup(17);
					sb.decayGroup(31);
					sb.volumeGroup(37);
					sb.addSp64Distribution("H", 100);
				});

			});

			vdypLayer.setBaseAreaByUtilization(
					Utils.utilizationVector(0.015282828f, 45.738297f, 0.5363535f, 1.2914546f, 2.353737f, 41.2049f)
			);
			vdypLayer.setLoreyHeightByUtilization(Utils.heightVector(7.016903f, 30.97237f));
			vdypLayer.setQuadraticMeanDiameterByUtilization(
					Utils.utilizationVector(6.0632977f, 31.308353f, 10.21287f, 15.043846f, 20.077644f, 36.7306f)
			);
			vdypLayer.setTreesPerHectareByUtilization(
					Utils.utilizationVector(5.2828283f, 594.11383f, 65.47475f, 72.666664f, 74.35353f, 388.87875f)
			);
			vdypLayer.setWholeStemVolumeByUtilization(
					Utils.utilizationVector(0.06363636f, 627.24994f, 2.6241412f, 9.197676f, 22.62818f, 592.8f)
			);
			vdypLayer.setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0.0f, 598.18396f, 0.38727272f, 6.9943438f, 20.327776f, 570.47455f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0.0f, 586.02844f, 0.3832323f, 6.916161f, 20.089392f, 558.63965f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0.0f, 583.4573f, 0.38252524f, 6.901414f, 20.037878f, 556.13544f)
			);
			vdypLayer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(0.0f, 554.5615f, 0.36595958f, 6.6044445f, 19.146969f, 528.44415f)
			);

			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("B");

				var baCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				baCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -2.3490973E-5f);
				baCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -4.934218E-5f);
				baCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -7.332418E-5f);
				baCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 1.461482E-4f);

				var dqCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				dqCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 0.0f);
				dqCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -0.014004059f);
				dqCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -0.043888856f);
				dqCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 0.0071104434f);

				var volCv = new MatrixMap3Impl<>(
						UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0.0f
				);

				volCv.put(UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -2.4918796E-4f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 0.0022960806f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 0.006271055f);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-0.0047421646f
				);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 0.035052672f
				);

				volCv.put(UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -6.892681E-5f);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -1.6121865E-4f);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						1.3177872E-4f
				);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, -0.001636486f
				);

				Map<UtilizationClassVariable, Float> smallCv = new EnumMap<>(UtilizationClassVariable.class);
				smallCv.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0f);
				smallCv.put(UtilizationClassVariable.LOREY_HEIGHT, 0f);
				smallCv.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0f);
				smallCv.put(UtilizationClassVariable.BASAL_AREA, -2.1394816E-7f);

				spec.setCompatibilityVariables(volCv, baCv, dqCv, smallCv);

				spec.setBaseAreaByUtilization(
						Utils.utilizationVector(
								0.0f, 0.41014498f, 0.005070707f, 0.013767676f, 0.023070706f, 0.36508077f
						)
				);
				spec.setLoreyHeightByUtilization(Utils.heightVector(8.0272f, 36.967514f));
				spec.setQuadraticMeanDiameterByUtilization(
						Utils.utilizationVector(6.1f, 31.85186f, 9.1706505f, 13.66034f, 18.178656f, 42.070774f)
				);
				spec.setTreesPerHectareByUtilization(
						Utils.utilizationVector(0.0f, 5.1472816f, 0.7676767f, 0.9393939f, 0.88888884f, 2.6262624f)
				);
				spec.setWholeStemVolumeByUtilization(
						Utils.utilizationVector(0.0f, 6.272525f, 0.018686868f, 0.076464646f, 0.17656565f, 6.000808f)
				);
				spec.setCloseUtilizationVolumeByUtilization(
						Utils.utilizationVector(0.0f, 6.0193934f, 9.0909086E-4f, 0.05030303f, 0.15363635f, 5.814545f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(0.0f, 5.87505f, 9.0909086E-4f, 0.05010101f, 0.15272726f, 5.671313f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(0.0f, 5.9056563f, 9.0909086E-4f, 0.050202016f, 0.15292929f, 5.701616f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.0f, 5.5734344f, 9.0909086E-4f, 0.047979794f, 0.14595959f, 5.378687f)
				);

			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("C");
				var baCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				baCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 6.5276026E-6f);
				baCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 5.373955E-6f);
				baCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 9.404421E-6f);
				baCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, -2.149582E-5f);

				var dqCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				dqCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -1.1962891E-4f);
				dqCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -1.9813539E-4f);
				dqCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, 5.8879855E-4f);
				dqCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 6.803894E-4f);

				var volCv = new MatrixMap3Impl<>(
						UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0.0f
				);

				volCv.put(UtilizationClass.U75TO125, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -2.88558E-5f);
				volCv.put(UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -2.4463178E-4f);
				volCv.put(
						UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						0.017329136f
				);
				volCv.put(
						UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, -0.15919617f
				);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 4.1239262E-5f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -1.0806322E-4f);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						9.806299E-4f
				);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, -0.0044211294f
				);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -2.3832321E-5f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -6.0749053E-6f);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						2.1846294E-4f
				);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, -0.0029561424f
				);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 9.3460085E-7f);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 2.4065972E-5f);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-1.8925668E-5f
				);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 3.4580233E-5f
				);

				Map<UtilizationClassVariable, Float> smallCv = new EnumMap<>(UtilizationClassVariable.class);
				smallCv.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0023201467f);
				smallCv.put(UtilizationClassVariable.LOREY_HEIGHT, 1.2850753E-6f);
				smallCv.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0010083826f);
				smallCv.put(UtilizationClassVariable.BASAL_AREA, 4.406223E-5f);

				spec.setCompatibilityVariables(volCv, baCv, dqCv, smallCv);

				spec.setBaseAreaByUtilization(
						Utils.utilizationVector(
								0.012555555f, 5.136462f, 0.12951516f, 0.3131616f, 0.5185757f, 4.1356964f
						)
				);
				spec.setLoreyHeightByUtilization(Utils.heightVector(6.4602f, 23.03769f));
				spec.setQuadraticMeanDiameterByUtilization(
						Utils.utilizationVector(5.997418f, 27.894548f, 10.063532f, 14.862596f, 19.873747f, 39.793953f)
				);
				spec.setTreesPerHectareByUtilization(
						Utils.utilizationVector(4.444444f, 84.04958f, 16.28283f, 18.050505f, 16.71717f, 33.25252f)
				);
				spec.setWholeStemVolumeByUtilization(
						Utils.utilizationVector(0.05121212f, 43.907677f, 0.60878783f, 1.9431312f, 3.861616f, 37.49414f)
				);
				spec.setCloseUtilizationVolumeByUtilization(
						Utils.utilizationVector(0.0f, 39.838383f, 0.11272726f, 1.4245454f, 3.3496969f, 34.951412f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(0.0f, 36.629692f, 0.110505044f, 1.3848485f, 3.2451513f, 31.889189f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(0.0f, 35.64939f, 0.110101f, 1.3765656f, 3.2180805f, 30.944645f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.0f, 33.246864f, 0.10454545f, 1.3045454f, 3.0379796f, 28.799898f)
				);

			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("S");
				var baCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				baCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -1.2635365E-5f);
				baCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -3.6683083E-5f);
				baCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -7.216871E-5f);
				baCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 1.2149811E-4f);

				var dqCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				dqCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -7.803917E-4f);
				dqCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, 9.682465E-4f);
				dqCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -0.0053552627f);
				dqCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, -2.3551942E-4f);

				var volCv = new MatrixMap3Impl<>(
						UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0.0f
				);

				volCv.put(UtilizationClass.U75TO125, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 1.8458366E-5f);
				volCv.put(UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -8.657909E-4f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -9.883404E-5f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -2.428794E-4f);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						0.009972191f
				);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 1.5911579E-4f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 8.441782E-4f);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-0.0014051724f
				);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -7.2431567E-6f);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -5.140305E-6f);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-2.8505327E-5f
				);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 5.271149E-4f
				);

				Map<UtilizationClassVariable, Float> smallCv = new EnumMap<>(UtilizationClassVariable.class);
				smallCv.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f);
				smallCv.put(UtilizationClassVariable.LOREY_HEIGHT, -5.6603396E-5f);
				smallCv.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f);
				smallCv.put(UtilizationClassVariable.BASAL_AREA, 3.352447E-6f);

				spec.setCompatibilityVariables(volCv, baCv, dqCv, smallCv);

				spec.setBaseAreaByUtilization(
						Utils.utilizationVector(
								0.0011616162f, 4.4495215f, 0.022474747f, 0.079909086f, 0.26358584f, 4.049323f
						)
				);
				spec.setLoreyHeightByUtilization(Utils.heightVector(8.2003f, 32.253902f));
				spec.setQuadraticMeanDiameterByUtilization(
						Utils.utilizationVector(6.3775334f, 33.434757f, 10.186823f, 15.028074f, 19.852716f, 37.923714f)
				);
				spec.setTreesPerHectareByUtilization(
						Utils.utilizationVector(0.36363637f, 50.67889f, 2.7575758f, 4.50505f, 8.515151f, 35.848484f)
				);
				spec.setWholeStemVolumeByUtilization(
						Utils.utilizationVector(
								0.004545454f, 56.45242f, 0.13040403f, 0.6863636f, 2.7667675f, 52.868885f
						)
				);
				spec.setCloseUtilizationVolumeByUtilization(
						Utils.utilizationVector(0.0f, 53.423737f, 0.02151515f, 0.549495f, 2.4642422f, 50.388485f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(0.0f, 52.985954f, 0.02141414f, 0.5464646f, 2.4494948f, 49.968582f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(0.0f, 52.90575f, 0.02141414f, 0.54606056f, 2.4473736f, 49.890903f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.0f, 50.71313f, 0.02060606f, 0.5241414f, 2.3488889f, 47.819595f)
				);

			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("D");

				var baCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				baCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -4.2549896E-6f);
				baCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -1.4847741E-5f);
				baCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -7.765949E-5f);
				baCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 9.7198485E-5f);

				var dqCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				dqCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -0.008364677f);
				dqCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -0.008026352f);
				dqCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -0.0018785477f);
				dqCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 3.5888673E-4f);

				var volCv = new MatrixMap3Impl<>(
						UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0.0f
				);

				volCv.put(UtilizationClass.U75TO125, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 0.0013236285f);
				volCv.put(UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 0.0061060176f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 4.6916964E-4f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 0.0010168457f);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						0.010494633f
				);

				volCv.put(UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 3.8318634E-5f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 1.2196541E-4f);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						7.2618487E-4f
				);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -1.4019013E-6f);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 3.7384034E-6f);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-2.4299621E-5f
				);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 4.0655137E-5f
				);

				Map<UtilizationClassVariable, Float> smallCv = new EnumMap<>(UtilizationClassVariable.class);

				smallCv.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f);
				smallCv.put(UtilizationClassVariable.LOREY_HEIGHT, -1.5245796E-5f);
				smallCv.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f);
				smallCv.put(UtilizationClassVariable.BASAL_AREA, 4.8476713E-6f);

				spec.setCompatibilityVariables(volCv, baCv, dqCv, smallCv);

				spec.setBaseAreaByUtilization(
						Utils.utilizationVector(
								0.0015656565f, 29.827923f, 0.014262626f, 0.051797975f, 0.46197978f, 29.070423f
						)
				);
				spec.setLoreyHeightByUtilization(Utils.heightVector(10.6033f, 33.933018f));
				spec.setQuadraticMeanDiameterByUtilization(
						Utils.utilizationVector(6.4799547f, 36.39499f, 10.470092f, 15.579478f, 20.52722f, 36.869785f)
				);
				spec.setTreesPerHectareByUtilization(
						Utils.utilizationVector(0.47474745f, 286.7147f, 1.6565655f, 2.7171717f, 13.959595f, 272.2828f)
				);
				spec.setWholeStemVolumeByUtilization(
						Utils.utilizationVector(0.007878787f, 464.165f, 0.110202014f, 0.56585854f, 6.073636f, 457.4153f)
				);
				spec.setCloseUtilizationVolumeByUtilization(
						Utils.utilizationVector(0.0f, 448.56998f, 0.057676766f, 0.50989896f, 5.6983833f, 442.30402f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(0.0f, 440.93735f, 0.057171714f, 0.5057576f, 5.65404f, 434.72037f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(0.0f, 439.67856f, 0.057070702f, 0.50555557f, 5.651313f, 433.46463f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.0f, 417.8736f, 0.054646462f, 0.4831313f, 5.3922224f, 411.9436f)
				);
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("H");

				var baCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				baCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, -5.420685E-5f);
				baCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -8.119345E-5f);
				baCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -5.1870345E-5f);
				baCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, 1.8692017E-4f);

				var dqCv = new MatrixMap2Impl<>(UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0.0f);
				dqCv.put(UtilizationClass.U75TO125, LayerType.PRIMARY, 1.7196656E-4f);
				dqCv.put(UtilizationClass.U125TO175, LayerType.PRIMARY, -7.5422286E-4f);
				dqCv.put(UtilizationClass.U175TO225, LayerType.PRIMARY, -0.0012542343f);
				dqCv.put(UtilizationClass.OVER225, LayerType.PRIMARY, -0.0010336685f);

				var volCv = new MatrixMap3Impl<>(
						UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0.0f
				);

				volCv.put(UtilizationClass.U75TO125, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -7.733822E-5f);
				volCv.put(UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -1.3294697E-4f);
				volCv.put(
						UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						0.011269417f
				);
				volCv.put(
						UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, -0.13499795f
				);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 2.7103424E-5f);
				volCv.put(UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 3.2465698E-4f);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-0.0010089016f
				);
				volCv.put(
						UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 0.005517883f
				);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, 4.112244E-5f);
				volCv.put(UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, 2.0864964E-4f);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-9.173107E-4f
				);
				volCv.put(
						UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 0.0027701568f
				);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, -1.074791E-5f);
				volCv.put(UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, -5.841255E-6f);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY,
						-5.373955E-5f
				);
				volCv.put(
						UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
						LayerType.PRIMARY, 3.7010195E-4f
				);

				Map<UtilizationClassVariable, Float> smallCv = new EnumMap<>(UtilizationClassVariable.class);

				smallCv.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f);
				smallCv.put(UtilizationClassVariable.LOREY_HEIGHT, 0.0f);
				smallCv.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f);
				smallCv.put(UtilizationClassVariable.BASAL_AREA, 0.0f);

				spec.setCompatibilityVariables(volCv, baCv, dqCv, smallCv);

				spec.setBaseAreaByUtilization(
						Utils.utilizationVector(0.0f, 5.914244f, 0.3650303f, 0.83281815f, 1.0865252f, 3.5843737f)
				);
				spec.setLoreyHeightByUtilization(Utils.heightVector(7.5464f, 22.89132f));
				spec.setQuadraticMeanDiameterByUtilization(
						Utils.utilizationVector(Float.NaN, 21.201519f, 10.276456f, 15.108315f, 20.090958f, 31.892609f)
				);
				spec.setTreesPerHectareByUtilization(
						Utils.utilizationVector(0.0f, 167.52338f, 44.010098f, 46.454544f, 34.272724f, 44.868683f)
				);
				spec.setWholeStemVolumeByUtilization(
						Utils.utilizationVector(0.0f, 56.45232f, 1.7560605f, 5.9258585f, 9.749595f, 39.020805f)
				);
				spec.setCloseUtilizationVolumeByUtilization(
						Utils.utilizationVector(0.0f, 50.332424f, 0.19444443f, 4.460101f, 8.661818f, 37.01606f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayByUtilization(
						Utils.utilizationVector(0.0f, 49.569794f, 0.19323231f, 4.428889f, 8.587777f, 36.359898f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
						Utils.utilizationVector(0.0f, 49.34848f, 0.1930303f, 4.423131f, 8.568384f, 36.163937f)
				);
				spec.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						Utils.utilizationVector(0.0f, 47.154343f, 0.18525252f, 4.244747f, 8.221919f, 34.502422f)
				);

			}

			cmp.computeUtilizationComponentsPrimary(bec, vdypLayer, volumeComputeMode, compatibilityVariableMode);

			assertThat(
					vdypLayer, hasUtilization(
							"baseAreaByUtilization", //
							0.015282828f, 45.738297f, 0.518534f, 1.2601388f, 2.2969778f, 41.662643f
					)
			);
			assertThat(
					vdypLayer, hasUtilization(
							"quadraticMeanDiameterByUtilization", //
							6.0690913f, 31.308353f, 10.204787f, 15.031048f, 20.060429f, 37.021965f
					)
			);
			assertThat(
					vdypLayer, hasUtilization(
							"treesPerHectareByUtilization", //
							5.2828283f, 594.11383f, 63.398567f, 71.014984f, 72.67525f, 387.0241f
					)
			);
			assertThat(vdypLayer, hasUtilizationHeight(7.016903f, 31.145563f));
			assertThat(
					vdypLayer, hasUtilization(
							"wholeStemVolumeByUtilization", //
							0.06363636f, 634.84357f, 2.5150185f, 8.897322f, 21.897877f, 601.5333f
					)
			);

			assertThat(
					vdypLayer, hasUtilization(
							"closeUtilizationVolumeByUtilization", //
							0.0f, 605.8782f, 0.37028643f, 6.755397f, 19.658886f, 579.0936f
					)
			);

			assertThat(
					vdypLayer, hasUtilization(
							"closeUtilizationVolumeNetOfDecayByUtilization", //
							0.0f, 593.36444f, 0.36635828f, 6.6791697f, 19.425755f, 566.8931f
					)
			);
			assertThat(
					vdypLayer, hasUtilization(
							"closeUtilizationVolumeNetOfDecayAndWasteByUtilization", //
							0.0f, 590.73737f, 0.365783f, 6.664928f, 19.37525f, 564.3314f
					)
			);
			assertThat(
					vdypLayer, hasUtilization(
							"closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization", //
							0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f // Seems odd?
					)
			);

		}
	}
}
