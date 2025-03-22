package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.Closeable;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

abstract class YieldTableWriter<T> implements Closeable {

	protected static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

	private final Class<T> rowValuesClass;
	private final Path yieldTableFilePath;

	protected T currentRecord;

	protected YieldTableWriter(Class<T> rowValuesClass, Path yieldTableFilePath) {
		this.rowValuesClass = rowValuesClass;
		this.yieldTableFilePath = yieldTableFilePath;
	}

	public Path getYieldTableFilePath() {
		return yieldTableFilePath;
	}

	public void startNewRecord() {
		if (currentRecord != null) {
			throw new IllegalStateException("startNewRecord()");
		}

		try {
			currentRecord = rowValuesClass.getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	abstract void recordPerPolygonDetails(Polygon polygon, int yieldTableNumber);

	abstract void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) throws YieldTableGenerationException;

	abstract void recordSpeciesComposition(YieldTableRowContext rowContext) throws YieldTableGenerationException;

	abstract void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	);

	void writeHeader() throws YieldTableGenerationException {
		// Some formats will not have an overall header.
	}

	void writePolygonTableHeader(
			Polygon polygon, Optional<LayerReportingInfo> layerReportingInfo, boolean doGenerateDetailedHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {
		// Some formats have no per-polygon header.
	}

	abstract void writeProjectionGrowthInfo() throws YieldTableGenerationException;

	void writePolygonTableTrailer(Integer yieldTableCount) throws YieldTableGenerationException {
		// Some formats have no per-polygon trailer.
	}

	void writeTrailer() throws YieldTableGenerationException {
		// Some formats have no trailer.
	}

	protected abstract void writeRecord() throws YieldTableGenerationException;

	final void endRecord() throws YieldTableGenerationException {
		if (currentRecord == null) {
			throw new IllegalStateException("endRecord()");
		}

		writeRecord();

		currentRecord = null;
	}
}
