package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes;

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
						MultiFieldPrefixes.Species, index, MultiFieldSuffixes.Percent, sri.getSp64Percent()
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

	void recordCfsBiomassDetails(EntityVolumeDetails volumeDetails, CfsBiomassVolumeDetails cfsBiomassVolumeDetails) {

		Validate.notNull(currentRecord, "YieldTableWriter: startNewRecord must be called once per row");

		if (containsValue(volumeDetails.closeUtilizationVolume())) {
			currentRecord.setCloseUtilizationVolume(volumeDetails.closeUtilizationVolume());
		}

		if (containsValue(cfsBiomassVolumeDetails.bioStemwood())) {
			currentRecord.setCfsBiomassStem(cfsBiomassVolumeDetails.bioStemwood());
		}

		if (containsValue(cfsBiomassVolumeDetails.bioBark())) {
			currentRecord.setCfsBiomassBark(cfsBiomassVolumeDetails.bioBark());
		}

		if (containsValue(cfsBiomassVolumeDetails.bioBranches())) {
			currentRecord.setCfsBiomassBranch(cfsBiomassVolumeDetails.bioBranches());
		}
		if (containsValue(cfsBiomassVolumeDetails.bioFoliage())) {
			currentRecord.setCfsBiomassFoliage(cfsBiomassVolumeDetails.bioFoliage());
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

	protected void recordDominantSpeciesByAge(int age, String speciesCode) {
		// Only required for Specific Yield Table output
	}

	protected void recordPolygonProjectionState(PolygonProjectionState state) {
		// Only required for Specific Yield Table output
	}

	protected void recordCulminationValues(int age, EntityVolumeDetails entityVolumeDetails) {
		// Only required for Specific Yield Table output

	}

	void recordMode(String projectionMode) {
		currentRecord.setMode(projectionMode);
	}

	protected EnumSet<YieldTable.Category> currentCategories = EnumSet.noneOf(YieldTable.Category.class);

	public void setPrioritizedCurrentCategories(ProjectionContext context) {
		// clear the current categories
		currentCategories = EnumSet.noneOf(YieldTable.Category.class);
		ValidatedParameters params = context.getParams();
		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
			extractCFSCategories(params);
		} else {
			extractMofCategories(params);
		}
	}

	public void setMOFCategories(ProjectionContext context) {
		// clear the current categories
		currentCategories = EnumSet.noneOf(YieldTable.Category.class);

		ValidatedParameters params = context.getParams();
		extractMofCategories(params);
	}

	private void extractMofCategories(ValidatedParameters params) {
		// add the relevant ones
		if (params.containsOption(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				&& params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {
			currentCategories.add(YieldTable.Category.LAYER_MOFBIOMASS);
		}
		if (params.containsOption(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				&& params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)) {
			currentCategories.add(YieldTable.Category.LAYER_MOFVOLUMES);
		}
		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				&& params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)) {
			currentCategories.add(YieldTable.Category.SPECIES_MOFVOLUME);
		}
		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				&& params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {
			currentCategories.add(YieldTable.Category.SPECIES_MOFBIOMASS);
		}

		extractGenericCategories(params);
	}

	public void setCFSCategories(ProjectionContext context) {
		// clear the current categories
		currentCategories = EnumSet.noneOf(YieldTable.Category.class);
		ValidatedParameters params = context.getParams();
		extractCFSCategories(params);
	}

	private void extractCFSCategories(ValidatedParameters params) {
		// set the relevant ones (this is a little silly currently)
		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
			currentCategories.add(YieldTable.Category.CFSBIOMASS);
		}

		extractGenericCategories(params);
	}

	private void extractGenericCategories(ValidatedParameters params) {

		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)) {
			currentCategories.add(YieldTable.Category.PROJECTION_MODE);
		}
		if (params.containsOption(Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)) {
			currentCategories.add(YieldTable.Category.POLYGON_ID);
		}
		if (params.containsOption(
				Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
		)) {
			currentCategories.add(YieldTable.Category.SECONDARY_HEIGHT);
		}
	}

	public boolean isCurrentlyWritingCategory(YieldTable.Category category) {
		return currentCategories.contains(category);
	}

	public EnumSet<YieldTable.Category> getCurrentCategories() {
		return currentCategories;
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
	 * @throws YieldTableGenerationException if any error occurs while writing the trailer
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
