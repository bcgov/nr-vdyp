package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.util.Iterator;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;

public class YieldTableRowIterator implements Iterator<YieldTableData> {

	private final ProjectionContext context;
	private YieldTableData row;

	private boolean rowIsCurrent;

	public YieldTableRowIterator(ProjectionContext context, YieldTableData row) {
		this.context = context;
		this.row = row;

		establishStartRow();
	}

	@Override
	public boolean hasNext() {
		if (!rowIsCurrent) {
			advanceToNextRow();
		}
		return rowIsCurrent;
	}

	@Override
	public YieldTableData next() {
		if (rowIsCurrent) {
			rowIsCurrent = false;
			return row;
		} else {
			throw new IllegalStateException("YieldTableRowIterator.next() called when there is no next row");
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

		var params = context.getValidatedParams();

		if (row.getYearAtDeath() == null) {

			row.setCurrentYearRangeYear(params.getYearStart());
			row.setCurrentAgeRangeYear(
					params.getAgeStart() == null ? null : params.getAgeStart() + row.getYearToAgeDifference()
			);
			row.setCurrentTableYear(row.getYearAtStartAge());

			if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {
				if (row.getCurrentTableYear() == null || row.getCurrentTableYear() > row.getMeasurementYear()) {
					row.setCurrentTableYear(row.getMeasurementYear());
				}
			}

			if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {
				if (row.getCurrentTableYear() == null || row.getCurrentTableYear() > row.getCurrentYear()) {
					row.setCurrentTableYear(row.getCurrentYear());
				}
			}

			if (params.getYearForcedIntoYearTable() != null) {
				if (row.getCurrentTableYear() == null
						|| row.getCurrentTableYear() > params.getYearForcedIntoYearTable()) {
					row.setCurrentTableYear(params.getYearForcedIntoYearTable());
				}
			}

		} else {
			row.setCurrentYearRangeYear(row.getYearAtDeath());
			row.setCurrentAgeRangeYear(row.getAgeAtDeath());
			row.setCurrentTableYear(row.getYearAtDeath());
		}

		// Find out to which range (year or age) the current year corresponds.

		row.setCurrentYearIsYearRow(row.getCurrentYearRangeYear() == row.getCurrentTableYear());
		row.setCurrentYearIsAgeRow(row.getCurrentAgeRangeYear() == row.getCurrentTableYear());

		// Compute the total stand age of the current year.

		if (row.getCurrentTableYear() != null && row.getYearToAgeDifference() != null) {

			row.setCurrentTableAge(row.getCurrentTableYear() - row.getYearToAgeDifference());
		}

		// Compute the year data to request based on the current table year.

		row.setCurrentTableYearToRequest(row.getCurrentTableYear());
		row.setCurrentTableAgeToRequest(row.getCurrentTableAge());

		// Finally, is the set of rows empty or not?
		rowIsCurrent = row.getCurrentTableYear() != null;
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
		var params = context.getValidatedParams();

		boolean bAnotherRowToBePrinted = true;
		boolean hasSetNextAge = false;

		// Advance the current table year 1 year to force a recalculation of the
		// next table year. If we don't then it could easily be that the current
		// age range or year range falls exactly on the current year and no actual
		// advancement would take place. By advancing the minimum step, we avoid
		// that problem.

		row.setCurrentTableYear(row.getCurrentTableYear() + 1);

		// If we are above the year range current year, advance that counter.

		while (row.getCurrentYearRangeYear() != null && row.getCurrentYearRangeYear() < row.getCurrentTableYear()) {
			row.setCurrentYearRangeYear(row.getCurrentYearRangeYear() + params.getAgeIncrement());
		}

		if (row.getCurrentYearRangeYear() != null && row.getCurrentYearRangeYear() > params.getYearEnd()) {
			row.setCurrentYearRangeYear(null);
		}

		// If we are above the age range current year, advance that counter.

		while (row.getCurrentAgeRangeYear() != null && row.getCurrentAgeRangeYear() < row.getCurrentTableYear()) {
			row.setCurrentAgeRangeYear(row.getCurrentAgeRangeYear() + params.getAgeIncrement());
		}

		if (row.getCurrentAgeRangeYear() != null
				&& row.getCurrentAgeRangeYear() > params.getAgeEnd() + row.getYearToAgeDifference()) {
			row.setCurrentAgeRangeYear(null);
		}

		// Now find the next lowest target year we will want to display.

		Integer candidateYear = null;

		if (row.getCurrentYearRangeYear() != null) {
			candidateYear = row.getCurrentYearRangeYear();
			hasSetNextAge = true;
		}

		if (row.getCurrentAgeRangeYear() != null
				&& (candidateYear == null || row.getCurrentAgeRangeYear() <= candidateYear)) {
			candidateYear = row.getCurrentAgeRangeYear();
			hasSetNextAge = true;
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& (row.getCurrentTableYear() <= row.getMeasurementYear())
				&& (candidateYear == null || row.getMeasurementYear() < candidateYear)) {
			candidateYear = row.getMeasurementYear();
			hasSetNextAge = true;
		}

		if (params.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
				&& row.getCurrentTableYear() <= row.getCurrentYear()
				&& (candidateYear == null || params.getYearForcedIntoYearTable() < candidateYear)) {
			candidateYear = row.getCurrentYear();
			hasSetNextAge = true;
		}

		if (params.getYearForcedIntoYearTable() != null
				&& row.getCurrentTableYear() <= params.getYearForcedIntoYearTable()
				&& (candidateYear == null || params.getYearForcedIntoYearTable() < candidateYear)) {
			candidateYear = params.getYearForcedIntoYearTable();
			hasSetNextAge = true;
		}

		// Compute the total stand age of the current year.

		row.setCurrentTableYear(candidateYear);

		if (row.getCurrentTableYear() != null && row.getYearToAgeDifference() != null) {
			row.setCurrentTableAge(row.getCurrentTableYear() - row.getYearToAgeDifference());
		}

		// Compute the year data to request based on the current table year. Before we used to make adjustments
		// based on Projection Type, but adjustment is now being done within the Extended Core Modules. Now, the
		// requested age is always the same as the current table year and age.

		row.setCurrentTableYearToRequest(row.getCurrentTableYear());
		row.setCurrentTableAgeToRequest(row.getCurrentTableAge());

		// Find out to which range (year or age) the current year corresponds.

		row.setCurrentYearIsYearRow(row.getCurrentYearRangeYear() == row.getCurrentTableYear());
		row.setCurrentYearIsAgeRow(row.getCurrentAgeRangeYear() == row.getCurrentTableYear());

		// Check if we have passed the end of the table range.

		if (row.getCurrentTableYear() != null && row.getCurrentTableYear() > row.getYearAtEndAge()
				&& row.getCurrentTableYear() > row.getMeasurementYear()
				&& row.getCurrentTableYear() > row.getCurrentYear()
				&& row.getCurrentTableYear() > params.getYearForcedIntoYearTable()) {

			bAnotherRowToBePrinted = false;
		}

		if (row.getYearAtDeath() != null && row.getYearAtDeath() > 0)
			bAnotherRowToBePrinted = false;

		if (bAnotherRowToBePrinted)
			bAnotherRowToBePrinted = hasSetNextAge;

		rowIsCurrent = bAnotherRowToBePrinted;
	}
}
