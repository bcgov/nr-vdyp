package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class DCSVYieldTableWriter extends AbstractCSVTypeYieldTableWriter<DCSVYieldTableRecordBean> {

	private DCSVYieldTableWriter(ProjectionContext context) throws YieldTableGenerationException {
		super(DCSVYieldTableRecordBean.class, context);
	}

	public static DCSVYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {
		var writer = new DCSVYieldTableWriter(context);

		writer.initialize();

		return writer;
	}

	@Override
	protected StatefulBeanToCsv<DCSVYieldTableRecordBean> createCsvOutputStream(FileWriter fileWriter) {
		return DCSVYieldTableRecordBean.createCsvOutputStream(fileWriter);
	}

	@Override
	protected DCSVYieldTableRecordBean convertToTargetFormat(YieldTableData data) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.convertToTargetFormat");
	}

	@Override
	public void writeHeader(
			Polygon polygonReportingInfo, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.writeHeader");
	}

	@Override
	public void writeCalendarYearAndLayerAge(YieldTableData row) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.writeCalendarYearAndLayerAge");
	}

	@Override
	public void writeSpeciesComposition(YieldTableData row) throws YieldTableGenerationException {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.writeSpeciesComposition");
	}
}