package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDetails;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedEcoZone;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsLiveConversionParams;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

/**
 * Calculates CFS Biomass Volume Details based on the provided EntityVolumeDetails (Polygon or Layer) for either an
 * entire polygon or a specific layer.
 *
 */
public class CfsBiomassCalculator {
	private static final Logger logger = LoggerFactory.getLogger(CfsBiomassCalculator.class);

	/**
	 * Calculates CFS Biomass Volume Details for a Polygon.
	 *
	 * @param volumeData The EntityVolumeDetails containing the raw volume data for a year.
	 * @param polygon    The Polygon for which the biomass is calculated.
	 * @return CfsBiomassVolumeDetails containing the calculated biomass details.
	 */
	public static CfsBiomassVolumeDetails
			calculateBiomassPolygonVolumeDetails(EntityVolumeDetails volumeData, Polygon polygon) {
		CfsBiomassConversionSupportedEcoZone ecoZone = getEcoZoneForPolygon(polygon);
		String mofSp64Code = getSp64Code(polygon, null);
		return calculateCfsBiomass(volumeData, ecoZone, mofSp64Code, polygon);
	}

	/**
	 * Calculates CFS Biomass Volume Details for a specific Layer within a Polygon.
	 *
	 * @param volumeData The EntityVolumeDetails containing the raw volume data for a year.
	 * @param polygon    The Polygon containing the layer.
	 * @param layer      The Layer for which the biomass is calculated.
	 * @return CfsBiomassVolumeDetails containing the calculated biomass details.
	 */
	public static CfsBiomassVolumeDetails
			calculateBiomassLayerVolumeDetails(EntityVolumeDetails volumeData, Polygon polygon, Layer layer) {
		CfsBiomassConversionSupportedEcoZone ecoZone = getEcoZoneForPolygon(polygon);
		if (layer.getIsDeadLayer()) {
			logger.error("Layer is a dead layer. Cannot calculate CFS Biomass for dead layers.");
			return CfsBiomassVolumeDetails.EMPTY;
		}
		String mofSp64Code = getSp64Code(polygon, layer);
		return calculateCfsBiomass(volumeData, ecoZone, mofSp64Code, polygon);
	}

	/**
	 * Determines the CFS EcoZone for a given Polygon.
	 *
	 * @param polygon The Polygon for which the EcoZone is determined.
	 * @return The CfsBiomassConversionSupportedEcoZone corresponding to the Polygon's EcoZone or BEC zone.
	 */
	public static CfsBiomassConversionSupportedEcoZone getEcoZoneForPolygon(Polygon polygon) {
		CfsBiomassConversionSupportedEcoZone ecoZone = CfsBiomassConversionSupportedEcoZone.UNKNOWN;
		if (polygon == null) {
			logger.warn("Polygon is null. Cannot determine EcoZone.");
			return ecoZone;
		}
		if (polygon.getCfsEcoZone() != null) {
			ecoZone = CfsBiomassConversionSupportedEcoZone.of(polygon.getCfsEcoZone().getCode());
			if (ecoZone != CfsBiomassConversionSupportedEcoZone.UNKNOWN) {
				logger.debug("Using CFS EcoZone from Polygon: {}", ecoZone);
			}
		}
		if (ecoZone == CfsBiomassConversionSupportedEcoZone.UNKNOWN) {

			logger.debug("Attempt to convert BEC zone to CFS EcoZone for Polygon: {}", polygon.getBecZone());
			ecoZone = CfsBiomassConversionSupportedEcoZone
					.fromBecZoneData(polygon.getBecZone(), null, null, polygon.getMapSheet());
			if (ecoZone != CfsBiomassConversionSupportedEcoZone.UNKNOWN) {
				logger.debug(
						"CFS EcoZone determined from BEC zone {} and MapSheet {}: {}", polygon.getBecZone(),
						polygon.getMapSheet(), ecoZone
				);
			} else {
				logger.error(
						"CFS EcoZone could not be determined for Polygon: {}. Using UNKNOWN EcoZone.",
						polygon.getBecZone()
				);
			}
		}
		return ecoZone;
	}

	public static String getSp64Code(Polygon polygon, Layer layer) {
		String sp64Code = null;
		if (layer == null && polygon != null) {
			layer = polygon.getPrimaryLayer();
		}
		if (layer != null) {
			Species species = layer.determineLeadingSp64(0);
			sp64Code = species != null ? species.getSpeciesCode() : null;
		} else {
			logger.error("Could not determine a primary layer for Polygon: {}. Cannot determine SP64 code.", polygon);
		}
		return sp64Code;
	}

	enum CfsBioProportionToUse {
		UNKNOWN, LOW_BOUND, COMPUTED, HIGH_BOUND
	}

	/**
	 * Calculates CFS Biomass Volume Details based on the provided EntityVolumeDetails, EcoZone, and species code.
	 * Currently in VDYP7 all calculations are performed for daead biomass proportions but then the results are
	 * suppressed. For the time being the dead calculations will not run in this version.
	 *
	 * @param volumeData EntityVolumeData that contains volume details for the layer or polygon
	 * @param ecoZone    The CfsBiomassConversionSupportedEcoZone for which the biomass is calculated.
	 * @param sp64Code   The species code used to look up conversion parameters.
	 * @param polygon    The polygon in case and messages need to be written to it
	 * @return CfsBiomassVolumeDetails containing the calculated biomass details.
	 */
	public static CfsBiomassVolumeDetails calculateCfsBiomass(
			EntityVolumeDetails volumeData, CfsBiomassConversionSupportedEcoZone ecoZone, String sp64Code,
			Polygon polygon
	) {

		CfsBiomassConversionCoefficientsDetails liveParams = SiteTool.lookupLiveCfsConversionParams(ecoZone, sp64Code);
		// Commented out because dead biomass is suppressed in the end in VDYP7
		// CfsBiomassConversionCoefficientsDetails deadParams = SiteTool.lookupDeadCfsConversionParams(ecoZone,
		// sp64Code);
		if (liveParams == null || !liveParams.containsData()) {
			logger.warn(
					"CFS Biomass Live Conversion Parameters not found for EcoZone: {} and Species Code: {}. Cannot calculate CFS Biomass",
					ecoZone, sp64Code
			);
			return CfsBiomassVolumeDetails.EMPTY;
		}
		double volume = volumeData.closeUtilizationVolume();
		if (volume == 0.0) {
			logger.debug("Supplied Volume is zero, so must CFS Biomass values and proportions.");
			return CfsBiomassVolumeDetails.ZERO_VOLUME;
		}

		CfsBioProportionToUse propToUse = determineCfsBioProportionToUse(volume, liveParams);
		double biomassMerchVolume = calculateCfsBiomassMerchVolume(volume, liveParams);
		double biomassNonMerchVolume = calculateCfsBiomassNonMerchVolume(liveParams, biomassMerchVolume);
		double totalBio = biomassMerchVolume + biomassNonMerchVolume;
		double biomassSaplingVolume = calculateCfsBiomassSaplingVolume(liveParams, totalBio);
		// Commented out because value was not used in the end in VDYP7
		// double propDead = calculateCfsBiomassPropDead(deadParams, volume);

		double propStemwood;
		double propBark;
		double propBranches;
		double propFoliage;
		if (propToUse == CfsBioProportionToUse.COMPUTED) {
			// Compute the proportions of the biomass components
			double aSum = liveParams.getParm(CfsLiveConversionParams.A1)
					+ (liveParams.getParm(CfsLiveConversionParams.A2) * volume)
					+ (liveParams.getParm(CfsLiveConversionParams.A3) * Math.log(5.0 + volume));
			double bSum = liveParams.getParm(CfsLiveConversionParams.B1)
					+ (liveParams.getParm(CfsLiveConversionParams.B2) * volume)
					+ (liveParams.getParm(CfsLiveConversionParams.B3) * Math.log(5.0 + volume));
			double cSum = liveParams.getParm(CfsLiveConversionParams.C1)
					+ (liveParams.getParm(CfsLiveConversionParams.C2) * volume)
					+ (liveParams.getParm(CfsLiveConversionParams.C3) * Math.log(5.0 + volume));

			double aExpr = Math.exp(aSum);
			double bExpr = Math.exp(bSum);
			double cExpr = Math.exp(cSum);

			double denom = 1.0 + aExpr + bExpr + cExpr;
			propStemwood = 1 / denom;
			propBark = aExpr / denom;
			propBranches = bExpr / denom;
			propFoliage = cExpr / denom;
		} else if (propToUse == CfsBioProportionToUse.LOW_BOUND) {
			logger.debug("Using Low Bound Proportions for CFS Biomass calculation.");
			// use minimum values
			propStemwood = liveParams.getParm(CfsLiveConversionParams.LOW_STEMWOOD_PROP);
			propBark = liveParams.getParm(CfsLiveConversionParams.LOW_STEMBARK_PROP);
			propBranches = liveParams.getParm(CfsLiveConversionParams.LOW_BRANCHES_PROP);
			propFoliage = liveParams.getParm(CfsLiveConversionParams.LOW_FOLIAGE_PROP);
		} else if (propToUse == CfsBioProportionToUse.HIGH_BOUND) {
			// use maximum values
			logger.debug("Using High Bound Proportions for CFS Biomass calculation.");
			propStemwood = liveParams.getParm(CfsLiveConversionParams.HIGH_STEMWOOD_PROP);
			propBark = liveParams.getParm(CfsLiveConversionParams.HIGH_STEMBARK_PROP);
			propBranches = liveParams.getParm(CfsLiveConversionParams.HIGH_BRANCHES_PROP);
			propFoliage = liveParams.getParm(CfsLiveConversionParams.HIGH_FOLIAGE_PROP);
		} else {
			logger.error("Invalid CfsBioProportionToUse: {}. Cannot calculate CFS Biomass proportions.", propToUse);
			propStemwood = propBark = propBranches = propFoliage = -9.0;
		}

		double sum = propStemwood + propBark + propBranches + propFoliage;
		if (propStemwood < 0 || propBark < 0 || propBranches < 0 || propFoliage < 0) {
			logger.warn(
					PolygonMessageKind.CFS_BIO_NOT_ALL_PROP_CALCULATED.getTemplate(), propStemwood, propBark,
					propBranches, propFoliage
			);
			polygon.addMessage(
					new PolygonMessage.Builder().polygon(polygon)
							.details(
									ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
									PolygonMessageKind.CFS_BIO_NOT_ALL_PROP_CALCULATED, propStemwood, propBark,
									propBranches, propFoliage
							).build()
			);

		} else if (Math.abs(1.0 - sum) > 1.0E-5) {
			logger.error(PolygonMessageKind.CFS_BIO_PROP_DO_NOT_SUM_TO_ONE.getTemplate(), sum);
			polygon.addMessage(
					new PolygonMessage.Builder().polygon(polygon)
							.details(
									ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
									PolygonMessageKind.CFS_BIO_PROP_DO_NOT_SUM_TO_ONE, sum
							).build()
			);
		}

		double bioStemwood = -9.0; // default to error code
		double bioBark = -9.0;
		double bioBranches = -9.0;
		double bioFoliage = -9.0;
		if (biomassMerchVolume >= 0.0) {
			bioStemwood = biomassMerchVolume + biomassNonMerchVolume + biomassSaplingVolume;
		}
		if (bioStemwood >= 0.0 && propStemwood >= 0) {
			if (propBark > 0.0)
				bioBark = (bioStemwood / propStemwood) * propBark;
			if (propBranches > 0.0)
				bioBranches = (bioStemwood / propStemwood) * propBranches;
			if (propFoliage > 0.0)
				bioFoliage = (bioStemwood / propStemwood) * propFoliage;
		}
		// Dead biomass is calculated but never used in VDYP7, so we will not calculate it here unless we decide to use
		// it
		double bioDead = -9.0; // propDead * (bioStemwood / propStemwood);

		return new CfsBiomassVolumeDetails(
				biomassMerchVolume, biomassNonMerchVolume, biomassSaplingVolume, propStemwood, propBark, propBranches,
				propFoliage, bioStemwood, bioBark, bioBranches, bioFoliage, bioDead
		);
	}

	/**
	 * Determines the proportion of CFS Biomass to use based on the close utilization volume and conversion
	 * coefficients. This influencing factor is if the close Utilization Volume it outside of the minimum or maximum
	 * thresholds.
	 * 
	 * @param volume       Close utilization volume
	 * @param coefficients The CfsBiomassConversionCoefficientsDetails containing the conversion parameters.
	 * @return The CfsBioProportionToUse indicating which proportions to use.
	 */
	private static CfsBioProportionToUse
			determineCfsBioProportionToUse(double volume, CfsBiomassConversionCoefficientsDetails coefficients) {
		if (volume < coefficients.getParm(CfsLiveConversionParams.MIN_VOLUME)) {
			logger.warn(
					"CFS Biomass close utilization volume {} is below minimum threshold {}. Use Low Bound Proportions.",
					volume, coefficients.getParm(CfsLiveConversionParams.MIN_VOLUME)
			);
			return CfsBioProportionToUse.LOW_BOUND;
		}
		if (volume > coefficients.getParm(CfsLiveConversionParams.MAX_VOLUME)) {
			logger.warn(
					"CFS Biomass close utilization volume {} is above maximum threshold {}. Use High Bound Proportions.",
					volume, coefficients.getParm(CfsLiveConversionParams.MAX_VOLUME)
			);
			return CfsBioProportionToUse.HIGH_BOUND;
		}
		return CfsBioProportionToUse.COMPUTED;
	}

	/**
	 * Calculates the merchantable biomass volume based on the close utilization volume and conversion coefficients.
	 *
	 * @param volume       Close utilization volume
	 * @param coefficients The CfsBiomassConversionCoefficientsDetails containing the conversion parameters.
	 * @return The calculated merchantable biomass volume.
	 */
	private static double
			calculateCfsBiomassMerchVolume(double volume, CfsBiomassConversionCoefficientsDetails coefficients) {

		if (volume < 0) {
			logger.error(
					"CFS Biomass close utilization volume not supplied: {}. Returning -9.0 to indicate error.", volume
			);
			return -9.0;
		}
		// Compute it
		return coefficients.getParm(CfsLiveConversionParams.A)
				* Math.pow(volume, coefficients.getParm(CfsLiveConversionParams.B));
	}

	/**
	 * Calculates the non-merchantable biomass volume based on the merchantable biomass volume and conversion
	 * coefficients.
	 *
	 * @param coefficients       The CfsBiomassConversionCoefficientsDetails containing the conversion parameters.
	 * @param biomassMerchVolume The merchantable biomass volume.
	 * @return The calculated non-merchantable biomass volume.
	 */
	private static double calculateCfsBiomassNonMerchVolume(
			CfsBiomassConversionCoefficientsDetails coefficients, double biomassMerchVolume
	) {
		double min = 1.0;
		double biomassNonMerchFactor = coefficients.getParm(CfsLiveConversionParams.K_NONMERCH)
				+ coefficients.getParm(CfsLiveConversionParams.A_NONMERCH)
						* Math.pow(biomassMerchVolume, coefficients.getParm(CfsLiveConversionParams.B_NONMERCH));

		biomassNonMerchFactor = Math
				.max(min, Math.min(coefficients.getParm(CfsLiveConversionParams.CAP_NONMERCH), biomassNonMerchFactor));
		return (biomassNonMerchFactor * biomassMerchVolume) - biomassMerchVolume;
	}

	/**
	 * Calculates the sapling biomass volume based on the total biomass and conversion coefficients.
	 *
	 * @param coefficients The CfsBiomassConversionCoefficientsDetails containing the conversion parameters.
	 * @param totalBio     The total biomass volume (merchantable + non-merchantable).
	 * @return The calculated sapling biomass volume.
	 */
	private static double
			calculateCfsBiomassSaplingVolume(CfsBiomassConversionCoefficientsDetails coefficients, double totalBio) {
		double min = 1.0;
		double saplingFactor = coefficients.getParm(CfsLiveConversionParams.K_SAP)
				+ coefficients.getParm(CfsLiveConversionParams.A_SAP)
						* Math.pow(totalBio, coefficients.getParm(CfsLiveConversionParams.B_SAP));
		saplingFactor = Math.max(min, Math.min(coefficients.getParm(CfsLiveConversionParams.CAP_SAP), saplingFactor));

		return (saplingFactor * totalBio) - totalBio;
	}

	/**
	 * Calculates the proportion of dead biomass based on the close utilization volume and conversion coefficients.
	 *
	 * @param coefficients The CfsBiomassConversionCoefficientsDetails containing the conversion parameters.
	 * @param volumeData   The EntityVolumeDetails containing the close utilization volume.
	 * @return The calculated proportion of dead biomass.
	 */
	private static double calculateCfsBiomassPropDead(
			CfsBiomassConversionCoefficientsDetails coefficients, EntityVolumeDetails volumeData
	) {
		return -9.0; // simply return error code until we wish to actually use this output
						// Implementation below
		/*
		 * double volume = volumeData.closeUtilizationVolume(); if (volume < 0) { return -9.0; } double
		 * volIntervalStart; double volIntervalEnd; double propIntervalStart; double propIntervalEnd; if (volume <
		 * coefficients.getParm(CfsDeadConversionParams.V1)) { volIntervalStart = 0.0; volIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.V1); propIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.PROP1); propIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.PROP2); } else if (volume <
		 * coefficients.getParm(CfsDeadConversionParams.V2)) { volIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.V1); volIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.V2); propIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.PROP2); propIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.PROP3); } else if (volume <
		 * coefficients.getParm(CfsDeadConversionParams.V3)) { volIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.V2); volIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.V3); propIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.PROP3); propIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.PROP4); } else if (volume <
		 * coefficients.getParm(CfsDeadConversionParams.V4)) { volIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.V3); volIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.V4); propIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.PROP4); propIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.PROP5); } else{ volIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.V4); volIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.V4); propIntervalStart =
		 * coefficients.getParm(CfsDeadConversionParams.PROP5); propIntervalEnd =
		 * coefficients.getParm(CfsDeadConversionParams.PROP5); }
		 * 
		 * if (Math.abs(volIntervalEnd - volIntervalStart) <= 1.0E-5) { // If the volume is exactly equal to the end of
		 * the interval, return the end of the interval return propIntervalEnd; }
		 * 
		 * double volFactor = (volume - volIntervalStart)/(volIntervalEnd - volIntervalStart); return propIntervalStart
		 * + (volFactor * (propIntervalEnd - propIntervalStart));
		 * 
		 */

	}

}
