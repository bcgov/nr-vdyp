package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.Closeable;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public interface YieldTableWriter extends Closeable {

	void writeHeader(
			Polygon polygon, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException;

	default void writeTrailer(Integer yieldTableCount) throws YieldTableGenerationException {
		// Many formats have no trailer.
	}

	void writeCalendarYearAndLayerAge(YieldTableData row) throws YieldTableGenerationException;

	void startNewRecord() throws YieldTableGenerationException;

	void writeSpeciesComposition(YieldTableData row) throws YieldTableGenerationException;
}
