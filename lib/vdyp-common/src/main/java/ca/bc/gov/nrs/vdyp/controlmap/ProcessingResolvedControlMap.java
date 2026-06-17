package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;
import ca.bc.gov.nrs.vdyp.sindex.enumerations.SiteIndexEquation;

public interface ProcessingResolvedControlMap extends ResolvedControlMap {

	/** 25 - SITE_CURVE_NUMBERS */
	MatrixMap2<String, Region, SiteIndexEquation> getSiteCurveMap();

	/** 28 - PARAM_ADJUSTMENTS */
	CompVarAdjustments getCompVarAdjustments();

	/** 43 - UPPER_BA_BY_CI_S0_P */
	MatrixMap3<Region, String, Integer, Float> getUpperBoundsCoefficients();

	/** 50 - HL_PRIMARY_SP_EQN_P1 */
	MatrixMap2<String, Region, Coefficients> getLoreyHeightPrimarySpeciesEquationP1Coefficients();

	/** 53 - HL_NONPRIMARY */
	MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getLoreyHeightNonPrimaryCoefficients();

	/** 101 - VTROL */
	@Override
	ProcessingControlVariables getControlVariables();

	/** 106 - BA_YIELD */
	MatrixMap2<String, String, Coefficients> getBasalAreaYieldCoefficients();

	/** 111 - BA_GROWTH_FIAT */
	Map<Region, GrowthFiatDetails> getBasalAreaGrowthFiatDetails();

	/** 117 - DQ_GROWTH_FIAT */
	Map<Region, GrowthFiatDetails> getQuadMeanDiameterGrowthFiatDetails();

	/** 121 - BA_GROWTH_EMPIRICAL */
	MatrixMap2<String, String, Coefficients> getBasalAreaGrowthEmpiricalCoefficients();

	/** 122 - DQ_GROWTH_EMPIRICAL */
	Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalCoefficients();

	/** 123 - DQ_GROWTH_EMPIRICAL_LIMITS */
	Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalLimits();

	/** 148 - PRIMARY_SP_BA_GROWTH */
	Map<Integer, ModelCoefficients> getPrimarySpeciesBasalAreaGrowthCoefficients();

	/** 149 - NON_PRIMARY_SP_BA_GROWTH */
	MatrixMap2<String, Integer, Optional<Coefficients>> getNonPrimarySpeciesBasalAreaGrowthCoefficients();

	/** 150 - PRIMARY_SP_DQ_GROWTH */
	Map<Integer, ModelCoefficients> getPrimarySpeciesQuadMeanDiameterGrowthCoefficients();

	/** 151 - NON_PRIMARY_SP_DQ_GROWTH */
	MatrixMap2<String, Integer, Optional<Coefficients>> getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients();

	/** 199 - DEBUG_SWITCHES */
	@Override
	ProcessingDebugSettings getDebugSettings();
}
