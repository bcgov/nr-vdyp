package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
	public void setCfsBiomassFoliage(Double cfsBiomassFoliage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassFoliage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassBranch(Double cfsBiomassBranch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassBranch() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassBark(Double cfsBiomassBark) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassBark() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCfsBiomassStem(Double cfsBiomassStem) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCfsBiomassStem() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastageBreakage(Double mofBiomassCuVolumeLessDecayWastageBreakage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastageBreakage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastage(Double mofBiomassCuVolumeLessDecayWastage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecay(Double mofBiomassCuVolumeLessDecay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassCloseUtilizationVolume(Double mofBiomassCloseUtilizationVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassCloseUtilizationVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMoFBiomassWholeStemVolume(Double mofBiomassWholeStemVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMoFBiomassWholeStemVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecayWastageBreakage(Double cuVolumeLessDecayWastageBreakage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecayWastageBreakage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecayWastage(Double cuVolumeLessDecayWastage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecayWastage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCuVolumeLessDecay(Double cuVolumeLessDecay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCuVolumeLessDecay() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCloseUtilizationVolume(Double closeUtilizationVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCloseUtilizationVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWholeStemVolume(Double wholeStemVolume) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getWholeStemVolume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBasalArea(Double basalArea) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBasalArea() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTreesPerHectare(Double treesPerHectare) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTreesPerHectare() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDiameter(Double diameter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDiameter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLoreyHeight(Double loreyHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLoreyHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSecondaryHeight(Double secondaryHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSecondaryHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDominantHeight(Double dominantHeight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDominantHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSiteIndex(Double siteIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSiteIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPercentStockable(Double percentStockable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPercentStockable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTotalAge(Integer totalAge) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTotalAge() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProjectionYear(Integer projectionYear) {
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
	public void setPolygonId(Long polygonId) {
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
	public void setFeatureId(Long featureId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFeatureId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTableNumber(int tableNumber) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTableNumber() {
		throw new UnsupportedOperationException();
	}
}
