package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.exp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.floor;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.log;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.pow;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.ratio;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.BaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.BreastHeightAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperBoundsParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperCoefficientParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.DoubleCoefficients;
import ca.bc.gov.nrs.vdyp.model.InputLayer;
import ca.bc.gov.nrs.vdyp.model.InputPolygon;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector.BinaryOperatorWithClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class EstimationMethods {

	public static final Logger log = LoggerFactory.getLogger(EstimationMethods.class);

	public static final float EMPIRICAL_OCCUPANCY = 0.85f;

	private ResolvedControlMap controlMap;

	public static final float LOW_CROWN_CLOSURE = 10f;

	public static final float MINIMUM_BASAL_AREA = 0.05f;

	public static final ValueOrMarker.Builder<Float, Boolean> FLOAT_OR_BOOL = ValueOrMarker
			.builder(Float.class, Boolean.class);

	public EstimationMethods(ResolvedControlMap controlMap) {
		this.controlMap = controlMap;
	}

	/**
	 * Returns the new value if the index is that of a utilization class that represents a size band, otherwise the old
	 * value
	 */
	public static final BinaryOperatorWithClass COPY_IF_BAND = (
			oldX, newX, uc
	) -> UtilizationClass.UTIL_CLASSES.contains(uc) ? newX : oldX;

	/**
	 * Returns the new value if the index is that of a utilization class is not the small band, otherwise the old value
	 */
	public static final BinaryOperatorWithClass COPY_IF_NOT_SMALL = (
			oldX, newX, uc
	) -> UtilizationClass.ALL_BUT_SMALL.contains(uc) ? newX : oldX;

	private float heightMultiplier(String genus, Region region, float treesPerHectarePrimary) {
		final var coeMap = controlMap.getHl1Coefficients();
		var coe = coeMap.get(genus, region).reindex(0);
		return coe.get(0) - coe.getCoe(1) + coe.getCoe(1) * exp(coe.getCoe(2) * (treesPerHectarePrimary - 100f));
	}

	/**
	 * EMP050 Method 1: Return the lorey height of the primary species based on the dominant height of the lead species.
	 *
	 * @param leadHeight             dominant height of the lead species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary species
	 * @return as described
	 */
	public float estimatePrimaryHeightFromLeadHeight(
			float leadHeight, String genus, Region region, float treesPerHectarePrimary
	) {
		return 1.3f + (leadHeight - 1.3f) * heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	/**
	 * EMP050 Method 2: Return the dominant height of the lead species based on the lorey height of the primary species.
	 *
	 * @param primaryHeight          lorey height of the primary species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary species
	 */
	public float estimateLeadHeightFromPrimaryHeight(
			float primaryHeight, String genus, Region region, float treesPerHectarePrimary
	) {
		return 1.3f + (primaryHeight - 1.3f) / heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	/**
	 * EMP051. Return the lorey height of the primary species based on the dominant height of the lead species.
	 *
	 * @param leadHeight dominant height of the lead species
	 * @param genus      Primary species
	 * @param region     Region of the polygon
	 */
	public float primaryHeightFromLeadHeightInitial(float leadHeight, String genus, Region region) {
		final var coeMap = controlMap.getHl2Coefficients();
		var coe = coeMap.get(genus, region);
		return 1.3f + coe.getCoe(1) * pow(leadHeight - 1.3f, coe.getCoe(2));
	}

	/**
	 * EMP053. Estimate the lorey height of a non-primary species of a primary layer.
	 * <p>
	 * Using eqns N1 and N2 from ipsjf124.doc
	 *
	 * @param vspec         The species.
	 * @param vspecPrime    The primary species.
	 * @param bec           The BEC zone containing the species.
	 * @param leadHeight    lead height of the layer
	 * @param primaryHeight height of the primary species
	 * @throws ProcessingException
	 */
	public float estimateNonPrimaryLoreyHeight(
			BaseVdypSpecies<?> vspec, BaseVdypSpecies<?> vspecPrime, BecDefinition bec, float leadHeight,
			float primaryHeight
	) {
		return estimateNonPrimaryLoreyHeight(vspec.getGenus(), vspecPrime.getGenus(), bec, leadHeight, primaryHeight);
	}

	/**
	 * EMP053. Estimate the lorey height of a non-primary species of a primary layer.
	 * <p>
	 * Using eqns N1 and N2 from ipsjf124.doc
	 *
	 * @param vspec         The species.
	 * @param vspecPrime    The primary species.
	 * @param bec           The BEC zone containing the species.
	 * @param leadHeight    lead height of the layer
	 * @param primaryHeight height of the primary species
	 */
	public float estimateNonPrimaryLoreyHeight(
			String vspec, String vspecPrime, BecDefinition bec, float leadHeight, float primaryHeight
	) {
		var coeMap = controlMap.getHlNonPrimaryCoefficients();

		var coe = coeMap.get(vspec, vspecPrime, bec.getRegion()).orElseGet(() -> NonprimaryHLCoefficients.getDefault());
		final int equationIndex = coe.getEquationIndex();
		var heightToUse = switch (equationIndex) {
		case 1 -> leadHeight;
		case 2 -> primaryHeight;
		default -> throw new IllegalStateException(
				MessageFormat
						.format("Expecting non-primay Lorey height equation index 1 or 2 but was {0}", equationIndex)
		);
		};
		return 1.3f + coe.getCoe(1) * pow(heightToUse - 1.3f, coe.getCoe(2));
	}

	/**
	 * EMP060. Estimate DQ for a species (primary or not). Using eqn in jf125.doc.
	 *
	 * Enforces mins and maxes from EMP061.
	 *
	 * @param spec                  Species of insterest
	 * @param allSpecies            Collection of all species on the layer
	 * @param region                BEC Region of the stand
	 * @param standQuadMeanDiameter Quadratic mean diameter of the stand
	 * @param standBaseArea         Base area of the stand
	 * @param standTreesPerHectare  Density opf the stand
	 * @param standLoreyHeight      Lorey height of the stand
	 * @return quadratic mean diameter of the species of interest
	 * @throws FatalProcessingException
	 */
	public float estimateQuadMeanDiameterForSpecies(
			VdypSpecies spec, // ISP, HLsp, DQsp
			Map<String, VdypSpecies> allSpecies, // FR
			Region region, // INDEX_IC
			float standQuadMeanDiameter, // DQ_TOT
			float standBaseArea, // BA_TOT
			float standTreesPerHectare, // TPH_TOT
			float standLoreyHeight // HL_TOT
	) throws FatalProcessingException {
		Map<String, Float> basalAreaFractionPerSpecies = new HashMap<>();
		allSpecies.values().stream().forEach(s -> basalAreaFractionPerSpecies.put(s.getGenus(), s.getFractionGenus()));

		return estimateQuadMeanDiameterForSpecies(
				spec.getGenus(), spec.getLoreyHeightByUtilization().get(UtilizationClass.ALL),
				spec.getQuadraticMeanDiameterByUtilization().get(UtilizationClass.ALL), basalAreaFractionPerSpecies,
				region, standQuadMeanDiameter, standBaseArea, standTreesPerHectare, standLoreyHeight
		);
	}

	/**
	 * EMP060. Estimate DQ for a species (primary or not). Using equations from jf125.doc.
	 *
	 * Enforces mins and maxes from EMP061.
	 *
	 * @param spAlias                     The alias of the species
	 * @param spLoreyHeight               The lorey height (all utilizations) of the species
	 * @param spQuadMeanDiameter          The quad-mean-diameter (all utilizations) of the species
	 * @param basalAreaFractionPerSpecies Basal area fractions of the per species in the stand
	 * @param region                      BEC Region of the stand
	 * @param standQuadMeanDiameter       Quadratic mean diameter of the stand
	 * @param standBaseArea               Base area of the stand
	 * @param standTreesPerHectare        Density opf the stand
	 * @param standLoreyHeight            Lorey height of the stand
	 * @return quadratic mean diameter of the species of interest
	 * @throws ProcessingException
	 */
	public float estimateQuadMeanDiameterForSpecies(
			String spAlias, float spLoreyHeight, // HLsp
			float spQuadMeanDiameter, // DQsp
			Map<String, Float> basalAreaFractionPerSpecies, // FR
			Region region, // INDEX_IC
			float standQuadMeanDiameter, // DQ_TOT
			float standBaseArea, // BA_TOT
			float standTreesPerHectare, // TPH_TOT
			float standLoreyHeight // HL_TOT
	) throws FatalProcessingException {

		double c = 0.00441786467;

		float minQuadMeanDiameter = min(7.6f, standQuadMeanDiameter);

		Float spFraction = basalAreaFractionPerSpecies.get(spAlias);

		// Quick solution
		if (spFraction >= 1f || standQuadMeanDiameter < minQuadMeanDiameter) {
			return standQuadMeanDiameter;
		}

		Map<String, DoubleCoefficients> coeMap = controlMap.getQuadMeanDiameterBySpeciesCoefficients();
		var specAliases = controlMap.getGenusDefinitionMap().getAllGeneraAliases();

		var specIt = specAliases.iterator();
		var spec1 = specIt.next();

		DoubleCoefficients cowMapResult = coeMap.get(spec1);

		double a2 = cowMapResult.getCoe(2);

		double fractionOther = 1f - spFraction; // FR_REST

		double a0 = cowMapResult.getCoe(0);
		double a1 = cowMapResult.getCoe(1);

		while (specIt.hasNext()) {
			var specIAlias = specIt.next();
			if (spAlias.equals(specIAlias)) {
				double multI = 1f;
				a0 += multI * coeMap.get(specIAlias).getCoe(0);
				a1 += multI * coeMap.get(specIAlias).getCoe(1);
			} else {
				float spIFraction = basalAreaFractionPerSpecies.getOrDefault(specIAlias, 0.0f);
				if (spIFraction > 0f) {
					double multI = -spIFraction / fractionOther;
					a0 += multI * coeMap.get(specIAlias).getCoe(0);
					a1 -= multI * coeMap.get(specIAlias).getCoe(1);
				}
			}
		}

		double loreyHeightSpec = spLoreyHeight;
		double loreyHeight1 = max(4f, loreyHeightSpec);
		double loreyHeight2 = (float) ( (standLoreyHeight - loreyHeightSpec * spFraction) / fractionOther);
		loreyHeight2 = max(4f, loreyHeight2);
		double loreyHeightRatio = Math.min(Math.max( (loreyHeight1 - 3f) / (loreyHeight2 - 3f), 0.05f), 20f);

		double r = Math.exp( (a0 + a1 * Math.log(loreyHeightRatio) + a2 * Math.log(standQuadMeanDiameter)));

		double baseArea1 = spFraction * standBaseArea;
		double baseArea2 = standBaseArea - baseArea1;

		double treesPerHectare1;
		if (Math.abs(r - 1f) < 0.0005) {
			treesPerHectare1 = spFraction * standTreesPerHectare;
		} else {
			double aa = (r - 1f) * c;
			double bb = c * (1f - r) * standTreesPerHectare + baseArea1 + baseArea2 * r;
			double cc = -baseArea1 * standTreesPerHectare;
			double term = bb * bb - 4 * aa * cc;
			if (term <= 0f) {
				throw new FatalProcessingException(
						"Term for trees per hectare calculation when estimating quadratic mean diameter for species "
								+ spAlias + " was " + term + " but should be positive."
				);
			}
			treesPerHectare1 = ( (-bb + Math.sqrt(term)) / (2 * aa));
			if (treesPerHectare1 <= 0f || treesPerHectare1 > standTreesPerHectare) {
				throw new FatalProcessingException(
						"Trees per hectare 1 for species " + spAlias + " was " + treesPerHectare1
								+ " but should be positive and less than or equal to stand trees per hectare "
								+ standTreesPerHectare
				);
			}
		}

		double quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
		double treesPerHectare2 = (standTreesPerHectare - treesPerHectare1);
		double quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);
		var limits = getLimitsForHeightAndDiameter(spAlias, region);

		quadMeanDiameter1 = estimateQuadMeanDiameterClampResult(
				limits, standTreesPerHectare, minQuadMeanDiameter, (float) loreyHeightSpec, (float) baseArea1,
				(float) baseArea2, (float) quadMeanDiameter1, (float) treesPerHectare2, (float) quadMeanDiameter2
		);
		return (float) quadMeanDiameter1;
	}

	float estimateQuadMeanDiameterClampResult(
			ComponentSizeLimits limits, float standTreesPerHectare, float minQuadMeanDiameter, float loreyHeightSpec,
			float baseArea1, float baseArea2, float quadMeanDiameter1, float treesPerHectare2, float quadMeanDiameter2
	) {
		float treesPerHectare1;
		float localTreesPerHectare2 = treesPerHectare2;
		if (quadMeanDiameter2 < minQuadMeanDiameter) {
			// species 2 is too small. Make target species smaller.
			quadMeanDiameter2 = minQuadMeanDiameter;
			localTreesPerHectare2 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea2, quadMeanDiameter2);
			treesPerHectare1 = standTreesPerHectare - localTreesPerHectare2;
			quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
		}

		final float dqMinSp = max(minQuadMeanDiameter, limits.minQuadMeanDiameterLoreyHeightRatio() * loreyHeightSpec);
		final float dqMaxSp = max(
				7.6f,
				min(limits.quadMeanDiameterMaximum(), limits.maxQuadMeanDiameterLoreyHeightRatio() * loreyHeightSpec)
		);
		if (quadMeanDiameter1 < dqMinSp) {
			quadMeanDiameter1 = dqMinSp;
			treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			localTreesPerHectare2 = standTreesPerHectare - treesPerHectare1;
			quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, localTreesPerHectare2);
		}
		if (quadMeanDiameter1 > dqMaxSp) {
			// target species is too big. Make target species smaller, DQ2 bigger.

			quadMeanDiameter1 = dqMaxSp;
			treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			localTreesPerHectare2 = standTreesPerHectare - treesPerHectare1;

			if (localTreesPerHectare2 > 0f && baseArea2 > 0f) {
				quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, localTreesPerHectare2);
			} else {
				quadMeanDiameter2 = 1000f;
			}

			// under rare circumstances, let DQ1 exceed DQMAXsp
			if (quadMeanDiameter2 < minQuadMeanDiameter) {
				quadMeanDiameter2 = minQuadMeanDiameter;
				localTreesPerHectare2 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea2, quadMeanDiameter2);
				treesPerHectare1 = standTreesPerHectare - localTreesPerHectare2;
				quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
			}

		}
		return quadMeanDiameter1;
	}

	/**
	 * EMP061. Return a <code>ComponentSizeLimits</code> instance for the given sp0 and region.
	 *
	 * @param sp0    the SP0 species
	 * @param region the region
	 * @return as described
	 */
	public ComponentSizeLimits getLimitsForHeightAndDiameter(String sp0, Region region) {
		return controlMap.getComponentSizeLimits().get(sp0, region);
	}

	/**
	 * EMP070. Estimate basal area by utilization class from the given parameters, after getting the estimation
	 * coefficients map from the control map.
	 *
	 * @param bec
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public void estimateBaseAreaByUtilization(
			BecDefinition bec, UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil, String genus
	) throws ProcessingException {

		var basalAreaUtilCompCoeMap = controlMap.getBasalAreaDiameterUtilizationComponentMap();

		float dq = quadMeanDiameterUtil.getAll();
		var b = Utils.utilizationVector();
		b.setCoe(0, baseAreaUtil.getAll());

		for (UtilizationClass uc : UtilizationClass.ALL_BANDS_BUT_LARGEST) {
			var coe = basalAreaUtilCompCoeMap.get(uc.index, genus, bec.getGrowthBec().getAlias());

			float a0 = coe.getCoe(1);
			float a1 = coe.getCoe(2);

			float logit;
			if (uc == UtilizationClass.U75TO125) {
				logit = a0 + a1 * pow(dq, 0.25f);
			} else {
				logit = a0 + a1 * dq;
			}
			b.set(uc, b.get(uc.previous().get()) * exponentRatio(logit));
			if (uc == UtilizationClass.U75TO125
					&& quadMeanDiameterUtil.getAll() < UtilizationClass.U125TO175.lowBound) {
				float ba12Max = (1f
						- pow( (quadMeanDiameterUtil.getCoe(1) - 7.4f) / (quadMeanDiameterUtil.getAll() - 7.4f), 2f))
						* b.getCoe(0);
				b.scalarInPlace(1, x -> min(x, ba12Max));
			}
		}

		baseAreaUtil.setCoe(1, baseAreaUtil.getAll() - b.getCoe(1));
		baseAreaUtil.setCoe(2, b.getCoe(1) - b.getCoe(2));
		baseAreaUtil.setCoe(3, b.getCoe(2) - b.getCoe(3));
		baseAreaUtil.setCoe(4, b.getCoe(3));
	}

	/**
	 * EMP071. Estimate DQ by utilization class. See ipsjf120.doc.
	 *
	 * @param bec
	 * @param quadMeanDiameterUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public void estimateQuadMeanDiameterByUtilization(
			BecDefinition bec, UtilizationVector quadMeanDiameterUtil, String genus
	) throws ProcessingException {
		log.atTrace().setMessage("Estimate DQ by utilization class for {} in BEC {}.  DQ for all >{} is {}")
				.addArgument(genus).addArgument(bec.getName()).addArgument(UtilizationClass.U75TO125.lowBound)
				.addArgument(quadMeanDiameterUtil.getAll());

		var coeMap = controlMap.getQuadMeanDiameterUtilizationComponentMap();

		float quadMeanDiameter07 = quadMeanDiameterUtil.getAll();

		for (var uc : UtilizationClass.UTIL_CLASSES) {
			log.atDebug().setMessage("For util level {}").addArgument(uc.className);
			var coe = coeMap.get(uc.index, genus, bec.getGrowthBec().getAlias());

			float a0 = coe.getCoe(1);
			float a1 = coe.getCoe(2);
			float a2 = coe.getCoe(3);

			log.atDebug().setMessage("a0={}, a1={}, a3={}").addArgument(a0).addArgument(a1).addArgument(a2);

			float logit;

			switch (uc) {
			case U75TO125:
				if (quadMeanDiameter07 < UtilizationClass.U75TO125.lowBound + 0.0001f) {
					quadMeanDiameterUtil.setAll(UtilizationClass.U75TO125.lowBound);
				} else {
					log.atDebug().setMessage("DQ = {} + a0 * (1 - exp(a1 / a0*(DQ07 - {}) ))**a2' )")
							.addArgument(UtilizationClass.U75TO125.lowBound)
							.addArgument(UtilizationClass.U75TO125.lowBound);

					logit = a1 / a0 * (quadMeanDiameter07 - UtilizationClass.U75TO125.lowBound);

					quadMeanDiameterUtil.setCoe(
							uc.index,
							min(
									UtilizationClass.U75TO125.lowBound + a0 * pow(1 - safeExponent(logit), a2),
									quadMeanDiameter07
							)
					);
				}
				break;
			case U125TO175, U175TO225:
				log.atDebug().setMessage(
						"LOGIT = a0 + a1*(SQ07 / 7.5)**a2,  DQ = (12.5 or 17.5) + 5 * exp(LOGIT) / (1 + exp(LOGIT))"
				);
				logit = a0 + a1 * pow(quadMeanDiameter07 / UtilizationClass.U75TO125.lowBound, a2);

				quadMeanDiameterUtil.setCoe(uc.index, uc.lowBound + 5f * exponentRatio(logit));
				break;
			case OVER225:
				float a3 = coe.getCoe(4);

				log.atDebug().setMessage(
						"Coeff A3 {}, LOGIT = a2 + a1*DQ07**a3,  DQ = DQ07 + a0 * (1 - exp(LOGIT) / (1 + exp(LOGIT)) )"
				);

				logit = a2 + a1 * pow(quadMeanDiameter07, a3);

				quadMeanDiameterUtil.setCoe(
						uc.index,
						max(UtilizationClass.OVER225.lowBound, quadMeanDiameter07 + a0 * (1f - exponentRatio(logit)))
				);
				break;
			case ALL, SMALL:
				throw new IllegalStateException(
						"Should not be attempting to process small component or all large components"
				);
			default:
				throw new IllegalStateException("Unknown utilization class " + uc);
			}

			log.atDebug().setMessage("Util DQ for class {} is {}").addArgument(uc.className)
					.addArgument(quadMeanDiameterUtil.getCoe(uc.index));
		}

		log.atTrace().setMessage("Estimated Diameters {}").addArgument(
				() -> UtilizationClass.UTIL_CLASSES.stream()
						.map(uc -> String.format("%s: %d", uc.className, quadMeanDiameterUtil.getCoe(uc.index)))
		);
	}

	/**
	 * EMP090. Return an estimate of the volume, per tree, of the whole stem, based on the given lorey height and quad
	 * mean diameter.
	 *
	 * @param volumeGroup      the species' volume group
	 * @param loreyHeight      the species' lorey height
	 * @param quadMeanDiameter the species' quadratic mean diameter
	 * @return as described
	 */
	public float estimateWholeStemVolumePerTree(int volumeGroup, float loreyHeight, float quadMeanDiameter) {
		// Using eqn in jf117.doc
		var totalStandWholeStemVolumeCoeMap = controlMap.getTotalStandWholeStepVolumeCoeMap();
		var coe = totalStandWholeStemVolumeCoeMap.get(volumeGroup).reindex(0);

		var logMeanVolume = //
				coe.getCoe(UtilizationClass.ALL.index) + //
						coe.getCoe(1) * log(quadMeanDiameter) + //
						coe.getCoe(2) * log(loreyHeight) + //
						coe.getCoe(3) * quadMeanDiameter + //
						coe.getCoe(4) / quadMeanDiameter + //
						coe.getCoe(5) * loreyHeight + //
						coe.getCoe(6) * quadMeanDiameter * quadMeanDiameter + //
						coe.getCoe(7) * loreyHeight * quadMeanDiameter + //
						coe.getCoe(8) * loreyHeight / quadMeanDiameter;

		return exp(logMeanVolume);
	}

	/**
	 * EMP091. Updates wholeStemVolumeUtil with estimated values.
	 *
	 * @param utilizationClass
	 * @param adjustCloseUtil
	 * @param volumeGroup
	 * @param hlSp
	 * @param wholeStemUtilizationComponentMap
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param wholeStemVolumeUtil
	 * @throws ProcessingException
	 */
	public void estimateWholeStemVolume(
			UtilizationClass utilizationClass, float adjustCloseUtil, int volumeGroup, Float hlSp,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil,
			UtilizationVector wholeStemVolumeUtil
	) throws ProcessingException {
		var wholeStemUtilizationComponentMap = controlMap.getWholeStemUtilizationComponentMap();
		var spDqAll = quadMeanDiameterUtil.getAll();

		estimateUtilization(baseAreaUtil, wholeStemVolumeUtil, utilizationClass, (uc, ba) -> {
			Coefficients wholeStemCoe = wholeStemUtilizationComponentMap.get(uc.index, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);

			// Fortran code uses 1 index into array when reading it here, but 0 index when
			// writing into it in the parser. I use 0 for both.
			var a0 = wholeStemCoe.getCoe(0);
			var a1 = wholeStemCoe.getCoe(1);
			var a2 = wholeStemCoe.getCoe(2);
			var a3 = wholeStemCoe.getCoe(3);

			var arg = a0 + a1 * log(hlSp) + a2 * log(quadMeanDiameterUtil.getCoe(uc.index))
					+ ( (uc != UtilizationClass.OVER225) ? a3 * log(spDqAll) : a3 * spDqAll);

			if (uc == utilizationClass) {
				arg += adjustCloseUtil;
			}

			var vbaruc = exp(arg); // volume base area ?? utilization class?

			return ba * vbaruc;
		}, x -> x <= 0f, 0f);

		if (utilizationClass == UtilizationClass.ALL) {
			normalizeUtilizationComponents(wholeStemVolumeUtil);
		}
	}

	/**
	 * EMP092. Updates closeUtilizationVolumeUtil with estimated values.
	 *
	 * @param utilizationClass
	 * @param aAdjust
	 * @param volumeGroup
	 * @param hlSp
	 * @param quadMeanDiameterUtil
	 * @param wholeStemVolumeUtil
	 * @param closeUtilizationVolumeUtil
	 * @throws ProcessingException
	 */
	public void estimateCloseUtilizationVolume(
			UtilizationClass utilizationClass, Coefficients aAdjust, int volumeGroup, float hlSp,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector wholeStemVolumeUtil,
			UtilizationVector closeUtilizationVolumeUtil
	) throws ProcessingException {
		var closeUtilizationCoeMap = controlMap.getCloseUtilizationCoeMap();

		estimateUtilization(wholeStemVolumeUtil, closeUtilizationVolumeUtil, utilizationClass, (uc, ws) -> {
			Coefficients closeUtilCoe = closeUtilizationCoeMap.get(uc.index, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);
			var a0 = closeUtilCoe.getCoe(1);
			var a1 = closeUtilCoe.getCoe(2);
			var a2 = closeUtilCoe.getCoe(3);

			var arg = a0 + a1 * quadMeanDiameterUtil.getCoe(uc.index) + a2 * hlSp + aAdjust.getCoe(uc.index);

			float ratio = ratio(arg, 7.0f);

			return ws * ratio;
		});

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationVolumeUtil);
		}
	}

	/**
	 * EMP093. Estimate volume NET OF DECAY by (DBH) utilization classes
	 *
	 * @param genus
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param decayGroup
	 * @param ageBreastHeight
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayVolume(
			String genus, Region region, UtilizationClass utilizationClass, Coefficients aAdjust, int decayGroup,
			float ageBreastHeight, UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayUtil
	) throws ProcessingException {
		var netDecayCoeMap = controlMap.getNetDecayCoeMap();
		var decayModifierMap = controlMap.getDecayModifierMap();

		var dqSp = quadMeanDiameterUtil.getAll();

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(closeUtilizationUtil, closeUtilizationNetOfDecayUtil, utilizationClass, (uc, cu) -> {
			Coefficients netDecayCoe = netDecayCoeMap.get(uc.index, decayGroup).orElseThrow(
					() -> new ProcessingException("Could not find net decay coefficients for group " + decayGroup)
			);
			var a0 = netDecayCoe.getCoe(1);
			var a1 = netDecayCoe.getCoe(2);
			var a2 = netDecayCoe.getCoe(3);

			float arg;
			if (uc != UtilizationClass.OVER225) {
				arg = a0 + a1 * log(dqSp) + a2 * ageTr;
			} else {
				arg = a0 + a1 * log(quadMeanDiameterUtil.getCoe(uc.index)) + a2 * ageTr;
			}

			arg += aAdjust.getCoe(uc.index) + decayModifierMap.get(genus, region);

			float ratio = ratio(arg, 8.0f);

			return cu * ratio;
		});

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayUtil);
		}
	}

	/**
	 * EMP094. Estimate utilization net of decay and waste
	 *
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param genus
	 * @param loreyHeight
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayAndWasteVolume(
			Region region, UtilizationClass utilizationClass, Coefficients aAdjust, String genus, float loreyHeight,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayUtil, UtilizationVector closeUtilizationNetOfDecayAndWasteUtil
	) throws ProcessingException {
		final var netDecayWasteCoeMap = controlMap.getNetDecayWasteCoeMap();
		final var wasteModifierMap = controlMap.getWasteModifierMap();

		estimateUtilization(
				closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil, utilizationClass,
				(i, netDecay) -> {
					if (Float.isNaN(netDecay) || netDecay <= 0f) {
						return 0f;
					}

					Coefficients netWasteCoe = netDecayWasteCoeMap.get(genus);
					if (netWasteCoe == null) {
						throw new ProcessingException("Could not find net waste coefficients for genus " + genus);
					}

					var a0 = netWasteCoe.getCoe(0);
					var a1 = netWasteCoe.getCoe(1);
					var a2 = netWasteCoe.getCoe(2);
					var a3 = netWasteCoe.getCoe(3);
					var a4 = netWasteCoe.getCoe(4);
					var a5 = netWasteCoe.getCoe(5);

					if (i == UtilizationClass.OVER225) {
						a0 += a5;
					}
					var frd = 1.0f - netDecay / closeUtilizationUtil.getCoe(i.index);

					float arg = a0 + a1 * frd + a3 * log(quadMeanDiameterUtil.getCoe(i.index)) + a4 * log(loreyHeight);

					arg += wasteModifierMap.get(genus, region);

					arg = clamp(arg, -10f, 10f);

					var frw = (1.0f - exp(a2 * frd)) * exp(arg) / (1f + exp(arg)) * (1f - frd);
					frw = min(frd, frw);

					float result = closeUtilizationUtil.getCoe(i.index) * (1f - frd - frw);

					/*
					 * Check for an apply adjustments. This is done after computing the result above to allow for
					 * clamping frw to frd
					 */
					if (aAdjust.getCoe(i.index) != 0f) {
						var ratio = result / netDecay;
						if (ratio < 1f && ratio > 0f) {
							arg = log(ratio / (1f - ratio));
							arg += aAdjust.getCoe(i.index);
							arg = clamp(arg, -10f, 10f);
							result = exp(arg) / (1f + exp(arg)) * netDecay;
						}
					}

					return result;
				}
		);

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayAndWasteUtil);
		}
	}

	/**
	 * EMP095. Estimate utilization net of decay, waste, and breakage
	 *
	 * @param utilizationClass
	 * @param breakageGroup
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @param closeUtilizationNetOfDecayWasteAndBreakageUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayWasteAndBreakageVolume(
			UtilizationClass utilizationClass, int breakageGroup, UtilizationVector quadMeanDiameterUtil,
			UtilizationVector closeUtilizationUtil, UtilizationVector closeUtilizationNetOfDecayAndWasteUtil,
			UtilizationVector closeUtilizationNetOfDecayWasteAndBreakageUtil
	) throws ProcessingException {
		var netBreakageCoeMap = controlMap.getNetBreakageMap();

		final var coefficients = netBreakageCoeMap.get(breakageGroup);
		if (coefficients == null) {
			throw new ProcessingException("Could not find net breakage coefficients for group " + breakageGroup);
		}

		final var a1 = coefficients.getCoe(1);
		final var a2 = coefficients.getCoe(2);
		final var a3 = coefficients.getCoe(3);
		final var a4 = coefficients.getCoe(4);

		estimateUtilization(
				closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil,
				utilizationClass, (uc, netWaste) -> {

					if (netWaste <= 0f) {
						return 0f;
					}
					var percentBroken = a1 + a2 * log(quadMeanDiameterUtil.getCoe(uc.index));
					percentBroken = clamp(percentBroken, a3, a4);
					var broken = min(percentBroken / 100 * closeUtilizationUtil.getCoe(uc.index), netWaste);
					return netWaste - broken;
				}
		);

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayWasteAndBreakageUtil);
		}
	}

	/**
	 * EMP106 - estimate basal area yield for the primary layer (from IPSJF160.doc)
	 *
	 * @param dominantHeight    dominant height (m)
	 * @param breastHeightAge   breast height age (years)
	 * @param baseAreaOverstory basal area of overstory (>= 0)
	 * @param fullOccupancy     if true, the empirically fitted curve is increased to become a full occupancy curve. If
	 *                          false, BAP is for mean conditions
	 * @param species           Species for the layer
	 * @param primarySpeciesId  Id of the primary species
	 * @param bec               BEC of the polygon
	 * @param baseAreaGroup     Index of the base area group
	 * @return as described
	 * @throws BreastHeightAgeLowException
	 */
	public float estimateBaseAreaYield(
			float dominantHeight, float breastHeightAge, Optional<Float> baseAreaOverstory, boolean fullOccupancy,
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, String primarySpeciesId,
			BecDefinition bec, int baseAreaGroup
	) throws BreastHeightAgeLowException {
		var coe = estimateBaseAreaYieldCoefficients(species, bec);

		float upperBoundBaseArea = upperBoundsBaseArea(bec.getRegion(), primarySpeciesId, baseAreaGroup);
		return estimateBaseAreaYield(
				coe, dominantHeight, breastHeightAge, baseAreaOverstory, fullOccupancy, upperBoundBaseArea
		);
	}

	/**
	 * EMP106 - estimate basal area yield for the primary layer (from IPSJF160.doc)
	 *
	 * @param estimateBasalAreaYieldCoefficients estimate basal area yield coefficients
	 * @param dominantHeight                     dominant height (m)
	 * @param breastHeightAge                    breast height age (years)
	 * @param veteranBaseArea                    basal area of overstory (>= 0)
	 * @param fullOccupancy                      if true, the empirically fitted curve is increased to become a full
	 *                                           occupancy curve. If false, BAP is for mean conditions
	 * @param upperBoundBasalArea                limit on the resulting basal area
	 * @return as described
	 * @throws StandProcessingException
	 */
	public float estimateBaseAreaYield(
			Coefficients estimateBasalAreaYieldCoefficients, float dominantHeight, float breastHeightAge,
			Optional<Float> veteranBasalArea, boolean fullOccupancy, float upperBoundBasalArea
	) throws BreastHeightAgeLowException {

		Optional<Float> maxBreastHeightAge = ((NonFipDebugSettings) controlMap.getDebugSettings())
				.getMaxBreastHeightAge();

		// The original Fortran had the following comment and a commented out modification to upperBoundsBaseArea
		// (BATOP98):

		/*
		 * And one POSSIBLY one last vestige of grouping by ITG.
		 *
		 * That limit applies to full occupancy and Empirical occupancy. They were derived as the 98th percentile of
		 * Empirical stocking, though adjusted PSPs were included. If the ouput of this routine is bumped up from
		 * empirical to full, MIGHT adjust this limit DOWN here, so that at end, it is correct. Tentatively decide NOT
		 * to do this:
		 */

		// if (fullOccupancy) {
		// upperBoundsBaseArea *= EMPOC;
		// }

		float ageToUse = maxBreastHeightAge.map(max -> Math.min(breastHeightAge, max)).orElse(breastHeightAge);

		if (ageToUse <= 0f) {
			throw new BreastHeightAgeLowException(LayerType.PRIMARY, Optional.of(ageToUse), Optional.of(0f));
		}

		float trAge = FloatMath.log(ageToUse);

		float a0 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a1 = estimateBasalAreaYieldCoefficients.getCoe(1);
		float a2 = estimateBasalAreaYieldCoefficients.getCoe(2);
		float a3 = estimateBasalAreaYieldCoefficients.getCoe(3);
		float a4 = estimateBasalAreaYieldCoefficients.getCoe(4);
		float a5 = estimateBasalAreaYieldCoefficients.getCoe(5);
		float a6 = estimateBasalAreaYieldCoefficients.getCoe(6);

		float a00 = Math.max(a0 + a1 * trAge, 0);
		float ap = Math.max(a3 + a4 * trAge, 0);

		float bap;
		if (dominantHeight <= a2) {
			bap = 0;
		} else {
			bap = a00 * FloatMath.pow(dominantHeight - a2, ap)
					* FloatMath.exp(a5 * dominantHeight + a6 * veteranBasalArea.orElse(0f));
			bap = Math.min(bap, upperBoundBasalArea);
		}

		if (fullOccupancy) {
			bap /= EMPIRICAL_OCCUPANCY;
		}

		return bap;
	}

	private Coefficients sumCoefficientsWeightedBySpeciesAndDecayBec(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec,
			MatrixMap2<String, String, Coefficients> coeMap, int size
	) {

		final String decayBecAlias = bec.getDecayBec().getAlias();

		return weightedCoefficientSum(
				size, 0, //
				species, //
				BaseVdypSpecies::getFractionGenus, // Weight by fraction
				spec -> coeMap.get(decayBecAlias, spec.getGenus())
		);
	}

	// TODO Make private after VDYP-1107
	public Coefficients sumCoefficientsWeightedBySpeciesAndDecayBec(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec, ControlKey key,
			int size
	) {
		MatrixMap2<String, String, Coefficients> coeMap = Utils
				.<MatrixMap2<String, String, Coefficients>>expectParsedControl(
						controlMap.getControlMap(), key, MatrixMap2.class
				);
		return sumCoefficientsWeightedBySpeciesAndDecayBec(species, bec, coeMap, size);
	}

	/**
	 * Create a coefficients object where its values are either a weighted sum of those for each of the given entities,
	 * or the value from one arbitrarily chose entity.
	 *
	 * @param <T>             The type of entity
	 * @param size            Size of the resulting coefficients object
	 * @param indexFrom       index from of the resulting coefficients object
	 * @param entities        the entities to do weighted sums over
	 * @param weight          the weight for a given entity
	 * @param getCoefficients the coefficients for a given entity
	 */
	public static <T> Coefficients weightedCoefficientSum(
			int size, int indexFrom, Collection<T> entities, ToDoubleFunction<T> weight,
			Function<T, Coefficients> getCoefficients
	) {
		var weighted = IntStream.range(indexFrom, size + indexFrom).boxed().toList();
		return weightedCoefficientSum(weighted, size, indexFrom, entities, weight, getCoefficients);
	}

	private static final int BA_COE_INDEX = 1;
	private static final int DQ_COE_INDEX = 2;

	/**
	 * UPPERGEN
	 * <p>
	 * Get the upper bound for basal area
	 *
	 * @param region
	 * @param primarySpeciesId
	 * @param primarySpeciesGroupNumber
	 * @return
	 */
	public float upperBoundsBaseArea(Region region, String primarySpeciesId, int primarySpeciesGroupNumber) {
		return getUpperBoundsCoefficient(region, primarySpeciesId, primarySpeciesGroupNumber, BA_COE_INDEX);
	}

	/**
	 * UPPERGEN
	 * <p>
	 * Get the upper bound for quadratic mean diameter
	 *
	 * @param region
	 * @param primarySpeciesId
	 * @param primarySpeciesGroupNumber
	 * @return
	 */
	public float upperBoundsQuadMeanDiameter(Region region, String primarySpeciesId, int primarySpeciesGroupNumber) {
		return getUpperBoundsCoefficient(region, primarySpeciesId, primarySpeciesGroupNumber, DQ_COE_INDEX);
	}

	/**
	 * UPPERGEN
	 */
	private float getUpperBoundsCoefficient(
			Region region, String primarySpeciesId, int primarySpeciesGroupNumber, int coefficient
	) {

		switch (controlMap.getDebugSettings().getUpperBoundsMode()) {
		case MODE_1:
			return controlMap.getUpperBounds().get(primarySpeciesGroupNumber).getCoe(UpperBoundsParser.BA_INDEX);
		case MODE_2:
		default:
			var upperBoundsCoefficients = controlMap.getUpperBoundsCoefficients();
			return upperBoundsCoefficients.get(region, primarySpeciesId, coefficient);
		}

	}

	/**
	 * // EMP107 /**
	 *
	 * @param dominantHeight   Dominant height (m)
	 * @param breastHeightAge  breast height age
	 * @param veteranBaseArea  Basal area of overstory (>= 0)
	 * @param species          Species for the layer
	 * @param primarySpeciesId Id of the primary species
	 * @param becZone          BEC of the polygon
	 * @param baseAreaGroup    Index of the base area group
	 * @return DQ of primary layer (w DBH >= 7.5)
	 * @throws StandProcessingException
	 */
	public float estimateQuadMeanDiameterYield(
			float dominantHeight, float breastHeightAge, Optional<Float> veteranBaseArea,
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, String primarySpeciesId,
			BecDefinition becZone, int baseAreaGroup
	) throws StandProcessingException {
		controlMap.getQuadMeanDiameterYieldCoefficients();
		final var coe = sumCoefficientsWeightedBySpeciesAndDecayBec(
				species, becZone, controlMap.getQuadMeanDiameterYieldCoefficients(), 6
		);
		Optional<Float> maxBreastHeightAge = ((NonFipDebugSettings) controlMap.getDebugSettings())
				.getMaxBreastHeightAge();
		float upperBoundQuadMeanDiameter = upperBoundsBaseArea(becZone.getRegion(), primarySpeciesId, baseAreaGroup);
		return estimateQuadMeanDiameterYield(
				coe, maxBreastHeightAge, dominantHeight, breastHeightAge, veteranBaseArea, upperBoundQuadMeanDiameter
		);
	}

	/**
	 * EMP107 - estimate DQ yield for the primary layer (from IPSJF161.doc)
	 *
	 * @param coefficients               coefficients weighted by species and decay bec zone
	 * @param controlVariable2Setting    the value of control variable 2
	 * @param dominantHeight             dominant height (m)
	 * @param breastHeightAge            breast height age (years)
	 * @param veteranBaseArea            basal area of overstory (>= 0)
	 * @param upperBoundQuadMeanDiameter upper bound on the result of this call
	 * @return quad-mean-diameter of primary layer (with DBH >= 7.5)
	 * @throws StandProcessingException in the event of a processing error
	 */
	public float estimateQuadMeanDiameterYield(
			Coefficients coefficients, Optional<Float> maxBreastHeightAge, float dominantHeight, float breastHeightAge,
			Optional<Float> veteranBaseArea, float upperBoundQuadMeanDiameter
	) throws StandProcessingException {

		if (dominantHeight <= 5) {
			return 7.6f;
		}

		float ageUse = maxBreastHeightAge.map(max -> Math.min(breastHeightAge, max)).orElse(breastHeightAge);

		if (ageUse <= 0f) {
			throw new BreastHeightAgeLowException(LayerType.PRIMARY, Optional.of(ageUse), Optional.of(0f));
		}

		final float trAge = FloatMath.log(ageUse);

		final float c0 = coefficients.getCoe(0);
		final float c1 = Math.max(coefficients.getCoe(1) + coefficients.getCoe(2) * trAge, 0f);
		final float c2 = Math.max(coefficients.getCoe(3) + coefficients.getCoe(4) * trAge, 0f);

		float dq = c0 + c1 * FloatMath.pow(dominantHeight - 5f, c2)
				* FloatMath.exp(veteranBaseArea.orElse(0.0f) * coefficients.getCoe(5));
		return FloatMath.clamp(dq, 7.6f, upperBoundQuadMeanDiameter);
	}

	/**
	 * EMP107 - estimate DQ yield for the primary layer (from IPSJF161.doc)
	 *
	 * @param coefficients               coefficients weighted by species and decay bec zone
	 * @param dominantHeight             dominant height (m)
	 * @param breastHeightAge            breast height age (years)
	 * @param veteranBaseArea            basal area of overstory (>= 0)
	 * @param upperBoundQuadMeanDiameter upper bound on the result of this call
	 * @return quad-mean-diameter of primary layer (with DBH >= 7.5)
	 * @throws StandProcessingException in the event of a processing error
	 */
	public float estimateQuadMeanDiameterYield(
			Coefficients coefficients, float dominantHeight, float breastHeightAge, Optional<Float> veteranBaseArea,
			float upperBoundQuadMeanDiameter
	) throws StandProcessingException {
		return estimateQuadMeanDiameterYield(
				coefficients, ((NonFipDebugSettings) controlMap.getDebugSettings()).getMaxBreastHeightAge(),
				dominantHeight, breastHeightAge, veteranBaseArea, upperBoundQuadMeanDiameter
		);

	}

	@FunctionalInterface
	public static interface UtilizationProcessor {
		float apply(UtilizationClass utilizationClass, float inputValue) throws ProcessingException;
	}

	public enum Strictness {
		/**
		 * Prefer to throw an exception
		 */
		STRICT,
		/**
		 * Prefer to tweak values to work and log a warning
		 */
		ADJUST,
		/**
		 * Prefer to accept values as they are and log a warning
		 */
		LENIENT
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the computation, UtilizationClass.ALL for all of
	 *                         them.
	 * @param processor        Given a utilization class, and the source utilization for that class, return the result
	 *                         utilization
	 * @param skip             a utilization class will be skipped and the result set to the default value if this is
	 *                         true for the value of the source utilization
	 * @param defaultValue     the default value
	 * @throws ProcessingException
	 */
	private static void estimateUtilization(
			UtilizationVector input, UtilizationVector output, UtilizationClass utilizationClass,
			UtilizationProcessor processor, Predicate<Float> skip, float defaultValue
	) throws ProcessingException {
		for (var uc : UtilizationClass.UTIL_CLASSES) {
			var inputValue = input.getCoe(uc.index);

			// it seems like this should be done after checking i against utilizationClass,
			// which could just be done as part of the processor definition, but this is how
			// VDYP7 did it.
			if (skip.test(inputValue)) {
				output.setCoe(uc.index, defaultValue);
				continue;
			}

			if (utilizationClass != UtilizationClass.ALL && utilizationClass != uc) {
				continue;
			}

			var result = processor.apply(uc, inputValue);
			output.setCoe(uc.index, result);
		}
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the computation, UtilizationClass.ALL for all of
	 *                         them.
	 * @param processor        Given a utilization class, and the source utilization for that class, return the result
	 *                         utilization
	 * @throws ProcessingException
	 */
	private static void estimateUtilization(
			UtilizationVector input, UtilizationVector output, UtilizationClass utilizationClass,
			UtilizationProcessor processor
	) throws ProcessingException {
		estimateUtilization(input, output, utilizationClass, processor, x -> false, 0f);
	}

	private static float exponentRatio(float logit) throws ProcessingException {
		float exp = safeExponent(logit);
		return exp / (1f + exp);
	}

	private static float safeExponent(float logit) throws ProcessingException {
		if (logit > 88f) {
			throw new ProcessingException("logit " + logit + " exceeds 88");
		}
		return exp(logit);
	}

	/**
	 * Normalizes the utilization components 1-4 so they sum to the value of component UtilizationClass.ALL
	 *
	 * @throws ProcessingException if the sum is not positive
	 */
	private static float normalizeUtilizationComponents(UtilizationVector components) throws ProcessingException {
		var sum = sumUtilizationComponents(components);
		var k = components.getAll() / sum;
		if (sum <= 0f) {
			throw new ProcessingException("Total volume " + sum + " was not positive.");
		}
		UtilizationClass.UTIL_CLASSES.forEach(uc -> components.setCoe(uc.index, components.getCoe(uc.index) * k));
		return k;
	}

	/**
	 * Sums the individual utilization components (1-4)
	 */
	private static float sumUtilizationComponents(UtilizationVector components) {
		return (float) UtilizationClass.UTIL_CLASSES.stream().mapToInt(x -> x.index).mapToDouble(components::getCoe)
				.sum();
	}

	/**
	 * Sums the individual utilization components (1-4) and stores the results in coefficient UtilizationClass.ALL
	 */
	private static float storeSumUtilizationComponents(UtilizationVector components) {
		var sum = sumUtilizationComponents(components);
		components.setCoe(UtilizationClass.ALL.index, sum);
		return sum;
	}

	/**
	 * EMP086
	 * <p>
	 * Estimate the whole stem volume of the small utilization class
	 *
	 * @param species                   The species
	 * @param loreyHeightSpecSmall      The Lorey height of the small class
	 * @param quadMeanDiameterSpecSmall The quadratic mean diameter of the small class
	 */
	public float estimateMeanVolumeSmall(
			BaseVdypSpecies<?> species, float loreyHeightSpecSmall, float quadMeanDiameterSpecSmall
	) {
		return estimateMeanVolumeSmall(species.getGenus(), loreyHeightSpecSmall, quadMeanDiameterSpecSmall);
	}

	/**
	 * EMP086
	 * <p>
	 * Estimate the whole stem volume of the small utilization class
	 *
	 * @param speciesId                 Species identifier (SP0)
	 * @param loreyHeightSpecSmall      The Lorey height of the small class
	 * @param quadMeanDiameterSpecSmall The quadratic mean diameter of the small class
	 */
	public float
			estimateMeanVolumeSmall(String speciesId, float loreyHeightSpecSmall, float quadMeanDiameterSpecSmall) {
		Coefficients coe = controlMap.getSmallComponentWholeStemVolumeCoefficients().get(speciesId);

		// EQN 1 in IPSJF119.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);
		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		return exp(
				a0 + a1 * log(quadMeanDiameterSpecSmall) + a2 * log(loreyHeightSpecSmall)
						+ a3 * quadMeanDiameterSpecSmall
		);
	}

	/**
	 * EMP085
	 * <p>
	 * Estimate lorey height for small component (4.0-7.5 cm diameter)
	 *
	 * @param <S>
	 *
	 * @param species                   Species with ALL component set for Lorey height and quadratic mean diameter
	 * @param quadMeanDiameterSpecSmall Small component quadratic mean diameter
	 * @return
	 */
	public <S extends BaseVdypSpecies<?> & VdypUtilizationHolder> float
			estimateSmallComponentLoreyHeight(S spec, float quadMeanDiameterSpecSmall) {
		return estimateSmallComponentLoreyHeight(
				spec.getGenus(), spec.getLoreyHeightByUtilization().getAll(), quadMeanDiameterSpecSmall,
				spec.getQuadraticMeanDiameterByUtilization().getAll()
		);
	}

	/**
	 * EMP085
	 * <p>
	 * Estimate lorey height for small component (4.0-7.5 cm diameter)
	 *
	 * @param speciesId                  Species identifier (SP0)
	 * @param speciesLoreyHeightAll      ALL component Lorey height
	 * @param quadMeanDiameterSpecSmall  Small component quadratic mean diameter
	 * @param speciesQuadMeanDiameterAll ALL component quadratic mean diameter
	 * @return
	 */
	public float estimateSmallComponentLoreyHeight(
			String speciesId, float speciesLoreyHeightAll, float quadMeanDiameterSpecSmall,
			float speciesQuadMeanDiameterAll
	) {
		Coefficients coe = controlMap.getSmallComponentLoreyHeightCoefficients().get(speciesId);

		// EQN 1 in IPSJF119.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);

		return 1.3f + (speciesLoreyHeightAll - 1.3f) //
				* exp(a0 * (pow(quadMeanDiameterSpecSmall, a1) - pow(speciesQuadMeanDiameterAll, a1)));

	}

	/**
	 * EMP082
	 * <p>
	 * Estimate the quadratic mean diameter for the small component (4.0-7.5 cm diameter)
	 *
	 * @param species Species with ALL component set for Lorey height
	 * @return
	 */
	public float estimateSmallComponentQuadMeanDiameter(VdypSpecies species) {
		return estimateSmallComponentQuadMeanDiameter(
				species.getGenus(), species.getLoreyHeightByUtilization().getAll()
		);
	}

	/**
	 * EMP082
	 * <p>
	 * Estimate the quadratic mean diameter for the small component (4.0-7.5 cm diameter)
	 *
	 * @param speciesId   Species identifier (SP0)
	 * @param loreyHeight ALL component Lorey height
	 * @return
	 */
	public float estimateSmallComponentQuadMeanDiameter(String speciesId, float loreyHeight) {
		Coefficients coe = controlMap.getSmallComponentQuadMeanDiameterCoefficients().get(speciesId);

		// EQN 5 in IPSJF118.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);

		float logit = a0 + a1 * loreyHeight;

		return 4.0f + 3.5f * exp(logit) / (1.0f + exp(logit));
	}

	/**
	 * EMP081
	 * <p>
	 * Estimate the conditional expected small component basal area. See IPSJF118.doc, equation 3.
	 *
	 * @param speciesId Species identifier (SP0)
	 * @param baAll     ALL component basal area
	 * @param lhAll     ALL component Lorey height
	 * @param region    the region in which the calculation is occurring
	 * @return small component basal area
	 */
	public float estimateSmallComponentConditionalExpectedBasalArea(
			String speciesId, float baAll, float lhAll, Region region
	) {
		Coefficients coe = controlMap.getSmallComponentBasalAreaCoefficients().get(speciesId);

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);
		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		float regionMultiplier = region == Region.COASTAL ? 1.0f : 0.0f;

		// FIXME VDYP-1146 due to a bug in VDYP7 it always treats this as interior. Replicating
		// that for now.
		regionMultiplier = 0f;

		float result = (a0 + a1 * regionMultiplier + a2 * baAll) * exp(a3 * lhAll);
		result = max(result, 0f);

		return result;
	}

	/**
	 * EMP081
	 * <p>
	 * Estimate the conditional expected small component basal area. See IPSJF118.doc, equation 3.
	 *
	 * @param species Species with ALL component set for Lorey height
	 * @param baAll   ALL component basal area
	 * @param region  the region in which the calculation is occurring
	 * @return small component basal area
	 */
	public <S extends BaseVdypSpecies<?> & VdypUtilizationHolder> float
			estimateSmallComponentConditionalExpectedBasalArea(S species, float baAll, Region region) {
		return estimateSmallComponentConditionalExpectedBasalArea(
				species.getGenus(), baAll, species.getLoreyHeightByUtilization().getAll(), region
		);
	}

	/**
	 * Estimate the conditional expected small component basal area. See IPSJF118.doc, equation 3.
	 *
	 * @param species           Species with ALL component set for Lorey height
	 * @param fractionAvailable Fraction of the polygon available
	 * @param region            the region in which the calculation is occurring
	 * @return small component basal area normalized for fraction available
	 */
	public <S extends BaseVdypSpecies<?> & VdypUtilizationHolder> float
			estimateSmallComponentConditionalExpectedBasalAreaNormalized(
					S species, float fractionAvailable, Region region
			) {
		return estimateSmallComponentConditionalExpectedBasalArea(
				species, species.getBaseAreaByUtilization().getAll() * fractionAvailable, region
		) / fractionAvailable;
	}

	/**
	 * EMP080
	 * <p>
	 * Calculate the small component probability of a species
	 *
	 * @param speciesId           Species identifier (SP0)
	 * @param yearsAtBreastHeight Years at breast height for primary species
	 * @param loreyHeight         current Lorey height of the stand
	 * @param region              the stand's region
	 * @return the small component probability of the species.
	 */
	public float estimateSmallComponentProbability(
			String speciesId, float yearsAtBreastHeight, float loreyHeight, Region region
	) {

		Coefficients coe = controlMap.getSmallComponentProbabilityCoefficients().get(speciesId);

		// EQN 1 in IPSJF118.doc

		float a0 = coe.getCoe(1);

		float a1 = switch (region) {
		case COASTAL -> coe.getCoe(2);
		case INTERIOR -> 0f;
		};

		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		float logit = a0 + //
				a1 + //
				a2 * yearsAtBreastHeight + //
				a3 * loreyHeight;

		return exp(logit) / (1.0f + exp(logit));
	}

	/**
	 * EMP080
	 * <p>
	 * Calculate the small component probability of a species
	 *
	 * @param layer   the layer containing the species
	 * @param species Species identifier (SP0)
	 * @param region  the stand's region
	 * @return the small component probability of the species.
	 */
	public float estimateSmallComponentProbability(VdypLayer layer, VdypSpecies species, Region region) {
		return estimateSmallComponentProbability(
				species.getGenus(), //
				layer.getComputedYearsAtBreastHeight().orElse(0f), //
				species.getLoreyHeightByUtilization().getAll(), //
				region
		);
	}

	public Coefficients estimateBaseAreaYieldCoefficients(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec
	) {
		var coe = sumCoefficientsWeightedBySpeciesAndDecayBec(species, bec, ControlKey.BA_YIELD, 7);

		// TODO confirm going over 0.5 should drop to 0 as this seems odd.
		coe.scalarInPlace(5, x -> x > 0.0f ? 0f : x);
		return coe;
	}

	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> S
			leadSpecies(L fipLayer) {
		return fipLayer.getSpecies().values().stream()
				.sorted(Utils.compareUsing(BaseVdypSpecies<? extends BaseVdypSite>::getFractionGenus).reversed())
				.findFirst().orElseThrow();
	}

	public <L extends BaseVdypLayer<?, ?>> Optional<Float> getLayerHeight(L layer) {
		return layer.getPrimarySite().flatMap(BaseVdypSite::getHeight);
	}

	/**
	 * EMP041
	 * <p>
	 * Estimate the quadratic mean diameter yield for the primary layer.
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @return The basal area.
	 */
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryQuadMeanDiameter(
					L layer, BecDefinition bec, float breastHeightAge, float baseAreaOverstory
			) {
		var coeMap = controlMap.getQuadMeanDiameterCoefficients();
		var modMap = controlMap.getQuadMeanDiameterModifiers();
		var upperBoundMap = controlMap.getUpperBoundsCoefficients();

		S leadGenus = leadSpecies(layer);

		var decayBecAlias = bec.getDecayBec().getAlias();
		Coefficients coe = EstimationMethods.weightedCoefficientSum(
				List.of(0, 1, 2, 3, 4), 8, 0, layer.getSpecies().values(), BaseVdypSpecies::getFractionGenus,
				s -> coeMap.get(decayBecAlias, s.getGenus())
		);

		var trAge = log(clamp(breastHeightAge, 5f, 350f));
		var height = getLayerHeight(layer).orElse(0f);

		if (height <= coe.getCoe(5)) {
			return 7.6f;
		}

		/* @formatter:off */
		//    C0 = A(0)
		//    C1 = EXP(A(1)) + EXP(A(2)) * TR_AGE
		//    C2 = EXP(A(3)) + EXP(A(4)) * TR_AGE
		/* @formatter:on */
		var c0 = coe.get(0);
		var c1 = exp(coe.getCoe(1)) + exp(coe.getCoe(2)) * trAge;
		var c2 = exp(coe.getCoe(3)) + exp(coe.getCoe(4)) * trAge;

		/* @formatter:off */
		//      DQ = C0 + ( C1*(HD - A(5))**C2 )**2 * exp(A(7)*BAV)
		//     &        * (1.0 - A(6)*CC/100.0)
		/* @formatter:on */

		var quadMeanDiameter = c0 + pow(c1 * pow(height - coe.getCoe(5), c2), 2)
				* exp(coe.getCoe(7) * baseAreaOverstory) * (1f - coe.getCoe(6) * layer.getCrownClosure() / 100f);

		/* @formatter:off */
		//      DQ = DQ * DQMOD200(JLEAD, INDEX_IC)
		/* @formatter:on */
		quadMeanDiameter *= modMap.get(leadGenus.getGenus(), bec.getRegion());

		quadMeanDiameter = max(quadMeanDiameter, 7.6f);

		if (!controlMap.getDebugSettings().getNoQuadraticMeanDiameterLimit()) {
			// See ISPSJF129
			var upperBound = upperBoundMap.get(bec.getRegion(), leadGenus.getGenus(), UpperCoefficientParser.DQ);
			quadMeanDiameter = min(quadMeanDiameter, upperBound);
		}

		return quadMeanDiameter;
	}

	/**
	 * EMP040
	 * <p>
	 * Estimate the basal area yield for the primary layer. Ensures that it does not go below the allowable minimum.
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param yieldFactor       Yield factor of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @param crownClosure      Crown closure percentage
	 * @param basalAreaMinimum  How should the basal area minimum be applied
	 * @return The basal area.
	 */
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryBaseArea(
					L layer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory,
					float crownClosure, Strictness basalAreaMinimum
			) throws BaseAreaLowException {
		boolean lowCrownClosure = layer.getCrownClosure() < LOW_CROWN_CLOSURE;
		crownClosure = lowCrownClosure ? LOW_CROWN_CLOSURE : crownClosure;

		var coeMap = controlMap.getBasalAreaCoefficients();
		var modMap = controlMap.getBasalAreaModifiers();
		var upperBoundMap = controlMap.getUpperBoundsCoefficients();

		S leadGenus = leadSpecies(layer);

		var decayBecAlias = bec.getDecayBec().getAlias();
		Coefficients coe = weightedCoefficientSum(
				List.of(0, 1, 2, 3, 4, 5), 9, 0, layer.getSpecies().values(), BaseVdypSpecies::getFractionGenus,
				s -> coeMap.get(decayBecAlias, s.getGenus())
		);

		float ageToUse = clamp(breastHeightAge, 5f, 350f);
		float trAge = FloatMath.log(ageToUse);

		/* @formatter:off */
						//      A00 = exp(A(0)) * ( 1.0 +  A(1) * TR_AGE  )
						/* @formatter:on */
		var a00 = exp(coe.getCoe(0)) * (1f + coe.getCoe(1) * trAge);

		/* @formatter:off */
						//      AP  = exp( A(3)) + exp(A(4)) * TR_AGE
						/* @formatter:on */
		float ap = FloatMath.exp(coe.getCoe(3)) + exp(coe.getCoe(4)) * trAge;

		var baseArea = 0f;

		float height = getLayerHeight(layer).orElse(0f);
		if (height > coe.getCoe(2) - 3f) {
			/* @formatter:off */
							//  if (HD .le. A(2) - 3.0) then
							//      BAP = 0.0
							//      GO TO 90
							//  else if (HD .lt. A(2)+3.0) then
							//      FHD = (HD- (A(2)-3.00) )**2 / 12.0
							//  else
							//      FHD = HD-A(2)
							//  end if
							/* @formatter:on */
			var fHeight = height <= coe.getCoe(2) + 3f ? //
					pow(height - (coe.getCoe(2) - 3), 2) / 12f //
					: height - coe.getCoe(2);

			/* @formatter:off */
							//      BAP =  A00 * (CCUSE/100) ** ( A(7) + A(8)*log(HD) )   *
							//     &      FHD**AP * exp( A(5)*HD  + A(6) * BAV )
							/* @formatter:on */
			baseArea = a00 * FloatMath.pow(crownClosure / 100, coe.getCoe(7) + coe.getCoe(8) * FloatMath.log(height))
					* FloatMath.pow(fHeight, ap) * exp(coe.getCoe(5) * height + coe.getCoe(6) * baseAreaOverstory);

			baseArea *= modMap.get(leadGenus.getGenus(), bec.getRegion());

			if (!controlMap.getDebugSettings().getNoBasalAreaLimit()) {
				// See ISPSJF128
				var upperBound = upperBoundMap.get(bec.getRegion(), leadGenus.getGenus(), UpperCoefficientParser.BA);
				baseArea = min(baseArea, upperBound);
			}

			if (lowCrownClosure) {
				baseArea *= layer.getCrownClosure() / LOW_CROWN_CLOSURE;
			}

		}

		baseArea *= yieldFactor;

		// This is to prevent underflow errors in later calculations
		// VDYP7 returned an error code in parallel with the modified result

		switch (basalAreaMinimum) {
		case ADJUST:
			if (baseArea < MINIMUM_BASAL_AREA) {
				log.atWarn().setMessage("Estimated basal area {} is too low. Increasing to {}.").addArgument(baseArea)
						.addArgument(MINIMUM_BASAL_AREA).log();
				baseArea = MINIMUM_BASAL_AREA;
			}
			break;
		case LENIENT:
			log.atWarn().setMessage("Estimated basal area {} is too low.").addArgument(baseArea)
					.addArgument(MINIMUM_BASAL_AREA).log();
			break;
		case STRICT:
			Utils.throwIfPresent(
					BaseAreaLowException
							.check(layer.getLayerType(), "Estimated base area", Optional.of(baseArea), 0.05f)
			);
			break;
		}

		return baseArea;
	}

	/**
	 * EMP040
	 * <p>
	 * Estimate the basal area yield for the primary layer. Ensures that it does not go below the allowable minimum.
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param yieldFactor       Yield factor of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @param basalAreaMinimum  How should the basal area minimum be applied
	 * @return The basal area.
	 */
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryBaseArea(
					L layer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory,
					Strictness basalAreaMinimum
			) throws BaseAreaLowException {
		return estimatePrimaryBaseArea(
				layer, bec, yieldFactor, breastHeightAge, baseAreaOverstory, layer.getCrownClosure(), basalAreaMinimum
		);
	}

	/**
	 * EMP040
	 * <p>
	 * Estimate the basal area yield for the primary layer. Determines CC from layer. Ensures that it does not go below
	 * the allowable minimum.
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param yieldFactor       Yield factor of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @return The basal area.
	 */
	// EMP040
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryBaseAreaAdjust(
					L layer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory
			) {
		try {
			return estimatePrimaryBaseArea(
					layer, bec, yieldFactor, breastHeightAge, baseAreaOverstory, Strictness.ADJUST
			);
		} catch (BaseAreaLowException e) {
			throw new IllegalArgumentException("This should not happen", e);
		}
	}

	/**
	 * EMP040
	 * <p>
	 * Estimate the basal area yield for the primary layer. Determines CC from layer. Throws an exception if the
	 * computed BA is below the allowable minimum
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param yieldFactor       Yield factor of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @return The basal area.
	 * @throws BaseAreaLowException if the computed BA is below the allowable minimum
	 */
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryBaseAreaStrict(
					L layer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory
			) throws BaseAreaLowException {
		return estimatePrimaryBaseArea(layer, bec, yieldFactor, breastHeightAge, baseAreaOverstory, Strictness.STRICT);
	}

	/**
	 * EMP040
	 * <p>
	 * Estimate the basal area yield for the primary layer. Ensures that it does not go below the allowable minimum.
	 *
	 * @param layer             The layer
	 * @param bec               BEC zone of the polygon
	 * @param yieldFactor       Yield factor of the polygon
	 * @param breastHeightAge   Breast height age
	 * @param baseAreaOverstory Basal area of the veteran layer if there is one, 0 otherwise.
	 * @param crownClosure      Crown closure percentage
	 * @return The basal area.
	 */
	public <L extends BaseVdypLayer<S, I> & InputLayer, S extends BaseVdypSpecies<I>, I extends BaseVdypSite> float
			estimatePrimaryBaseAreaAdjust(
					L layer, BecDefinition bec, float yieldFactor, float breastHeightAge, float baseAreaOverstory,
					float crownClosure
			) {
		try {
			return estimatePrimaryBaseArea(
					layer, bec, yieldFactor, breastHeightAge, baseAreaOverstory, crownClosure, Strictness.ADJUST
			);
		} catch (BaseAreaLowException e) {
			throw new IllegalStateException("This should never happen", e);
		}
	}

	/**
	 * Create a coefficients object where its values are either a weighted sum of those for each of the given entities,
	 * or the value from one arbitrarily chose entity.
	 *
	 * @param <T>             The type of entity
	 * @param weighted        the indicies of the coefficients that should be weighted sums, those that are not included
	 *                        are assumed to be constant across all entities and one is choses arbitrarily.
	 * @param size            Size of the resulting coefficients object
	 * @param indexFrom       index from of the resulting coefficients object
	 * @param entities        the entities to do weighted sums over
	 * @param weight          the weight for a given entity
	 * @param getCoefficients the coefficients for a given entity
	 */
	public static <T> Coefficients weightedCoefficientSum(
			Collection<Integer> weighted, int size, int indexFrom, Collection<T> entities, ToDoubleFunction<T> weight,
			Function<T, Coefficients> getCoefficients
	) {
		Coefficients coe = Coefficients.empty(size, indexFrom);

		// Do the summation in double precision
		var coeWorking = new double[size];
		Arrays.fill(coeWorking, 0.0);

		for (var entity : entities) {
			var entityCoe = getCoefficients.apply(entity);
			double fraction = weight.applyAsDouble(entity);
			log.atInfo().addArgument(entity).addArgument(fraction).addArgument(entityCoe)
					.setMessage("For entity {} with fraction {} adding coefficients {}").log();
			for (int i : weighted) {
				coeWorking[i - indexFrom] += (entityCoe.getCoe(i)) * fraction;
			}
		}
		// Reduce back to float once done
		for (int i : weighted) {
			coe.setCoe(i, (float) coeWorking[i - indexFrom]);
		}

		// Pick one entity to fill in the fixed coefficients
		// Choice is arbitrary, they should all be the same
		var anyCoe = getCoefficients.apply(entities.iterator().next());

		for (int i = indexFrom; i < size + indexFrom; i++) {
			if (weighted.contains(i))
				continue;
			coe.setCoe(i, anyCoe.getCoe(i));
		}
		return coe;
	}

	/**
	 * EMP097
	 * <p>
	 * Estimate the diameter for a veteran layer.
	 *
	 * @param speciesId
	 * @param bec
	 * @param loreyHeight
	 * @return
	 */
	public float estimateVeteranQuadMeanDiameter(String speciesId, BecDefinition bec, float loreyHeight) {

		var vetDqMap = controlMap.getVeteranQuadMeanDiameterCoefficients();

		var coe = vetDqMap.get(speciesId, bec.getRegion());
		var a0 = coe.getCoe(1);
		var a1 = coe.getCoe(2);
		var a2 = coe.getCoe(3);

		return a0 + a1 * pow(loreyHeight, a2);
	}

	/**
	 * EMP098
	 * <p>
	 * Estimate the basal area for a veteran layer.
	 *
	 * @param height
	 * @param crownClosure
	 * @param speciesId
	 * @param region
	 * @return
	 */
	public float estimateVeteranBasalArea(float height, float crownClosure, String speciesId, Region region) {
		var coefficients = controlMap.getVeteranBasalAreaCoefficients().getM(speciesId, region);

		// mismatched index is copied from VDYP7
		float a0 = coefficients.getCoe(1);
		float a1 = coefficients.getCoe(2);
		float a2 = coefficients.getCoe(3);

		float baseArea = a0 * pow(max(height - a1, 0.0f), a2);

		baseArea *= crownClosure / 4.0f;

		baseArea = max(baseArea, 0.01f);

		return baseArea;
	}

	/**
	 * TODO
	 *
	 * @param <P2>
	 * @param <L2>
	 * @param <S2>
	 * @param <I2>
	 * @param polygon
	 * @param vetLayer
	 * @param primaryLayer
	 * @return
	 */
	public <P2 extends BaseVdypPolygon<L2, Optional<Float>, S2, I2> & InputPolygon, L2 extends BaseVdypLayer<S2, I2> & InputLayer, S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
			float estimatePercentForestLand(P2 polygon, Optional<L2> vetLayer, L2 primaryLayer) {
		var resultOrIsVeteran = isVeteranForEstimatePercentForestLand(polygon, vetLayer);
		return estimatePercentForestLand(polygon, vetLayer, primaryLayer, resultOrIsVeteran);
	}

	/**
	 * TODO
	 *
	 * @param <P2>
	 * @param <L2>
	 * @param <S2>
	 * @param <I2>
	 * @param polygon
	 * @param vetLayer
	 * @param primaryLayer
	 * @param resultOrIsVeteran
	 * @return
	 */
	public <P2 extends BaseVdypPolygon<L2, Optional<Float>, S2, I2> & InputPolygon, L2 extends BaseVdypLayer<S2, I2> & InputLayer, S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
			float estimatePercentForestLand(
					P2 polygon, Optional<L2> vetLayer, L2 primaryLayer, ValueOrMarker<Float, Boolean> resultOrIsVeteran
			) {
		if (polygon.getPercentAvailable().isPresent()) {
			return polygon.getPercentAvailable().get();
		}

		assert primaryLayer != null;
		float yieldFactor = polygon.getYieldFactor();

		final boolean veteran;
		{
			if (resultOrIsVeteran.isValue()) {
				return resultOrIsVeteran.getValue().orElseThrow();
			}

			veteran = resultOrIsVeteran.getMarker().orElseThrow();
		}

		float primaryAgeTotal = getLayerAgeTotal(primaryLayer).orElseThrow();

		float crownClosure = crownClosureForPercentForestLand(vetLayer, primaryLayer, veteran, primaryAgeTotal);

		/*
		 * assume that CC occurs at age 25 and that most land goes to 90% occupancy but that occupancy increases only 1%
		 * /yr with no increases after ages 25. });
		 */

		// Obtain the percent yield (in comparison with CC = 90%)

		float crownClosureTop = 90f;
		float breastHeightAge = primaryAgeTotal
				- primaryLayer.getPrimarySite().flatMap(BaseVdypSite::getYearsToBreastHeight).orElseThrow();

		var bec = polygon.getBiogeoclimaticZone();

		breastHeightAge = max(5.0f, breastHeightAge);
		// EMP040
		float baseAreaTop = estimatePrimaryBaseAreaAdjust(
				primaryLayer, bec, yieldFactor, breastHeightAge, 0f, crownClosureTop
		);
		// EMP040
		float baseAreaHat = estimatePrimaryBaseAreaAdjust(
				primaryLayer, bec, yieldFactor, breastHeightAge, 0f, crownClosure
		);

		float percentYield;
		if (baseAreaTop > 0f && baseAreaHat > 0f) {
			percentYield = min(100f, 100f * baseAreaHat / baseAreaTop);
		} else {
			percentYield = 90f;
		}

		float gainMax;
		if (primaryAgeTotal > 125f) {
			gainMax = 0f;
		} else if (primaryAgeTotal < 25f) {
			gainMax = max(90f - percentYield, 0);
		} else {
			gainMax = max(90f - percentYield, 0);
			gainMax = min(gainMax, 125 - primaryAgeTotal);
		}

		return floor(min(percentYield + gainMax, 100f));

	}

	public <P2 extends BaseVdypPolygon<L2, Optional<Float>, S2, I2>, L2 extends BaseVdypLayer<S2, I2> & InputLayer, S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
			ValueOrMarker<Float, Boolean> isVeteranForEstimatePercentForestLand(P2 polygon, Optional<L2> vetLayer) {
		boolean veteran = vetLayer//
				.filter(layer -> getLayerHeight(layer).orElse(0f) > 0f) //
				.filter(layer -> layer.getCrownClosure() > 0f)//
				.isPresent(); // LAYERV

		return FLOAT_OR_BOOL.marker(veteran);
	}

	public <P2 extends BaseVdypPolygon<L2, Optional<Float>, S2, I2>, L2 extends BaseVdypLayer<S2, I2> & InputLayer, S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
			float crownClosureForPercentForestLand(
					Optional<L2> vetLayer, L2 primaryLayer, boolean veteran, float primaryAgeTotal
			) {
		float crownClosure = primaryLayer.getCrownClosure();

		// Assume crown closure linear with age, to 25.
		if (primaryAgeTotal < 25f) {
			crownClosure *= 25f / primaryAgeTotal;
		}
		// define crown closure as the SUM of two layers
		if (veteran) {
			crownClosure += vetLayer.map(InputLayer::getCrownClosure).orElse(0f);
		}

		crownClosure = clamp(crownClosure, 0, 100);
		return crownClosure;
	}

	public <P2 extends BaseVdypPolygon<L2, Optional<Float>, S2, I2>, L2 extends BaseVdypLayer<S2, I2> & InputLayer, S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite>
			Optional<Float> getLayerAgeTotal(L2 layer) {
		return layer.getPrimarySite().flatMap(BaseVdypSite::getAgeTotal);
	}
}
