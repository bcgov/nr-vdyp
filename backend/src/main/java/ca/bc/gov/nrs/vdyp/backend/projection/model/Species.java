package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvLayerRecordBean.SpeciesDetails;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEstimationType;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

/**
 * Holds all the information surrounding a single species component.
 */
public class Species implements Comparable<Species> {

	private static final Logger logger = LoggerFactory.getLogger(Species.class);

	// BUSINESS KEY - all fields not nullable

	/** The Stand containing the Species. */
	private Stand parentComponent;

	private String speciesCode;

	// Required Members

	private double speciesPercent = 0.0;
	
	// Optional Members
	
	private Double totalAge;
	private Double ageAtBreastHeight;
	private Double dominantHeight;
	private Double siteIndex;
	private Double yearsToBreastHeight;
	private SiteIndexEquation siteCurve;

	private Double suppliedTotalAge;
	private Double suppliedDominantHeight;
	private Double suppliedSiteIndex;

	private Integer numTimesSupplied = 0;

	/**
	 * For each of the times this species was supplied, this array holds the individual species percents that were
	 * supplied. This is necessary because when projected results are requested, those results will have to be prorated
	 * by the input percent.
	 */
	private Map<Integer /* duplicate number */, Double> percentsPerDuplicate;

	private Species() {
	}

	public Stand getParentComponent() {
		return parentComponent;
	}

	public String getSpeciesCode() {
		return speciesCode;
	}

	public double getSpeciesPercent() {
		return speciesPercent;
	}

	public Double getTotalAge() {
		return totalAge;
	}

	public Double getAgeAtBreastHeight() {
		return ageAtBreastHeight;
	}

	public Double getDominantHeight() {
		return dominantHeight;
	}

	public Double getSiteIndex() {
		return siteIndex;
	}

	public Double getYearsToBreastHeight() {
		return yearsToBreastHeight;
	}

	public SiteIndexEquation getSiteCurve() {
		return siteCurve;
	}

	public Double getSuppliedTotalAge() {
		return suppliedTotalAge;
	}

	public Double getSuppliedDominantHeight() {
		return suppliedDominantHeight;
	}

	public Double getSuppliedSiteIndex() {
		return suppliedSiteIndex;
	}

	public Integer getNumTimesSupplied() {
		return numTimesSupplied;
	}

	public Map<Integer, Double> getPercentsPerDuplicate() {
		return percentsPerDuplicate;
	}

	public static class Builder {
		private Species species = new Species();
		private boolean speciesPercentHasBeenSet = false;

		public Builder parentComponent(Stand parentComponent) {
			species.parentComponent = parentComponent;
			return this;
		}

		public Builder speciesCode(String speciesCode) {
			species.speciesCode = speciesCode;
			return this;
		}

		public Builder speciesPercent(double speciesPercent) {
			species.speciesPercent = speciesPercent;
			speciesPercentHasBeenSet = true;
			return this;
		}
		
		public Builder ageAtBreastHeight(Double ageAtBreastHeight) {
			species.ageAtBreastHeight = ageAtBreastHeight;
			return this;
		}

		public Builder totalAge(Double totalAge) {
			species.totalAge = totalAge;
			return this;
		}

		public Builder dominantHeight(Double dominantHeight) {
			species.dominantHeight = dominantHeight;
			return this;
		}

		public Builder siteIndex(Double siteIndex) {
			species.siteIndex = siteIndex;
			return this;
		}

		public Builder yearsToBreastHeight(Double yearsToBreastHeight) {
			species.yearsToBreastHeight = yearsToBreastHeight;
			return this;
		}

		public Builder siteCurve(SiteIndexEquation siteCurve) {
			species.siteCurve = siteCurve;
			return this;
		}

		public Builder suppliedTotalAge(Double suppliedTotalAge) {
			species.suppliedTotalAge = suppliedTotalAge;
			return this;
		}

		public Builder suppliedDominantHeight(Double suppliedDominantHeight) {
			species.suppliedDominantHeight = suppliedDominantHeight;
			return this;
		}

		public Builder suppliedSiteIndex(Double suppliedSiteIndex) {
			species.suppliedSiteIndex = suppliedSiteIndex;
			return this;
		}

		public Builder percentsPerDuplicate(Map<Integer, Double> percentsPerDuplicate) {
			species.percentsPerDuplicate = percentsPerDuplicate;
			return this;
		}

		public Species build() {
			if (species.speciesCode == null) {
				throw new IllegalStateException("Attempt to create a Species with no species code");
			}

			if (species.parentComponent == null) {
				throw new IllegalStateException("Attempt to create a Species with no parent (Stand) component given");
			}
			
			if (! speciesPercentHasBeenSet) {
				throw new IllegalStateException("Attempt to create a Species with no species percentage given");
			}

			return species;
		}
	}

	/**
	 * Determine if the Site information for this species is equivalent to that of <code>other</code>, a
	 * <code>SpeciesDetails</code> instance. Note that SpeciesDetail does not contain a full set of Site information -
	 * only age and dominant height, and then only for certain species of a layer. Fields that are <code>other</code> in
	 * either are considered equivalent.
	 * 
	 * @param other the SpeciesDetails object to compare against
	 * @return as described
	 * @see SpeciesDetails
	 */
	public boolean equivalentSiteInfo(SpeciesDetails other) {

		return equivalentSiteInfoValue(this.totalAge, other.estimatedAge())
				&& equivalentSiteInfoValue(this.dominantHeight, other.estimatedHeight());
	}

	/**
	 * Determine if the Site information for this species is equivalent to that of <code>other</code>. Fields that are
	 * <code>other</code> in either are considered equivalent.
	 * 
	 * @param other the Species object to compare against
	 * @return as described
	 */
	public boolean equivalentSiteInfo(Species other) {

		return equivalentSiteInfoValue(this.totalAge, other.totalAge)
				&& equivalentSiteInfoValue(this.dominantHeight, other.dominantHeight)
				&& equivalentSiteInfoValue(this.siteIndex, other.siteIndex)
				&& equivalentSiteInfoValue(this.yearsToBreastHeight, other.yearsToBreastHeight)
				&& equivalentSiteInfoValue(this.ageAtBreastHeight, other.ageAtBreastHeight)
				&& equivalentSiteInfoValue(this.siteCurve, other.siteCurve);
	}

	private <T> boolean equivalentSiteInfoValue(T d1, T d2) {
		return d1 == null || d2 == null || d1.equals(d2);
	}

	/**
	 * A duplicate of an existing species (sp64) has been supplied for a stand. Merge the duplicate into this species.
	 * 
	 * @param duplicate
	 */
	public void addDuplicate(Species duplicate) {

		speciesPercent += duplicate.getSpeciesPercent();
		percentsPerDuplicate.put(numTimesSupplied++, duplicate.getSpeciesPercent());

		if (getTotalAge() == null) {
			totalAge = duplicate.totalAge;
		}

		if (getDominantHeight() == null) {
			dominantHeight = duplicate.getDominantHeight();
		}

		if (getSiteIndex() == null) {
			siteIndex = duplicate.getSiteIndex();
		}

		if (getYearsToBreastHeight() == null) {
			yearsToBreastHeight = duplicate.getYearsToBreastHeight();
		}

		if (getAgeAtBreastHeight() == null) {
			ageAtBreastHeight = duplicate.getAgeAtBreastHeight();
		}

		if (getSiteCurve() == null) {
			siteCurve = duplicate.getSiteCurve();
		}

		if (getSuppliedTotalAge() == null) {
			suppliedTotalAge = duplicate.getSuppliedTotalAge();
		}

		if (getSuppliedDominantHeight() == null) {
			yearsToBreastHeight = duplicate.getSuppliedDominantHeight();
		}

		if (getSuppliedSiteIndex() == null) {
			suppliedSiteIndex = duplicate.getSuppliedSiteIndex();
		}
	}

	/**
	 * A new species has been added to the Stand of which this Species is the Species Group. Update this Species (Group)
	 * to reflect this.
	 * 
	 * @param species the sp64 added.
	 */
	public void updateAfterSpeciesAdded(Species species) {

		speciesPercent += species.speciesPercent;

		if (totalAge == null && species.getTotalAge() != null) {

			totalAge = species.getTotalAge();
			dominantHeight = species.getDominantHeight();
			siteCurve = species.siteCurve;
			ageAtBreastHeight = species.ageAtBreastHeight;
			yearsToBreastHeight = species.getYearsToBreastHeight();
			siteIndex = species.getSiteIndex();

			suppliedTotalAge = species.getSuppliedTotalAge();
			suppliedDominantHeight = species.getSuppliedDominantHeight();
			suppliedSiteIndex = species.getSiteIndex();
		}
	}

	/**
	 * This species has been added to a layer (and polygon.) Infer the values of undefined fields from the context,
	 * where possible.
	 */
	public void calculateUndefinedFieldValues() {

		Layer layer = this.parentComponent.getParentComponent();
		Polygon polygon = layer.getPolygon();

		boolean isCoastal = polygon.isCoastal();

		boolean haveComputedSiteIndexCurve = false;
		boolean haveComputedTotalAge = false;
		double lTotalAge = totalAge == null ? -9.0 : totalAge;
		boolean haveComputedAgeAtBreastHeight = false;
		double lAgeAtBreastHeight = ageAtBreastHeight == null ? -9.0 : ageAtBreastHeight;
		boolean haveComputedYearsToBreastHeight = false;
		double lYearsToBreastHeight = yearsToBreastHeight == null ? -9.0 : yearsToBreastHeight;
		boolean haveComputedDominantHeight = false;
		double lDominantHeight = dominantHeight == null ? -9.0 : dominantHeight;
		boolean haveComputedSiteIndex = false;
		double lSiteIndex = siteIndex == null ? -9.0 : siteIndex;

		boolean keepTrying = true;
		while (keepTrying) {

			keepTrying = false;

			if (siteCurve == null) {

				if (!haveComputedSiteIndex && lSiteIndex == -9.0 && lTotalAge >= 30.0 && lDominantHeight > 0) {

					this.siteIndex = dominantHeightAndAgeToSiteIndex();

					keepTrying = true;
					haveComputedSiteIndex = true;

					logger.debug("Calculated Site Index using {} with parameters:", "dominantHeightAndAgeToSiteIndex");
					logger.debug(
							"   %32s: %s ('%s')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
					);
					logger.debug("   %32s: %.2d", "Total Age", totalAge);
					logger.debug("%32s: %.2d", "Dominant Height", dominantHeight);
					logger.debug("   %32s: %.2d", "Result", siteIndex);
				}

				if (!haveComputedYearsToBreastHeight && yearsToBreastHeight == null && siteIndex != null) {

					try {
						this.yearsToBreastHeight = SiteTool.yearsToBreastHeight(siteCurve, siteIndex);

						keepTrying = true;
						haveComputedYearsToBreastHeight = true;

						logger.debug(
								"Calculated Years to Breast Height using {} with parameters:",
								"SiteTool_YearsToBreastHeight"
						);
						logger.debug(
								"   %32s: %s ('%s')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
						);
						logger.debug("   %32s: %.2d", "Site Index", siteIndex);
						logger.debug("   %32s: %.2d", "Result", yearsToBreastHeight);

					} catch (CommonCalculatorException e) {
						logger.error(
								"CommonCalculatorException encountered in call SiteTool.yearsToBreastHeight({}, {}){}; not computing Years to Breast Height",
								siteCurve, siteIndex, e.getMessage() != null ? ": reason " + e.getMessage() : ""
						);
					}
				}

				// When we encounter FIPSTART Non-Productive polygons, apply the following condition:
				//
				// If Total_Age - YTBH <= 0.5 then
				// YTBH = MAX (0.1 , TotalAge - 0.6)
				if (polygon.getInventoryStandard() == InventoryStandard.FIP && lTotalAge >= 0.0
						&& yearsToBreastHeight != null && totalAge - yearsToBreastHeight <= 0.5
						&& ("NP".equals(polygon.getNonProductiveDescriptor())
								|| "AF".equals(polygon.getNonProductiveDescriptor()))) {

					logger.debug("Applying Special Case YTBH Calculation where species has the following attributes:");
					logger.debug("   FIP Inventory Standard");
					logger.debug("   Non-Productive Polygon (%s)", polygon.getInventoryStandard());
					logger.debug("   Total Age (%.3f) - Computed YTBH (%.3f) is <= 0.5", totalAge, yearsToBreastHeight);
					logger.debug("Applying Special Case YTBH Calculation of:");
					logger.debug("   YTBH = MAX( 0.1, TotalAge - 0.6 )");

					this.yearsToBreastHeight = totalAge - 0.6;
					if (yearsToBreastHeight < 0.1) {
						yearsToBreastHeight = 0.1;
					}

					logger.debug("   Years to Breast Height = %.3f", yearsToBreastHeight);
				}

				if (!haveComputedTotalAge && totalAge == null && lDominantHeight > 0.0 && lSiteIndex > 0.0) {

					this.totalAge = dominantHeightAndSiteIndexToAge();

					keepTrying = true;
					haveComputedTotalAge = true;

					logger.debug("Calculated Total Age using {} with parameters:", "dominantHeightAndSiteIndexToAge");
					logger.debug(
							"   %32s: %s ('%s')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
					);
					logger.debug("%32s: %.2d", "Dominant Height", dominantHeight);
					logger.debug("   %32s: %.2d", "Site Index", siteIndex);
					logger.debug("   %32s: %.2d", "Years to Breast Height", yearsToBreastHeight);
					logger.debug("   %32s: %.2d", "Result", totalAge);
				}

				if (!haveComputedDominantHeight && dominantHeight == null && lTotalAge > 0.0 && lSiteIndex > 0.0) {

					dominantHeight = totalAgeAndSiteIndexToDominantHeight();

					keepTrying = true;
					haveComputedDominantHeight = true;

					logger.debug(
							"Calculated Dominant Height using {} with parameters:",
							"totalAgeAndSiteIndexToDominantHeight"
					);
					logger.debug(
							"   %32s: %s ('%s')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
					);
					logger.debug("   %32s: %.2d", "Total Age", totalAge);
					logger.debug("   %32s: %.2d", "Site Index", siteIndex);
					logger.debug("   %32s: %.2d", "Years to Breast Height", yearsToBreastHeight);
					logger.debug("   %32s: %.2d", "Result", dominantHeight);

					// If the calculated height is <= 0.0, substitute a nominal height
					if (dominantHeight <= 0.0) {
						dominantHeight = 0.1;
						logger.debug("Clamped computed Dominant Height to %.2d", dominantHeight);
					}
				}
			} else if (!haveComputedSiteIndexCurve) {

				var shortName = SiteTool.getSpeciesShortName(SiteTool.getSpeciesIndex(speciesCode));
				siteCurve = SiteTool.getSICurve(shortName, isCoastal);

				keepTrying = true;
				haveComputedSiteIndexCurve = true;

				logger.debug("Determined Site Curve to be: {} {}", SiteTool.getSICurveName(siteCurve), siteCurve);
			}

			if (!haveComputedTotalAge && totalAge == null && lAgeAtBreastHeight > 0.0 && lYearsToBreastHeight >= 0.0) {

				var ageAtBreastHeightRef = new Reference<Double>(lAgeAtBreastHeight);
				var yearsToBreastHeightRef = new Reference<Double>(lYearsToBreastHeight);
				var computedTotalAgeRef = new Reference<Double>(-9.0);

				SiteTool.fillInAgeTriplet(computedTotalAgeRef, ageAtBreastHeightRef, yearsToBreastHeightRef);

				this.totalAge = computedTotalAgeRef.get();

				logger.debug("Calculated Total Age to be: %.2f", totalAge);
			}

			if (!haveComputedAgeAtBreastHeight && ageAtBreastHeight == null && lTotalAge > 0.0
					&& lYearsToBreastHeight >= 0.0) {

				var ageAtBreastHeightRef = new Reference<Double>(-9.0);
				var yearsToBreastHeightRef = new Reference<Double>(lYearsToBreastHeight);
				var computedTotalAgeRef = new Reference<Double>(lTotalAge);

				SiteTool.fillInAgeTriplet(computedTotalAgeRef, ageAtBreastHeightRef, yearsToBreastHeightRef);

				this.ageAtBreastHeight = yearsToBreastHeightRef.get();

				logger.debug("Calculated Years to Breast Height to be: %.2f", yearsToBreastHeightRef);
			}
		}

		// If any computations result in values less than zero, reset those to null (i.e., not computed.)

		if (dominantHeight != null && dominantHeight < 0.0) {
			dominantHeight = null;
		}

		if (siteIndex != null && siteIndex < 0.0) {
			siteIndex = null;
		}

		if (totalAge != null && totalAge < 0.0) {
			totalAge = null;
		}

		if (yearsToBreastHeight != null && yearsToBreastHeight < 0.0) {
			yearsToBreastHeight = null;
		}
	}

	/**
	 * Compute Dominant Height given an Age and Site Index.
	 * 
	 * <p>
	 * This routine is a wrapper to the SiteTool library that adds some additional business logic around computing
	 * height taking advantage of some information available to it that is not available in the SiteTool library.
	 * <p>
	 * These business rules are as follows (see IPSCB220.doc and refer to the section pertaining to polygon
	 * DCK/092G046/0247):
	 * <p>
	 * When Site Index returned by SINDEX is < 2.0m, then
	 * <ul>
	 * <li>set Site Index to NULL
	 * <li>return projected age as usual
	 * <li>return projected height as equal to input height
	 * <li>do not submit polygon to VRISTART or FIPSTART
	 * <li>set per Hectare yields to null
	 * <li>issue an appropriate warning message
	 * </ul>
	 * 
	 * @return the calculated Dominant Height and null if the height could not be computed.
	 */
	private Double totalAgeAndSiteIndexToDominantHeight() {
		Double computedHeight = null;

		assert totalAge != null;
		assert siteCurve != null;

		if (siteIndex == null || siteIndex < Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD) {

			logger.debug(
					"Site Index of %.2f is less than minimum Site Index of %.2f.", siteIndex,
					Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
			);

			computedHeight = dominantHeight;

			Layer layer = parentComponent.getParentComponent();
			Polygon polygon = layer.getPolygon();
			polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));

			ValidationMessage message = new ValidationMessage(ValidationMessageKind.LOW_SITE_INDEX_ERROR, layer, siteIndex, this);
			polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());

			logger.error("Site Index was too low to compute a Dominant Height. Using value %.1f", computedHeight);

		} else {

			if (yearsToBreastHeight == null || yearsToBreastHeight < 0.0) {
				try {
					yearsToBreastHeight = SiteTool.yearsToBreastHeight(siteCurve, siteIndex);

					logger.debug("Years to Breast Height was computed to be %.1f", yearsToBreastHeight);

					computedHeight = SiteTool.ageAndSiteIndexToHeight(
							siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, siteIndex, yearsToBreastHeight
					);

					logger.debug("Dominant Height from SiteTools call was computed to be %.1f", computedHeight);

				} catch (CommonCalculatorException e) {
					logger.error(
							"CommonCalculatorException encountered in call SiteTool.yearsToBreastHeight({}, {}){}; not computing Years to Breast Height",
							siteCurve, siteIndex, e.getMessage() != null ? ": reason " + e.getMessage() : ""
					);
				}
			}
		}

		return computedHeight;
	}

	private Double dominantHeightAndSiteIndexToAge() {

		Double computedHeight = null;

		try {
			siteIndex = SiteTool.heightAndAgeToSiteIndex(
					siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
					SiteIndexEstimationType.SI_EST_ITERATE
			);

			logger.debug("Computed Site Index from SiteTools call: %.2f", siteIndex);

			if (siteIndex < Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD) {

				logger.warn(
						"Site Index of %.2f is less than minimum Site Index of %.2f; setting Site Index to null", siteIndex,
						Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
				);

				Layer layer = parentComponent.getParentComponent();
				Polygon polygon = layer.getPolygon();
				polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));

				ValidationMessage message = new ValidationMessage(ValidationMessageKind.LOW_SITE_INDEX_WARNING, layer, siteIndex, this);
				polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());

			}
		} catch (CommonCalculatorException e) {
			logger.error(
					"CommonCalculatorException encountered in call SiteTool.heightAndAgeToSiteIndex({}, {}, {}, {}, {}){}; not computing Site Index",
					siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
					SiteIndexEstimationType.SI_EST_ITERATE, e.getMessage() != null ? ": reason " + e.getMessage() : ""
			);
		}

		return computedHeight;
	}

	private Double dominantHeightAndAgeToSiteIndex() {
		assert totalAge != null;
		assert dominantHeight != null;

		Double computedSiteIndex = null;
		try {

			computedSiteIndex = SiteTool.heightAndAgeToSiteIndex(siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight, SiteIndexEstimationType.SI_EST_ITERATE);

			assert computedSiteIndex != null;
			
			if (computedSiteIndex < Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD) {
	
				logger.warn(
						"Site Index of %.2f is less than minimum Site Index of %.2f. Setting siteIndex to null",
						computedSiteIndex, Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
				);
	
				Layer layer = parentComponent.getParentComponent();
				Polygon polygon = layer.getPolygon();
				polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));
	
				ValidationMessage message = new ValidationMessage(ValidationMessageKind.LOW_SITE_INDEX_ERROR, layer, siteIndex, this);
				polygon.getMessages().add(new PolygonMessage.Builder().setLayer(layer).setMessage(message).build());
			
				computedSiteIndex = null;
			}
		} catch (CommonCalculatorException e) {
			logger.error(
					"CommonCalculatorException encountered in call SiteTool.heightAndAgeToSiteIndex({}, {}, {}, {}, {}){}; not computing Site Index",
					siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
					SiteIndexEstimationType.SI_EST_ITERATE, e.getMessage() != null ? ": reason " + e.getMessage() : ""
			);
		}

		return computedSiteIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Species that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return parentComponent.hashCode() * 17 + speciesCode.hashCode();
	}

	@Override
	public int compareTo(Species that) {
		int standComparisonResult = this.parentComponent.compareTo(that.parentComponent);
		if (standComparisonResult == 0) {
			return this.speciesCode.compareTo(that.speciesCode);
		} else {
			return standComparisonResult;
		}
	}

	@Override
	public String toString() {
		return parentComponent.toString() + " " + speciesCode;
	}
}
