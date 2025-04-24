package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes;

abstract class YieldTableWriter<T extends YieldTableRowBean> implements Closeable {

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

	void startNewRecord() {
		if (currentRecord != null) {
			throw new IllegalStateException("startNewRecord()");
		}

		try {
			currentRecord = rowValuesClass.getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	protected void recordPolygonAndLayerDetails(int yieldTableNumber, YieldTableRowContext rowContext) {
		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		var polygon = rowContext.getPolygon();

		currentRecord.setDistrict(polygon.getDistrict());
		currentRecord.setFeatureId(polygon.getFeatureId());
		currentRecord.setMapId(polygon.getMapSheet());
		currentRecord.setPolygonId(polygon.getPolygonNumber());
		currentRecord.setTableNumber(yieldTableNumber);

		if (!rowContext.isPolygonTable()) {
			currentRecord.setLayerId(rowContext.getLayerReportingInfo().getLayerID());
		}
	}

	void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		if (rowContext.getCurrentTableYear() != null) {
			currentRecord.setProjectionYear(rowContext.getCurrentTableYear());
		}
		if (rowContext.getCurrentTableAge() != null) {
			currentRecord.setTotalAge(rowContext.getCurrentTableAge());
		}
	}

	void recordSpeciesComposition(YieldTableRowContext rowContext) {

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
			currentRecord.setPercentStockable(percentStockable);
		}

		if (siteIndex != null) {
			currentRecord.setSiteIndex(siteIndex);
		}

		if (dominantHeight != null) {
			currentRecord.setDominantHeight(dominantHeight);
		}

		if (secondaryHeight != null) {
			currentRecord.setSecondaryHeight(secondaryHeight);
		}
	}

	void recordGrowthDetails(EntityGrowthDetails growthDetails, EntityVolumeDetails volumeDetails) {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		if (containsValue(growthDetails.loreyHeight())) {
			currentRecord.setLoreyHeight(growthDetails.loreyHeight());
		}

		if (containsValue(growthDetails.diameter())) {
			currentRecord.setDiameter(growthDetails.diameter());
		}

		if (containsValue(growthDetails.treesPerHectare())) {
			currentRecord.setTreesPerHectare(growthDetails.treesPerHectare());
		}

		if (containsValue(growthDetails.basalArea())) {
			currentRecord.setBasalArea(growthDetails.basalArea());
		}

		if (containsValue(volumeDetails.wholeStemVolume())) {
			currentRecord.setWholeStemVolume(volumeDetails.wholeStemVolume());
		}

		if (containsValue(volumeDetails.closeUtilizationVolume())) {
			currentRecord.setCloseUtilizationVolume(volumeDetails.closeUtilizationVolume());
		}

		if (containsValue(volumeDetails.cuVolumeLessDecay())) {
			currentRecord.setCuVolumeLessDecay(volumeDetails.cuVolumeLessDecay());
		}

		if (containsValue(volumeDetails.cuVolumeLessDecayWastage())) {
			currentRecord.setCuVolumeLessDecayWastage(volumeDetails.cuVolumeLessDecayWastage());
		}

		if (containsValue(volumeDetails.cuVolumeLessDecayWastageBreakage())) {
			currentRecord.setCuVolumeLessDecayWastageBreakage(volumeDetails.cuVolumeLessDecayWastageBreakage());
		}
	}

	private boolean containsValue(Double value) {
		return value != null && value > 0;
	}

	protected void recordPerSpeciesVolumeInfo(int spIndex, EntityVolumeDetails volume, EntityVolumeDetails mofBiomass) {

		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.WholeStemVolume,
				volume.wholeStemVolume()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.CloseUtilizationVolume,
				volume.closeUtilizationVolume()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.CuVolumeLessDecay,
				volume.cuVolumeLessDecay()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.CuVolumeLessDecayWastage,
				volume.cuVolumeLessDecayWastage()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.CuVolumeLessDecayWastageBreakage,
				volume.cuVolumeLessDecayWastageBreakage()
		);

		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.MoFBiomassWholeStemVolume,
				mofBiomass.wholeStemVolume()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.MoFBiomassCloseUtilizationVolume,
				mofBiomass.closeUtilizationVolume()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.MoFBiomassCuVolumeLessDecay,
				mofBiomass.cuVolumeLessDecay()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex, MultiFieldSuffixes.MoFBiomassCuVolumeLessDecayWastage,
				mofBiomass.cuVolumeLessDecayWastage()
		);
		currentRecord.setSpeciesFieldValue(
				MultiFieldPrefixes.SpeciesProjection, spIndex,
				MultiFieldSuffixes.MoFBiomassCuVolumeLessDecayWastageBreakage,
				mofBiomass.cuVolumeLessDecayWastageBreakage()
		);
	}

	void recordMode(String projectionMode) {
		currentRecord.setMode(projectionMode);
	}

	/**
	 * Write the yield table header into the output stream. This is a default method that does nothing, the requirement
	 * for a number of the output formats.
	 *
	 * @throws YieldTableGenerationException
	 */
	void writeHeader() throws YieldTableGenerationException {
		// Some formats will not have an overall header, or have one that
		// is automatically produced.
	}

	/**
	 * Write the per-polygon header, described by the following parameters, to the output. This is a default method that
	 * does nothing, the requirement for a number of the output formats.
	 *
	 * @param polygon
	 * @param layerReportingInfo
	 * @param doGenerateDetailedHeader
	 * @param yieldTableCount
	 * @throws YieldTableGenerationException
	 */
	void writePolygonTableHeader(
			Polygon polygon, Optional<LayerReportingInfo> layerReportingInfo, boolean doGenerateDetailedHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {
		// Some formats have no per-polygon header.
	}

	protected abstract void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException;

	protected abstract void writeProjectionGrowthInfo() throws YieldTableGenerationException;

	/**
	 * Write the per-polygon yield table trailer into the output stream. This is a default method that does nothing, the
	 * requirement for a number of the output formats.
	 *
	 * @param yieldTableNumber the yield table number
	 * @throws YieldTableGenerationException
	 */
	void writePolygonTableTrailer(Integer yieldTableNumber) throws YieldTableGenerationException {
		// Some formats have no per-polygon trailer.
	}

	/**
	 * Write the yield table trailer into the output stream. This is a default method that does nothing, the requirement
	 * for a number of the output formats.
	 *
	 * @param yieldTableNumber the yield table number
	 * @throws YieldTableGenerationException
	 */
	void writeTrailer() throws YieldTableGenerationException {
		// Some formats have no trailer.
	}

	final void endRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {
		if (currentRecord == null) {
			throw new IllegalStateException("endRecord()");
		}

		writeRecord(rowContext);

		currentRecord = null;
	}
}
