package ca.bc.gov.nrs.vdyp.common.controlmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.controlmap.CachingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ResolvedControlMapTest {

	private static final Logger logger = LoggerFactory.getLogger(ResolvedControlMapTest.class);

	@Test
	void testCachingResolvedControlMap() throws IOException, ResourceParseException {
		logger.info(this.getClass().getName() + ":testCachingResolvedControlMap running...");

		var rawControlMap = TestUtils.loadControlMap(Path.of("VDYP.CTR"));

		var cachingControlMap = new CachingResolvedControlMapImpl(rawControlMap);

		assertThat(cachingControlMap.getControlMap(), is(rawControlMap));

		Object e;
		e = cachingControlMap.getBecLookup();
		assertThat(e, instanceOf(BecLookup.class));
		e = cachingControlMap.getGenusDefinitionMap();
		assertThat(e, instanceOf(GenusDefinitionMap.class));
		e = cachingControlMap.getNetDecayWasteCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getNetDecayCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getWasteModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getDecayModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getCloseUtilizationCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getTotalStandWholeStepVolumeCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getWholeStemUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getQuadMeanDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = cachingControlMap.getBasalAreaDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = cachingControlMap.getSmallComponentWholeStemVolumeCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getSmallComponentLoreyHeightCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getSmallComponentQuadMeanDiameterCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getSmallComponentBasalAreaCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getSmallComponentProbabilityCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getMaximumAgeBySiteCurveNumber();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getUpperBounds();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getDefaultEquationGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getEquationModifierGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getHl1Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getHl2Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getHl3Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getHlNonPrimaryCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = cachingControlMap.getComponentSizeLimits();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getNetBreakageMap();
		assertThat(e, instanceOf(Map.class));
		e = cachingControlMap.getVolumeEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getDecayEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getBreakageEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = cachingControlMap.getQuadMeanDiameterBySpeciesCoefficients();
		assertThat(e, instanceOf(Map.class));
	}
}
