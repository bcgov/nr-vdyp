package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

abstract class AbstractCSVTypeYieldTableWriter<T extends YieldTableRowBean> extends YieldTableWriter<T> {

	public static final String YIELD_TABLE_FILE_NAME = "Output_YldTbl.csv";

	protected final ProjectionContext context;

	private FileWriter fileWriter;
	private StatefulBeanToCsv<T> csvWriter;

	protected AbstractCSVTypeYieldTableWriter(Class<T> rowValuesClass, ProjectionContext context) {
		super(rowValuesClass, Path.of(context.getExecutionFolder().toString(), YIELD_TABLE_FILE_NAME));

		this.context = context;
	}

	protected void initialize() throws YieldTableGenerationException {
		csvWriter = createCsvOutputStream(createFileWriter());
	}

	public void write(T row) throws CsvException {
		csvWriter.write(row);
	}

	private FileWriter createFileWriter() throws YieldTableGenerationException {

		try {
			fileWriter = new FileWriter(getYieldTableFilePath().toString());
			return fileWriter;
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	protected abstract StatefulBeanToCsv<T> createCsvOutputStream(FileWriter fileWriter);

	@Override
	public final void writePolygonTableHeader(
			Polygon polygonReportingInfo, Optional<LayerReportingInfo> layerReportingInfo,
			boolean doGenerateDetailedTableHeader, Integer yieldTableCount
	) {
		// CSV output formats do not have per-polygon headers
	}

	@Override
	protected void writeProjectionGrowthInfo() {
		// Subsumed by "writeRecord"
	}

	@Override
	protected void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {
		try {
			write(currentRecord);
		} catch (CsvException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	@Override
	public void close() {
		Utils.close(fileWriter, "AbstractCSVTypeYieldTableWriter<T>.fileWriter");
		fileWriter = null;
	}
}
