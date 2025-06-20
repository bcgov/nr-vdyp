package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

public class CustomMappingStrategy<T> extends HeaderColumnNameMappingStrategy<T> {

	private final Field[] fields;

	public CustomMappingStrategy(List<Field> filteredFields) {

		this.fields = filteredFields.stream().toArray(Field[]::new);

		Arrays.sort(fields, (f1, f2) -> {
			CsvBindByPosition position1 = f1.getAnnotation(CsvBindByPosition.class);
			CsvBindByPosition position2 = f2.getAnnotation(CsvBindByPosition.class);
			return Integer.compare(position1.position(), position2.position());
		});
	}

	@Override
	public String[] generateHeader(T bean) {
		String[] header = new String[fields.length];
		int position = 0;
		for (Field f : fields) {
			header[position++] = getName(f);
		}
		headerIndex.initializeHeaderIndex(header);
		return header;
	}

	private String getName(Field f) {
		CsvBindByName csvBindByName = f.getAnnotation(CsvBindByName.class);
		CsvCustomBindByName csvCustomBindByName = f.getAnnotation(CsvCustomBindByName.class);
		return csvCustomBindByName != null
				? csvCustomBindByName.column() == null || csvCustomBindByName.column().isEmpty() ? f.getName()
						: csvCustomBindByName.column()
				: csvBindByName.column() == null || csvBindByName.column().isEmpty() ? f.getName()
						: csvBindByName.column();
	}
}
