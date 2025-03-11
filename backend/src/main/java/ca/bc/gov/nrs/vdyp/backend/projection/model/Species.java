package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.HashMap;
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
	private Stand stand;

	/**
	 * If this represents an Sp0 (a Species Group), this field stores its Sp0 code. If this represents an Sp64, this
	 * field stores its Sp64 code.
	 */
	private String speciesCode;

	// Required Members

	private double speciesPercent = 0.0;

	// Optional Members - never set these directly; always call the setter.

	private Double totalAge;
	private Double ageAtBreastHeight;
	private Double dominantHeight;
	private Double siteIndex;
	private Double yearsToBreastHeight;
	private SiteIndexEquation siteCurve;

	private Double suppliedTotalAge;
	private Double suppliedDominantHeight;
	private Double suppliedSiteIndex;

	// The number of duplicates of this species.

	private Integer nDuplicates = 0;

	/**
	 * For each of the times this species was supplied, this array holds the individual species percents that were
	 * supplied. This is necessary because when projected results are requested, those results will have to be prorated
	 * by the input percent.
	 */
	private Map<Integer /* duplicate number */, Double> percentsPerDuplicate = new HashMap<>();

	private Species() {
	}

	public Stand getStand() {
		return stand;
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

	public Integer getNDuplicates() {
		return nDuplicates;
	}

	public Map<Integer, Double> getPercentsPerDuplicate() {
		return percentsPerDuplicate;
	}

	// MUTABLE field setters

	void setTotalAge(Double totalAge) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.totalAge = totalAge;
		if (this.suppliedTotalAge == null) {
			this.suppliedTotalAge = this.totalAge;
		}
	}

	void setSiteIndex(Double siteIndex) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.siteIndex = siteIndex;
		if (this.suppliedSiteIndex == null) {
			this.suppliedSiteIndex = this.siteIndex;
		}
	}

	public void setDominantHeight(Double dominantHeight) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.dominantHeight = dominantHeight;
		if (this.suppliedDominantHeight == null) {
			this.suppliedDominantHeight = this.dominantHeight;
		}
	}

	public void setYearsToBreastHeight(Double yearsToBreastHeight) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.yearsToBreastHeight = yearsToBreastHeight;
	}

	public void setAgeAtBreastHeight(Double ageAtBreastHeight) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.ageAtBreastHeight = ageAtBreastHeight;
	}

	void resetDominantHeight() {
		stand.getLayer().getPolygon().ensureUnlocked();

		setDominantHeight(null);
	}

	void setSiteCurve(SiteIndexEquation siteCurve) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.siteCurve = siteCurve;
	}

	/**
	 * Adjust the percentage of the species (and the duplicate within the species) by the given amount.
	 *
	 * @param adjustment       the adjustment amount, possibly negative.
	 * @param duplicationIndex the duplication index to adjust. If the species has no duplicates, supply 0.
	 */
	public void adjustSpeciesPercent(double adjustment, int duplicationIndex) {
		stand.getLayer().getPolygon().ensureUnlocked();

		this.speciesPercent += adjustment;
		this.percentsPerDuplicate.put(duplicationIndex, this.percentsPerDuplicate.get(duplicationIndex) + adjustment);
	}

	public static class Builder {
		private Species species = new Species();
		private boolean speciesPercentHasBeenSet = false;

		public Builder stand(Stand stand) {
			species.stand = stand;
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
			species.setAgeAtBreastHeight(ageAtBreastHeight);
			return this;
		}

		public Builder totalAge(Double totalAge) {
			species.setTotalAge(totalAge);
			return this;
		}

		public Builder dominantHeight(Double dominantHeight) {
			species.setDominantHeight(dominantHeight);
			return this;
		}

		public Builder siteIndex(Double siteIndex) {
			species.setSiteIndex(siteIndex);
			return this;
		}

		public Builder yearsToBreastHeight(Double yearsToBreastHeight) {
			species.setYearsToBreastHeight(yearsToBreastHeight);
			return this;
		}

		public Builder siteCurve(SiteIndexEquation siteCurve) {
			species.setSiteCurve(siteCurve);
			return this;
		}

		public Species build() {
			if (species.speciesCode == null) {
				throw new IllegalStateException("Attempt to create a Species with no species code");
			}

			if (species.stand == null) {
				throw new IllegalStateException("Attempt to create a Species with no parent (Stand) component given");
			}

			if (!speciesPercentHasBeenSet) {
				throw new IllegalStateException("Attempt to create a Species with no species percentage given");
			}

			species.percentsPerDuplicate.put(0, species.speciesPercent);

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
		percentsPerDuplicate.put(nDuplicates++, duplicate.getSpeciesPercent());

		if (getTotalAge() == null) {
			setTotalAge(duplicate.getTotalAge());
		}

		if (getDominantHeight() == null) {
			setDominantHeight(duplicate.getDominantHeight());
		}

		if (getSiteIndex() == null) {
			setSiteIndex(duplicate.getSiteIndex());
		}

		if (getYearsToBreastHeight() == null) {
			setYearsToBreastHeight(duplicate.getYearsToBreastHeight());
		}

		if (getAgeAtBreastHeight() == null) {
			setAgeAtBreastHeight(duplicate.getAgeAtBreastHeight());
		}

		if (getSiteCurve() == null) {
			setSiteCurve(duplicate.getSiteCurve());
		}
	}

	/**
	 * A new species has been added to the Stand of which this Species is a Species Group. Update this Species (Group)
	 * to reflect this.
	 *
	 * @param sp64 the sp64 added.
	 */
	public void updateAfterSp64Added(Species sp64) {

		speciesPercent += sp64.speciesPercent;

		assert nDuplicates == 0;
		this.percentsPerDuplicate.put(0, speciesPercent);

		if (totalAge == null && sp64.getTotalAge() != null) {

			ageAtBreastHeight = sp64.getAgeAtBreastHeight();
			dominantHeight = sp64.getDominantHeight();
			siteCurve = sp64.getSiteCurve();
			siteIndex = sp64.getSiteIndex();
			suppliedDominantHeight = sp64.getSuppliedDominantHeight();
			suppliedSiteIndex = sp64.getSiteIndex();
			suppliedTotalAge = sp64.getSuppliedTotalAge();
			totalAge = sp64.getTotalAge();
			yearsToBreastHeight = sp64.getYearsToBreastHeight();
		}
	}

	/**
	 * V7Int_FillInSpeciesComponent
	 *
	 * This species has been added to a layer (and polygon.) Infer the values of undefined fields from the context,
	 * where possible.
	 */
	public void calculateUndefinedFieldValues() {

		Layer layer = this.stand.getLayer();
		Polygon polygon = layer.getPolygon();

		boolean isCoastal = polygon.isCoastal();

		boolean haveComputedSiteIndexCurve = false;
		boolean haveComputedTotalAge = false;
		boolean haveComputedAgeAtBreastHeight = false;
		boolean haveComputedYearsToBreastHeight = false;
		boolean haveComputedDominantHeight = false;
		boolean haveComputedSiteIndex = false;

		boolean keepTrying = true;
		while (keepTrying) {

			keepTrying = false;

			if (siteCurve != null) {

				if (!haveComputedSiteIndex && siteIndex == null && eval(totalAge) >= 30.0 && eval(dominantHeight) > 0) {

					this.setSiteIndex(dominantHeightAndAgeToSiteIndex());

					keepTrying = true;
					haveComputedSiteIndex = true;

					logger.debug(
							"{}: calculated Site Index using dominantHeightAndAgeToSiteIndex with parameters:", this
					);
					logger.debug(
							"   {}: {} ('{}')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
					);
					logger.debug("   {}: {}", "Total Age", totalAge);
					logger.debug("   {}: {}", "Dominant Height", dominantHeight);
					logger.debug("   {}: {}", "Resulting Site Index", siteIndex);
				}

				if (!haveComputedYearsToBreastHeight && yearsToBreastHeight == null && siteIndex != null) {

					try {
						this.setYearsToBreastHeight(SiteTool.yearsToBreastHeight(siteCurve, siteIndex));

						keepTrying = true;
						haveComputedYearsToBreastHeight = true;

						logger.debug(
								"{}: calculated Years to Breast Height using {} with parameters:",
								"SiteTool_YearsToBreastHeight", this
						);
						logger.debug(
								"   {}: {} ('{}')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
						);
						logger.debug("   {}: {}", "Site Index", siteIndex);
						logger.debug("   {}: {}", "Result", yearsToBreastHeight);

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
				if (polygon.getInventoryStandard() == InventoryStandard.FIP && eval(totalAge) >= 0.0
						&& eval(totalAge) - eval(yearsToBreastHeight) <= 0.5
						&& ("NP".equals(polygon.getNonProductiveDescriptor())
								|| "AF".equals(polygon.getNonProductiveDescriptor()))) {

					logger.debug(
							"{}: applying Special Case YTBH Calculation where species has the following attributes:",
							this
					);
					logger.debug("   FIP Inventory Standard");
					logger.debug("   Non-Productive Polygon (%s)", polygon.getInventoryStandard());
					logger.debug("   Total Age (%.3f) - Computed YTBH (%.3f) is <= 0.5", totalAge, yearsToBreastHeight);
					logger.debug("   Applying Special Case YTBH Calculation of: YTBH = MAX( 0.1, TotalAge - 0.6 )");

					double newYearsToBreastHeight = totalAge - 0.6;
					if (newYearsToBreastHeight < 0.1) {
						newYearsToBreastHeight = 0.1;
					}

					setYearsToBreastHeight(newYearsToBreastHeight);

					logger.debug("   (Re)calculated Years to Breast Height to be %.3f", newYearsToBreastHeight);
				}

				if (!haveComputedTotalAge && totalAge == null && eval(dominantHeight) > 0.0 && eval(siteIndex) > 0.0) {

					this.setTotalAge(dominantHeightAndSiteIndexToAge());

					keepTrying = true;
					haveComputedTotalAge = true;

					logger.debug(
							"{}: calculated Total Age using dominantHeightAndSiteIndexToAge with parameters:", this
					);
					logger.debug(
							"   %32s: %s ('%s')", "Site Curve Number", siteCurve, SiteTool.getSICurveName(siteCurve)
					);
					logger.debug("   %32s: %.2d", "Dominant Height", dominantHeight);
					logger.debug("   %32s: %.2d", "Site Index", siteIndex);
					logger.debug("   %32s: %.2d", "Years to Breast Height", yearsToBreastHeight);
					logger.debug("   %32s: %.2d", "Result", totalAge);
				}

				if (!haveComputedDominantHeight && dominantHeight == null && eval(totalAge) > 0.0
						&& eval(siteIndex) > 0.0) {

					setDominantHeight(totalAgeAndSiteIndexToDominantHeight());

					keepTrying = true;
					haveComputedDominantHeight = true;

					logger.debug(
							"{}: calculated Dominant Height using totalAgeAndSiteIndexToDominantHeight with parameters:",
							this
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
						dominantHeight = 0.01;
						logger.debug("   clamped computed Dominant Height to %.2d", dominantHeight);
					}
				}
			} else if (!haveComputedSiteIndexCurve) {

				var shortName = SiteTool.getSpeciesShortName(SiteTool.getSpeciesIndex(speciesCode));
				setSiteCurve(SiteTool.getSICurve(shortName, isCoastal));

				keepTrying = true;
				haveComputedSiteIndexCurve = true;

				logger.debug(
						"{}: determined Site Curve to be: {} {}", this, SiteTool.getSICurveName(siteCurve), siteCurve
				);
			}

			if (!haveComputedTotalAge && totalAge == null && eval(ageAtBreastHeight) > 0.0
					&& eval(yearsToBreastHeight) >= 0.0) {

				var ageAtBreastHeightRef = new Reference<Double>(ageAtBreastHeight);
				var yearsToBreastHeightRef = new Reference<Double>(yearsToBreastHeight);
				var computedTotalAgeRef = new Reference<Double>(Vdyp7Constants.EMPTY_DECIMAL);

				SiteTool.fillInAgeTriplet(computedTotalAgeRef, ageAtBreastHeightRef, yearsToBreastHeightRef);

				setTotalAge(computedTotalAgeRef.get());

				keepTrying = true;
				haveComputedTotalAge = true;

				logger.debug("{}: calculated Total Age to be: %.2f", this, totalAge);
			}

			if (!haveComputedAgeAtBreastHeight && ageAtBreastHeight == null && eval(totalAge) > 0.0
					&& eval(yearsToBreastHeight) >= 0.0) {

				var ageAtBreastHeightRef = new Reference<Double>(Vdyp7Constants.EMPTY_DECIMAL);
				var yearsToBreastHeightRef = new Reference<Double>(yearsToBreastHeight);
				var computedTotalAgeRef = new Reference<Double>(totalAge);

				SiteTool.fillInAgeTriplet(computedTotalAgeRef, ageAtBreastHeightRef, yearsToBreastHeightRef);

				setAgeAtBreastHeight(yearsToBreastHeightRef.get());

				keepTrying = true;
				haveComputedAgeAtBreastHeight = true;

				logger.debug("{}: calculated Years to Breast Height to be: {}", this, yearsToBreastHeightRef.get());
			}
		}

		// If any computations result in values less than zero, reset those to null (i.e., not computed.)

		if (dominantHeight != null && dominantHeight < 0.0) {
			setDominantHeight(null);
		}

		if (siteIndex != null && siteIndex < 0.0) {
			setSiteIndex(null);
		}

		if (totalAge != null && totalAge < 0.0) {
			setTotalAge(null);
		}

		if (yearsToBreastHeight != null && yearsToBreastHeight < 0.0) {
			setYearsToBreastHeight(null);
		}
	}

	private double eval(Double d) {
		return d == null ? Vdyp7Constants.EMPTY_DECIMAL : d;
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
					"{}: site Index of %.2f is less than minimum Site Index of %.2f.", this, siteIndex,
					Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
			);

			computedHeight = dominantHeight;

			Layer layer = stand.getLayer();
			Polygon polygon = layer.getPolygon();
			polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));

			ValidationMessage message = new ValidationMessage(
					ValidationMessageKind.LOW_SITE_INDEX_ERROR, polygon, layer.getLayerId(), siteIndex, this
			);
			polygon.addDefinitionMessage(new PolygonMessage.Builder().layer(layer).message(message).build());

			logger.error(
					"{}: site Index was too low to compute a Dominant Height. Using value {}...", this, computedHeight
			);

		} else {

			if (yearsToBreastHeight == null || yearsToBreastHeight < 0.0) {
				try {
					setYearsToBreastHeight(SiteTool.yearsToBreastHeight(siteCurve, siteIndex));

					logger.debug("{}: years to Breast Height was computed to be {}", this, yearsToBreastHeight);

					computedHeight = SiteTool.ageAndSiteIndexToHeight(
							siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, siteIndex, yearsToBreastHeight
					);

					logger.debug("{}: dominant Height from SiteTools call was computed to be {}", this, computedHeight);

				} catch (CommonCalculatorException e) {
					logger.error(
							"{}: commonCalculatorException encountered in call SiteTool.yearsToBreastHeight({}, {}){}; not computing Years to Breast Height",
							this, siteCurve, siteIndex, e.getMessage() != null ? ": reason " + e.getMessage() : ""
					);
				}
			}
		}

		return computedHeight;
	}

	public Double dominantHeightAndSiteIndexToAge() {

		Double computedHeight = null;

		try {
			setSiteIndex(
					SiteTool.heightAndAgeToSiteIndex(
							siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
							SiteIndexEstimationType.SI_EST_ITERATE
					)
			);

			logger.debug("{}: computed Site Index from SiteTools call: %.2f", this, siteIndex);

			if (siteIndex < Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD) {

				logger.warn(
						"{}: site Index of %.2f is less than minimum Site Index of %.2f; setting Site Index to null",
						this, siteIndex, Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
				);

				Layer layer = stand.getLayer();
				Polygon polygon = layer.getPolygon();
				polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));

				ValidationMessage message = new ValidationMessage(
						ValidationMessageKind.LOW_SITE_INDEX_WARNING, polygon, layer.getLayerId(), siteIndex, this
				);
				polygon.addDefinitionMessage(new PolygonMessage.Builder().layer(layer).message(message).build());

			}
		} catch (CommonCalculatorException e) {
			logger.error(
					"{}: CommonCalculatorException encountered in call SiteTool.heightAndAgeToSiteIndex({}, {}, {}, {}, {}){}; not computing Site Index",
					this, siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
					SiteIndexEstimationType.SI_EST_ITERATE, e.getMessage() != null ? ": reason " + e.getMessage() : ""
			);
		}

		return computedHeight;
	}

	public Double dominantHeightAndAgeToSiteIndex() {
		assert totalAge != null;
		assert dominantHeight != null;

		Double computedSiteIndex = null;
		try {

			computedSiteIndex = SiteTool.heightAndAgeToSiteIndex(
					siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
					SiteIndexEstimationType.SI_EST_ITERATE
			);

			assert computedSiteIndex != null;

			if (computedSiteIndex < Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD) {

				logger.warn(
						"{}: siteIndex of %.2f is less than minimum Site Index of %.2f. Setting siteIndex to null",
						this, computedSiteIndex, Vdyp7Constants.LOW_SITE_INDEX_THRESHOLD
				);

				Layer layer = stand.getLayer();
				Polygon polygon = layer.getPolygon();
				polygon.disableProjectionsOfType(layer.determineProjectionType(polygon));

				ValidationMessage message = new ValidationMessage(
						ValidationMessageKind.LOW_SITE_INDEX_ERROR, polygon, layer.getLayerId(), siteIndex, this
				);
				polygon.addDefinitionMessage(new PolygonMessage.Builder().layer(layer).message(message).build());

				computedSiteIndex = null;
			}
		} catch (CommonCalculatorException e) {
			logger.error(
					"{}: CommonCalculatorException encountered in call SiteTool.heightAndAgeToSiteIndex({}, {}, {}, {}, {}){}; not computing Site Index",
					this, siteCurve, totalAge, SiteIndexAgeType.SI_AT_TOTAL, dominantHeight,
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
		return stand.hashCode() * 17 + speciesCode.hashCode();
	}

	@Override
	public int compareTo(Species that) {
		int standComparisonResult = this.stand.compareTo(that.stand);
		if (standComparisonResult == 0) {
			return this.speciesCode.compareTo(that.speciesCode);
		} else {
			return standComparisonResult;
		}
	}

	@Override
	public String toString() {
		return stand.toString() + " " + speciesCode;
	}
}
