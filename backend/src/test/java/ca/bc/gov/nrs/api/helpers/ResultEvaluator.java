package ca.bc.gov.nrs.api.helpers;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResultEvaluator {
	enum FAILURE_TYPE {
		EXACT_MATCH, TOLERANCE, MISSING_VALUE
	}

	static final double TOLERANCE = 0.01;
	Logger logger = LoggerFactory.getLogger(ResultEvaluator.class);
	// Ignore TABLE_NUM column because it iss not guaranteed to match
	static final Pattern BASE_804_AFFECTED = Pattern.compile("TABLE_NUM");

	static Pattern eitherRegexp(Pattern... patterns) {
		if (patterns.length == 0)
			throw new IllegalArgumentException("Must have at least one pattern");
		if (patterns.length == 1)
			return patterns[0];
		return Pattern
				.compile(Arrays.stream(patterns).map(p -> "(?:" + p.toString() + ")").collect(Collectors.joining("|")));
	}

	void logError(String featureId, String message, FAILURE_TYPE failureType, String columnName) {
		if (!errorCountByFeatureId.containsKey(featureId)) {
			logger.error("First error for {}: {}", featureId, message);
			errorCountByFeatureId.put(featureId, 0);
			errorsByFeatureId.put(featureId, "");
			switch (failureType) {
			case EXACT_MATCH -> exactMatchFailureFeatureIDs.add(featureId);
			case TOLERANCE -> toleranceFailureFeatureIDs.add(featureId);
			case MISSING_VALUE -> missingFailureFeatureIDs.add(featureId);
			}
			String key = columnName + " " + failureType.name();
			if (!errorFeaturesByFirstFailureType.containsKey(key)) {
				errorFeaturesByFirstFailureType.put(key, new HashSet<>());
			}
			errorFeaturesByFirstFailureType.get(key).add(featureId);
		}
		errorCountByFeatureId.put(featureId, errorCountByFeatureId.get(featureId) + 1);
		errorsByFeatureId.put(featureId, errorsByFeatureId.get(featureId) + "\n" + message);
	}

	static final Pattern NUMERIC_PATTERN = Pattern.compile(
			"PRJ_LOREY_HT|PRJ_DIAMETER|PRJ_TPH|PRJ_BA|PRJ_DOM_HT|PRJ_SCND_HT|PRJ_VOL_WS|PRJ_VOL_CU|PRJ_VOL_D|PRJ_VOL_DW|PRJ_VOL_DWB|PRJ_SP1_VOL_WS|PRJ_SP1_VOL_CU|PRJ_SP1_VOL_D|PRJ_SP1_VOL_DW|PRJ_SP1_VOL_DWB|PRJ_SP2_VOL_WS|PRJ_SP2_VOL_CU|PRJ_SP2_VOL_D|PRJ_SP2_VOL_DW|PRJ_SP2_VOL_DWB|PRJ_SP3_VOL_WS|PRJ_SP3_VOL_CU|PRJ_SP3_VOL_D|PRJ_SP3_VOL_DW|PRJ_SP3_VOL_DWB|PRJ_SP4_VOL_WS|PRJ_SP4_VOL_CU|PRJ_SP4_VOL_D|PRJ_SP4_VOL_DW|PRJ_SP4_VOL_DWB|PRJ_SP5_VOL_WS|PRJ_SP5_VOL_CU|PRJ_SP5_VOL_D|PRJ_SP5_VOL_DW|PRJ_SP5_VOL_DWB|PRJ_SP6_VOL_WS|PRJ_SP6_VOL_CU|PRJ_SP6_VOL_D|PRJ_SP6_VOL_DW|PRJ_SP6_VOL_DWB|SPECIES_1_PCNT|SPECIES_2_PCNT|SPECIES_3_PCNT|SPECIES_4_PCNT|SPECIES_5_PCNT|SPECIES_6_PCNT|PRJ_PCNT_STOCK"
	);
	static final Predicate<String> NUMERIC_COLUMNS = eitherRegexp(NUMERIC_PATTERN).asMatchPredicate();
	static final Pattern EXACT_MATCH_PATTERN = Pattern.compile(
			"FEATURE_ID|DISTRICT|MAP_ID|POLYGON_ID|LAYER_ID|PROJECTION_YEAR|PRJ_TOTAL_AGE|SPECIES_1_CODE|SPECIES_2_CODE|SPECIES_3_CODE|SPECIES_4_CODE|SPECIES_5_CODE|SPECIES_6_CODE|PRJ_SITE_INDEX|PRJ_MODE"
	);
	static final Predicate<String> EXACT_MATCHCOLUMNS = eitherRegexp(EXACT_MATCH_PATTERN).asMatchPredicate();
	HashMap<String, Integer> errorCountByFeatureId = new HashMap<>();
	HashMap<String, String> errorsByFeatureId = new HashMap<>();
	Set<String> exactMatchFailureFeatureIDs = new HashSet<>();
	Set<String> toleranceFailureFeatureIDs = new HashSet<>();
	Set<String> missingFailureFeatureIDs = new HashSet<>();
	Map<String, Set<String>> errorFeaturesByFirstFailureType = new HashMap<>();
	Set<String> missingFeatureIDs = new HashSet<>();

	public void compareResults(CSVReader actualReader, CSVReader expectedReader, Pattern ignorePattern)
			throws IOException, CsvException {
		Predicate<String> ignoreColumns;
		if (ignorePattern == null)
			ignoreColumns = eitherRegexp(BASE_804_AFFECTED).asMatchPredicate();
		else
			ignoreColumns = eitherRegexp(ignorePattern).asMatchPredicate();
		Set<String> seenFeatureIds = new HashSet<>();
		Set<Integer> ignoredIndices = new HashSet<>();
		Set<Integer> exactMatchIndices = new HashSet<>();
		Set<Integer> toleranceIndices = new HashSet<>();
		String[] actualHeader = actualReader.readNext();
		if (actualHeader == null) {
			throw new IOException("No header in actual YieldTable.csv");
		}
		for (int i = 0; i < actualHeader.length; i++) {

			if (ignoreColumns.test(actualHeader[i])) {
				ignoredIndices.add(i);
			}
			if (EXACT_MATCHCOLUMNS.test(actualHeader[i])) {
				exactMatchIndices.add(i);
			}
			if (NUMERIC_COLUMNS.test(actualHeader[i])) {
				toleranceIndices.add(i);
			}
		}
		String[] expectedHeader = expectedReader.readNext();
		if (expectedHeader == null) {
			throw new IOException("No header in expected YieldTable.csv");
		}
		boolean keepActualLine = false, keepExpectedLine = false;
		// Assume headers match
		int lineNum = 0; // after header
		String[] actualLine = null;
		String[] expectedLine = null;
		while (true) {
			lineNum++;
			if (!keepActualLine)
				actualLine = actualReader.readNext();
			if (!keepExpectedLine)
				expectedLine = expectedReader.readNext();
			if (actualLine == null && expectedLine == null)
				break;
			if (actualLine == null || expectedLine == null) {
				logger.info(
						"Different number of lines at line {} {}", lineNum,
						(actualLine == null ? "actual file ended" : "expected file ended")
				);
				break;
			}
			String featureID = expectedLine[1];
			seenFeatureIds.add(featureID);

			keepActualLine = false;
			keepExpectedLine = false;
			if (!featureID.equals(actualLine[1])) {
				String actualfeatureID = actualLine[1];
				if (Integer.parseInt(featureID) < Integer.parseInt(actualfeatureID)) {
					missingFeatureIDs.add(featureID);
					keepActualLine = true;
				} else {
					keepExpectedLine = true;
				}
				continue;
			}

			if (Arrays.equals(expectedLine, actualLine))
				continue;

			if ("Back".equals(actualLine[actualLine.length - 1]))
				continue;
			for (int i = 0; i < actualLine.length; i++) {
				if (ignoredIndices.contains(i))
					continue;
				String a = actualLine[i];
				String e = expectedLine[i];
				String error = null;
				if (exactMatchIndices.contains(i)) {
					if (!e.equals(a)) {
						error = String
								.format("Line %d column %s expected '%s' but was '%s'", lineNum, actualHeader[i], e, a);
						logError(featureID, error, FAILURE_TYPE.EXACT_MATCH, expectedHeader[i]);
					}
				} else if (toleranceIndices.contains(i)) {
					if (e.trim().isEmpty())
						continue;

					try {
						double ad = Double.parseDouble(a);
						double ed = Double.parseDouble(e);
						double relDiff = Math.abs(ed - ad) / Math.abs(ed);
						if (relDiff >= TOLERANCE) {
							error = String.format(
									"Line %d column %s expected '%s' but was '%s' (relative diff %.2f%%)", lineNum,
									actualHeader[i], e, a, relDiff * 100
							);
							logError(featureID, error, FAILURE_TYPE.TOLERANCE, expectedHeader[i]);
						}
					} catch (NumberFormatException ex) {
						if (!e.equals(a)) {
							error = String.format(
									"Line %d column %s expected '%s' but was '%s' (unparseable)", lineNum,
									actualHeader[i], e, a
							);
							logError(featureID, error, FAILURE_TYPE.MISSING_VALUE, expectedHeader[i]);
						}
					}
				}
			}
		}
		logger.info("Saw {} unique feature IDs", seenFeatureIds.size());
		logger.info("{} feature IDs had errors", errorCountByFeatureId.size());
		logger.info("Missing feature ids ({})", missingFeatureIDs.size());
		logger.info("Exact match failure feature ids ({})", exactMatchFailureFeatureIDs.size());
		logger.info("Tolerance failure feature ids ({})", toleranceFailureFeatureIDs.size());
		logger.info("Missing value failure feature ids ({})", missingFailureFeatureIDs.size());
		errorFeaturesByFirstFailureType.entrySet().stream().sorted(
				(a, b) -> b.getValue().size() - a.getValue().size()
		)
				.forEach(
						e -> logger.info("Feature IDs with first failure type {} ({})", e.getKey(), e.getValue().size())
				);

	}
}
