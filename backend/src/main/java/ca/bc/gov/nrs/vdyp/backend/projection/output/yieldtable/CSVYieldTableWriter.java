package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import org.apache.commons.lang3.Validate;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldSuffixes;

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

	@Override
	public void writeHeader() {
		if (this.context.getValidatedParams()
				.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE)) {

		}
	}

	@Override
	protected void recordPerPolygonDetails(Polygon polygon, int yieldTableNumber) {
		currentRecord.setDistrict(polygon.getDistrict());
		currentRecord.setFeatureId(Long.toString(polygon.getFeatureId()));
		currentRecord.setMapId(polygon.getMapSheet());
		currentRecord.setPolygonId(polygon.getPolygonNumber() == null ? "" : Long.toString(polygon.getPolygonNumber()));
		currentRecord.setTableNumber(Integer.toString(yieldTableNumber));
	}

	@Override
	public void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {

		Validate.notNull(
				currentRecord, "CSVYieldTableWriter.recordCalendarYearAndLayerAge: currentRecord must not be null"
		);

		currentRecord.setProjectionYear(Integer.toString(rowContext.getCurrentTableYear()));
		currentRecord.setTotalAge(Integer.toString(rowContext.getCurrentTableAge()));
	}

	@Override
	public void recordSpeciesComposition(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		Validate.notNull(currentRecord, "CSVYieldTableWriter.recordSpeciesComposition: currentRecord must not be null");

		if (rowContext.isPolygonTable()) {
			// Species code and percent are all set to null in Vdyp7; here, they are already null so
			// nothing needs to be done.
		} else {
			int index = 1;
			for (var sri : rowContext.getLayerReportingInfo().getOrderedSpecies()) {
				currentRecord.setSpeciesFieldValue(
						MultiFieldPrefixes.Species, index, MultiFieldSuffixes.Code, sri.getSp64Name()
				);
				currentRecord.setSpeciesFieldValue(
						MultiFieldPrefixes.Species, index, MultiFieldSuffixes.Percent,
						Double.valueOf(sri.getSp64Percent()).toString()
				);
				index += 1;
			}
		}
	}

	@Override
	void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	) {
		if (percentStockable != null) {
			currentRecord.setPercentStockable(Double.toString(percentStockable));
		}

		if (siteIndex != null) {
			currentRecord.setSiteIndex(Double.toString(siteIndex));
		}

		if (dominantHeight != null) {
			currentRecord.setDominantHeight(Double.toString(dominantHeight));
		}

		if (secondaryHeight != null) {
			currentRecord.setSecondaryHeight(Double.toString(secondaryHeight));
		}
	}
}
