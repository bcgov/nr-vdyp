package ca.bc.gov.nrs.api.helpers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class ResultYieldTable extends HashMap<String, Map<String, Map<String, Map<String, String>>>> {

	private static final long serialVersionUID = 1L;

	public ResultYieldTable(String yieldTableContent) {

		CSVReader reader = new CSVReaderBuilder(new StringReader(yieldTableContent)).build();
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

	public static void compareWithTolerance(ResultYieldTable yt1, ResultYieldTable yt2, double tolerance) {

		Assert.assertEquals(yt1.keySet(), yt2.keySet());

		for (var p1e : yt1.entrySet()) {
			var p1 = p1e.getValue();
			var p2 = yt2.get(p1e.getKey());

			Assert.assertEquals(p1.keySet(), p2.keySet());

			for (var y1e : p1.entrySet()) {
				var l1 = y1e.getValue();
				var l2 = p2.get(y1e.getKey());

				Assert.assertEquals(l1.keySet(), l2.keySet());

				for (var l1e : l1.entrySet()) {
					var r1 = l1e.getValue();
					var r2 = l2.get(l1e.getKey());

					Assert.assertEquals(r1.keySet(), r2.keySet());

					for (var field : r1.entrySet()) {
						var f1 = field.getValue();
						var f2 = r2.get(field.getKey());

						Assert.assertEquals(f1.getClass(), f2.getClass());

						try {
							var f1AsInt = Integer.parseInt(f1);
							var f2AsInt = Integer.parseInt(f2);
							Assert.assertEquals(f1AsInt, f2AsInt);
						} catch (NumberFormatException e) {
							// drop through to next type
						}

						try {
							var f1AsDouble = Double.parseDouble(f1);
							var f2AsDouble = Double.parseDouble(f2);

							var f1Lower = f1AsDouble * (1.0 - tolerance);
							var f1Upper = f2AsDouble * (1.0 + tolerance);

							Assert.assertTrue(f1Lower <= f2AsDouble && f2AsDouble <= f1Upper);
						} catch (NumberFormatException e) {

							// All other types
							Assert.assertEquals(f1, f2);
						}
					}
				}
			}
		}
	}
}
