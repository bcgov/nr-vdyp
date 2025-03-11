package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class PLOTSYYieldTableWriter extends AbstractCSVTypeYieldTableWriter<PLOTSYYieldTableRecordBean> {

	private PLOTSYYieldTableWriter(ProjectionContext context) throws YieldTableGenerationException {
		super(PLOTSYYieldTableRecordBean.class, context);
	}

	public static PLOTSYYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {
		var writer = new PLOTSYYieldTableWriter(context);

		writer.initialize();

		return writer;
	}

	@Override
	protected StatefulBeanToCsv<PLOTSYYieldTableRecordBean> createCsvOutputStream(FileWriter fileWriter) {
		return PLOTSYYieldTableRecordBean.createCsvOutputStream(fileWriter);
	}

	@Override
	protected PLOTSYYieldTableRecordBean convertToTargetFormat(YieldTableData data) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.convertToTargetFormat");
	}

	@Override
	public void writeHeader(
			Polygon polygonReportingInfo, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.writeHeader");
	}

	@Override
	public void writeCalendarYearAndLayerAge(YieldTableData row) throws YieldTableGenerationException {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.writeCalendarYearAndLayerAge");
	}

	@Override
	public void writeSpeciesComposition(YieldTableData row) throws YieldTableGenerationException {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.writeSpeciesComposition");
	}
}
