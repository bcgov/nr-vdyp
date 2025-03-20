package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

public class DCSVYieldTableRecordBean implements YieldTableRowValues {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DCSVYieldTableRecordBean.class);

	public static StatefulBeanToCsv<DCSVYieldTableRecordBean> createCsvOutputStream(FileWriter writer) {
		return new StatefulBeanToCsvBuilder<DCSVYieldTableRecordBean>(writer) //
				.build();
	}

	public DCSVYieldTableRecordBean() {
		// default constructor necessary for reflection
	}

	@Override
	public void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, String value
	) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMode(String mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassFoliage(String cfsBiomassFoliage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassFoliage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassBranch(String cfsBiomassBranch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassBranch() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassBark(String cfsBiomassBark) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassBark() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassStem(String cfsBiomassStem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassStem() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastageBreakage(String mofBiomassCuVolumeLessDecayWastageBreakage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastageBreakage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastage(String mofBiomassCuVolumeLessDecayWastage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecay(String mofBiomassCuVolumeLessDecay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCloseUtilizationVolume(String mofBiomassCloseUtilizationVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCloseUtilizationVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassWholeStemVolume(String mofBiomassWholeStemVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassWholeStemVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecayWastageBreakage(String cuVolumeLessDecayWastageBreakage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecayWastageBreakage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecayWastage(String cuVolumeLessDecayWastage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecayWastage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecay(String cuVolumeLessDecay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCloseUtilizationVolume(String closeUtilizationVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCloseUtilizationVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWholeStemVolume(String wholeStemVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getWholeStemVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBasalArea(String basalArea) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBasalArea() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTreesPerHectare(String treesPerHectare) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTreesPerHectare() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDiameter(String diameter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDiameter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLoreyHeight(String loreyHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLoreyHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSecondaryHeight(String secondaryHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSecondaryHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDominantHeight(String dominantHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDominantHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSiteIndex(String siteIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSiteIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPercentStockable(String percentStockable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPercentStockable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTotalAge(String totalAge) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTotalAge() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProjectionYear(String projectionYear) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getProjectionYear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLayerId(String layerId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLayerId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPolygonId(String polygonId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPolygonId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMapId(String mapId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMapId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDistrict(String district) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDistrict() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFeatureId(String featureId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFeatureId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTableNumber(String tableNumber) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTableNumber() {
		throw new UnsupportedOperationException();
	}
}
