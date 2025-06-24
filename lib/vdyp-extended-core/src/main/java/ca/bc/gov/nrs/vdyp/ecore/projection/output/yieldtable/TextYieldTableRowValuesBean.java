package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes.Species;
import static ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes.SpeciesProjection;
import static ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

public class TextYieldTableRowValuesBean implements YieldTableRowBean {

	private String tableNumber;
	private String featureId;
	private String district;
	private String mapId;
	private String polygonId;
	private String layerId;
	private String projectionYear;
	private String totalAge;
	private String species1Code;
	private String species1Percent;
	private String species2Code;
	private String species2Percent;
	private String species3Code;
	private String species3Percent;
	private String species4Code;
	private String species4Percent;
	private String species5Code;
	private String species5Percent;
	private String species6Code;
	private String species6Percent;
	private String percentStockable;
	private String siteIndex;
	private String dominantHeight;
	private String secondaryHeight;
	private String loreyHeight;
	private String diameter;
	private String treesPerHectare;
	private String basalArea;
	private String wholeStemVolume;
	private String closeUtilizationVolume;
	private String cuVolumeLessDecay;
	private String cuVolumeLessDecayWastage;
	private String cuVolumeLessDecayWastageBreakage;
	private String moFBiomassWholeStemVolume;
	private String moFBiomassCloseUtilizationVolume;
	private String moFBiomassCuVolumeLessDecay;
	private String moFBiomassCuVolumeLessDecayWastage;
	private String moFBiomassCuVolumeLessDecayWastageBreakage;
	private String species1WholeStemVolume;
	private String species1CloseUtilizationVolume;
	private String species1CuVolumeLessDecay;
	private String species1CuVolumeLessDecayWastage;
	private String species1CuVolumeLessDecayWastageBreakage;
	private String species2WholeStemVolume;
	private String species2CloseUtilizationVolume;
	private String species2CuVolumeLessDecay;
	private String species2CuVolumeLessDecayWastage;
	private String species2CuVolumeLessDecayWastageBreakage;
	private String species3WholeStemVolume;
	private String species3CloseUtilizationVolume;
	private String species3CuVolumeLessDecay;
	private String species3CuVolumeLessDecayWastage;
	private String species3CuVolumeLessDecayWastageBreakage;
	private String species4WholeStemVolume;
	private String species4CloseUtilizationVolume;
	private String species4CuVolumeLessDecay;
	private String species4CuVolumeLessDecayWastage;
	private String species4CuVolumeLessDecayWastageBreakage;
	private String species5WholeStemVolume;
	private String species5CloseUtilizationVolume;
	private String species5CuVolumeLessDecay;
	private String species5CuVolumeLessDecayWastage;
	private String species5CuVolumeLessDecayWastageBreakage;
	private String species6WholeStemVolume;
	private String species6CloseUtilizationVolume;
	private String species6CuVolumeLessDecay;
	private String species6CuVolumeLessDecayWastage;
	private String species6CuVolumeLessDecayWastageBreakage;
	private String species1MoFBiomassWholeStemVolume;
	private String species1MoFBiomassCloseUtilizationVolume;
	private String species1MoFBiomassCuVolumeLessDecay;
	private String species1MoFBiomassCuVolumeLessDecayWastage;
	private String species1MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String species2MoFBiomassWholeStemVolume;
	private String species2MoFBiomassCloseUtilizationVolume;
	private String species2MoFBiomassCuVolumeLessDecay;
	private String species2MoFBiomassCuVolumeLessDecayWastage;
	private String species2MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String species3MoFBiomassWholeStemVolume;
	private String species3MoFBiomassCloseUtilizationVolume;
	private String species3MoFBiomassCuVolumeLessDecay;
	private String species3MoFBiomassCuVolumeLessDecayWastage;
	private String species3MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String species4MoFBiomassWholeStemVolume;
	private String species4MoFBiomassCloseUtilizationVolume;
	private String species4MoFBiomassCuVolumeLessDecay;
	private String species4MoFBiomassCuVolumeLessDecayWastage;
	private String species4MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String species5MoFBiomassWholeStemVolume;
	private String species5MoFBiomassCloseUtilizationVolume;
	private String species5MoFBiomassCuVolumeLessDecay;
	private String species5MoFBiomassCuVolumeLessDecayWastage;
	private String species5MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String species6MoFBiomassWholeStemVolume;
	private String species6MoFBiomassCloseUtilizationVolume;
	private String species6MoFBiomassCuVolumeLessDecay;
	private String species6MoFBiomassCuVolumeLessDecayWastage;
	private String species6MoFBiomassCuVolumeLessDecayWastageBreakage;
	private String cfsBiomassStem;
	private String cfsBiomassBark;
	private String cfsBiomassBranch;
	private String cfsBiomassFoliage;
	private String mode;

	public TextYieldTableRowValuesBean() {
		// default constructor necessary for reflection
	}

	@Override
	public String getTableNumber() {
		return tableNumber;
	}

	@Override
	public void setTableNumber(int tableNumber) {
		this.tableNumber = FieldFormatter.format(tableNumber);
	}

	@Override
	public String getFeatureId() {
		return featureId;
	}

	@Override
	public void setFeatureId(Long featureId) {
		this.featureId = FieldFormatter.format(featureId);
	}

	@Override
	public String getDistrict() {
		return district;
	}

	@Override
	public void setDistrict(String district) {
		this.district = district;
	}

	@Override
	public String getMapId() {
		return mapId;
	}

	@Override
	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	@Override
	public String getPolygonId() {
		return polygonId;
	}

	@Override
	public void setPolygonId(Long polygonId) {
		this.polygonId = FieldFormatter.format(polygonId);
	}

	@Override
	public String getLayerId() {
		return layerId;
	}

	@Override
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	@Override
	public String getProjectionYear() {
		return projectionYear;
	}

	@Override
	public void setProjectionYear(Integer projectionYear) {
		this.projectionYear = FieldFormatter.format(projectionYear);
	}

	@Override
	public String getTotalAge() {
		return totalAge;
	}

	@Override
	public void setTotalAge(Integer totalAge) {
		this.totalAge = FieldFormatter.format(totalAge);
	}

	public String getSpecies1Code() {
		return species1Code;
	}

	public void setSpecies1Code(String species1Code) {
		this.species1Code = species1Code;
	}

	public String getSpecies1Percent() {
		return species1Percent;
	}

	public void setSpecies1Percent(Double species1Percent) {
		this.species1Percent = FieldFormatter.format(species1Percent);
	}

	public String getSpecies2Code() {
		return species2Code;
	}

	public void setSpecies2Code(String species2Code) {
		this.species2Code = species2Code;
	}

	public String getSpecies2Percent() {
		return species2Percent;
	}

	public void setSpecies2Percent(Double species2Percent) {
		this.species2Percent = FieldFormatter.format(species2Percent);
	}

	public String getSpecies3Code() {
		return species3Code;
	}

	public void setSpecies3Code(String species3Code) {
		this.species3Code = species3Code;
	}

	public String getSpecies3Percent() {
		return species3Percent;
	}

	public void setSpecies3Percent(Double species3Percent) {
		this.species3Percent = FieldFormatter.format(species3Percent);
	}

	public String getSpecies4Code() {
		return species4Code;
	}

	public void setSpecies4Code(String species4Code) {
		this.species4Code = species4Code;
	}

	public String getSpecies4Percent() {
		return species4Percent;
	}

	public void setSpecies4Percent(Double species4Percent) {
		this.species4Percent = FieldFormatter.format(species4Percent);
	}

	public String getSpecies5Code() {
		return species5Code;
	}

	public void setSpecies5Code(String species5Code) {
		this.species5Code = species5Code;
	}

	public String getSpecies5Percent() {
		return species5Percent;
	}

	public void setSpecies5Percent(Double species5Percent) {
		this.species5Percent = FieldFormatter.format(species5Percent);
	}

	public String getSpecies6Code() {
		return species6Code;
	}

	public void setSpecies6Code(String species6Code) {
		this.species6Code = species6Code;
	}

	public String getSpecies6Percent() {
		return species6Percent;
	}

	public void setSpecies6Percent(Double species6Percent) {
		this.species6Percent = FieldFormatter.format(species6Percent);
	}

	@Override
	public String getPercentStockable() {
		return percentStockable;
	}

	@Override
	public void setPercentStockable(Double percentStockable) {
		this.percentStockable = FieldFormatter.format(percentStockable);
	}

	@Override
	public String getSiteIndex() {
		return siteIndex;
	}

	@Override
	public void setSiteIndex(Double siteIndex) {
		this.siteIndex = FieldFormatter.format(siteIndex);
	}

	@Override
	public String getDominantHeight() {
		return dominantHeight;
	}

	@Override
	public void setDominantHeight(Double dominantHeight) {
		this.dominantHeight = FieldFormatter.format(dominantHeight);
	}

	@Override
	public String getSecondaryHeight() {
		return secondaryHeight;
	}

	@Override
	public void setSecondaryHeight(Double secondaryHeight) {
		this.secondaryHeight = FieldFormatter.format(secondaryHeight);
	}

	@Override
	public String getLoreyHeight() {
		return loreyHeight;
	}

	@Override
	public void setLoreyHeight(Double loreyHeight) {
		this.loreyHeight = FieldFormatter.format(loreyHeight);
	}

	@Override
	public String getDiameter() {
		return diameter;
	}

	@Override
	public void setDiameter(Double diameter) {
		this.diameter = FieldFormatter.format(diameter);
	}

	@Override
	public String getTreesPerHectare() {
		return treesPerHectare;
	}

	@Override
	public void setTreesPerHectare(Double treesPerHectare) {
		this.treesPerHectare = FieldFormatter.format(treesPerHectare);
	}

	@Override
	public String getBasalArea() {
		return basalArea;
	}

	@Override
	public void setBasalArea(Double basalArea) {
		this.basalArea = FieldFormatter.format(basalArea);
	}

	@Override
	public String getWholeStemVolume() {
		return wholeStemVolume;
	}

	@Override
	public void setWholeStemVolume(Double wholeStemVolume) {
		this.wholeStemVolume = FieldFormatter.format(wholeStemVolume);
	}

	@Override
	public String getCloseUtilizationVolume() {
		return closeUtilizationVolume;
	}

	@Override
	public void setCloseUtilizationVolume(Double closeUtilizationVolume) {
		this.closeUtilizationVolume = FieldFormatter.format(closeUtilizationVolume);
	}

	@Override
	public String getCuVolumeLessDecay() {
		return cuVolumeLessDecay;
	}

	@Override
	public void setCuVolumeLessDecay(Double cuVolumeLessDecay) {
		this.cuVolumeLessDecay = FieldFormatter.format(cuVolumeLessDecay);
	}

	@Override
	public String getCuVolumeLessDecayWastage() {
		return cuVolumeLessDecayWastage;
	}

	@Override
	public void setCuVolumeLessDecayWastage(Double cuVolumeLessDecayWastage) {
		this.cuVolumeLessDecayWastage = FieldFormatter.format(cuVolumeLessDecayWastage);
	}

	@Override
	public String getCuVolumeLessDecayWastageBreakage() {
		return cuVolumeLessDecayWastageBreakage;
	}

	@Override
	public void setCuVolumeLessDecayWastageBreakage(Double cuVolumeLessDecayWastageBreakage) {
		this.cuVolumeLessDecayWastageBreakage = FieldFormatter.format(cuVolumeLessDecayWastageBreakage);
	}

	@Override
	public String getMoFBiomassWholeStemVolume() {
		return moFBiomassWholeStemVolume;
	}

	@Override
	public void setMoFBiomassWholeStemVolume(Double moFBiomassWholeStemVolume) {
		this.moFBiomassWholeStemVolume = FieldFormatter.format(moFBiomassWholeStemVolume);
	}

	@Override
	public String getMoFBiomassCloseUtilizationVolume() {
		return moFBiomassCloseUtilizationVolume;
	}

	@Override
	public void setMoFBiomassCloseUtilizationVolume(Double moFBiomassCloseUtilizationVolume) {
		this.moFBiomassCloseUtilizationVolume = FieldFormatter.format(moFBiomassCloseUtilizationVolume);
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecay() {
		return moFBiomassCuVolumeLessDecay;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecay(Double moFBiomassCuVolumeLessDecay) {
		this.moFBiomassCuVolumeLessDecay = FieldFormatter.format(moFBiomassCuVolumeLessDecay);
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastage() {
		return moFBiomassCuVolumeLessDecayWastage;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastage(Double moFBiomassCuVolumeLessDecayWastage) {
		this.moFBiomassCuVolumeLessDecayWastage = FieldFormatter.format(moFBiomassCuVolumeLessDecayWastage);
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastageBreakage() {
		return moFBiomassCuVolumeLessDecayWastageBreakage;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastageBreakage(Double moFBiomassCuVolumeLessDecayWastageBreakage) {
		this.moFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(moFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies1WholeStemVolume() {
		return species1WholeStemVolume;
	}

	public void setSpecies1WholeStemVolume(Double species1WholeStemVolume) {
		this.species1WholeStemVolume = FieldFormatter.format(species1WholeStemVolume);
	}

	public String getSpecies1CloseUtilizationVolume() {
		return species1CloseUtilizationVolume;
	}

	public void setSpecies1CloseUtilizationVolume(Double species1CloseUtilizationVolume) {
		this.species1CloseUtilizationVolume = FieldFormatter.format(species1CloseUtilizationVolume);
	}

	public String getSpecies1CuVolumeLessDecay() {
		return species1CuVolumeLessDecay;
	}

	public void setSpecies1CuVolumeLessDecay(Double species1CuVolumeLessDecay) {
		this.species1CuVolumeLessDecay = FieldFormatter.format(species1CuVolumeLessDecay);
	}

	public String getSpecies1CuVolumeLessDecayWastage() {
		return species1CuVolumeLessDecayWastage;
	}

	public void setSpecies1CuVolumeLessDecayWastage(Double species1CuVolumeLessDecayWastage) {
		this.species1CuVolumeLessDecayWastage = FieldFormatter.format(species1CuVolumeLessDecayWastage);
	}

	public String getSpecies1CuVolumeLessDecayWastageBreakage() {
		return species1CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies1CuVolumeLessDecayWastageBreakage(Double species1CuVolumeLessDecayWastageBreakage) {
		this.species1CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species1CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies2WholeStemVolume() {
		return species2WholeStemVolume;
	}

	public void setSpecies2WholeStemVolume(Double species2WholeStemVolume) {
		this.species2WholeStemVolume = FieldFormatter.format(species2WholeStemVolume);
	}

	public String getSpecies2CloseUtilizationVolume() {
		return species2CloseUtilizationVolume;
	}

	public void setSpecies2CloseUtilizationVolume(Double species2CloseUtilizationVolume) {
		this.species2CloseUtilizationVolume = FieldFormatter.format(species2CloseUtilizationVolume);
	}

	public String getSpecies2CuVolumeLessDecay() {
		return species2CuVolumeLessDecay;
	}

	public void setSpecies2CuVolumeLessDecay(Double species2CuVolumeLessDecay) {
		this.species2CuVolumeLessDecay = FieldFormatter.format(species2CuVolumeLessDecay);
	}

	public String getSpecies2CuVolumeLessDecayWastage() {
		return species2CuVolumeLessDecayWastage;
	}

	public void setSpecies2CuVolumeLessDecayWastage(Double species2CuVolumeLessDecayWastage) {
		this.species2CuVolumeLessDecayWastage = FieldFormatter.format(species2CuVolumeLessDecayWastage);
	}

	public String getSpecies2CuVolumeLessDecayWastageBreakage() {
		return species2CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies2CuVolumeLessDecayWastageBreakage(Double species2CuVolumeLessDecayWastageBreakage) {
		this.species2CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species2CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies3WholeStemVolume() {
		return species3WholeStemVolume;
	}

	public void setSpecies3WholeStemVolume(Double species3WholeStemVolume) {
		this.species3WholeStemVolume = FieldFormatter.format(species3WholeStemVolume);
	}

	public String getSpecies3CloseUtilizationVolume() {
		return species3CloseUtilizationVolume;
	}

	public void setSpecies3CloseUtilizationVolume(Double species3CloseUtilizationVolume) {
		this.species3CloseUtilizationVolume = FieldFormatter.format(species3CloseUtilizationVolume);
	}

	public String getSpecies3CuVolumeLessDecay() {
		return species3CuVolumeLessDecay;
	}

	public void setSpecies3CuVolumeLessDecay(Double species3CuVolumeLessDecay) {
		this.species3CuVolumeLessDecay = FieldFormatter.format(species3CuVolumeLessDecay);
	}

	public String getSpecies3CuVolumeLessDecayWastage() {
		return species3CuVolumeLessDecayWastage;
	}

	public void setSpecies3CuVolumeLessDecayWastage(Double species3CuVolumeLessDecayWastage) {
		this.species3CuVolumeLessDecayWastage = FieldFormatter.format(species3CuVolumeLessDecayWastage);
	}

	public String getSpecies3CuVolumeLessDecayWastageBreakage() {
		return species3CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies3CuVolumeLessDecayWastageBreakage(Double species3CuVolumeLessDecayWastageBreakage) {
		this.species3CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species3CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies4WholeStemVolume() {
		return species4WholeStemVolume;
	}

	public void setSpecies4WholeStemVolume(Double species4WholeStemVolume) {
		this.species4WholeStemVolume = FieldFormatter.format(species4WholeStemVolume);
	}

	public String getSpecies4CloseUtilizationVolume() {
		return species4CloseUtilizationVolume;
	}

	public void setSpecies4CloseUtilizationVolume(Double species4CloseUtilizationVolume) {
		this.species4CloseUtilizationVolume = FieldFormatter.format(species4CloseUtilizationVolume);
	}

	public String getSpecies4CuVolumeLessDecay() {
		return species4CuVolumeLessDecay;
	}

	public void setSpecies4CuVolumeLessDecay(Double species4CuVolumeLessDecay) {
		this.species4CuVolumeLessDecay = FieldFormatter.format(species4CuVolumeLessDecay);
	}

	public String getSpecies4CuVolumeLessDecayWastage() {
		return species4CuVolumeLessDecayWastage;
	}

	public void setSpecies4CuVolumeLessDecayWastage(Double species4CuVolumeLessDecayWastage) {
		this.species4CuVolumeLessDecayWastage = FieldFormatter.format(species4CuVolumeLessDecayWastage);
	}

	public String getSpecies4CuVolumeLessDecayWastageBreakage() {
		return species4CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies4CuVolumeLessDecayWastageBreakage(Double species4CuVolumeLessDecayWastageBreakage) {
		this.species4CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species4CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies5WholeStemVolume() {
		return species5WholeStemVolume;
	}

	public void setSpecies5WholeStemVolume(Double species5WholeStemVolume) {
		this.species5WholeStemVolume = FieldFormatter.format(species5WholeStemVolume);
	}

	public String getSpecies5CloseUtilizationVolume() {
		return species5CloseUtilizationVolume;
	}

	public void setSpecies5CloseUtilizationVolume(Double species5CloseUtilizationVolume) {
		this.species5CloseUtilizationVolume = FieldFormatter.format(species5CloseUtilizationVolume);
	}

	public String getSpecies5CuVolumeLessDecay() {
		return species5CuVolumeLessDecay;
	}

	public void setSpecies5CuVolumeLessDecay(Double species5CuVolumeLessDecay) {
		this.species5CuVolumeLessDecay = FieldFormatter.format(species5CuVolumeLessDecay);
	}

	public String getSpecies5CuVolumeLessDecayWastage() {
		return species5CuVolumeLessDecayWastage;
	}

	public void setSpecies5CuVolumeLessDecayWastage(Double species5CuVolumeLessDecayWastage) {
		this.species5CuVolumeLessDecayWastage = FieldFormatter.format(species5CuVolumeLessDecayWastage);
	}

	public String getSpecies5CuVolumeLessDecayWastageBreakage() {
		return species5CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies5CuVolumeLessDecayWastageBreakage(Double species5CuVolumeLessDecayWastageBreakage) {
		this.species5CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species5CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies6WholeStemVolume() {
		return species6WholeStemVolume;
	}

	public void setSpecies6WholeStemVolume(Double species6WholeStemVolume) {
		this.species6WholeStemVolume = FieldFormatter.format(species6WholeStemVolume);
	}

	public String getSpecies6CloseUtilizationVolume() {
		return species6CloseUtilizationVolume;
	}

	public void setSpecies6CloseUtilizationVolume(Double species6CloseUtilizationVolume) {
		this.species6CloseUtilizationVolume = FieldFormatter.format(species6CloseUtilizationVolume);
	}

	public String getSpecies6CuVolumeLessDecay() {
		return species6CuVolumeLessDecay;
	}

	public void setSpecies6CuVolumeLessDecay(Double species6CuVolumeLessDecay) {
		this.species6CuVolumeLessDecay = FieldFormatter.format(species6CuVolumeLessDecay);
	}

	public String getSpecies6CuVolumeLessDecayWastage() {
		return species6CuVolumeLessDecayWastage;
	}

	public void setSpecies6CuVolumeLessDecayWastage(Double species6CuVolumeLessDecayWastage) {
		this.species6CuVolumeLessDecayWastage = FieldFormatter.format(species6CuVolumeLessDecayWastage);
	}

	public String getSpecies6CuVolumeLessDecayWastageBreakage() {
		return species6CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies6CuVolumeLessDecayWastageBreakage(Double species6CuVolumeLessDecayWastageBreakage) {
		this.species6CuVolumeLessDecayWastageBreakage = FieldFormatter.format(species6CuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies1MoFBiomassWholeStemVolume() {
		return species1MoFBiomassWholeStemVolume;
	}

	public void setSpecies1MoFBiomassWholeStemVolume(Double species1MoFBiomassWholeStemVolume) {
		this.species1MoFBiomassWholeStemVolume = FieldFormatter.format(species1MoFBiomassWholeStemVolume);
	}

	public String getSpecies1MoFBiomassCloseUtilizationVolume() {
		return species1MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies1MoFBiomassCloseUtilizationVolume(Double species1MoFBiomassCloseUtilizationVolume) {
		this.species1MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species1MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecay() {
		return species1MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecay(Double species1MoFBiomassCuVolumeLessDecay) {
		this.species1MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species1MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecayWastage() {
		return species1MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecayWastage(Double species1MoFBiomassCuVolumeLessDecayWastage) {
		this.species1MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species1MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species1MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species1MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species1MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species1MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies2MoFBiomassWholeStemVolume() {
		return species2MoFBiomassWholeStemVolume;
	}

	public void setSpecies2MoFBiomassWholeStemVolume(Double species2MoFBiomassWholeStemVolume) {
		this.species2MoFBiomassWholeStemVolume = FieldFormatter.format(species2MoFBiomassWholeStemVolume);
	}

	public String getSpecies2MoFBiomassCloseUtilizationVolume() {
		return species2MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies2MoFBiomassCloseUtilizationVolume(Double species2MoFBiomassCloseUtilizationVolume) {
		this.species2MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species2MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecay() {
		return species2MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecay(Double species2MoFBiomassCuVolumeLessDecay) {
		this.species2MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species2MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecayWastage() {
		return species2MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecayWastage(Double species2MoFBiomassCuVolumeLessDecayWastage) {
		this.species2MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species2MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species2MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species2MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species2MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species2MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies3MoFBiomassWholeStemVolume() {
		return species3MoFBiomassWholeStemVolume;
	}

	public void setSpecies3MoFBiomassWholeStemVolume(Double species3MoFBiomassWholeStemVolume) {
		this.species3MoFBiomassWholeStemVolume = FieldFormatter.format(species3MoFBiomassWholeStemVolume);
	}

	public String getSpecies3MoFBiomassCloseUtilizationVolume() {
		return species3MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies3MoFBiomassCloseUtilizationVolume(Double species3MoFBiomassCloseUtilizationVolume) {
		this.species3MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species3MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecay() {
		return species3MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecay(Double species3MoFBiomassCuVolumeLessDecay) {
		this.species3MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species3MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecayWastage() {
		return species3MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecayWastage(Double species3MoFBiomassCuVolumeLessDecayWastage) {
		this.species3MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species3MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species3MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species3MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species3MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species3MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies4MoFBiomassWholeStemVolume() {
		return species4MoFBiomassWholeStemVolume;
	}

	public void setSpecies4MoFBiomassWholeStemVolume(Double species4MoFBiomassWholeStemVolume) {
		this.species4MoFBiomassWholeStemVolume = FieldFormatter.format(species4MoFBiomassWholeStemVolume);
	}

	public String getSpecies4MoFBiomassCloseUtilizationVolume() {
		return species4MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies4MoFBiomassCloseUtilizationVolume(Double species4MoFBiomassCloseUtilizationVolume) {
		this.species4MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species4MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecay() {
		return species4MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecay(Double species4MoFBiomassCuVolumeLessDecay) {
		this.species4MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species4MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecayWastage() {
		return species4MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecayWastage(Double species4MoFBiomassCuVolumeLessDecayWastage) {
		this.species4MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species4MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species4MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species4MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species4MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species4MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies5MoFBiomassWholeStemVolume() {
		return species5MoFBiomassWholeStemVolume;
	}

	public void setSpecies5MoFBiomassWholeStemVolume(Double species5MoFBiomassWholeStemVolume) {
		this.species5MoFBiomassWholeStemVolume = FieldFormatter.format(species5MoFBiomassWholeStemVolume);
	}

	public String getSpecies5MoFBiomassCloseUtilizationVolume() {
		return species5MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies5MoFBiomassCloseUtilizationVolume(Double species5MoFBiomassCloseUtilizationVolume) {
		this.species5MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species5MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecay() {
		return species5MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecay(Double species5MoFBiomassCuVolumeLessDecay) {
		this.species5MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species5MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecayWastage() {
		return species5MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecayWastage(Double species5MoFBiomassCuVolumeLessDecayWastage) {
		this.species5MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species5MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species5MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species5MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species5MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species5MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	public String getSpecies6MoFBiomassWholeStemVolume() {
		return species6MoFBiomassWholeStemVolume;
	}

	public void setSpecies6MoFBiomassWholeStemVolume(Double species6MoFBiomassWholeStemVolume) {
		this.species6MoFBiomassWholeStemVolume = FieldFormatter.format(species6MoFBiomassWholeStemVolume);
	}

	public String getSpecies6MoFBiomassCloseUtilizationVolume() {
		return species6MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies6MoFBiomassCloseUtilizationVolume(Double species6MoFBiomassCloseUtilizationVolume) {
		this.species6MoFBiomassCloseUtilizationVolume = FieldFormatter.format(species6MoFBiomassCloseUtilizationVolume);
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecay() {
		return species6MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecay(Double species6MoFBiomassCuVolumeLessDecay) {
		this.species6MoFBiomassCuVolumeLessDecay = FieldFormatter.format(species6MoFBiomassCuVolumeLessDecay);
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecayWastage() {
		return species6MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecayWastage(Double species6MoFBiomassCuVolumeLessDecayWastage) {
		this.species6MoFBiomassCuVolumeLessDecayWastage = FieldFormatter
				.format(species6MoFBiomassCuVolumeLessDecayWastage);
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species6MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecayWastageBreakage(
			Double species6MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species6MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(species6MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	@Override
	public String getCfsBiomassStem() {
		return cfsBiomassStem;
	}

	@Override
	public void setCfsBiomassStem(Double cfsBiomassStem) {
		this.cfsBiomassStem = FieldFormatter.format(cfsBiomassStem);
	}

	@Override
	public String getCfsBiomassBark() {
		return cfsBiomassBark;
	}

	@Override
	public void setCfsBiomassBark(Double cfsBiomassBark) {
		this.cfsBiomassBark = FieldFormatter.format(cfsBiomassBark);
	}

	@Override
	public String getCfsBiomassBranch() {
		return cfsBiomassBranch;
	}

	@Override
	public void setCfsBiomassBranch(Double cfsBiomassBranch) {
		this.cfsBiomassBranch = FieldFormatter.format(cfsBiomassBranch);
	}

	@Override
	public String getCfsBiomassFoliage() {
		return cfsBiomassFoliage;
	}

	@Override
	public void setCfsBiomassFoliage(Double cfsBiomassFoliage) {
		this.cfsBiomassFoliage = FieldFormatter.format(cfsBiomassFoliage);
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void setMode(String mode) {
		this.mode = mode;
	}

	private record MultiFieldKey(
			String method, //
			MultiFieldPrefixes prefix, //
			int speciesNumber, //
			MultiFieldSuffixes suffix //
	) {
		@Override
		public String toString() {
			return method + prefix.name() + speciesNumber + suffix.name();
		}
	}

	static private final Map<MultiFieldKey, Method> getters = new HashMap<>();
	static private final Map<MultiFieldKey, Method> setters = new HashMap<>();

	static {
		Class<TextYieldTableRowValuesBean> clazz = TextYieldTableRowValuesBean.class;
		try {
			for (int i = 1; i <= 6; i++) {
				getters.put(
						new MultiFieldKey("get", Species, i, Code), clazz.getDeclaredMethod("getSpecies" + i + "Code")
				);
				getters.put(
						new MultiFieldKey("get", Species, i, Percent),
						clazz.getDeclaredMethod("getSpecies" + i + "Percent")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, WholeStemVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "WholeStemVolume")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, CloseUtilizationVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "CloseUtilizationVolume")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, CuVolumeLessDecay),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecay")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, CuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecayWastage")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, CuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecayWastageBreakage")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, MoFBiomassWholeStemVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassWholeStemVolume")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, MoFBiomassCloseUtilizationVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCloseUtilizationVolume")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, MoFBiomassCuVolumeLessDecay),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecay")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecayWastage")
				);
				getters.put(
						new MultiFieldKey("get", SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecayWastageBreakage")
				);

				setters.put(
						new MultiFieldKey("set", Species, i, Code),
						clazz.getDeclaredMethod("setSpecies" + i + "Code", String.class)
				);
				setters.put(
						new MultiFieldKey("set", Species, i, Percent),
						clazz.getDeclaredMethod("setSpecies" + i + "Percent", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, WholeStemVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "WholeStemVolume", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, CloseUtilizationVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "CloseUtilizationVolume", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, CuVolumeLessDecay),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecay", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, CuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecayWastage", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, CuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecayWastageBreakage", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, MoFBiomassWholeStemVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassWholeStemVolume", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, MoFBiomassCloseUtilizationVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCloseUtilizationVolume", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, MoFBiomassCuVolumeLessDecay),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCuVolumeLessDecay", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCuVolumeLessDecayWastage", Double.class)
				);
				setters.put(
						new MultiFieldKey("set", SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod(
								"setSpecies" + i + "MoFBiomassCuVolumeLessDecayWastageBreakage", Double.class
						)
				);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix) {

		var key = new MultiFieldKey("get", prefix, speciesNumber, suffix);

		Validate.isTrue(getters.containsKey(key), MessageFormat.format("getters must contain key {0}", key));

		try {
			return (String) getters.get(key).invoke(this);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, String value
	) {
		var key = new MultiFieldKey("set", prefix, speciesNumber, suffix);
		Validate.isTrue(
				setters.containsKey(key),
				MessageFormat.format(
						" TextYieldTableRowValuesBean.setSpeciesFieldValue: setters must already contain key {0}", key
				)
		);

		try {
			setters.get(key).invoke(this, value);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, Double value
	) {
		var key = new MultiFieldKey("set", prefix, speciesNumber, suffix);
		Validate.isTrue(
				setters.containsKey(key),
				MessageFormat.format(
						" TextYieldTableRowValuesBean.setSpeciesFieldValue: setters must already contain key {0}", key
				)
		);

		try {
			setters.get(key).invoke(this, value);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, Integer value
	) {
		var key = new MultiFieldKey("set", prefix, speciesNumber, suffix);
		Validate.isTrue(
				setters.containsKey(key),
				MessageFormat.format(
						" TextYieldTableRowValuesBean.setSpeciesFieldValue: setters must already contain key {0}", key
				)
		);

		try {
			setters.get(key).invoke(this, value);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
