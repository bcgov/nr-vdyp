package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import com.opencsv.bean.StatefulBeanToCsv;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;

class PLOTSYYieldTableWriter extends AbstractCSVTypeYieldTableWriter<PLOTSYYieldTableRecordBean> {

	private PLOTSYYieldTableWriter(ProjectionContext context) {
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
	public void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.recordCalendarYearAndLayerAge");
	}

	@Override
	public void recordSpeciesComposition(YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.recordSpeciesComposition");
	}

	@Override
	protected void recordPolygonAndLayerDetails(int yieldTableNumber, YieldTableRowContext rowContext) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.recordPerPolygonDetails");
	}

	@Override
	void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.recordSiteInformation");
	}

	@Override
	void recordGrowthDetails(EntityGrowthDetails growthDetails, EntityVolumeDetails entityVolumeDetails) {
		throw new UnsupportedOperationException("PLOTSYYieldTableWriter.recordGrowthDetails");
	}
}
