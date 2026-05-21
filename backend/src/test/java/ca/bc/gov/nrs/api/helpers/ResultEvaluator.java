package ca.bc.gov.nrs.api.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
	public enum FAILURE_TYPE {
		EXACT_MATCH, TOLERANCE, MISSING_VALUE
	}

	/**
	 * Identifies the first failure that should route a polygon into a subset.
	 *
	 * @param columnName  the yield-table column that first failed
	 * @param failureType the kind of failure observed in that column
	 */
	public record FailureCriterion(String columnName, FAILURE_TYPE failureType) {

		public FailureCriterion {
			Objects.requireNonNull(columnName, "columnName must not be null");
			Objects.requireNonNull(failureType, "failureType must not be null");
		}

		public static FailureCriterion exactMatch(String columnName) {
			return new FailureCriterion(columnName, FAILURE_TYPE.EXACT_MATCH);
		}

		public static FailureCriterion tolerance(String columnName) {
			return new FailureCriterion(columnName, FAILURE_TYPE.TOLERANCE);
		}

		public static FailureCriterion missingValue(String columnName) {
			return new FailureCriterion(columnName, FAILURE_TYPE.MISSING_VALUE);
		}
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
			FailureCriterion key = new FailureCriterion(columnName, failureType);
			if (!errorFeaturesByFirstFailureType.containsKey(key)) {
				errorFeaturesByFirstFailureType.put(key, new HashSet<>());
			}
			errorFeaturesByFirstFailureType.get(key).add(featureId);
		}
		errorCountByFeatureId.put(featureId, errorCountByFeatureId.get(featureId) + 1);
		errorsByFeatureId.put(featureId, errorsByFeatureId.get(featureId) + "\n" + message);
	}

	static final Pattern NUMERIC_PATTERN = Pattern.compile(
			"PRJ_SITE_INDEX|PRJ_LOREY_HT|PRJ_DIAMETER|PRJ_TPH|PRJ_BA|PRJ_DOM_HT|PRJ_SCND_HT|PRJ_VOL_WS|PRJ_VOL_CU|PRJ_VOL_D|PRJ_VOL_DW|PRJ_VOL_DWB|PRJ_SP1_VOL_WS|PRJ_SP1_VOL_CU|PRJ_SP1_VOL_D|PRJ_SP1_VOL_DW|PRJ_SP1_VOL_DWB|PRJ_SP2_VOL_WS|PRJ_SP2_VOL_CU|PRJ_SP2_VOL_D|PRJ_SP2_VOL_DW|PRJ_SP2_VOL_DWB|PRJ_SP3_VOL_WS|PRJ_SP3_VOL_CU|PRJ_SP3_VOL_D|PRJ_SP3_VOL_DW|PRJ_SP3_VOL_DWB|PRJ_SP4_VOL_WS|PRJ_SP4_VOL_CU|PRJ_SP4_VOL_D|PRJ_SP4_VOL_DW|PRJ_SP4_VOL_DWB|PRJ_SP5_VOL_WS|PRJ_SP5_VOL_CU|PRJ_SP5_VOL_D|PRJ_SP5_VOL_DW|PRJ_SP5_VOL_DWB|PRJ_SP6_VOL_WS|PRJ_SP6_VOL_CU|PRJ_SP6_VOL_D|PRJ_SP6_VOL_DW|PRJ_SP6_VOL_DWB|SPECIES_1_PCNT|SPECIES_2_PCNT|SPECIES_3_PCNT|SPECIES_4_PCNT|SPECIES_5_PCNT|SPECIES_6_PCNT|PRJ_PCNT_STOCK"
	);
	static final Predicate<String> NUMERIC_COLUMNS = eitherRegexp(NUMERIC_PATTERN).asMatchPredicate();
	static final Pattern EXACT_MATCH_PATTERN = Pattern.compile(
			"FEATURE_ID|DISTRICT|MAP_ID|POLYGON_ID|LAYER_ID|PROJECTION_YEAR|PRJ_TOTAL_AGE|SPECIES_1_CODE|SPECIES_2_CODE|SPECIES_3_CODE|SPECIES_4_CODE|SPECIES_5_CODE|SPECIES_6_CODE|PRJ_MODE"
	);
	static final Predicate<String> EXACT_MATCHCOLUMNS = eitherRegexp(EXACT_MATCH_PATTERN).asMatchPredicate();
	HashMap<String, Integer> errorCountByFeatureId = new HashMap<>();
	HashMap<String, String> errorsByFeatureId = new HashMap<>();
	Set<String> exactMatchFailureFeatureIDs = new HashSet<>();
	Set<String> toleranceFailureFeatureIDs = new HashSet<>();
	Set<String> missingFailureFeatureIDs = new HashSet<>();
	Map<FailureCriterion, Set<String>> errorFeaturesByFirstFailureType = new HashMap<>();
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
					logError(
							featureID, String.format("Feature ID %s missing from actual results", featureID),
							FAILURE_TYPE.MISSING_VALUE, "FEATURE_ID"
					);
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
		errorFeaturesByFirstFailureType.entrySet().stream().sorted((a, b) -> b.getValue().size() - a.getValue().size()) //
				.forEach(
						e -> logger.info(
								"Feature IDs with first failure type {} {} ({})", e.getKey().columnName(),
								e.getKey().failureType(), e.getValue().size()
						)
				);

	}

	/**
	 * Creates polygon/layer input subsets from the failures captured by the most recent comparison. Each polygon is
	 * routed by its first recorded failure only, which keeps the generated subsets disjoint even when that polygon has
	 * later failures too.
	 *
	 * @param polygonInputFile  source polygon input file
	 * @param layerInputFile    source layer input file
	 * @param outputDirectory   directory that will receive one child directory per created subset
	 * @param subsetDefinitions mapping from subset name to the first-failure criteria that should route polygons into
	 *                          that subset
	 * @return feature IDs written to each created subset; subsets with no matching failures are omitted
	 * @throws IOException              if an input file cannot be read or an output file cannot be written
	 * @throws IllegalArgumentException if the same failure criterion is assigned to more than one subset
	 */
	public Map<String, Set<String>> createInputSubsetsByFirstFailure(
			InputStream polygonInputFile, InputStream layerInputFile, Path outputDirectory,
			Map<String, Set<FailureCriterion>> subsetDefinitions
	) throws IOException {

		Map<FailureCriterion, String> subsetNameByCriterion = new HashMap<>();
		for (var subsetDefinition : subsetDefinitions.entrySet()) {
			String subsetName = subsetDefinition.getKey();
			for (FailureCriterion criterion : subsetDefinition.getValue()) {
				String previousSubsetName = subsetNameByCriterion.putIfAbsent(criterion, subsetName);
				if (previousSubsetName != null && !previousSubsetName.equals(subsetName)) {
					throw new IllegalArgumentException(
							"Failure criterion " + criterion + " is assigned to more than one subset"
					);
				}
			}
		}

		Map<String, Set<String>> featureIdsBySubset = new LinkedHashMap<>();
		for (var firstFailure : errorFeaturesByFirstFailureType.entrySet()) {
			String subsetName = subsetNameByCriterion.get(firstFailure.getKey());
			if (subsetName != null) {
				featureIdsBySubset.computeIfAbsent(subsetName, key -> new HashSet<>()).addAll(firstFailure.getValue());
			}
		}

		if (featureIdsBySubset.isEmpty()) {
			logger.info("No failing polygons matched the requested subset definitions");
			return featureIdsBySubset;
		}

		Map<String, String> subsetByFeatureId = new HashMap<>();
		for (var subset : featureIdsBySubset.entrySet()) {
			for (String featureId : subset.getValue()) {
				String previousSubsetName = subsetByFeatureId.putIfAbsent(featureId, subset.getKey());
				if (previousSubsetName != null && !previousSubsetName.equals(subset.getKey())) {
					throw new IllegalStateException(
							"Feature ID " + featureId + " matched more than one subset despite first-failure routing"
					);
				}
			}
		}

		writeSubsetInputFiles(
				polygonInputFile, outputDirectory, featureIdsBySubset.keySet(), subsetByFeatureId,
				"VDYP7_INPUT_POLY.csv"
		);
		writeSubsetInputFiles(
				layerInputFile, outputDirectory, featureIdsBySubset.keySet(), subsetByFeatureId, "VDYP7_INPUT_LAYER.csv"
		);

		return featureIdsBySubset;
	}

	private void writeSubsetInputFiles(
			InputStream sourceInputFile, Path outputDirectory, Set<String> subsetNames,
			Map<String, String> subsetByFeatureId, String fileName
	) throws IOException {

		Map<String, BufferedWriter> writersBySubset = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceInputFile))) {
			String header = reader.readLine();
			if (header == null) {
				throw new IOException("No header in input file " + sourceInputFile);
			}

			for (String subsetName : subsetNames) {
				Path subsetDirectory = outputDirectory.resolve(subsetName);
				Files.createDirectories(subsetDirectory);

				BufferedWriter writer = Files.newBufferedWriter(subsetDirectory.resolve(fileName));
				writersBySubset.put(subsetName, writer);
				writer.write(header);
				writer.newLine();
			}

			String line;
			while ( (line = reader.readLine()) != null) {
				String subsetName = subsetByFeatureId.get(firstCsvField(line));
				if (subsetName != null) {
					BufferedWriter writer = writersBySubset.get(subsetName);
					writer.write(line);
					writer.newLine();
				}
			}
		} finally {
			IOException closeFailure = null;
			for (BufferedWriter writer : writersBySubset.values()) {
				try {
					writer.close();
				} catch (IOException e) {
					if (closeFailure == null) {
						closeFailure = e;
					} else {
						closeFailure.addSuppressed(e);
					}
				}
			}
			if (closeFailure != null) {
				throw closeFailure;
			}
		}
	}

	static String firstCsvField(String line) {
		if (line.isEmpty()) {
			return "";
		}

		if (line.charAt(0) != '"') {
			int commaIndex = line.indexOf(',');
			return commaIndex == -1 ? line : line.substring(0, commaIndex);
		}

		StringBuilder firstField = new StringBuilder();
		for (int i = 1; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c != '"') {
				firstField.append(c);
				continue;
			}

			if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
				firstField.append('"');
				i++;
				continue;
			}

			return firstField.toString();
		}

		return firstField.toString();
	}

}
