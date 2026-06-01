package ca.bc.gov.nrs.vdyp.coomon.controlmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ProcessingResolvedControlMapTest {

	private static final Logger logger = LoggerFactory.getLogger(ProcessingResolvedControlMapTest.class);

	@Test
	void testResolvedControlMap() throws IOException, ResourceParseException {
		logger.info(this.getClass().getName() + ":testForwardResolvedControlMap running...");

		var parser = new ProcessingControlParser();
		var rawControlMap = TestUtils.loadControlMap(parser, Path.of("VDYP.CTR"));
		var resolvedControlMap = new ProcessingResolvedControlMapImpl(rawControlMap);

		assertThat(resolvedControlMap.getControlMap(), is(rawControlMap));

		Object e;
		e = resolvedControlMap.getDebugSettings();
		assertThat(e, instanceOf(ProcessingDebugSettings.class));
		e = resolvedControlMap.getControlVariables();
		assertThat(e, instanceOf(ProcessingControlVariables.class));
		e = resolvedControlMap.getSiteCurveMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getCompVarAdjustments();
		assertThat(e, instanceOf(CompVarAdjustments.class));
		e = resolvedControlMap.getBasalAreaYieldCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getQuadMeanDiameterYieldCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getBasalAreaGrowthFiatDetails();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getBasalAreaGrowthEmpiricalCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getUpperBoundsCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = resolvedControlMap.getQuadMeanDiameterGrowthFiatDetails();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getQuadMeanDiameterGrowthEmpiricalCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getQuadMeanDiameterGrowthEmpiricalLimits();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getLoreyHeightPrimarySpeciesEquationP1Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getLoreyHeightNonPrimaryCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = resolvedControlMap.getPrimarySpeciesBasalAreaGrowthCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getNonPrimarySpeciesBasalAreaGrowthCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getPrimarySpeciesQuadMeanDiameterGrowthCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getBecLookup();
		assertThat(e, instanceOf(BecLookup.class));
		e = resolvedControlMap.getGenusDefinitionMap();
		assertThat(e, instanceOf(GenusDefinitionMap.class));
		e = resolvedControlMap.getNetDecayWasteCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getNetDecayCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getWasteModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getDecayModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getCloseUtilizationCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getTotalStandWholeStepVolumeCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getWholeStemUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getQuadMeanDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = resolvedControlMap.getBasalAreaDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = resolvedControlMap.getSmallComponentWholeStemVolumeCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getSmallComponentLoreyHeightCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getSmallComponentQuadMeanDiameterCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getSmallComponentBasalAreaCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getSmallComponentProbabilityCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getMaximumAgeBySiteCurveNumber();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getUpperBounds();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getDefaultEquationGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getEquationModifierGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getHl1Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getHl2Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getHl3Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getHlNonPrimaryCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = resolvedControlMap.getComponentSizeLimits();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getNetBreakageMap();
		assertThat(e, instanceOf(Map.class));
		e = resolvedControlMap.getVolumeEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getDecayEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getBreakageEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = resolvedControlMap.getQuadMeanDiameterBySpeciesCoefficients();
		assertThat(e, instanceOf(Map.class));
	}
}
