package ca.bc.gov.nrs.vdyp.forward.controlmap;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.controlmap.CachingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;

public class ForwardResolvedControlMapImpl extends CachingResolvedControlMapImpl implements ForwardResolvedControlMap {

	private final Optional<ForwardDebugSettings> debugSettings;
	private final Optional<ForwardControlVariables> forwardControlVariables;
	private final Optional<MatrixMap2<String, Region, SiteIndexEquation>> siteCurveMap;
	private final Optional<CompVarAdjustments> compVarAdjustments;
	private final Optional<MatrixMap2<String, String, Coefficients>> basalAreaYieldCoefficients;
	private final Optional<Map<Region, GrowthFiatDetails>> basalAreaGrowthFiatDetails;
	private final Optional<MatrixMap2<String, String, Coefficients>> basalAreaGrowthEmpiricalCoefficients;
	private final Optional<MatrixMap3<Region, String, Integer, Float>> upperBoundCoefficients;
	private final Optional<MatrixMap2<String, String, Coefficients>> quadMeanDiameterYieldCoefficients;
	private final Optional<Map<Region, GrowthFiatDetails>> quadMeanDiameterGrowthFiatDetails;
	private final Optional<Map<Integer, Coefficients>> quadMeanDiameterGrowthEmpiricalCoefficients;
	private final Optional<Map<Integer, Coefficients>> quadMeanDiameterGrowthEmpiricalLimits;
	private final Optional<MatrixMap2<String, Region, Coefficients>> loreyHeightPrimarySpeciesEquationP1Coefficients;
	private final Optional<MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>> loreyHeightNonPrimaryCoefficients;
	private final Optional<Map<Integer, ModelCoefficients>> primarySpeciesBasalAreaGrowthCoefficients;
	private final Optional<MatrixMap2<String, Integer, Optional<Coefficients>>> nonPrimarySpeciesBasalAreaGrowthCoefficients;
	private final Optional<Map<Integer, ModelCoefficients>> primaryQuadMeanDiameterGrowthCoefficients;
	private final Optional<MatrixMap2<String, Integer, Optional<Coefficients>>> nonPrimaryQuadMeanDiameterGrowthCoefficients;

	public ForwardResolvedControlMapImpl(Map<String, Object> controlMap) {

		super(controlMap);

		this.debugSettings = get(ControlKey.DEBUG_SWITCHES, ForwardDebugSettings.class);
		this.forwardControlVariables = get(ControlKey.VTROL, ForwardControlVariables.class);
		this.siteCurveMap = get(ControlKey.SITE_CURVE_NUMBERS, MatrixMap2.class);
		this.compVarAdjustments = get(ControlKey.PARAM_ADJUSTMENTS, CompVarAdjustments.class);
		this.basalAreaYieldCoefficients = get(ControlKey.BA_YIELD, MatrixMap2.class);
		this.basalAreaGrowthFiatDetails = get(ControlKey.BA_GROWTH_FIAT, Map.class);
		this.basalAreaGrowthEmpiricalCoefficients = get(ControlKey.BA_GROWTH_EMPIRICAL, MatrixMap2.class);
		this.upperBoundCoefficients = get(ControlKey.UPPER_BA_BY_CI_S0_P, MatrixMap3.class);
		this.quadMeanDiameterYieldCoefficients = get(ControlKey.DQ_YIELD, MatrixMap2.class);
		this.quadMeanDiameterGrowthFiatDetails = get(ControlKey.DQ_GROWTH_FIAT, Map.class);
		this.quadMeanDiameterGrowthEmpiricalCoefficients = get(ControlKey.DQ_GROWTH_EMPIRICAL, Map.class);
		this.quadMeanDiameterGrowthEmpiricalLimits = get(ControlKey.DQ_GROWTH_EMPIRICAL_LIMITS, Map.class);
		this.loreyHeightPrimarySpeciesEquationP1Coefficients = this
				.get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
		this.loreyHeightNonPrimaryCoefficients = get(ControlKey.HL_NONPRIMARY, MatrixMap3.class);
		this.primarySpeciesBasalAreaGrowthCoefficients = get(ControlKey.PRIMARY_SP_BA_GROWTH, Map.class);
		this.nonPrimarySpeciesBasalAreaGrowthCoefficients = this
				.get(ControlKey.NON_PRIMARY_SP_BA_GROWTH, MatrixMap2.class);
		this.primaryQuadMeanDiameterGrowthCoefficients = get(ControlKey.PRIMARY_SP_DQ_GROWTH, Map.class);
		this.nonPrimaryQuadMeanDiameterGrowthCoefficients = this
				.get(ControlKey.NON_PRIMARY_SP_DQ_GROWTH, MatrixMap2.class);
	}

	@Override
	public ForwardDebugSettings getDebugSettings() {
		return debugSettings.orElseThrow(() -> new NoSuchElementException("debugSettings"));
	}

	@Override
	public ForwardControlVariables getForwardControlVariables() {
		return forwardControlVariables.orElseThrow(() -> new NoSuchElementException("forwardControlVariables"));
	}

	@Override
	public MatrixMap2<String, Region, SiteIndexEquation> getSiteCurveMap() {
		return siteCurveMap.orElseThrow(() -> new NoSuchElementException("siteCurveMap"));
	}

	@Override
	public CompVarAdjustments getCompVarAdjustments() {
		return compVarAdjustments.orElseThrow(() -> new NoSuchElementException("compVarAdjustments"));
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getBasalAreaYieldCoefficients() {
		return basalAreaYieldCoefficients.orElseThrow(() -> new NoSuchElementException("basalAreaYieldCoefficients"));
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getQuadMeanDiameterYieldCoefficients() {
		return quadMeanDiameterYieldCoefficients
				.orElseThrow(() -> new NoSuchElementException("quadMeanDiameterYieldCoefficients"));
	}

	@Override
	public Map<Region, GrowthFiatDetails> getBasalAreaGrowthFiatDetails() {
		return basalAreaGrowthFiatDetails.orElseThrow(() -> new NoSuchElementException("basalAreaGrowthFiatDetails"));
	}

	@Override
	public MatrixMap2<String, String, Coefficients> getBasalAreaGrowthEmpiricalCoefficients() {
		return basalAreaGrowthEmpiricalCoefficients
				.orElseThrow(() -> new NoSuchElementException("basalAreaGrowthEmpiricalCoefficients"));
	}

	@Override
	public MatrixMap3<Region, String, Integer, Float> getUpperBoundsCoefficients() {
		return upperBoundCoefficients.orElseThrow(() -> new NoSuchElementException("upperBoundCoefficients"));
	}

	@Override
	public Map<Region, GrowthFiatDetails> getQuadMeanDiameterGrowthFiatDetails() {
		return quadMeanDiameterGrowthFiatDetails
				.orElseThrow(() -> new NoSuchElementException("quadMeanDiameterGrowthFiatDetails"));
	}

	@Override
	public Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalCoefficients() {
		return quadMeanDiameterGrowthEmpiricalCoefficients
				.orElseThrow(() -> new NoSuchElementException("quadMeanDiameterGrowthEmpiricalCoefficients"));
	}

	@Override
	public Map<Integer, Coefficients> getQuadMeanDiameterGrowthEmpiricalLimits() {
		return quadMeanDiameterGrowthEmpiricalLimits
				.orElseThrow(() -> new NoSuchElementException("quadMeanDiameterGrowthEmpiricalLimits"));
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getLoreyHeightPrimarySpeciesEquationP1Coefficients() {
		return loreyHeightPrimarySpeciesEquationP1Coefficients
				.orElseThrow(() -> new NoSuchElementException("loreyHeightPrimarySpeciesEquationP1Coefficients"));
	}

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>
			getLoreyHeightNonPrimaryCoefficients() {
		return loreyHeightNonPrimaryCoefficients
				.orElseThrow(() -> new NoSuchElementException("loreyHeightNonPrimaryCoefficients"));
	}

	@Override
	public Map<Integer, ModelCoefficients> getPrimarySpeciesBasalAreaGrowthCoefficients() {
		return primarySpeciesBasalAreaGrowthCoefficients
				.orElseThrow(() -> new NoSuchElementException("primarySpeciesBasalAreaGrowthCoefficients"));
	}

	@Override
	public MatrixMap2<String, Integer, Optional<Coefficients>> getNonPrimarySpeciesBasalAreaGrowthCoefficients() {
		return nonPrimarySpeciesBasalAreaGrowthCoefficients
				.orElseThrow(() -> new NoSuchElementException("nonPrimarySpeciesBasalAreaGrowthCoefficients"));
	}

	@Override
	public Map<Integer, ModelCoefficients> getPrimarySpeciesQuadMeanDiameterGrowthCoefficients() {
		return primaryQuadMeanDiameterGrowthCoefficients
				.orElseThrow(() -> new NoSuchElementException("primaryQuadMeanDiameterGrowthCoefficients"));
	}

	@Override
	public MatrixMap2<String, Integer, Optional<Coefficients>>
			getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients() {
		return nonPrimaryQuadMeanDiameterGrowthCoefficients
				.orElseThrow(() -> new NoSuchElementException("nonPrimaryQuadMeanDiameterGrowthCoefficients"));
	}
}
