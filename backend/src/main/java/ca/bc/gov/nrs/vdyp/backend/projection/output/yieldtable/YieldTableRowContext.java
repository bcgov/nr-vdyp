package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.Validate;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

class YieldTableRowContext {

	/** the Polygon for which this row was generated */
	private final Polygon polygon;
	/** the state of the projection at completion of projection */
	private final PolygonProjectionState state;

	/**
	 * the Layer to which this applies. This -may be null-, indicating the yield table is summarizing polygon
	 * information only
	 */
	private final LayerReportingInfo layerReportingInfo;

	/** The reference calendar year for the current table. */
	private int referenceYear;
	/** The age corresponding to referenceYear. */
	private int referenceAge;

	/** The reference year or year of death, whichever is lower. */
	private int measurementYear;
	/** The age corresponding to measurementYear. */
	private int measurementAge;

	/** The calendar year today. */
	private int nowYear;
	/** The age corresponding to nowYear. */
	private int nowAge;

	/** The amount to subtract from a calendar year to convert it to a yield table age. */
	private int yearToAgeDifference;
	/** The number of species found in the current yield table. */
	private int numSpecies;
	/** Records the number of years from the current layer's age to the stand total age. */
	private int layerAgeOffset;

	/** The calendar year at the start of the age range. */
	private Integer yearAtStartAge;
	/** The age corresponding to yearAtStartAge. */
	private Integer ageAtStartYear;
	/** The calendar year at the end of the age range. */
	private Integer yearAtEndAge;
	/** The age corresponding to yearAtEndAge. */
	private Integer ageAtEndYear;

	/** The calendar year when the layer was killed. If still alive, null */
	private Integer yearAtDeath;
	/** The age corresponding to yearAtDeath; null if none. */
	private Integer ageAtDeath;

	/**
	 * Marks the start of the yield table gap (if one is required based on the age and year ranges and the combine
	 * operator). Otherwise null.
	 */
	private Integer yearAtGapStart;
	/** Marks the end of the yield table gap, if any. Otherwise null. */
	private Integer yearAtGapEnd;
	/** The age corresponding to yearAtGapStart; null if none. */
	private Integer ageAtGapStart;
	/** The age corresponding to yearAtGapEnd; null if none. */
	private Integer ageAtGapEnd;

	/** The year of the current yield table row being generated. */
	private Integer currentTableYear;
	/** The age corresponding to currentTableAge; null if none. */
	private Integer currentTableAge;

	/** The current year as we progress through each of the two ranges while producing the yield tables. */
	private Integer currentYearRangeYear;
	/** The age corresponding to currentYearRangeYear. */
	private Integer currentAgeRangeYear;

	/**
	 * For the current table year, these members represent the year data to request. Normally the year will match the
	 * current table year, however this mechanism allows us to select a different year's data for the current table row.
	 * This is handy for the Veteran layer where we want to always display data from the reference year despite the year
	 * of the yield table being displayed.
	 */
	private Integer currentTableYearToRequest;
	/** The age corresponding to currentTableAgeToRequest. */
	private Integer currentTableAgeToRequest;

	/** Indicates whether or not the current year corresponds to a year range/increment */
	private Boolean currentYearIsYearRow;
	/** Indicates whether or not the current year corresponds to a age range/increment */
	private Boolean currentYearIsAgeRow;

	/** The calendar year the yields first become valid. */
	private Integer yearYieldsValid;
	// Removed as no longer used: private Integer ageYieldsValid;

	/** The method by which the current layer was projected. */
	private ProjectionTypeCode projectionType = ProjectionTypeCode.UNKNOWN;

	private final List<YieldTableSpeciesDetails> sortedSpeciesArray;

	public static YieldTableRowContext of(
			ProjectionContext context, Polygon polygon, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo
	) {
		return new YieldTableRowContext(context, polygon, state, layerReportingInfo);
	}

	private YieldTableRowContext(
			ProjectionContext context, Polygon polygon, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo
	) {
		Validate.notNull(polygon, "YieldTable.obtainStandYield(): polygon must not be null");
		Validate.notNull(state, "YieldTable.obtainStandYield(): state must not be null");

		this.polygon = polygon;
		this.state = state;

		this.layerReportingInfo = layerReportingInfo;
		this.sortedSpeciesArray = new ArrayList<>();

		if (this.layerReportingInfo != null) {
			for (var species : layerReportingInfo.getOrderedSpecies()) {
				var speciesDetails = new YieldTableSpeciesDetails(
						species.getSp64Name(), species.getAsSuppliedIndex(), species.getSp64Percent()
				);
				this.sortedSpeciesArray.add(speciesDetails);
			}
		}

		this.referenceYear = polygon.getReferenceYear();

		calculateMeasurementInformation();

		calculateTableRangeInformation(context.getParams());

		calculateLayerAgeOffsets();

		validate();
	}

	public void validate() {
		assertBothNullOrPredicateTrue(yearAtStartAge, yearAtEndAge, (a, b) -> a < b);
		assertBothNullOrPredicateTrue(yearAtGapStart, yearAtGapEnd, (a, b) -> a < b);
		assertBothNullOrPredicateTrue(ageAtStartYear, ageAtEndYear, (a, b) -> a < b);
		assertBothNullOrPredicateTrue(ageAtGapStart, ageAtGapEnd, (a, b) -> a < b);
		assertBothNullOrPredicateTrue(currentTableYear, currentTableAge, (a, b) -> a == b + yearToAgeDifference);
		assertEqualNullity(currentYearIsAgeRow, currentYearIsYearRow);
		Validate.isTrue(referenceYear == referenceAge + yearToAgeDifference);
		Validate.isTrue(measurementYear == measurementAge + yearToAgeDifference);
		Validate.isTrue(nowYear == nowAge + yearToAgeDifference);
		Validate.isTrue(numSpecies >= 0);
		Validate.isTrue(layerAgeOffset == 0.0);
	}

	private <T> void assertBothNullOrPredicateTrue(T a, T b, BiFunction<T, T, Boolean> f) {
		assertEqualNullity(yearAtStartAge, yearAtEndAge);
		Validate.isTrue(a == null || f.apply(a, b), MessageFormat.format("f({0}, {1}) is false", a, b));
	}

	private void assertEqualNullity(Object o1, Object o2) {
		Validate.isTrue(o1 == o2 || (o1 != null && o2 != null), "the nullity of o1 is opposite to that of o2");
	}

	private void calculateMeasurementInformation() {
		Integer yearOfDeath = null;

		Layer layer;
		if (isPolygonTable()) {
			layer = null;

			try {
				Double ageAtYear = polygon.determineStandAgeAtYear(getReferenceYear());
				this.referenceAge = ageAtYear == null ? null : ageAtYear.intValue();
			} catch (PolygonValidationException e) {
				// There can't possibly still be a validation failure at this point.
				throw new IllegalStateException(new YieldTableGenerationException(e));
			}

			var primaryLayer = polygon.getPrimaryLayer();
			yearOfDeath = primaryLayer.getYearOfDeath();
			if (yearOfDeath == null) {
				yearOfDeath = polygon.getYearOfDeath();
			}

			numSpecies = 0;
		} else {
			layer = layerReportingInfo.getLayer();

			projectionType = layerReportingInfo.getProcessedAsVDYP7Layer();

			yearOfDeath = layer.getYearOfDeath();
			if (yearOfDeath == null) {
				yearOfDeath = polygon.getYearOfDeath();
			}

			if (projectionType == ProjectionTypeCode.DEAD) {

				if (yearOfDeath != null) {
					var layerAgeAtDeath = layer.determineLayerAgeAtYear(yearOfDeath);
					ageAtDeath = layerAgeAtDeath == null ? null : layerAgeAtDeath.intValue();
				}
			}

			Double ageAtYear = layer.determineLayerAgeAtYear(getReferenceYear());
			referenceAge = ageAtYear == null ? null : ageAtYear.intValue();

			numSpecies = layer.getSp64sAsSupplied().size();
		}

		yearAtDeath = yearOfDeath;

		// In case the polygon has a dead stem layer and the year of death
		// occurred after the reference year, use the year of death as the
		// measurement year.
		//
		// Note that this needs to be an explicitly separate calculation from
		// the section that sets the '.yearAtDeath' member because that attribute
		// should only be set for the Dead Stem Layer. This is so the yield table
		// generator can mark the row as a Year of Death

		if (yearOfDeath != null && yearOfDeath > getReferenceYear()) {

			Double relevantAge;
			if (isPolygonTable()) {
				try {
					relevantAge = polygon.determineStandAgeAtYear(yearOfDeath);
				} catch (PolygonValidationException e) {
					// There can't possibly still be a validation failure at this point.
					throw new IllegalStateException(new YieldTableGenerationException(e));
				}
			} else {
				Validate.isTrue(layer != null, "YieldTableRowIterator.createTableRow(): layer is null");
				relevantAge = layer.determineLayerAgeAtYear(yearOfDeath);
			}

			measurementYear = yearOfDeath;
			measurementAge = relevantAge == null ? null : relevantAge.intValue();
		} else {
			measurementYear = referenceYear;
			measurementAge = referenceAge;
		}

		yearToAgeDifference = measurementYear - measurementAge;

		nowYear = LocalDate.now().getYear();
		nowAge = nowYear - yearToAgeDifference;
	}

	private void calculateTableRangeInformation(ValidatedParameters params) {

		var startAge = params.getAgeStart();
		var endAge = params.getAgeEnd();
		var startYear = params.getYearStart();
		var endYear = params.getYearEnd();

		// Start the range arithmetic.
		// Case 1:
		// Age Range supplied. Year Range not supplied.
		// Age Range defines the resulting range.
		//
		// Case 2:
		// Age Range not supplied. Year Range supplied.
		// Year Range defines the resulting range.
		//
		// Case 3:
		// Both ranges supplied.
		// Range combination logic must be applied.

		boolean ageSupplied = startAge != null && endAge != null;
		boolean yearSupplied = startYear != null && endYear != null;

		Validate.isTrue(
				ageSupplied || yearSupplied,
				"YieldTableRow.calculateTableRangeInformation(): neither age nor year was supplied"
		);

		if (/* case 1 */ ageSupplied && !yearSupplied) {
			this.ageAtStartYear = startAge;
			this.ageAtEndYear = endAge;

			this.yearAtStartAge = startAge + yearToAgeDifference;
			this.yearAtEndAge = endAge + yearToAgeDifference;
		} else if (/* case 2 */ yearSupplied && !ageSupplied) {
			this.yearAtStartAge = startYear;
			this.yearAtEndAge = endYear;

			this.ageAtStartYear = startYear - yearToAgeDifference;
			this.ageAtEndYear = endYear - yearToAgeDifference;
		} else /* case 3 - both supplied */ {

			// For each of the Combine Operators, the following options to be
			// considered:
			// 1. yearStart < ageStart and yearEnd < ageStart
			// 2. yearStart < ageStart and yearEnd == ageStart
			// 3. yearStart < ageStart and yearEnd < ageEnd
			// 4. yearStart < ageStart and yearEnd == ageEnd
			// 5. yearStart < ageStart and yearEnd > ageEnd
			// 6. yearStart == ageStart and yearEnd < ageEnd
			// 7. yearStart == ageStart and yearEnd == ageEnd
			// 8. yearStart == ageStart and yearEnd > ageEnd
			// 9. yearStart > ageStart and yearEnd < ageEnd
			// 10. yearStart > ageStart and yearEnd == ageEnd
			// 11. yearStart > ageStart and yearEnd > ageEnd
			// 12. yearStart == ageEnd and yearEnd > ageEnd
			// 13. yearStart > ageEnd and yearEnd > ageEnd

			int ageRangeStartYear = startAge + yearToAgeDifference;
			int ageRangeEndYear = endAge + yearToAgeDifference;
			int yearRangeStartYear = startYear;
			int yearRangeEndYear = endYear;

			Validate.isTrue(
					ageRangeStartYear < ageRangeEndYear,
					MessageFormat.format(
							"YieldTableRow.calculateTableRangeInformation(): {0} was not less than {1}",
							ageRangeStartYear, ageRangeEndYear
					)
			);
			Validate.isTrue(
					ageRangeStartYear < ageRangeEndYear,
					MessageFormat.format(
							"YieldTableRow.calculateTableRangeInformation(): {0} was not less than {1}",
							yearRangeStartYear, yearRangeEndYear
					)
			);

			int range1Start;
			int range1End;
			int range2Start;
			int range2End;

			/* case 1 */
			if (yearRangeEndYear < ageRangeStartYear) {
				range1Start = yearRangeStartYear;
				range1End = yearRangeEndYear;

				range2Start = ageRangeStartYear;
				range2End = ageRangeEndYear;
			}

			/* case 13 */
			else if (yearRangeStartYear > ageRangeEndYear) {
				range1Start = ageRangeStartYear;
				range1End = ageRangeEndYear;

				range2Start = yearRangeStartYear;
				range2End = yearRangeEndYear;
			}

			/* cases 2, 3, 4, 6, 7 */
			else if (yearRangeStartYear <= ageRangeStartYear && yearRangeEndYear <= ageRangeEndYear) {
				range1Start = yearRangeStartYear;
				range1End = ageRangeEndYear;

				range2Start = ageRangeStartYear;
				range2End = yearRangeEndYear;
			}

			/* cases 5, 8 */
			else if (yearRangeStartYear <= ageRangeStartYear && yearRangeEndYear > ageRangeEndYear) {
				range1Start = yearRangeStartYear;
				range1End = yearRangeEndYear;

				range2Start = ageRangeStartYear;
				range2End = ageRangeEndYear;
			}

			/* cases 9, 10 */
			else if (yearRangeStartYear > ageRangeStartYear && yearRangeEndYear <= ageRangeEndYear) {
				range1Start = ageRangeStartYear;
				range1End = ageRangeEndYear;

				range2Start = yearRangeStartYear;
				range2End = yearRangeEndYear;
			}

			/* cases 11, 12 */
			else if (yearRangeStartYear <= ageRangeEndYear && yearRangeEndYear > ageRangeEndYear) {
				range1Start = ageRangeStartYear;
				range1End = yearRangeEndYear;

				range2Start = yearRangeStartYear;
				range2End = ageRangeEndYear;
			}

			else {
				throw new IllegalStateException(
						MessageFormat.format(
								"Year start/end ({0}/{1}) and Age start/end ({2}/{3}) relationship incorrectly not implemented",
								yearRangeStartYear, yearRangeEndYear, ageRangeStartYear, ageRangeEndYear
						)
				);
			}

			// Combine the two ranges.
			//
			// At the end of the above process, one of two case will occur:
			//
			// 1. If the supplied ranges are disjoint, then Range 1 and Range 2 represent the lesser and the greater of
			// the two ranges.
			//
			// 2. Otherwise, Range 1 spans the minimum year to the maximum year. Range 2 spans the interior end points
			// of the overlap.

			switch (params.getCombineAgeYearRange()) {
			case INTERSECT:
				if (range1End < range2Start) {
					/* Disjoint: The intersection is the empty range. */

					yearAtStartAge = null;
					yearAtEndAge = null;

					yearAtGapStart = null;
					yearAtGapEnd = null;
				} else {
					/* Overlapping: The intersection is the interior end points of the overlap. There is no gap. */
					yearAtStartAge = range2Start;
					yearAtEndAge = range2End;

					yearAtGapStart = null;
					yearAtGapEnd = null;
				}
				break;

			case UNION:
				if (range1End < range2Start) {
					/* Disjoint: The union is the sum of the two ranges with the gap in between. */
					yearAtStartAge = range1Start;
					yearAtEndAge = range2End;

					yearAtGapStart = range1End;
					yearAtGapEnd = range2Start;
				} else {
					/* Overlapping: The union is the range 1 (the external end points of the range). There is no gap. */
					yearAtStartAge = range1Start;
					yearAtEndAge = range1End;

					yearAtGapStart = null;
					yearAtGapEnd = null;
				}

				break;

			case DIFFERENCE:
				if (range1End < range2Start) {
					/*
					 * Disjoint: The difference is the set of years not in common. For disjoint sets, this is the same
					 * as the union.
					 */

					yearAtStartAge = range1Start;
					yearAtEndAge = range2End;

					yearAtGapStart = range1End;
					yearAtGapEnd = range2Start;

				} else {
					/* Overlapping: The difference are the years lying outside of the interior points of the overlap. */

					if ( (range1Start < range2Start) && (range1End > range2End)) {
						yearAtStartAge = range1Start;
						yearAtEndAge = range1End;

						yearAtGapStart = range2Start - 1;
						yearAtGapEnd = range2End + 1;
					} else if ( (range1Start < range2Start) && (range1End == range2End)) {
						yearAtStartAge = range1Start;
						yearAtEndAge = range2Start - 1;

						yearAtGapStart = null;
						yearAtGapEnd = null;
					} else if ( (range1Start == range2Start) && (range1End > range2End)) {
						yearAtStartAge = range2End + 1;
						yearAtEndAge = range1End;

						yearAtGapStart = null;
						yearAtGapEnd = null;
					} else {
						yearAtStartAge = null;
						yearAtEndAge = null;

						yearAtGapStart = null;
						yearAtGapEnd = null;
					}
				}

				break;
			}
		}

		if (yearAtDeath != null) {
			yearAtStartAge = yearAtDeath;
			yearAtEndAge = yearAtDeath;
		}

		if (yearAtStartAge != null) {
			ageAtStartYear = yearAtStartAge - yearToAgeDifference;
		}
		if (yearAtEndAge != null) {
			ageAtEndYear = yearAtEndAge - yearToAgeDifference;
		}
		if (yearAtGapStart != null) {
			ageAtGapStart = yearAtGapStart - yearToAgeDifference;
		}
		if (yearAtGapEnd != null) {
			ageAtGapEnd = yearAtGapEnd - yearToAgeDifference;
		}
	}

	/**
	 * from yldtable.c lines 3241 - 3356
	 * <p>
	 * 2005/03/28: Added some logic to make the VDYP7CORE more in keeping with how VDYP7Batch generates its yield
	 * tables.
	 * <p>
	 * Comments taken from that source code:
	 * <p>
	 * 2004/11/17: According to Cam's Nov.9, 2004 e-mail, we want the yield table to reflect the ages of the primary
	 * species within the particular layer. Further, there should be no age corrections made to adjust the displayed age
	 * to be relative to the primary layer age.
	 * <p>
	 * 2004/11/25: Further to the above note, the primary species for a layer is the species VDYP7CORE determines to be
	 * the primary species at reference age rather than the leading species as supplied.
	 * <p>
	 * 2007/02/10: According recent telephone conversations and e-mails, we are going to disable the layer offset
	 * calculations. The main reason for this is because the years for which you want a projection may not have been
	 * computed. Further, by adjusting backwards, you may require BACKGROW to have run while it had explicitly been set
	 * to not run or it ran resulting in an error.
	 * <p>
	 * The solution here is to leave the age correction logic inside the code so that it can be re-activated easily and
	 * leave the age offset at zero so that there is no effect due to age correction.
	 * <p>
	 * By way of example:
	 * <p>
	 * A primary and secondary species differ in age by 10 years. The stand is projected over the time range of the
	 * primary species. VDYP7 determines the secondary species to be the leading species. The arithmetic to get the year
	 * at which the secondary species could require a projection for the stand before or after the range of years over
	 * which the stand was originally projected.
	 */
	private void calculateLayerAgeOffsets() {
		layerAgeOffset = 0;
	}

	public Polygon getPolygon() {
		return polygon;
	}

	// NOTE:
	// Some getters that return an unboxed version of their value, such as an int rather than
	// the field type of Integer. (It is asserted that) such fields will have a value at the
	// time the method is called.

	public PolygonProjectionState getPolygonProjectionState() {
		return state;
	}

	public LayerReportingInfo getLayerReportingInfo() {
		return layerReportingInfo;
	}

	public boolean isPolygonTable() {
		return layerReportingInfo == null;
	}

	public int getReferenceYear() {
		return referenceYear;
	}

	public int getReferenceAge() {
		return referenceAge;
	}

	public int getMeasurementYear() {
		return measurementYear;
	}

	public int getMeasurementAge() {
		return measurementAge;
	}

	public int getNowYear() {
		return nowYear;
	}

	public int getNowAge() {
		return nowAge;
	}

	public Integer getYearAtStartAge() {
		return yearAtStartAge;
	}

	public Integer getYearAtEndAge() {
		return yearAtEndAge;
	}

	public Integer getAgeAtStartYear() {
		return ageAtStartYear;
	}

	public Integer getAgeAtEndYear() {
		return ageAtEndYear;
	}

	public Integer getYearAtDeath() {
		return yearAtDeath;
	}

	public Integer getAgeAtDeath() {
		return ageAtDeath;
	}

	public Integer getYearAtGapStart() {
		return yearAtGapStart;
	}

	public Integer getYearAtGapEnd() {
		return yearAtGapEnd;
	}

	public Integer getAgeAtGapStart() {
		return ageAtGapStart;
	}

	public Integer getAgeAtGapEnd() {
		return ageAtGapEnd;
	}

	public Integer getCurrentTableYear() {
		return currentTableYear;
	}

	public Integer getCurrentTableAge() {
		return currentTableAge;
	}

	public Integer getCurrentYearRangeYear() {
		return currentYearRangeYear;
	}

	public Integer getCurrentAgeRangeYear() {
		return currentAgeRangeYear;
	}

	public Integer getCurrentTableYearToRequest() {
		return currentTableYearToRequest;
	}

	public Integer getCurrentTableAgeToRequest() {
		return currentTableAgeToRequest;
	}

	public Boolean getCurrentYearIsYearRow() {
		return currentYearIsYearRow;
	}

	public Boolean getCurrentYearIsAgeRow() {
		return currentYearIsAgeRow;
	}

	public Integer getYearYieldsValid() {
		return yearYieldsValid;
	}

	public int getYearToAgeDifference() {
		return yearToAgeDifference;
	}

	public int getNumSpecies() {
		return numSpecies;
	}

	public Integer getLayerAgeOffset() {
		return layerAgeOffset;
	}

	public ProjectionTypeCode getProjectionType() {
		return projectionType;
	}

	public List<YieldTableSpeciesDetails> getSortedSpeciesArray() {
		return Collections.unmodifiableList(sortedSpeciesArray);
	}

	public void setCurrentAgeRangeYear(Integer currentAgeRangeYear) {
		this.currentAgeRangeYear = currentAgeRangeYear;
	}

	public void setCurrentTableYearToRequest(Integer currentTableYearToRequest) {
		this.currentTableYearToRequest = currentTableYearToRequest;
	}

	public void setCurrentTableAgeToRequest(Integer currentTableAgeToRequest) {
		this.currentTableAgeToRequest = currentTableAgeToRequest;
	}

	public void setCurrentYearIsYearRow(Boolean currentYearIsYearRow) {
		this.currentYearIsYearRow = currentYearIsYearRow;
	}

	public void setCurrentYearIsAgeRow(Boolean currentYearIsAgeRow) {
		this.currentYearIsAgeRow = currentYearIsAgeRow;
	}

	public void setCurrentTableYear(Integer currentTableYear) {
		this.currentTableYear = currentTableYear;
	}

	public void setCurrentTableAge(Integer currentTableAge) {
		this.currentTableAge = currentTableAge;
	}

	public void setCurrentYearRangeYear(Integer currentYearRangeYear) {
		this.currentYearRangeYear = currentYearRangeYear;
	}

	public void setLayerAgeOffset(Integer layerAgeOffset) {
		this.layerAgeOffset = layerAgeOffset;
	}
}
