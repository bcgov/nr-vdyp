package ca.bc.gov.nrs.vdyp.coomon.controlmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.TestProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ProcessingResolvedControlMapTest {
	ProcessingControlParser parser;

	@BeforeEach
	void setup() {
		parser = new TestProcessingControlParser();
	}

	static List<Arguments> properties() {
		return List.of(
				Arguments.of("debugSettings", ProcessingDebugSettings.class),
				Arguments.of("controlVariables", ProcessingControlVariables.class),
				Arguments.of("siteCurveMap", MatrixMap2.class),
				Arguments.of("compVarAdjustments", CompVarAdjustments.class),
				Arguments.of("basalAreaYieldCoefficients", MatrixMap2.class),
				Arguments.of("quadMeanDiameterYieldCoefficients", MatrixMap2.class),
				Arguments.of("upperBoundsCoefficients", MatrixMap3.class),
				Arguments.of("loreyHeightPrimarySpeciesEquationP1Coefficients", MatrixMap2.class),
				Arguments.of("loreyHeightNonPrimaryCoefficients", MatrixMap3.class),
				Arguments.of("becLookup", BecLookup.class),
				Arguments.of("genusDefinitionMap", GenusDefinitionMap.class),
				Arguments.of("netDecayWasteCoeMap", Map.class), Arguments.of("netDecayCoeMap", MatrixMap2.class),
				Arguments.of("closeUtilizationCoeMap", MatrixMap2.class),
				Arguments.of("totalStandWholeStepVolumeCoeMap", Map.class),
				Arguments.of("wholeStemUtilizationComponentMap", MatrixMap2.class),
				Arguments.of("quadMeanDiameterUtilizationComponentMap", MatrixMap3.class),
				Arguments.of("basalAreaDiameterUtilizationComponentMap", MatrixMap3.class),
				Arguments.of("smallComponentWholeStemVolumeCoefficients", Map.class),
				Arguments.of("smallComponentLoreyHeightCoefficients", Map.class),
				Arguments.of("smallComponentQuadMeanDiameterCoefficients", Map.class),
				Arguments.of("smallComponentBasalAreaCoefficients", Map.class),
				Arguments.of("smallComponentProbabilityCoefficients", Map.class),
				Arguments.of("maximumAgeBySiteCurveNumber", Map.class), Arguments.of("upperBounds", Map.class),
				Arguments.of("defaultEquationGroup", MatrixMap2.class),
				Arguments.of("equationModifierGroup", MatrixMap2.class),
				Arguments.of("hl1Coefficients", MatrixMap2.class), Arguments.of("hl2Coefficients", MatrixMap2.class),
				Arguments.of("hl3Coefficients", MatrixMap2.class),
				Arguments.of("hlNonPrimaryCoefficients", MatrixMap3.class),
				Arguments.of("componentSizeLimits", MatrixMap2.class), Arguments.of("netBreakageMap", Map.class),
				Arguments.of("volumeEquationGroups", MatrixMap2.class),
				Arguments.of("decayEquationGroups", MatrixMap2.class),
				Arguments.of("breakageEquationGroups", MatrixMap2.class),
				Arguments.of("quadMeanDiameterBySpeciesCoefficients", Map.class)
		);
	}

	@Test
	void testGetControlMap() {
		var rawControlMap = TestUtils.loadControlMap(parser, Path.of("VDYPBACK.CTR"));
		var resolvedControlMap = new ProcessingResolvedControlMapImpl(rawControlMap);

		assertThat(resolvedControlMap.getControlMap(), is(rawControlMap));
	}

	@ParameterizedTest
	@MethodSource("properties")
	void testHasProperty(String property, Class<?> klazz) throws IOException, ResourceParseException {

		var rawControlMap = TestUtils.loadControlMap(parser, Path.of("VDYPBACK.CTR"));
		var resolvedControlMap = new ProcessingResolvedControlMapImpl(rawControlMap);

		assertThat(resolvedControlMap, hasProperty(property, instanceOf(klazz)));
	}
}
