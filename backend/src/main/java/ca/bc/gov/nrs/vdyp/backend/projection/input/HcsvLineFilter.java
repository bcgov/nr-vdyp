package ca.bc.gov.nrs.vdyp.backend.projection.input;

import com.opencsv.bean.CsvToBeanFilter;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;

public class HcsvLineFilter extends AbstractCsvLineFilter implements CsvToBeanFilter {

	private static final String LEADING_HEADER = "FEATURE_ID";

	public HcsvLineFilter(boolean doFilterBlankLines, boolean doFilterHeaderLine) {
		super(doFilterBlankLines, doFilterHeaderLine, LEADING_HEADER);
	}

	@Override
	public boolean allowLine(String[] cellsOfLine) {

		boolean doAllowLine = super.allowLine(cellsOfLine);
		if (doAllowLine) {
			if (getFeatureSet().contains(Feature.FILTER_BLANK_LINES) && cellsOfLine.length > 0
					&& cellsOfLine[0].equals(Vdyp7Constants.EMPTY_INT_TEXT)) {
				// Filter out lines with FEATURE_ID == -9 (VDYP7 "unset" value.)
				return false;
			}
		}
		
		return doAllowLine;
	}
}
