package ca.bc.gov.nrs.api.helpers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class YieldTable {

	private Map<String, Map<String, String>> content = new HashMap<>();

	public YieldTable(String yieldTableContent) {

		CSVReader reader = new CSVReaderBuilder(new StringReader(yieldTableContent)).build();
		try {
			List<String[]> myEntries = reader.readAll();
			if (myEntries.size() > 1) {

				var rowIterator = myEntries.iterator();
				String[] columnNames = rowIterator.next();

				rowIterator.forEachRemaining(fields -> {
					if (fields.length != columnNames.length) {
						throw new IllegalArgumentException(
								"row " + content.size() + " has " + fields.length + " fields; expecting "
										+ columnNames.length
						);
					}

					var row = new HashMap<String, String>();

					var columnNumber = 0;
					for (var field : fields) {
						row.put(columnNames[columnNumber++], field);
					}
				});
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
