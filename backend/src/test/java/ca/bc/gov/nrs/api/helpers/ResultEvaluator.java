package ca.bc.gov.nrs.api.helpers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

	private final double TOLERANCE = 0.01;
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
			throws IOException, URISyntaxException, CsvException {
		Predicate<String> IGNORE_COLUMNS;
		if (ignorePattern == null)
			IGNORE_COLUMNS = eitherRegexp(BASE_804_AFFECTED).asMatchPredicate();
		else
			IGNORE_COLUMNS = eitherRegexp(ignorePattern).asMatchPredicate();
		Set<String> seenFeatureIds = new HashSet<>();
		Set<Integer> ignoredIndices = new HashSet<>();
		Set<Integer> exactMatchIndices = new HashSet<>();
		Set<Integer> toleranceIndices = new HashSet<>();
		String[] actualHeader = actualReader.readNext();
		if (actualHeader == null) {
			throw new IOException("No header in actual YieldTable.csv");
		}
		for (int i = 0; i < actualHeader.length; i++) {

			if (IGNORE_COLUMNS.test(actualHeader[i])) {
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
		boolean KeepActualLine = false, keepExpectedLine = false;
		// Assume headers match
		int lineNum = 0; // after header
		String[] actualLine = null;
		String[] expectedLine = null;
		while (true) {
			lineNum++;
			if (!KeepActualLine)
				actualLine = actualReader.readNext();
			if (!keepExpectedLine)
				expectedLine = expectedReader.readNext();
			if (actualLine == null && expectedLine == null)
				break;
			if (actualLine == null || expectedLine == null) {
				logger.info(
						"Different number of lines at line " + lineNum + " "
								+ (actualLine == null ? "actual file ended" : "expected file ended")
				);
				break;
			}
			String featureID = expectedLine[1];
			if (seenFeatureIds.add(featureID)) {
				if (seenFeatureIds.size() % 1000 == 0) {
					logger.info("Seen {} unique feature IDs", seenFeatureIds.size());
				}
			}

			KeepActualLine = false;
			keepExpectedLine = false;
			if (!featureID.equals(actualLine[1])) {
				String actualfeatureID = actualLine[1];
				if (Integer.parseInt(featureID) < Integer.parseInt(actualfeatureID)) {
					missingFeatureIDs.add(featureID);
					KeepActualLine = true;
				} else {
					keepExpectedLine = true;
				}
				continue;
			}
			if (Arrays.equals(expectedLine, actualLine))
				continue;
			else {
				// logger.info("expect:{}", String.join(",", expectedLine));
				// logger.info("actual:{}", String.join(",", actualLine));
			}
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
					// if (e.trim().isEmpty() && a.trim().isEmpty()) {
					// continue; // consider empty strings as equal for numeric columns
					// }

					try {
						double ad = Double.parseDouble(a);
						double ed = Double.parseDouble(e);
						double relDiff = Math.abs(ed - ad) / Math.abs(ed);
						if (relDiff >= TOLERANCE) {
							// logger.error("Line {} column {} expected '{}' but was '{}' (relative diff {}%)",
							// lineNum, actualHeader[i], e, a, relDiff * 100);
							error = String.format(
									"Line %d column %s expected '%s' but was '%s' (relative diff %.2f%%)", lineNum,
									actualHeader[i], e, a, relDiff * 100
							);
							logError(featureID, error, FAILURE_TYPE.TOLERANCE, expectedHeader[i]);
						}
					} catch (NumberFormatException ex) {
						if (!e.equals(a)) {
							if ("PRJ_SCND_HT".equals(actualHeader[i])) {
								boolean bp = true;

							}
							// logger.error("Line {} column {} expected '{}' but was '{}' (unparseable)",
							// lineNum, actualHeader[i], e, a);
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
		logger.info("Missing feature ids ({}): \n {}", missingFeatureIDs.size());
		logger.info("Exact match failure feature ids ({})", exactMatchFailureFeatureIDs.size());
		logger.info("Tolerance failure feature ids ({})", toleranceFailureFeatureIDs.size());
		logger.info("Missing value failure feature ids ({})", missingFailureFeatureIDs.size());
		// generateBucketInputFiles();
		errorFeaturesByFirstFailureType.entrySet().stream().sorted((a, b) -> b.getValue().size() - a.getValue().size()) // descending
																														// by
																														// set
																														// size
				.forEach(
						e -> logger.info("Feature IDs with first failure type {} ({})", e.getKey(), e.getValue().size())
				);

	}

	/*
	 * public void generateBucketInputFiles() throws IOException { Path outputDir = Paths.get(OUTPUT_DIR);
	 * Files.createDirectories(outputDir);
	 *
	 * // Build a reverse lookup: featureId -> bucket names it belongs to // (a featureId could appear in multiple
	 * buckets) Map<String, Set<String>> featureIdToBuckets = new HashMap<>(); for (Map.Entry<String, Set<String>>
	 * bucket : FAILURE_BUCKETS.entrySet()) { for (String failureType : bucket.getValue()) { Set<String> featureIds =
	 * errorFeaturesByFirstFailureType.get(failureType); if (featureIds != null) { for (String featureId : featureIds) {
	 * featureIdToBuckets.computeIfAbsent(featureId, k -> new HashSet<>()).add(bucket.getKey()); } } } } for (String
	 * featureId : missingFeatureIDs) { featureIdToBuckets.computeIfAbsent(featureId, k -> new
	 * HashSet<>()).add("missing_from_table"); }
	 *
	 * // Open one BufferedWriter per bucket up front Map<String, BufferedWriter> writers = new LinkedHashMap<>(); try {
	 * for (String bucketName : FAILURE_BUCKETS.keySet()) { Path polyOutFile = outputDir.resolve(bucketName +
	 * "Poly.csv"); writers.put(bucketName + "Poly", Files.newBufferedWriter(polyOutFile)); Path layerOutFile =
	 * outputDir.resolve(bucketName + "Layer.csv"); writers.put(bucketName + "Layer",
	 * Files.newBufferedWriter(layerOutFile)); } Path polyOutFile = outputDir.resolve("missing_from_table" +
	 * "Poly.csv"); writers.put("missing_from_table" + "Poly", Files.newBufferedWriter(polyOutFile)); Path layerOutFile
	 * = outputDir.resolve("missing_from_table" + "Layer.csv"); writers.put("missing_from_table" + "Layer",
	 * Files.newBufferedWriter(layerOutFile));
	 *
	 * try ( BufferedReader polyReader = Files.newBufferedReader(Paths.get(POLYGON_INPUT_CSV)); BufferedReader
	 * layerReader = Files.newBufferedReader(Paths.get(LAYER_INPUT_CSV)); ) {
	 *
	 * // Handle header String polyHeader = polyReader.readLine(); String layerHeader = layerReader.readLine(); if
	 * (polyHeader == null || layerHeader == null) return;
	 *
	 * int featureIdIndex = 0;
	 *
	 * // Write header to every bucket file for (Map.Entry<String, BufferedWriter> writerSet : writers.entrySet()) { if
	 * (writerSet.getKey().endsWith("Poly")) { writerSet.getValue().write(polyHeader); } else {
	 * writerSet.getValue().write(layerHeader); } writerSet.getValue().newLine(); }
	 *
	 * // Stream through the file one line at a time String line; Map<String, Integer> rowCounts = new HashMap<>();
	 *
	 * while ( (line = polyReader.readLine()) != null) { String[] cols = line.split(",", -1); if (cols.length <=
	 * featureIdIndex) continue;
	 *
	 * String featureId = cols[featureIdIndex].trim(); Set<String> matchedBuckets = featureIdToBuckets.get(featureId);
	 *
	 * if (matchedBuckets != null) { for (String bucketName : matchedBuckets) { writers.get(bucketName +
	 * "Poly").write(line); writers.get(bucketName + "Poly").newLine(); rowCounts.merge(bucketName + "Poly", 1,
	 * Integer::sum); } } } while ( (line = layerReader.readLine()) != null) { String[] cols = line.split(",", -1); if
	 * (cols.length <= featureIdIndex) continue;
	 *
	 * String featureId = cols[featureIdIndex].trim(); Set<String> matchedBuckets = featureIdToBuckets.get(featureId);
	 *
	 * if (matchedBuckets != null) { for (String bucketName : matchedBuckets) { writers.get(bucketName +
	 * "Layer").write(line); writers.get(bucketName + "Layer").newLine(); rowCounts.merge(bucketName + "Layer", 1,
	 * Integer::sum); } } } }
	 *
	 * } finally { // Always close all writers, even if an exception is thrown mid-file for (BufferedWriter writer :
	 * writers.values()) { writer.close(); } } }
	 */

	private static final Map<String, Set<String>> FAILURE_BUCKETS = new LinkedHashMap<>();
	static {
		FAILURE_BUCKETS
				.put("projection_year_mismatch", Set.of("PROJECTION_YEAR EXACT_MATCH", "PRJ_TOTAL_AGE EXACT_MATCH"));
		FAILURE_BUCKETS.put("projection_missing_lorey_ht", Set.of("PRJ_LOREY_HT MISSING_VALUE"));
		FAILURE_BUCKETS.put(
				"projection_core_values_error",
				Set.of(
						"PRJ_DIAMETER TOLERANCE", "PRJ_LOREY_HT TOLERANCE", "PRJ_DOM_HT TOLERANCE", "PRJ_TPH TOLERANCE",
						"PRJ_BA TOLERANCE", "PRJ_PCNT_STOCK TOLERANCE"
				)
		);
		FAILURE_BUCKETS.put("mising_secondary_height", Set.of("PRJ_SCND_HT MISSING_VALUE"));
		FAILURE_BUCKETS.put(
				"projection_layer_vol_error",
				Set.of("PRJ_VOL_WS TOLERANCE", "PRJ_VOL_CU TOLERANCE", "PRJ_VOL_CU MISSING_VALUE")
		);
		FAILURE_BUCKETS.put(
				"projection_spec_vol_error",
				Set.of(
						"PRJ_SP2_VOL_WS TOLERANCE", "PRJ_SP3_VOL_WS TOLERANCE", "PRJ_SP1_VOL_CU TOLERANCE",
						"PRJ_SP2_VOL_CU TOLERANCE", "PRJ_SP4_VOL_WS TOLERANCE", "PRJ_SP3_VOL_CU TOLERANCE",
						"PRJ_SP3_VOL_DW TOLERANCE", "PRJ_SP2_VOL_DW TOLERANCE", "PRJ_SP2_VOL_DWB TOLERANCE",
						"PRJ_SP1_VOL_DWB TOLERANCE", "PRJ_SP3_VOL_D TOLERANCE", "PRJ_SP5_VOL_DW TOLERANCE",
						"PRJ_SP1_VOL_DW TOLERANCE", "PRJ_SP1_VOL_D TOLERANCE", "PRJ_SP4_VOL_CU TOLERANCE",
						"PRJ_SP2_VOL_D TOLERANCE", "PRJ_SP4_VOL_DW TOLERANCE", "PRJ_SP1_VOL_WS TOLERANCE",
						"PRJ_SP5_VOL_WS TOLERANCE", "PRJ_SP5_VOL_CU TOLERANCE"
				)
		);
		FAILURE_BUCKETS.put("other_missing_values", Set.of("LAYER_ID EXACT_MATCH", "PRJ_SITE_INDEX EXACT_MATCH"));
	}
}
