package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.Closeable;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldSuffixes;

abstract class YieldTableWriter<T extends YieldTableRowValues> implements Closeable {

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

	protected void recordPerPolygonDetails(Polygon polygon, int yieldTableNumber) {
		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		currentRecord.setDistrict(polygon.getDistrict());
		currentRecord.setFeatureId(Long.toString(polygon.getFeatureId()));
		currentRecord.setMapId(polygon.getMapSheet());
		currentRecord.setPolygonId(polygon.getPolygonNumber() == null ? "" : Long.toString(polygon.getPolygonNumber()));
		currentRecord.setTableNumber(Integer.toString(yieldTableNumber));
	}

	public void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		if (rowContext.getCurrentTableYear() != null) {
			currentRecord.setProjectionYear(Integer.toString(rowContext.getCurrentTableYear()));
		}
		if (rowContext.getCurrentTableAge() != null) {
			currentRecord.setTotalAge(Integer.toString(rowContext.getCurrentTableAge()));
		}
	}

	public void recordSpeciesComposition(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

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

	void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	) {
		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

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

	void recordGrowthDetails(EntityGrowthDetails growthDetails, EntityVolumeDetails volumeDetails) {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		if (growthDetails.loreyHeight() != null) {
			currentRecord.setLoreyHeight(Double.toString(growthDetails.loreyHeight()));
		}

		if (growthDetails.diameter() != null) {
			currentRecord.setDiameter(Double.toString(growthDetails.diameter()));
		}

		if (growthDetails.treesPerHectare() != null) {
			currentRecord.setTreesPerHectare(Double.toString(growthDetails.treesPerHectare()));
		}

		if (growthDetails.basalArea() != null) {
			currentRecord.setBasalArea(Double.toString(growthDetails.basalArea()));
		}

		if (volumeDetails.wholeStemVolume() != null) {
			currentRecord.setWholeStemVolume(Double.toString(volumeDetails.wholeStemVolume()));
		}

		if (volumeDetails.closeUtilizationVolume() != null) {
			currentRecord.setCloseUtilizationVolume(Double.toString(volumeDetails.closeUtilizationVolume()));
		}

		if (volumeDetails.cuVolumeLessDecay() != null) {
			currentRecord.setCuVolumeLessDecay(Double.toString(volumeDetails.cuVolumeLessDecay()));
		}

		if (volumeDetails.cuVolumeLessDecayWastage() != null) {
			currentRecord.setCuVolumeLessDecayWastage(Double.toString(volumeDetails.cuVolumeLessDecayWastage()));
		}

		if (volumeDetails.cuVolumeLessDecayWastageBreakage() != null) {
			currentRecord.setCuVolumeLessDecayWastageBreakage(
					Double.toString(volumeDetails.cuVolumeLessDecayWastageBreakage())
			);
		}
	}

	void writeHeader() throws YieldTableGenerationException {
		// Some formats will not have an overall header, or have one that
		// is automatically produced.
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

	public void recordMode(String projectionMode) {
		currentRecord.setMode(projectionMode);
	}
}
