package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvException;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.CSVYieldTableRecordBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.CSVYieldTableRecordBean.MultiFieldSuffixes;

public class CSVYieldTableWriter extends AbstractCSVTypeYieldTableWriter<CSVYieldTableRecordBean> {

	private CSVYieldTableWriter(ProjectionContext context) {
		super(CSVYieldTableRecordBean.class, context);

		context.getYieldTable();
	}

	public static AbstractCSVTypeYieldTableWriter<CSVYieldTableRecordBean> of(ProjectionContext context)
			throws YieldTableGenerationException {

		AbstractCSVTypeYieldTableWriter<CSVYieldTableRecordBean> writer = new CSVYieldTableWriter(context);

		writer.initialize();

		return writer;
	}

	@Override
	protected StatefulBeanToCsv<CSVYieldTableRecordBean> createCsvOutputStream(FileWriter fileWriter) {
		return CSVYieldTableRecordBean.createCsvOutputStream(fileWriter);
	}

	@Override
	protected CSVYieldTableRecordBean convertToTargetFormat(YieldTableData data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeHeader(
			Polygon polygonReportingInfo, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {

		assert currentRecord == null;

		var headerRowRecord = new CSVYieldTableRecordBean();

		headerRowRecord.setTableNumber(Integer.toString(yieldTableCount));
		headerRowRecord.setFeatureId(Long.toString(polygonReportingInfo.getFeatureId()));
		headerRowRecord.setDistrict(polygonReportingInfo.getDistrict());
		headerRowRecord.setMapId(polygonReportingInfo.getMapSheet());
		headerRowRecord.setPolygonId(Long.toString(polygonReportingInfo.getPolygonNumber()));
		if (layerReportingInfo != null) {
			headerRowRecord.setLayerId(layerReportingInfo.getLayerID());
		}

		try {
			write(headerRowRecord);
		} catch (CsvException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	@Override
	public void writeCalendarYearAndLayerAge(YieldTableData row) {

		assert currentRecord != null;

		currentRecord.setProjectionYear(Integer.toString(row.getCurrentTableYear()));
		currentRecord.setTotalAge(Integer.toString(row.getCurrentTableAge()));
	}

	@Override
	public void writeSpeciesComposition(YieldTableData row) throws YieldTableGenerationException {

		assert currentRecord != null;

		var isPolygonTable = row.getLayerReportingInfo() != null;

		if (isPolygonTable) {
			// Species code and percent are all set to null in Vdyp7; here, they are already null so
			// nothing needs to be done.
		} else {
			int index = 1;
			for (var sri : row.getLayerReportingInfo().getOrderedSpecies()) {
				currentRecord.setSpeciesField(
						MultiFieldPrefixes.SPECIES_, index, MultiFieldSuffixes._CODE, sri.getSp64Name()
				);
				currentRecord.setSpeciesField(
						MultiFieldPrefixes.SPECIES_, index, MultiFieldSuffixes._PCNT, sri.getSp64Percent()
				);
				index += 1;
			}
		}
	}
}
