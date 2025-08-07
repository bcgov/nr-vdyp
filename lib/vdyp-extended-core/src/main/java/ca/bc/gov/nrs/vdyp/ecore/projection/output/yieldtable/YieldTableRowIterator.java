package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;

class YieldTableRowIterator implements Iterator<YieldTableRowContext> {

	private final ProjectionContext context;
	private YieldTableRowContext rowContext;
	private final int ageIncrement;
	private boolean rowIsCurrent;

	public YieldTableRowIterator(
			ProjectionContext context, Polygon polygon, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo
	) {
		this(context, polygon, state, layerReportingInfo, null);
	}

	public YieldTableRowIterator(
			ProjectionContext context, Polygon polygon, PolygonProjectionState state,
			LayerReportingInfo layerReportingInfo, Integer ageIncrementOverride
	) {
		this.context = context;
		this.rowContext = YieldTableRowContext.of(context, polygon, state, layerReportingInfo);
		if (ageIncrementOverride != null) {
			ageIncrement = ageIncrementOverride;
		} else {
			ageIncrement = context.getParams().getAgeIncrement();
		}
		establishStartRow();
	}

	@Override
	public boolean hasNext() {
		if (!rowIsCurrent && rowContext.getCurrentTableYear() != null) {
			advanceToNextRow();
		}
		return rowIsCurrent;
	}

	@Override
	public YieldTableRowContext next() {
		if (rowIsCurrent) {
			rowIsCurrent = false;
			return rowContext;
		} else {
			throw new NoSuchElementException("YieldTableRowIterator.next() called when there is no next row");
		}
	}

	/**
	 * <b>lcl_ComputeTableStartYearAge</b>
	 * <p>
	 * Determine the starting point for the yield table.
	 * <p>
	 * Note:
	 * <p>
	 * Because of the possibility of combined ranges, resulting gaps in the combined ranges, the ability to force a
	 * reference year and a current year row, the determination of the current year
	 */
	private void establishStartRow() {

		var params = context.getParams();

		if (rowContext.getYearAtDeath() == null) {

			rowContext.setCurrentYearRangeYear(params.getYearStart());
			rowContext.setCurrentAgeRangeYear(
					params.getAgeStart() == null ? null : params.getAgeStart() + rowContext.getYearToAgeDifference()
			);
			rowContext.setCurrentTableYear(rowContext.getYearAtStartAge());

			if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {
				if (rowContext.getCurrentTableYear() == null
						|| rowContext.getCurrentTableYear() > rowContext.getMeasurementYear()) {
					rowContext.setCurrentTableYear(rowContext.getMeasurementYear());
				}
			}

			if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {
				if (rowContext.getCurrentTableYear() == null
						|| rowContext.getCurrentTableYear() > rowContext.getNowYear()) {
					rowContext.setCurrentTableYear(rowContext.getNowYear());
				}
			}

			if (params.getYearForcedIntoYieldTable() != null) {
				if (rowContext.getCurrentTableYear() == null
						|| rowContext.getCurrentTableYear() > params.getYearForcedIntoYieldTable()) {
					rowContext.setCurrentTableYear(params.getYearForcedIntoYieldTable());
				}
			}

		} else {
			rowContext.setCurrentYearRangeYear(rowContext.getYearAtDeath());
			rowContext.setCurrentAgeRangeYear(rowContext.getAgeAtDeath());
			rowContext.setCurrentTableYear(rowContext.getYearAtDeath());
		}

		// Find out to which range (year or age) the current year corresponds.

		rowContext.setCurrentYearIsYearRow(
				rowContext.getCurrentYearRangeYear() != null && rowContext.getCurrentTableYear() != null
						&& rowContext.getCurrentYearRangeYear().equals(rowContext.getCurrentTableYear())
		);
		rowContext.setCurrentYearIsAgeRow(
				rowContext.getCurrentAgeRangeYear() != null && rowContext.getCurrentTableYear() != null
						&& rowContext.getCurrentAgeRangeYear().equals(rowContext.getCurrentTableYear())
		);

		// Compute the total stand age of the current year.

		if (rowContext.getCurrentTableYear() != null) {
			rowContext.setCurrentTableAge(rowContext.getCurrentTableYear() - rowContext.getYearToAgeDifference());
		}

		// Compute the year data to request based on the current table year.

		rowContext.setCurrentTableYearToRequest(rowContext.getCurrentTableYear());
		rowContext.setCurrentTableAgeToRequest(rowContext.getCurrentTableAge());

		// Finally, is the set of rows empty or not?
		rowIsCurrent = rowContext.getCurrentTableYear() != null;

		rowContext.validate();
	}

	/**
	 * <b>lcl_ComputeNextTableYearAge</b>
	 * <p>
	 * Determine the next year of the yield table to be produced.
	 * <p>
	 * Because of the potential folding of the year and age ranges as well as the possibility of forcing a reference
	 * and/or current year, the next year computation is not straight-forward.
	 * <p>
	 * 2013/08/10: Trap for the condition of Increment parameter being '0' and treat as '1' (the same as if it were
	 * supplied as '-9'). This is important to avoid an infinite loop situation that occurs for DCSV output. In that
	 * case, if the '-inc' command line parameter is supplied, it reaches here as '0'.
	 */
	private void advanceToNextRow() {
		var params = context.getParams();

		boolean bAnotherRowToBePrinted = true;
		boolean hasSetNextAge = false;

		// Advance the current table year 1 year to force a recalculation of the
		// next table year. If we don't then it could easily be that the current
		// age range or year range falls exactly on the current year and no actual
		// advancement would take place. By advancing the minimum step, we avoid
		// that problem.

		rowContext.setCurrentTableYear(rowContext.getCurrentTableYear() + 1);

		// If we are above the year range current year, advance that counter.

		while (rowContext.getCurrentYearRangeYear() != null
				&& rowContext.getCurrentYearRangeYear() < rowContext.getCurrentTableYear()) {
			rowContext.setCurrentYearRangeYear(rowContext.getCurrentYearRangeYear() + ageIncrement);
		}

		if (rowContext.getCurrentYearRangeYear() != null
				&& (params.getYearEnd() == null || rowContext.getCurrentYearRangeYear() > params.getYearEnd())) {
			rowContext.setCurrentYearRangeYear(null);
		}

		// If we are above the age range current year, advance that counter.

		while (rowContext.getCurrentAgeRangeYear() != null
				&& rowContext.getCurrentAgeRangeYear() < rowContext.getCurrentTableYear()) {
			rowContext.setCurrentAgeRangeYear(rowContext.getCurrentAgeRangeYear() + ageIncrement);
		}

		if (rowContext.getCurrentAgeRangeYear() != null && (params.getAgeEnd() == null
				|| rowContext.getCurrentAgeRangeYear() > params.getAgeEnd() + rowContext.getYearToAgeDifference())) {
			rowContext.setCurrentAgeRangeYear(null);
		}

		// Now find the next lowest target year we will want to display.

		Integer candidateYear = null;

		if (rowContext.getCurrentYearRangeYear() != null) {
			candidateYear = rowContext.getCurrentYearRangeYear();
			hasSetNextAge = true;
		}

		if (rowContext.getCurrentAgeRangeYear() != null
				&& (candidateYear == null || rowContext.getCurrentAgeRangeYear() <= candidateYear)) {
			candidateYear = rowContext.getCurrentAgeRangeYear();
			hasSetNextAge = true;
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& (rowContext.getCurrentTableYear() <= rowContext.getMeasurementYear())
				&& (candidateYear == null || rowContext.getMeasurementYear() < candidateYear)) {
			candidateYear = rowContext.getMeasurementYear();
			hasSetNextAge = true;
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& rowContext.getCurrentTableYear() <= rowContext.getNowYear()
				&& (candidateYear == null || rowContext.getNowYear() < candidateYear)) {
			candidateYear = rowContext.getNowYear();
			hasSetNextAge = true;
		}

		if (params.getYearForcedIntoYieldTable() != null
				&& rowContext.getCurrentTableYear() <= params.getYearForcedIntoYieldTable()
				&& (candidateYear == null || params.getYearForcedIntoYieldTable() < candidateYear)) {
			candidateYear = params.getYearForcedIntoYieldTable();
			hasSetNextAge = true;
		}

		// Compute the total stand age of the current year.

		rowContext.setCurrentTableYear(candidateYear);
		if (candidateYear != null) {
			rowContext.setCurrentTableAge(candidateYear - rowContext.getYearToAgeDifference());
		} else {
			rowContext.setCurrentTableAge(null);
		}

		// Compute the year data to request based on the current table year. Before we used to make adjustments
		// based on Projection Type, but adjustment is now being done within the Extended Core Modules. Now, the
		// requested age is always the same as the current table year and age.

		rowContext.setCurrentTableYearToRequest(rowContext.getCurrentTableYear());
		rowContext.setCurrentTableAgeToRequest(rowContext.getCurrentTableAge());

		// Find out to which range (year or age) the current year corresponds.

		rowContext.setCurrentYearIsYearRow(
				rowContext.getCurrentYearRangeYear() != null
						&& rowContext.getCurrentYearRangeYear().equals(rowContext.getCurrentTableYear())
		);
		rowContext.setCurrentYearIsAgeRow(
				rowContext.getCurrentAgeRangeYear() != null
						&& rowContext.getCurrentAgeRangeYear().equals(rowContext.getCurrentTableYear())
		);

		// Check if we have passed the end of the table range.

		if (rowContext.getCurrentTableYear() != null //
				&& (rowContext.getYearAtEndAge() != null
						&& rowContext.getCurrentTableYear() > rowContext.getYearAtEndAge()) //
				&& rowContext.getCurrentTableYear() > rowContext.getMeasurementYear() //
				&& rowContext.getCurrentTableYear() > rowContext.getNowYear() //
				&& (params.getYearForcedIntoYieldTable() == null
						|| rowContext.getCurrentTableYear() > params.getYearForcedIntoYieldTable())) {

			bAnotherRowToBePrinted = false;
		}

		if (rowContext.getYearAtDeath() != null)
			bAnotherRowToBePrinted = false;

		if (bAnotherRowToBePrinted)
			bAnotherRowToBePrinted = hasSetNextAge;

		rowIsCurrent = bAnotherRowToBePrinted;

		rowContext.validate();
	}
}
