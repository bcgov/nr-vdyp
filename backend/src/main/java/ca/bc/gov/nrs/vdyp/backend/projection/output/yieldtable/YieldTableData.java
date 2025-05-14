package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class YieldTableData {

	private final Polygon polygon;
	private final LayerReportingInfo layerReportingInfo;

	private Integer referenceYear;
	private Integer referenceAge;
	private Integer measurementYear;
	private Integer measurementAge;
	private Integer currentYear;
	private Integer currentAge;
	private Integer yearAtStartAge;
	private Integer yearAtEndAge;
	private Integer ageAtStartYear;
	private Integer ageAtEndYear;
	private Integer yearAtDeath;
	private Integer ageAtDeath;
	private Integer yearAtGapStart;
	private Integer yearAtGapEnd;
	private Integer ageAtGapStart;
	private Integer ageAtGapEnd;
	private Integer currentTableYear;
	private Integer currentTableAge;
	private Integer currentYearRangeYear;
	private Integer currentAgeRangeYear;
	private Integer currentTableYearToRequest;
	private Integer currentTableAgeToRequest;
	private Boolean currentYearIsYearRow = false;
	private Boolean currentYearIsAgeRow = false;
	private Integer yearYieldsValid;
	// Removed as no longer used: private Integer ageYieldsValid;
	private Integer yearToAgeDifference;
	private Integer numSpecies = 0;
	private Double layerAgeOffset = 0.0;

	ProjectionTypeCode projectionType = ProjectionTypeCode.UNKNOWN;

	List<YieldTableSpeciesDetails> sortedSpeciesArray;

	public YieldTableData(Polygon polygon, LayerReportingInfo layerReportingInfo) {
		this.polygon = polygon;
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
	}

	public Polygon getPolygon() {
		return polygon;
	}

	public LayerReportingInfo getLayerReportingInfo() {
		return layerReportingInfo;
	}

	public boolean isPolygonTable() {
		return layerReportingInfo == null;
	}

	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Integer getReferenceAge() {
		return referenceAge;
	}

	public Integer getMeasurementYear() {
		return measurementYear;
	}

	public Integer getMeasurementAge() {
		return measurementAge;
	}

	public Integer getCurrentYear() {
		return currentYear;
	}

	public Integer getCurrentAge() {
		return currentAge;
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

	public Integer getYearToAgeDifference() {
		return yearToAgeDifference;
	}

	public Integer getNumSpecies() {
		return numSpecies;
	}

	public Double getLayerAgeOffset() {
		return layerAgeOffset;
	}

	public ProjectionTypeCode getProjectionType() {
		return projectionType;
	}

	public List<YieldTableSpeciesDetails> getSortedSpeciesArray() {
		return sortedSpeciesArray;
	}

	public void setReferenceYear(Integer referenceYear) {
		this.referenceYear = referenceYear;
	}

	public void setReferenceAge(Integer referenceAge) {
		this.referenceAge = referenceAge;
	}

	public void setMeasurementYear(Integer measurementYear) {
		this.measurementYear = measurementYear;
	}

	public void setMeasurementAge(Integer measurementAge) {
		this.measurementAge = measurementAge;
	}

	public void setCurrentYear(Integer currentYear) {
		this.currentYear = currentYear;
	}

	public void setCurrentAge(Integer currentAge) {
		this.currentAge = currentAge;
	}

	public void setYearAtStartAge(Integer yearAtStartAge) {
		this.yearAtStartAge = yearAtStartAge;
	}

	public void setYearAtEndAge(Integer yearAtEndAge) {
		this.yearAtEndAge = yearAtEndAge;
	}

	public void setAgeAtStartYear(Integer ageAtStartYear) {
		this.ageAtStartYear = ageAtStartYear;
	}

	public void setAgeAtEndYear(Integer ageAtEndYear) {
		this.ageAtEndYear = ageAtEndYear;
	}

	public void setYearAtDeath(Integer yearAtDeath) {
		this.yearAtDeath = yearAtDeath;
	}

	public void setAgeAtDeath(Integer ageAtDeath) {
		this.ageAtDeath = ageAtDeath;
	}

	public void setYearAtGapStart(Integer yearAtGapStart) {
		this.yearAtGapStart = yearAtGapStart;
	}

	public void setYearAtGapEnd(Integer yearAtGapEnd) {
		this.yearAtGapEnd = yearAtGapEnd;
	}

	public void setAgeAtGapStart(Integer ageAtGapStart) {
		this.ageAtGapStart = ageAtGapStart;
	}

	public void setAgeAtGapEnd(Integer ageAtGapEnd) {
		this.ageAtGapEnd = ageAtGapEnd;
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

	public void setYearYieldsValid(Integer yearYieldsValid) {
		this.yearYieldsValid = yearYieldsValid;
	}

	public void setYearToAgeDifference(Integer yearToAgeDifference) {
		this.yearToAgeDifference = yearToAgeDifference;
	}

	public void setNumSpecies(Integer numSpecies) {
		this.numSpecies = numSpecies;
	}

	public void setLayerAgeOffset(Double layerAgeOffset) {
		this.layerAgeOffset = layerAgeOffset;
	}

	public void setProjectionType(ProjectionTypeCode projectionType) {
		this.projectionType = projectionType;
	}

	public void setSortedSpeciesArray(List<YieldTableSpeciesDetails> sortedSpeciesArray) {
		this.sortedSpeciesArray = sortedSpeciesArray;
	}

	public void calculateTableRangeInformation(ProjectionContext context) {

		assert measurementAge != null && measurementYear != null;

		yearToAgeDifference = measurementYear - measurementAge;

		currentYear = LocalDate.now().getYear();
		currentAge = currentYear - yearToAgeDifference;

		var startAge = context.getValidatedParams().getAgeStart();
		var endAge = context.getValidatedParams().getAgeEnd();
		var startYear = context.getValidatedParams().getYearStart();
		var endYear = context.getValidatedParams().getYearEnd();

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
		assert ageSupplied || yearSupplied /* or both */;

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

			assert ageRangeStartYear < ageRangeEndYear;
			assert yearRangeStartYear < yearRangeEndYear;

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

			switch (context.getValidatedParams().getCombineAgeYearRange()) {
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
	}
}
