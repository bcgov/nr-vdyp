package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Strings;
import com.opencsv.bean.CsvToBeanFilter;

public class AbstractCsvLineFilter implements CsvToBeanFilter {

	public enum Feature {
		FILTER_BLANK_LINES, FILTER_HEADER_LINE
	}

	private Set<Feature> featureSet = new HashSet<>();
	private int nLinesSeen = 0;
	private String leadingHeader = null;

	public AbstractCsvLineFilter(boolean doFilterBlankLines) {
		featureSet = Set.of(new Feature[] { Feature.FILTER_BLANK_LINES });
	}

	public AbstractCsvLineFilter(boolean doFilterBlankLines, boolean doFilterHeaderLine, String leadingHeader) {
		this(leadingHeader);

		featureSet = Set.of(new Feature[] { Feature.FILTER_BLANK_LINES, Feature.FILTER_HEADER_LINE });
	}

	public AbstractCsvLineFilter(String leadingHeader) {
		if (leadingHeader == null || leadingHeader.isBlank()) {
			throw new IllegalArgumentException();
		}
		this.leadingHeader = leadingHeader;

		featureSet = Set.of(new Feature[] { Feature.FILTER_HEADER_LINE });
	}

	protected Set<Feature> getFeatureSet() {
		return Collections.unmodifiableSet(featureSet);
	}

	@Override
	public boolean allowLine(String[] cellsOfLine) {

		boolean headerLine = false;
		boolean blankLine = true;

		var doFilterHeaderLines = featureSet.contains(Feature.FILTER_HEADER_LINE);
		var doFilterBlankLines = featureSet.contains(Feature.FILTER_BLANK_LINES);

		if (cellsOfLine.length > 0) {
			if (nLinesSeen == 0 && cellsOfLine[0].trim().equals(leadingHeader)) {
				headerLine = true;
				blankLine = false;
			} else {
				if (doFilterBlankLines) {
					for (int i = 0; i < cellsOfLine.length; i++) {
						if (!Strings.isNullOrEmpty(cellsOfLine[i].trim())) {
							blankLine = false;
							break;
						}
					}
				}
			}
		}

		nLinesSeen += 1;

		return false == (doFilterHeaderLines && headerLine || doFilterBlankLines && blankLine);
	}
}
