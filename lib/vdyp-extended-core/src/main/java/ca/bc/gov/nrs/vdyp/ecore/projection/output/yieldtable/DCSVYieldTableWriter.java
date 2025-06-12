package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;

class DCSVYieldTableWriter extends AbstractCSVTypeYieldTableWriter<DCSVYieldTableRecordBean> {

	private DCSVYieldTableWriter(ProjectionContext context) {
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
	protected void recordPolygonAndLayerDetails(int yieldTableNumber, YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.recordPerPolygonDetails");
	}

	@Override
	public void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.writeCalendarYearAndLayerAge");
	}

	@Override
	public void recordSpeciesComposition(YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.writeSpeciesComposition");
	}

	@Override
	void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.recordSiteInformation");
	}

	@Override
	void recordGrowthDetails(EntityGrowthDetails growthDetails, EntityVolumeDetails entityVolumeDetails) {
		throw new UnsupportedOperationException("DCSVYieldTableWriter.recordGrowthDetails");
	}
}
