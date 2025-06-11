package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;

class CSVYieldTableWriter extends AbstractCSVTypeYieldTableWriter<CSVYieldTableRowValuesBean> {

	private CSVYieldTableWriter(ProjectionContext context) {
		super(CSVYieldTableRowValuesBean.class, context);
	}

	public static CSVYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {

		CSVYieldTableWriter writer = new CSVYieldTableWriter(context);

		writer.initialize();

		return writer;
	}

	@Override
	protected StatefulBeanToCsv<CSVYieldTableRowValuesBean> createCsvOutputStream(FileWriter fileWriter) {
		return CSVYieldTableRowValuesBean.createCsvOutputStream(fileWriter, context);
	}
}
