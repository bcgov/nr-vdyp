package ca.bc.gov.nrs.api.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class ResultYieldTable extends HashMap<String, Map<String, Map<String, Map<String, String>>>> {

	private static final long serialVersionUID = 1L;

	public ResultYieldTable(String yieldTableContent) {
		this(new StringReader(yieldTableContent));
	}

	public ResultYieldTable(Reader yieldTableContent) {

		CSVReader reader = new CSVReaderBuilder(yieldTableContent).build();
		try {
			List<String[]> myEntries = reader.readAll();
			if (myEntries.size() > 1) {

				var rowIterator = myEntries.iterator();
				String[] columnNames = rowIterator.next();

				rowIterator.forEachRemaining(fields -> {
					if (fields.length != columnNames.length) {
						throw new IllegalArgumentException(
								"row " + this.size() + " has " + fields.length + " fields; expecting "
										+ columnNames.length
						);
					}

					var row = new HashMap<String, String>();

					var columnNumber = 0;
					for (var field : fields) {
						row.put(columnNames[columnNumber++], field);
					}

					var featureId = row.get("FEATURE_ID");
					var layerId = row.get("LAYER_ID");
					var year = row.get("PROJECTION_YEAR");

					this.computeIfAbsent(featureId, k -> new HashMap<String, Map<String, Map<String, String>>>())
							.computeIfAbsent(layerId, k -> new HashMap<String, Map<String, String>>()) //
							.put(year, row);
				});
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void
			compareWithTolerance(ResultYieldTable expectedTable, ResultYieldTable actualTable, double tolerance) {
		compareWithTolerance(expectedTable, actualTable, tolerance, hasKey -> false);
	}

	static <T> void assertKeysetEquals(Set<T> expected, Set<T> actual, Predicate<T> ignore, String message) {
		var expectedFiltered = expected.stream().filter(x -> !ignore.test(x)).collect(Collectors.toSet());
		var actualFiltered = actual.stream().filter(x -> !ignore.test(x)).collect(Collectors.toSet());
		assertEquals(expectedFiltered, actualFiltered, message);
	}

	public static void compareWithTolerance(
			ResultYieldTable expectedTable, ResultYieldTable actualTable, double tolerance,
			Predicate<String> ignoredFields
	) {

		assertEquals(expectedTable.keySet(), actualTable.keySet(), "Feature IDs don't match expected");

		for (var featureEntry : expectedTable.entrySet()) {
			var expectedFeature = featureEntry.getValue();
			var actualFeature = actualTable.get(featureEntry.getKey());
			var featureId = featureEntry.getKey();
			assertKeysetEquals(
					expectedFeature.keySet(), actualFeature.keySet(), ignoredFields,
					"Layer IDs for " + featureId + " don't match expected"
			);

			for (var layerEntry : expectedFeature.entrySet()) {
				var expectedLayer = layerEntry.getValue();
				var actualLayer = actualFeature.get(layerEntry.getKey());
				var layerId = featureId + ":" + layerEntry.getKey();
				assertKeysetEquals(
						expectedLayer.keySet(), actualLayer.keySet(), ignoredFields,
						"Years  for " + layerId + " don't match expected"
				);

				for (var rowEntry : expectedLayer.entrySet()) {
					var expectedRow = rowEntry.getValue();
					var actualRow = actualLayer.get(rowEntry.getKey());
					var yearId = layerId + ":" + rowEntry.getKey();
					assertKeysetEquals(
							expectedRow.keySet(), actualRow.keySet(), ignoredFields,
							"Fields for " + yearId + " don't match expected"
					);

					for (var field : expectedRow.entrySet()) {
						if (ignoredFields.test(field.getKey())) {
							continue;
						}
						var fieldId = yearId + ":" + field.getKey();
						var expectedField = field.getValue();
						var actualField = actualRow.get(field.getKey());

						assertEquals(
								expectedField.getClass(), actualField.getClass(), "Mismatched types for " + fieldId
						);

						try {
							var expectedAsInt = Integer.parseInt(expectedField);
							var actualAsInt = Integer.parseInt(actualField);
							assertEquals(expectedAsInt, actualAsInt, fieldId + " doesn't match");
						} catch (NumberFormatException e) {
							// drop through to next type
						}

						try {
							var expectedAsDouble = Double.parseDouble(expectedField);
							var actualAsDouble = Double.parseDouble(actualField);

							assertEquals(
									expectedAsDouble, actualAsDouble, expectedAsDouble * tolerance,
									fieldId + " doesn't match "
											+ 100 * (expectedAsDouble - actualAsDouble) / expectedAsDouble
											+ "% difference"
							);
						} catch (NumberFormatException e) {

							// All other types
							assertEquals(expectedField, actualField, fieldId + " doesn't match");
						}
					}
				}
			}
		}
	}
}
