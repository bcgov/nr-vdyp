package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvException;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;

abstract public class AbstractCSVTypeYieldTableWriter<T> implements YieldTableWriter {

	public static final String YIELD_TABLE_FILE_NAME = "Output_YldTbl.csv";

	protected final ProjectionContext context;
	protected final Path outputFilePath;
	private final Class<T> beanClass;

	private FileWriter fileWriter;
	private StatefulBeanToCsv<T> csvWriter;
	protected T currentRecord;

	protected AbstractCSVTypeYieldTableWriter(Class<T> beanClass, ProjectionContext context) {
		this.context = context;
		this.outputFilePath = Path.of(context.getExecutionFolder().toString(), YIELD_TABLE_FILE_NAME);
		this.beanClass = beanClass;
	}

	@Override
	public void startNewRecord() {
		if (currentRecord != null) {
			throw new IllegalStateException("startNewRecord()");
		}

		try {
			currentRecord = beanClass.getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void write(YieldTableData data) throws CsvException {

		csvWriter.write(convertToTargetFormat(data));
	}

	public void write(T row) throws CsvException {

		csvWriter.write(row);
	}

	protected void initialize() throws YieldTableGenerationException {
		csvWriter = createCsvOutputStream(createFileWriter());
	}

	protected abstract T convertToTargetFormat(YieldTableData data);

	protected abstract StatefulBeanToCsv<T> createCsvOutputStream(FileWriter fileWriter);

	private FileWriter createFileWriter() throws YieldTableGenerationException {

		try {
			fileWriter = new FileWriter(outputFilePath.toString());
			return fileWriter;
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	@Override
	public void close() throws IOException {
		fileWriter.close();
	}
}
