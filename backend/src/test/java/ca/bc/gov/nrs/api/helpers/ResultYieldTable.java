package ca.bc.gov.nrs.api.helpers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
							.computeIfAbsent(year, k -> new HashMap<String, Map<String, String>>()) //
							.put(layerId, row);
				});
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
