package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecies;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasUtilization;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasUtilizationHeight;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.utilization;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.utilizationHeight;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

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
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
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
	}
}
