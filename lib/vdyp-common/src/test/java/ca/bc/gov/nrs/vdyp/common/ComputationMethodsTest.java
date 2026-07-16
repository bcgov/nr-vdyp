package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
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
		void testSimple() throws ProcessingException {
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

				lb.addSpecies(sb -> {
					sb.genus("B", 3);
					sb.percentGenus(0.890327f);
					sb.breakageGroup(3);
					sb.decayGroup(7);
					sb.volumeGroup(12);
					sb.addSp64Distribution("B", 100);
				});

				lb.addSpecies(sb -> {
					sb.genus("C", 4);
					sb.percentGenus(0.890327f);
					sb.breakageGroup(3);
					sb.decayGroup(7);
					sb.volumeGroup(12);
					sb.addSp64Distribution("B", 100);
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
			}
			{
				final VdypSpecies spec = vdypLayer.getSpeciesBySp0("C");
				spec.setBaseAreaByUtilization(Utils.utilizationVector(0, 0.39730823f, 0, 0, 0, 0));
				spec.setLoreyHeightByUtilization(Utils.utilizationVector(8.394591f, 38.600353f, 0, 0, 0, 0));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(6.135865f, 31.66131f, 0, 0, 0, 0));
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(0, 5.0463796f, 0, 0, 0, 0));
			}

			cmp.computeUtilizationComponentsPrimary(bec, vdypLayer, volumeComputeMode, compatibilityVariableMode);

		}
	}
}
