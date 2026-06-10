package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.closeUtilMap;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.utilization;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class EstimationMethodsTest {

	Map<String, Object> controlMap;
	BecLookup becLookup;
	EstimationMethods emp;

	@BeforeEach
	void setup() {
		controlMap = TestUtils.loadControlMap();
		var resolvedControlMap = TestUtils.resolveControlMap(controlMap);
		emp = new EstimationMethods(resolvedControlMap);
		becLookup = (BecLookup) controlMap.get(ControlKey.BEC_DEF.name());
	}

	@Nested
	class BasalAreaEstimation {
		@Test
		void testWhenBasalAreaAllIsZero() throws ProcessingException {
			var becDefinition = becLookup.get("IDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(1);

			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector basalAreaByUtilization = Utils.utilizationVector(0.0f);

			emp.estimateBaseAreaByUtilization(
					becDefinition, quadMeanDiameterByUtilization, basalAreaByUtilization, genus.getAlias()
			);

			for (var c : basalAreaByUtilization) {
				assertThat(c, is(0.0f));
			}
		}

		@Test
		void testWhenBasalAreaAllIsTenAndQuadMeanDiametersAreAllZero() throws ProcessingException {
			var becDefinition = becLookup.get("IDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(1);

			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector basalAreaByUtilization = Utils.utilizationVector(10.0f);

			emp.estimateBaseAreaByUtilization(
					becDefinition, quadMeanDiameterByUtilization, basalAreaByUtilization, genus.getAlias()
			);

			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.SMALL.index), is(0.0f));
			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.ALL.index), is(10.0f));
			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.U75TO125.index), is(10.0f));
			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.U125TO175.index), is(0.0f));
			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.U175TO225.index), is(0.0f));
			assertThat(basalAreaByUtilization.getCoe(UtilizationClass.OVER225.index), is(0.0f));
		}

		@Test
		void testTypical() throws ProcessingException {

			var becDefinition = becLookup.get("CDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(31.5006275f);
			UtilizationVector basalAreaByUtilization = Utils.utilizationVector(0.406989872f);

			emp.estimateBaseAreaByUtilization(
					becDefinition, quadMeanDiameterByUtilization, basalAreaByUtilization, genus.getAlias()
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(
					basalAreaByUtilization,
					contains(0.0f, 0.406989872f, 0.00509467721f, 0.0138180256f, 0.023145527f, 0.36493164f)
			);
		}

		@Test
		void testWithInstantiatedControlMap() throws ProcessingException {
			var dq = Utils.utilizationVector();
			var ba = Utils.utilizationVector();
			dq.setCoe(0, 31.6622887f);
			dq.setCoe(1, 10.0594692f);
			dq.setCoe(2, 14.966774f);
			dq.setCoe(3, 19.9454956f);
			dq.setCoe(4, 46.1699982f);

			ba.setCoe(0, 0.397305071f);

			var bec = Utils.getBec("CWH", controlMap);

			emp.estimateBaseAreaByUtilization(bec, dq, ba, "B");

			assertThat(
					ba,
					VdypMatchers
							.utilization(0f, 0.397305071f, 0.00485289097f, 0.0131751001f, 0.0221586525f, 0.357118428f)
			);

		}

	}

	@Nested
	class CloseUtilizationEstimation {

		@Test
		void testWhenWholeStemVolumeIsZero() throws ProcessingException {

			var becDefinition = becLookup.get("IDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(1);

			var volumeEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class
			);
			int volumeGroup = volumeEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector wholeStemVolumeByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationVolume = Utils.utilizationVector(0.0f);
			float loreyHeight = 30.0f;

			emp.estimateCloseUtilizationVolume(
					UtilizationClass.U75TO125, aAdjust, volumeGroup, loreyHeight, quadMeanDiameterByUtilization,
					wholeStemVolumeByUtilization, closeUtilizationVolume
			);

			assertThat(closeUtilizationVolume, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		}

		@Test
		void testTypical() throws ProcessingException {

			var becDefinition = becLookup.get("CDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			var volumeEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class
			);
			int volumeGroup = volumeEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils
					.utilizationVector(0.0f, 31.5006275f, 9.17065048f, 13.6603403f, 18.1786556f, 42.0707741f);
			UtilizationVector wholeStemVolumeByUtilization = Utils
					.utilizationVector(0.0f, 0.0186868683f, 0.0764646456f, 0.176565647f, 6.00080776f);
			UtilizationVector closeUtilizationVolume = Utils.utilizationVector(0.0f);
			float loreyHeight = 36.7552986f;

			emp.estimateCloseUtilizationVolume(
					UtilizationClass.U175TO225, aAdjust, volumeGroup, loreyHeight, quadMeanDiameterByUtilization,
					wholeStemVolumeByUtilization, closeUtilizationVolume
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(closeUtilizationVolume, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.15350838f, 0.0f));
		}

		@Test
		void testVeteran() throws Exception {
			TestUtils.populateControlMapCloseUtilization(controlMap, closeUtilMap(12));

			var utilizationClass = UtilizationClass.OVER225;
			var aAdjust = new Coefficients(new float[] { 0f, 0f, 0f, -0.0981800035f }, 1);
			var volumeGroup = 12;
			var lorieHeight = 26.2000008f;
			var quadMeanDiameterUtil = Utils.utilizationVector(51.8356705f, 0f, 0f, 0f, 51.8356705f);
			var wholeStemVolumeUtil = Utils.utilizationVector(0f, 0f, 0f, 0f, 6.11904192f);

			var closeUtilizationUtil = Utils.utilizationVector(0f, 0f, 0f, 0f, 0f);

			emp.estimateCloseUtilizationVolume(
					utilizationClass, aAdjust, volumeGroup, lorieHeight, quadMeanDiameterUtil, wholeStemVolumeUtil,
					closeUtilizationUtil
			);

			assertThat(closeUtilizationUtil, utilization(0f, 0f, 0f, 0f, 0f, 5.86088896f));

		}
	}

	@Nested
	class CloseUtilizationLessDecayEstimation {
		@Test
		void testWhenCloseUtilizationIsZero() throws ProcessingException {

			var becDefinition = becLookup.get("CDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationNetOfDecay = Utils.utilizationVector(0.0f);

			var volumeEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class
			);
			int volumeGroup = volumeEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			emp.estimateNetDecayVolume(
					genus.getAlias(), becDefinition.getRegion(), UtilizationClass.U175TO225, aAdjust, volumeGroup, 0.0f,
					quadMeanDiameterByUtilization, closeUtilization, closeUtilizationNetOfDecay
			);

			assertThat(closeUtilizationNetOfDecay, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		}

		@Test
		void testTypical() throws ProcessingException {

			var becDefinition = becLookup.get("CWH").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils
					.utilizationVector(0.0f, 31.5006275f, 9.17065048f, 13.6603403f, 18.1786556f, 42.0707741f);
			UtilizationVector closeUtilization = Utils
					.utilizationVector(0.0f, 6.01939344f, 0.000909090857f, 0.0503030308f, 0.153636351f, 5.81454515f);
			UtilizationVector closeUtilizationNetOfDecay = Utils.utilizationVector(0.0f);

			var decayEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.DECAY_GROUPS, MatrixMap2.class
			);
			int decayGroup = decayEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			emp.estimateNetDecayVolume(
					genus.getAlias(), becDefinition.getRegion(), UtilizationClass.U175TO225, aAdjust, decayGroup, 54.0f,
					quadMeanDiameterByUtilization, closeUtilization, closeUtilizationNetOfDecay
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(closeUtilizationNetOfDecay, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.15293269f, 0.0f));
		}
	}

	@Nested
	class CloseUtilizationLessDecayAndWastageEstimation {
		@Test
		void testWhenCloseUtilizationLessDecayIsZero() throws ProcessingException {

			var becDefinition = becLookup.get("CDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationNetOfDecay = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationNetOfDecayAndWastage = Utils.utilizationVector(0.0f);

			emp.estimateNetDecayAndWasteVolume(
					becDefinition.getRegion(), UtilizationClass.U175TO225, aAdjust, genus.getAlias(), 0.0f,
					quadMeanDiameterByUtilization, closeUtilization, closeUtilizationNetOfDecay,
					closeUtilizationNetOfDecayAndWastage
			);

			assertThat(closeUtilizationNetOfDecay, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		}

		@Test
		void testTypical() throws ProcessingException {

			var becDefinition = becLookup.get("CWH").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			Coefficients aAdjust = Utils.utilizationVector(0.0f);
			UtilizationVector quadMeanDiameterByUtilization = Utils
					.utilizationVector(0.0f, 31.5006275f, 9.17065048f, 13.6603403f, 18.1786556f, 42.0707741f);
			UtilizationVector closeUtilization = Utils
					.utilizationVector(0.0f, 6.01939344f, 0.000909090857f, 0.0503030308f, 0.153636351f, 5.81454515f);
			UtilizationVector closeUtilizationNetOfDecay = Utils
					.utilizationVector(0.0f, 5.90565634f, 0.000909090857f, 0.0502020158f, 0.152929291f, 5.70161581f);
			UtilizationVector closeUtilizationNetOfDecayAndWastage = Utils.utilizationVector(0.0f);

			emp.estimateNetDecayAndWasteVolume(
					becDefinition.getRegion(), UtilizationClass.U175TO225, aAdjust, genus.getAlias(), 36.7552986f,
					quadMeanDiameterByUtilization, closeUtilization, closeUtilizationNetOfDecay,
					closeUtilizationNetOfDecayAndWastage
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(closeUtilizationNetOfDecayAndWastage, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.15271991f, 0.0f));
		}
	}

	@Nested
	class CloseUtilizationLessDecayAndWastageAndBreakageEstimation {

		@Test
		void testWhenCloseUtilizationLessDecayAndWastageIsZero() throws ProcessingException {

			var becDefinition = becLookup.get("CDF").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilization = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationNetOfDecayAndWastage = Utils.utilizationVector(0.0f);
			UtilizationVector closeUtilizationNetOfDecayWastageAndBreakage = Utils.utilizationVector(0.0f);

			var breakageEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.BREAKAGE_GROUPS, MatrixMap2.class
			);
			int breakageGroup = breakageEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			emp.estimateNetDecayWasteAndBreakageVolume(
					UtilizationClass.U175TO225, breakageGroup, quadMeanDiameterByUtilization, closeUtilization,
					closeUtilizationNetOfDecayAndWastage, closeUtilizationNetOfDecayWastageAndBreakage
			);

			assertThat(closeUtilizationNetOfDecayAndWastage, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		}

		@Test
		void testTypical() throws ProcessingException {

			var becDefinition = becLookup.get("CWH").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			UtilizationVector quadMeanDiameterByUtilization = Utils
					.utilizationVector(0.0f, 31.5006275f, 9.17065048f, 13.6603403f, 18.1786556f, 42.0707741f);
			UtilizationVector closeUtilization = Utils
					.utilizationVector(0.0f, 6.01939344f, 0.000909090857f, 0.0503030308f, 0.153636351f, 5.81454515f);
			UtilizationVector closeUtilizationNetOfDecayAndWastage = Utils
					.utilizationVector(0.0f, 0.0f, 0.0f, 0.0f, 0.15271991f, 0.0f);
			UtilizationVector closeUtilizationNetOfDecayWastageAndBreakage = Utils.utilizationVector(0.0f);

			var breakageEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.BREAKAGE_GROUPS, MatrixMap2.class
			);
			int breakageGroup = breakageEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			emp.estimateNetDecayWasteAndBreakageVolume(
					UtilizationClass.U175TO225, breakageGroup, quadMeanDiameterByUtilization, closeUtilization,
					closeUtilizationNetOfDecayAndWastage, closeUtilizationNetOfDecayWastageAndBreakage
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(
					closeUtilizationNetOfDecayWastageAndBreakage, contains(0.0f, 0.0f, 0.0f, 0.0f, 0.14595404f, 0.0f)
			);
		}

	}

	@Nested
	class EstimateQuadMeanDiameterByUtilization {

		@Test
		void testTest1() throws Exception {
			var controlMap = TestUtils.loadControlMap();

			var coe = Utils.utilizationVector();
			coe.setAll(31.6622887f);

			var bec = Utils.getBec("CWH", controlMap);

			emp.estimateQuadMeanDiameterByUtilization(bec, coe, "B");

			assertThat(coe, utilization(0f, 31.6622887f, 10.0594692f, 14.966774f, 19.9454956f, 46.1699982f));
		}

		@Test
		void testTest2() throws Exception {
			var controlMap = TestUtils.loadControlMap();

			var coe = Utils.utilizationVector();
			coe.setAll(13.4943399f);

			var bec = Utils.getBec("MH", controlMap);

			emp.estimateQuadMeanDiameterByUtilization(bec, coe, "L");

			assertThat(coe, utilization(0f, 13.4943399f, 10.2766619f, 14.67033f, 19.4037666f, 25.719244f));
		}

	}

	@Nested
	class EstimateQuadMeanDiameterForSpecies {
		@Test
		void testSimple() throws Exception {

			var layer = VdypLayer.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
			});

			// sp 3, 4, 5, 8, 15
			// sp B, C, D, H, S
			var spec1 = VdypSpecies.build(layer, builder -> {
				builder.genus("B", controlMap);
				builder.volumeGroup(12);
				builder.decayGroup(7);
				builder.breakageGroup(5);
				builder.percentGenus(1f);
			});
			spec1.getLoreyHeightByUtilization().setCoe(0, 38.7456512f);
			spec1.setFractionGenus(0.00817133673f);

			var spec2 = VdypSpecies.build(layer, builder -> {
				builder.genus("C", controlMap);
				builder.volumeGroup(4);
				builder.decayGroup(14);
				builder.breakageGroup(6);
				builder.percentGenus(7f);
			});
			spec2.getLoreyHeightByUtilization().setCoe(0, 22.8001652f);
			spec2.setFractionGenus(0.0972022042f);

			var spec3 = VdypSpecies.build(layer, builder -> {
				builder.genus("D", controlMap);
				builder.volumeGroup(25);
				builder.decayGroup(19);
				builder.breakageGroup(12);
				builder.percentGenus(74f);
			});
			spec3.getLoreyHeightByUtilization().setCoe(0, 33.6889763f);
			spec3.setFractionGenus(0.695440531f);

			var spec4 = VdypSpecies.build(layer, lb -> {
				lb.genus("H", controlMap);
				lb.volumeGroup(37);
				lb.decayGroup(31);
				lb.breakageGroup(17);
				lb.percentGenus(9f);
				lb.addSite(ib -> {
					ib.ageTotal(55f);
					ib.yearsToBreastHeight(1f);
					ib.yearsAtBreastHeightAuto();
					ib.height(32.2999992f);
				});
			});
			spec4.getLoreyHeightByUtilization().setCoe(0, 24.3451157f);
			spec4.setFractionGenus(0.117043354f);

			var spec5 = VdypSpecies.build(layer, builder -> {
				builder.genus("S", controlMap);
				builder.volumeGroup(66);
				builder.decayGroup(54);
				builder.breakageGroup(28);
				builder.percentGenus(9f);
			});
			spec5.getLoreyHeightByUtilization().setCoe(0, 34.6888771f);
			spec5.setFractionGenus(0.082142584f);

			Map<String, VdypSpecies> specs = new HashMap<>();
			specs.put(spec1.getGenus(), spec1);
			specs.put(spec2.getGenus(), spec2);
			specs.put(spec3.getGenus(), spec3);
			specs.put(spec4.getGenus(), spec4);
			specs.put(spec5.getGenus(), spec5);

			float dq = emp.estimateQuadMeanDiameterForSpecies(
					spec1, specs, Region.COASTAL, 30.2601795f, 44.6249847f, 620.504883f, 31.6603775f
			);

			assertThat(dq, closeTo(31.7022133f));

		}

		@Test
		void testClampSimple() {
			var limits = new ComponentSizeLimits(48.3f, 68.7f, 0.729f, 1.718f);
			float standTreesPerHectare = 620.5049f;
			float minQuadMeanDiameter = 7.6f;
			float loreyHeightSpec = 38.74565f;
			float baseArea1 = 0.36464578f;
			float baseArea2 = 44.260338f;
			float quadMeanDiameter1 = 31.697449f;
			float treesPerHectare2 = 615.8839f;
			float quadMeanDiameter2 = 30.249138f;

			float dq = emp.estimateQuadMeanDiameterClampResult(
					limits, standTreesPerHectare, minQuadMeanDiameter, loreyHeightSpec, baseArea1, baseArea2,
					quadMeanDiameter1, treesPerHectare2, quadMeanDiameter2
			);

			assertThat(dq, is(quadMeanDiameter1));

		}

		@Test
		void testClampToLow2() {
			var limits = new ComponentSizeLimits(48.3f, 68.7f, 0.729f, 1.718f);
			float standTreesPerHectare = 620.5049f;
			float minQuadMeanDiameter = 7.6f;
			float loreyHeightSpec = 38.74565f;

			float baseArea1 = 44.36464578f;
			float baseArea2 = 0.1f;

			float quadMeanDiameter2 = 7.3f; // Less than minQuadMeanDiameter 7.6

			float treesPerHectare2 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea2, quadMeanDiameter2);
			float treesPerHectare1 = standTreesPerHectare - treesPerHectare2;

			float quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);

			float dq = emp.estimateQuadMeanDiameterClampResult(
					limits, standTreesPerHectare, minQuadMeanDiameter, loreyHeightSpec, baseArea1, baseArea2,
					quadMeanDiameter1, treesPerHectare2, quadMeanDiameter2
			);

			assertThat(dq, closeTo(30.722431f));

		}

		@Test
		void testClampToLow1() {
			var limits = new ComponentSizeLimits(48.3f, 68.7f, 0.729f, 1.718f);
			float standTreesPerHectare = 620.5049f;
			float minQuadMeanDiameter = 7.6f;
			float loreyHeightSpec = 38.74565f;

			float baseArea1 = 30f;
			float baseArea2 = 10f;

			float quadMeanDiameter1 = 26f; // Less than computed min of 28.245578

			float treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			float treesPerHectare2 = standTreesPerHectare - treesPerHectare1;

			float quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);

			float dq = emp.estimateQuadMeanDiameterClampResult(
					limits, standTreesPerHectare, minQuadMeanDiameter, loreyHeightSpec, baseArea1, baseArea2,
					quadMeanDiameter1, treesPerHectare2, quadMeanDiameter2
			);

			assertThat(dq, closeTo(28.245578f));

		}

		@Test
		void testClampToHigh1() {
			var limits = new ComponentSizeLimits(48.3f, 68.7f, 0.729f, 1.718f);
			float standTreesPerHectare = 620.5049f;
			float minQuadMeanDiameter = 7.6f;
			float loreyHeightSpec = 38.74565f;

			float baseArea1 = 30f;
			float baseArea2 = 10f;

			float quadMeanDiameter1 = 70f; // More than than computed max of 66.565033

			float treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			float treesPerHectare2 = standTreesPerHectare - treesPerHectare1;

			float quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);

			float dq = emp.estimateQuadMeanDiameterClampResult(
					limits, standTreesPerHectare, minQuadMeanDiameter, loreyHeightSpec, baseArea1, baseArea2,
					quadMeanDiameter1, treesPerHectare2, quadMeanDiameter2
			);

			assertThat(dq, closeTo(66.565033f));

		}

	}

	@Nested
	class WholeStemVolumeEstimation {

		@Test
		void testPrimary() throws ProcessingException {

			var becDefinition = becLookup.get("CWH").get();

			var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
			var genus = genera.getByIndex(3);

			UtilizationVector quadMeanDiameterByUtilization = Utils
					.utilizationVector(0.0f, 31.5006275f, 9.17065048f, 13.6603403f, 18.1786556f, 42.0707741f);
			UtilizationVector basalAreaByUtilization = Utils
					.utilizationVector(0.0f, 0.406989872f, 0.00507070683f, 0.0137676764f, 0.0230707061f, 0.365080774f);
			UtilizationVector wholeStemVolumeByUtilization = Utils
					.utilizationVector(0.0f, 6.27250576f, 0.0f, 0.0f, 0.0f, 0.0f);

			var volumeEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
					controlMap, ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class
			);
			int volumeGroup = volumeEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

			emp.estimateWholeStemVolume(
					UtilizationClass.ALL, 0.0f, volumeGroup, 36.7552986f, quadMeanDiameterByUtilization,
					basalAreaByUtilization, wholeStemVolumeByUtilization
			);

			// Result of run in FORTRAN VDYP7 with the above parameters.
			assertThat(
					wholeStemVolumeByUtilization,
					contains(0.0f, 6.27250576f, 0.01865777f, 0.07648385f, 0.17615195f, 6.00121212f)
			);
		}

		@Test
		void testVeteran() throws Exception {
			var utilizationClass = UtilizationClass.OVER225;
			var aAdjust = 0.10881f;
			var volumeGroup = 12;
			var lorieHeight = 26.2000008f;
			var quadMeanDiameterUtil = Utils.utilizationVector(51.8356705f, 0f, 0f, 0f, 51.8356705f);
			var baseAreaUtil = Utils.utilizationVector(0.492921442f, 0f, 0f, 0f, 0.492921442f);
			var wholeStemVolumeUtil = Utils.utilizationVector();

			emp.estimateWholeStemVolume(
					utilizationClass, aAdjust, volumeGroup, lorieHeight, quadMeanDiameterUtil, baseAreaUtil,
					wholeStemVolumeUtil
			);

			assertThat(wholeStemVolumeUtil, utilization(0f, 0f, 0f, 0f, 0f, 6.11904192f));

		}

	}

	@Test
	void testWholeStemVolumePerTreeEstimation() {

		var becDefinition = becLookup.get("CWH").get();

		var genera = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());
		var genus = genera.getByIndex(3);

		var volumeEquationGroupMatrix = Utils.<MatrixMap2<String, String, Integer>>expectParsedControl(
				controlMap, ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class
		);
		int volumeGroup = volumeEquationGroupMatrix.get(genus.getAlias(), becDefinition.getAlias());

		float result = emp.estimateWholeStemVolumePerTree(volumeGroup, 36.7552986f, 31.5006275f);

		// Result of run in FORTRAN VDYP7 with the above parameters.
		assertThat(result, is(1.2011181f));
	}

	@Nested
	class EstimateNonPrimaryLoreyHeight {

		@Test
		void testEqn1() throws Exception {

			var bec = Utils.getBec("CWH", controlMap);

			var spec = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});
			var specPrime = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("H", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});

			var result = emp.estimateNonPrimaryLoreyHeight(
					spec.getGenus(), specPrime.getGenus(), bec, 24.2999992f, 20.5984688f
			);

			assertThat(result, closeTo(21.5356998f));

		}

		@Test
		void testEqn2() throws Exception {

			var bec = Utils.getBec("CWH", controlMap);

			var spec = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});
			var specPrime = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("D", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});

			var result = emp.estimateNonPrimaryLoreyHeight(
					spec.getGenus(), specPrime.getGenus(), bec, 35.2999992f, 33.6889763f
			);

			assertThat(result, closeTo(38.7456512f));

		}

		@ParameterizedTest
		@ValueSource(ints = { Integer.MIN_VALUE, -8, -1, 0, 3, 8, Integer.MAX_VALUE })
		void testBadEqn(int eqn) throws Exception {

			MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> coeMap = Utils
					.expectParsedControl(controlMap, ControlKey.HL_NONPRIMARY, MatrixMap3.class);
			// Force the equation number to be something unsupported.
			coeMap.put(
					"B", "D", Region.COASTAL, Optional.of(new NonprimaryHLCoefficients(new float[] { 1f, 2f }, eqn))
			);

			var bec = Utils.getBec("CWH", controlMap);

			var spec = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("B", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});
			var specPrime = VdypSpecies.build(builder -> {
				builder.polygonIdentifier("Test", 2024);
				builder.layerType(LayerType.PRIMARY);
				builder.genus("D", controlMap);
				builder.percentGenus(50f);
				builder.volumeGroup(-1);
				builder.decayGroup(-1);
				builder.breakageGroup(-1);
			});

			var ex = assertThrows(
					IllegalStateException.class,
					() -> emp.estimateNonPrimaryLoreyHeight(
							spec.getGenus(), specPrime.getGenus(), bec, 35.2999992f, 33.6889763f
					)
			);

			// Should have a useful error message
			assertThat(
					ex,
					hasProperty(
							"message",
							is(
									MessageFormat.format(
											"Expecting non-primay Lorey height equation index 1 or 2 but was {0}", eqn
									)
							)
					)
			);

		}

	}

	@Nested
	class PrimaryLeadHeightConversion {

		@Test
		void testPrimaryHeightFromLeadHeight() {
			float result = emp.estimatePrimaryHeightFromLeadHeight(20.0f, "B", Region.COASTAL, 40.260403f);

			assertThat(result, closeTo(19.870464f));
		}

		@Test
		void testLeadHeightFromPrimaryHeight() {
			float result = emp.estimateLeadHeightFromPrimaryHeight(19.870464f, "B", Region.COASTAL, 40.260403f);

			assertThat(result, closeTo(20));
		}

	}

	@Nested
	class EstimateMeanVolumeSmall {

		@ParameterizedTest
		@CsvSource(
			{ // Values taken from VDYP7 via debugger
					"S, 5.58619356, 4.69048452, 0.00447751069", "S, 5.94472694, 7.54808998, 0.00964565482",
					"PL, 5.73309135, 5.04876852, 0.00573747745", "H, 6.32094097, 7.15886297, 0.00975632109",
					"Y, 6.0864749, 7.53712893, 0.0137963342" }
		)
		void testSimple(String speciesId, float dq, float hl, float expectedVolume) throws Exception {

			float result = emp.estimateMeanVolumeSmall(speciesId, hl, dq);

			assertThat(result, closeTo(expectedVolume));
		}

		@ParameterizedTest
		@CsvSource(
			{ // Values taken from VDYP7 via debugger
					"S, 5.58619356, 4.69048452, 0.00447751069", "S, 5.94472694, 7.54808998, 0.00964565482",
					"PL, 5.73309135, 5.04876852, 0.00573747745", "H, 6.32094097, 7.15886297, 0.00975632109",
					"Y, 6.0864749, 7.53712893, 0.0137963342" }
		)
		void testSpeciesObject(String speciesId, float dq, float hl, float expectedVolume) throws Exception {

			var em = EasyMock.createControl();
			BaseVdypSpecies<?> spec = em.createMock(BaseVdypSpecies.class);
			EasyMock.expect(spec.getGenus()).andStubReturn(speciesId);
			em.replay();
			float result = emp.estimateMeanVolumeSmall(spec, hl, dq);
			assertThat(result, closeTo(expectedVolume));
			em.verify();
		}

	}

	@Nested
	class EstimateLoreyHeightSmall {

		static Collection<Arguments> data() {
			return List.of(
					Arguments.of("B", 12.8474216f, 5.68512583f, 13.5389509f, 5.72298288f),
					Arguments.of("B", 12.9123373f, 5.68627961f, 13.6021004f, 5.72802305f),
					Arguments.of("PL", 13.9236240f, 6.08169651f, 17.2023621f, 7.21131372f),
					Arguments.of("S", 16.6616268f, 5.88927603f, 19.7704563f, 6.11308193f)
			);
		}

		@ParameterizedTest
		@MethodSource("data")
		void testSimple(String speciesId, float hlAll, float dqSmall, float dqAll, float expectedHeight)
				throws Exception {

			float result = emp.estimateSmallComponentLoreyHeight(speciesId, hlAll, dqSmall, dqAll);

			assertThat(result, closeTo(expectedHeight));
		}

		@ParameterizedTest
		@MethodSource("data")
		void testSpeciesObject(String speciesId, float hlAll, float dqSmall, float dqAll, float expectedHeight)
				throws Exception {

			var em = EasyMock.createControl();
			VdypSpecies spec = em.createMock(VdypSpecies.class);
			EasyMock.expect(spec.getGenus()).andStubReturn(speciesId);
			EasyMock.expect(spec.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, hlAll));
			EasyMock.expect(spec.getQuadraticMeanDiameterByUtilization()).andStubReturn(Utils.utilizationVector(dqAll));
			em.replay();

			float result = emp.estimateSmallComponentLoreyHeight(spec, dqSmall);
			em.verify();

			assertThat(result, closeTo(expectedHeight));
		}
	}

	@Nested
	class EstimateQuadraticMeanDiameterSmall {

		static Collection<Arguments> data() {
			return List.of(
					Arguments.of("B", 12.9761534f, 5.68740845f), Arguments.of("B", 13.4484148f, 5.69578362f),
					Arguments.of("PL", 14.7377281f, 6.12132778f), Arguments.of("S", 16.9553261f, 5.89840364f)
			);
		}

		@ParameterizedTest
		@MethodSource("data")
		void testSimple(String speciesId, float hlAll, float expectedDq) throws Exception {

			float result = emp.estimateSmallComponentQuadMeanDiameter(speciesId, hlAll);

			assertThat(result, closeTo(expectedDq));
		}

		@ParameterizedTest
		@MethodSource("data")
		void testSpeciesObject(String speciesId, float hlAll, float expectedDq) throws Exception {

			var em = EasyMock.createControl();
			VdypSpecies spec = em.createMock(VdypSpecies.class);
			EasyMock.expect(spec.getGenus()).andStubReturn(speciesId);
			EasyMock.expect(spec.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, hlAll));
			em.replay();

			float result = emp.estimateSmallComponentQuadMeanDiameter(spec);
			em.verify();

			assertThat(result, closeTo(expectedDq));
		}

	}

	@Nested
	class EstimateConditionalBasalAreaSmall {

		static Collection<Arguments> coast() {
			// FIXME These are wrong, see VDYP-1146
			return List.of(
					Arguments.of("B", 0.406989872f, 36.7552986f, Region.COASTAL, 5.99528221e-7f),
					Arguments.of("C", 5.0969491f, 22.9584007f, Region.COASTAL, 0.0274789277f)
			);
		}

		static Collection<Arguments> interior() {
			return List.of(
					Arguments.of("S", 3.0541997f, 6.95058966f, Region.INTERIOR, 0.758663595f),
					Arguments.of("S", 12.794775f, 9.05280018f, Region.INTERIOR, 1.74533093f),
					Arguments.of("PL", 56.6666641f, 11.4125996f, Region.INTERIOR, 4.46649837f)
			);
		}

		static Collection<Arguments> withFractionAvailable() {
			// For each of the tests done for the un-normalized version, do the normalized test with the given and
			// expected BAs normalized
			return Stream.concat(coast().stream(), interior().stream())
					.flatMap(
							args -> Stream.of(0.25f, 0.5f, 0.75f, 1f).map(
									frac -> Arguments.of(
											args.get()[0], //
											(float) args.get()[1] / frac, //
											args.get()[2], //
											args.get()[3], //
											frac, //
											(float) args.get()[4] / frac
									)
							)
					).toList();
		}

		@ParameterizedTest
		@MethodSource({ "coast", "interior" })
		void testSimple(String speciesId, float baAll, float hlAll, Region region, float expectedDq) throws Exception {

			float result = emp.estimateSmallComponentConditionalExpectedBasalArea(speciesId, baAll, hlAll, region);

			assertThat(result, closeTo(expectedDq));
		}

		@ParameterizedTest
		@MethodSource({ "coast", "interior" })
		void testSpeciesObject(String speciesId, float baAll, float hlAll, Region region, float expectedDq)
				throws Exception {

			var em = EasyMock.createControl();
			VdypSpecies species = em.createMock(VdypSpecies.class);
			EasyMock.expect(species.getGenus()).andStubReturn(speciesId);
			EasyMock.expect(species.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, hlAll));
			em.replay();

			float result = emp.estimateSmallComponentConditionalExpectedBasalArea(species, baAll, region);
			em.verify();

			assertThat(result, closeTo(expectedDq));
		}

		@ParameterizedTest
		@MethodSource({ "withFractionAvailable" })
		void testSpeciesObjectNormalized(
				String speciesId, float baAll, float hlAll, Region region, float fraction, float expectedDq
		) throws Exception {

			var em = EasyMock.createControl();
			VdypSpecies species = em.createMock(VdypSpecies.class);
			EasyMock.expect(species.getGenus()).andStubReturn(speciesId);
			EasyMock.expect(species.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, hlAll));
			EasyMock.expect(species.getBaseAreaByUtilization()).andStubReturn(Utils.heightVector(0f, baAll));
			em.replay();

			float result = emp.estimateSmallComponentConditionalExpectedBasalAreaNormalized(species, fraction, region);
			em.verify();

			assertThat(result, closeTo(expectedDq));
		}

	}

	@Nested
	class EstimateSmallProbability {

		static Collection<Arguments> data() {
			return List.of(
					Arguments.of("S", 80.7f, 10.2395f, Region.INTERIOR, 0.0730085522f),
					Arguments.of("S", 91.7f, 11.0868368f, Region.INTERIOR, 0.06185959f),
					Arguments.of("B", 242.7f, 33.4065018f, Region.COASTAL, 0.0155089973f)
			);
		}

		@ParameterizedTest
		@MethodSource({ "data" })
		void testSimple(String speciesId, float yearsAtbreastHeight, float hlAll, Region region, float expected) {

			float result = emp.estimateSmallComponentProbability(speciesId, yearsAtbreastHeight, hlAll, region);

			assertThat(result, closeTo(expected));
		}

		@ParameterizedTest
		@MethodSource({ "data" })
		void testSpeciesObject(
				String speciesId, float yearsAtbreastHeight, float hlAll, Region region, float expected
		) {

			var em = EasyMock.createControl();
			VdypSpecies species = em.createMock(VdypSpecies.class);
			VdypLayer layer = em.createMock(VdypLayer.class);
			EasyMock.expect(species.getGenus()).andStubReturn(speciesId);
			EasyMock.expect(species.getLoreyHeightByUtilization()).andStubReturn(Utils.heightVector(0f, hlAll));
			EasyMock.expect(layer.getComputedYearsAtBreastHeight()).andStubReturn(Optional.of(yearsAtbreastHeight));
			em.replay();

			float result = emp.estimateSmallComponentProbability(layer, species, region);
			em.verify();

			assertThat(result, closeTo(expected));
		}

	}
}
