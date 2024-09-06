package ca.bc.gov.nrs.vdyp.forward.controlmap;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.controlmap.CachingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;

public class ForwardResolvedControlMapImpl extends CachingResolvedControlMapImpl implements ForwardResolvedControlMap {

	private final ForwardDebugSettings debugSettings;
	private final ForwardControlVariables forwardControlVariables;
	private final MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap;
	private final CompVarAdjustments compVarAdjustments;
	private final MatrixMap2<String, String, Coefficients> basalAreaYieldCoefficients;
	private final Map<Region, GrowthFiatDetails> basalAreaGrowthFiatDetails;
	private final MatrixMap2<String, String, Coefficients> basalAreaGrowthEmpiricalCoefficients;
	private final MatrixMap3<Region, String, Integer, Float> upperBoundCoefficients;
	private final MatrixMap2<String, String, Coefficients> quadMeanDiameterYieldCoefficients;
	private final Map<Region, GrowthFiatDetails> quadMeanDiameterGrowthFiatDetails;
	private final Map<Integer, Coefficients> quadMeanDiameterGrowthEmpiricalCoefficients;
	private final Map<Integer, Coefficients> quadMeanDiameterGrowthEmpiricalLimits;
	private final MatrixMap2<String, Region, Coefficients> loreyHeightPrimarySpeciesEquationP1Coefficients;
	private final MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> loreyHeightNonPrimaryCoefficients;
	private final Map<Integer, ModelCoefficients> primarySpeciesBasalAreaGrowthCoefficients;
	private final MatrixMap2<String, Integer, Optional<Coefficients>> nonPrimarySpeciesBasalAreaGrowthCoefficients;
	private final Map<Integer, ModelCoefficients> primaryQuadMeanDiameterGrowthCoefficients;
	private final MatrixMap2<String, Integer, Optional<Coefficients>> nonPrimaryQuadMeanDiameterGrowthCoefficients;

	public ForwardResolvedControlMapImpl(Map<String, Object> controlMap) {

		super(controlMap);

		this.debugSettings = new ForwardDebugSettings(get(ControlKey.DEBUG_SWITCHES, DebugSettings.class));
		this.forwardControlVariables = get(ControlKey.VTROL, ForwardControlVariables.class);
		this.siteCurveMap = Utils.expectParsedControl(controlMap, ControlKey.SITE_CURVE_NUMBERS, MatrixMap2.class);
		this.compVarAdjustments = this.get(ControlKey.PARAM_ADJUSTMENTS, CompVarAdjustments.class);
		this.basalAreaYieldCoefficients = this.get(ControlKey.BA_YIELD, MatrixMap2.class);
		this.basalAreaGrowthFiatDetails = this.get(ControlKey.BA_GROWTH_FIAT, Map.class);
		this.basalAreaGrowthEmpiricalCoefficients = this.get(ControlKey.BA_GROWTH_EMPIRICAL, MatrixMap2.class);
		this.upperBoundCoefficients = this.get(ControlKey.UPPER_BA_BY_CI_S0_P, MatrixMap3.class);
		this.quadMeanDiameterYieldCoefficients = this.get(ControlKey.DQ_YIELD, MatrixMap2.class);
		this.quadMeanDiameterGrowthFiatDetails = this.get(ControlKey.DQ_GROWTH_FIAT, Map.class);
		this.quadMeanDiameterGrowthEmpiricalCoefficients = this.get(ControlKey.DQ_GROWTH_EMPIRICAL, Map.class);
		this.quadMeanDiameterGrowthEmpiricalLimits = this.get(ControlKey.DQ_GROWTH_EMPIRICAL_LIMITS, Map.class);
		this.loreyHeightPrimarySpeciesEquationP1Coefficients = this
				.get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
		this.loreyHeightNonPrimaryCoefficients = this.get(ControlKey.HL_NONPRIMARY, MatrixMap3.class);
		this.primarySpeciesBasalAreaGrowthCoefficients = this.get(ControlKey.PRIMARY_SP_BA_GROWTH, Map.class);
		this.nonPrimarySpeciesBasalAreaGrowthCoefficients = this
				.get(ControlKey.NON_PRIMARY_SP_BA_GROWTH, MatrixMap2.class);
		this.primaryQuadMeanDiameterGrowthCoefficients = this.get(ControlKey.PRIMARY_SP_DQ_GROWTH, Map.class);
		this.nonPrimaryQuadMeanDiameterGrowthCoefficients = this
				.get(ControlKey.NON_PRIMARY_SP_DQ_GROWTH, MatrixMap2.class);
	}

	@Override
	public ForwardDebugSettings getDebugSettings() {
		return debugSettings;
	}

	@Override
	public ForwardControlVariables getForwardControlVariables() {
		return forwardControlVariables;
	}

	@Override
	public MatrixMap2<String, Region, SiteIndexEquation> getSiteCurveMap() {
		return siteCurveMap;
	}

	@Override
	public CompVarAdjustments getCompVarAdjustments() {
		return compVarAdjustments;
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getBasalAreaYieldCoefficients() {
		return basalAreaYieldCoefficients;
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getQuadMeanDiameterYieldCoefficients() {
		return quadMeanDiameterYieldCoefficients;
	}

	@Override
	public Map<Region, GrowthFiatDetails> getBasalAreaGrowthFiatDetails() {
		return basalAreaGrowthFiatDetails;
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getBasalAreaGrowthEmpiricalCoefficients() {
		return basalAreaGrowthEmpiricalCoefficients;
	}

	@Override
	public MatrixMap3<Region, String, Integer, Float> getUpperBoundsCoefficients() {
		return upperBoundCoefficients;
	}

	@Override
	public Map<Region, GrowthFiatDetails> getQuadMeanDiameterGrowthFiatDetails() {
		return quadMeanDiameterGrowthFiatDetails;
	}

	@Override
	public Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalCoefficients() {
		return quadMeanDiameterGrowthEmpiricalCoefficients;
	}

	@Override
	public Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalLimits() {
		return quadMeanDiameterGrowthEmpiricalLimits;
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getLoreyHeightPrimarySpeciesEquationP1Coefficients() {
		return loreyHeightPrimarySpeciesEquationP1Coefficients;
	}

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>
			getLoreyHeightNonPrimaryCoefficients() {
		return loreyHeightNonPrimaryCoefficients;
	}

	@Override
	public Map<Integer, ModelCoefficients> getPrimarySpeciesBasalAreaGrowthCoefficients() {
		return primarySpeciesBasalAreaGrowthCoefficients;
	}

	@Override
	public MatrixMap2<String, Integer, Optional<Coefficients>> getNonPrimarySpeciesBasalAreaGrowthCoefficients() {
		return nonPrimarySpeciesBasalAreaGrowthCoefficients;
	}

	@Override
	public Map<Integer, ModelCoefficients> getPrimarySpeciesQuadMeanDiameterGrowthCoefficients() {
		return primaryQuadMeanDiameterGrowthCoefficients;
	}

	@Override
	public MatrixMap2<String, Integer, Optional<Coefficients>>
			getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients() {
		return nonPrimaryQuadMeanDiameterGrowthCoefficients;
	}
}
