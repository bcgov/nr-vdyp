package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldPrefixes.*;
import static ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldSuffixes.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
class TextYieldTableRowValuesBean implements YieldTableRowValues {

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
	public void setTableNumber(String tableNumber) {
		this.tableNumber = tableNumber;
	}

	@Override
	public String getFeatureId() {
		return featureId;
	}

	@Override
	public void setFeatureId(String featureId) {
		this.featureId = featureId;
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
	public void setPolygonId(String polygonId) {
		this.polygonId = polygonId;
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
	public void setProjectionYear(String projectionYear) {
		this.projectionYear = projectionYear;
	}

	@Override
	public String getTotalAge() {
		return totalAge;
	}

	@Override
	public void setTotalAge(String totalAge) {
		this.totalAge = totalAge;
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

	public void setSpecies1Percent(String species1Percent) {
		this.species1Percent = species1Percent;
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

	public void setSpecies2Percent(String species2Percent) {
		this.species2Percent = species2Percent;
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

	public void setSpecies3Percent(String species3Percent) {
		this.species3Percent = species3Percent;
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

	public void setSpecies4Percent(String species4Percent) {
		this.species4Percent = species4Percent;
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

	public void setSpecies5Percent(String species5Percent) {
		this.species5Percent = species5Percent;
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

	public void setSpecies6Percent(String species6Percent) {
		this.species6Percent = species6Percent;
	}

	@Override
	public String getPercentStockable() {
		return percentStockable;
	}

	@Override
	public void setPercentStockable(String percentStockable) {
		this.percentStockable = percentStockable;
	}

	@Override
	public String getSiteIndex() {
		return siteIndex;
	}

	@Override
	public void setSiteIndex(String siteIndex) {
		this.siteIndex = siteIndex;
	}

	@Override
	public String getDominantHeight() {
		return dominantHeight;
	}

	@Override
	public void setDominantHeight(String dominantHeight) {
		this.dominantHeight = dominantHeight;
	}

	@Override
	public String getSecondaryHeight() {
		return secondaryHeight;
	}

	@Override
	public void setSecondaryHeight(String secondaryHeight) {
		this.secondaryHeight = secondaryHeight;
	}

	@Override
	public String getLoreyHeight() {
		return loreyHeight;
	}

	@Override
	public void setLoreyHeight(String loreyHeight) {
		this.loreyHeight = loreyHeight;
	}

	@Override
	public String getDiameter() {
		return diameter;
	}

	@Override
	public void setDiameter(String diameter) {
		this.diameter = diameter;
	}

	@Override
	public String getTreesPerHectare() {
		return treesPerHectare;
	}

	@Override
	public void setTreesPerHectare(String treesPerHectare) {
		this.treesPerHectare = treesPerHectare;
	}

	@Override
	public String getBasalArea() {
		return basalArea;
	}

	@Override
	public void setBasalArea(String basalArea) {
		this.basalArea = basalArea;
	}

	@Override
	public String getWholeStemVolume() {
		return wholeStemVolume;
	}

	@Override
	public void setWholeStemVolume(String wholeStemVolume) {
		this.wholeStemVolume = wholeStemVolume;
	}

	@Override
	public String getCloseUtilizationVolume() {
		return closeUtilizationVolume;
	}

	@Override
	public void setCloseUtilizationVolume(String closeUtilizationVolume) {
		this.closeUtilizationVolume = closeUtilizationVolume;
	}

	@Override
	public String getCuVolumeLessDecay() {
		return cuVolumeLessDecay;
	}

	@Override
	public void setCuVolumeLessDecay(String cuVolumeLessDecay) {
		this.cuVolumeLessDecay = cuVolumeLessDecay;
	}

	@Override
	public String getCuVolumeLessDecayWastage() {
		return cuVolumeLessDecayWastage;
	}

	@Override
	public void setCuVolumeLessDecayWastage(String cuVolumeLessDecayWastage) {
		this.cuVolumeLessDecayWastage = cuVolumeLessDecayWastage;
	}

	@Override
	public String getCuVolumeLessDecayWastageBreakage() {
		return cuVolumeLessDecayWastageBreakage;
	}

	@Override
	public void setCuVolumeLessDecayWastageBreakage(String cuVolumeLessDecayWastageBreakage) {
		this.cuVolumeLessDecayWastageBreakage = cuVolumeLessDecayWastageBreakage;
	}

	@Override
	public String getMoFBiomassWholeStemVolume() {
		return moFBiomassWholeStemVolume;
	}

	@Override
	public void setMoFBiomassWholeStemVolume(String moFBiomassWholeStemVolume) {
		this.moFBiomassWholeStemVolume = moFBiomassWholeStemVolume;
	}

	@Override
	public String getMoFBiomassCloseUtilizationVolume() {
		return moFBiomassCloseUtilizationVolume;
	}

	@Override
	public void setMoFBiomassCloseUtilizationVolume(String moFBiomassCloseUtilizationVolume) {
		this.moFBiomassCloseUtilizationVolume = moFBiomassCloseUtilizationVolume;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecay() {
		return moFBiomassCuVolumeLessDecay;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecay(String moFBiomassCuVolumeLessDecay) {
		this.moFBiomassCuVolumeLessDecay = moFBiomassCuVolumeLessDecay;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastage() {
		return moFBiomassCuVolumeLessDecayWastage;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastage(String moFBiomassCuVolumeLessDecayWastage) {
		this.moFBiomassCuVolumeLessDecayWastage = moFBiomassCuVolumeLessDecayWastage;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastageBreakage() {
		return moFBiomassCuVolumeLessDecayWastageBreakage;
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastageBreakage(String moFBiomassCuVolumeLessDecayWastageBreakage) {
		this.moFBiomassCuVolumeLessDecayWastageBreakage = moFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies1WholeStemVolume() {
		return species1WholeStemVolume;
	}

	public void setSpecies1WholeStemVolume(String species1WholeStemVolume) {
		this.species1WholeStemVolume = species1WholeStemVolume;
	}

	public String getSpecies1CloseUtilizationVolume() {
		return species1CloseUtilizationVolume;
	}

	public void setSpecies1CloseUtilizationVolume(String species1CloseUtilizationVolume) {
		this.species1CloseUtilizationVolume = species1CloseUtilizationVolume;
	}

	public String getSpecies1CuVolumeLessDecay() {
		return species1CuVolumeLessDecay;
	}

	public void setSpecies1CuVolumeLessDecay(String species1CuVolumeLessDecay) {
		this.species1CuVolumeLessDecay = species1CuVolumeLessDecay;
	}

	public String getSpecies1CuVolumeLessDecayWastage() {
		return species1CuVolumeLessDecayWastage;
	}

	public void setSpecies1CuVolumeLessDecayWastage(String species1CuVolumeLessDecayWastage) {
		this.species1CuVolumeLessDecayWastage = species1CuVolumeLessDecayWastage;
	}

	public String getSpecies1CuVolumeLessDecayWastageBreakage() {
		return species1CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies1CuVolumeLessDecayWastageBreakage(String species1CuVolumeLessDecayWastageBreakage) {
		this.species1CuVolumeLessDecayWastageBreakage = species1CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies2WholeStemVolume() {
		return species2WholeStemVolume;
	}

	public void setSpecies2WholeStemVolume(String species2WholeStemVolume) {
		this.species2WholeStemVolume = species2WholeStemVolume;
	}

	public String getSpecies2CloseUtilizationVolume() {
		return species2CloseUtilizationVolume;
	}

	public void setSpecies2CloseUtilizationVolume(String species2CloseUtilizationVolume) {
		this.species2CloseUtilizationVolume = species2CloseUtilizationVolume;
	}

	public String getSpecies2CuVolumeLessDecay() {
		return species2CuVolumeLessDecay;
	}

	public void setSpecies2CuVolumeLessDecay(String species2CuVolumeLessDecay) {
		this.species2CuVolumeLessDecay = species2CuVolumeLessDecay;
	}

	public String getSpecies2CuVolumeLessDecayWastage() {
		return species2CuVolumeLessDecayWastage;
	}

	public void setSpecies2CuVolumeLessDecayWastage(String species2CuVolumeLessDecayWastage) {
		this.species2CuVolumeLessDecayWastage = species2CuVolumeLessDecayWastage;
	}

	public String getSpecies2CuVolumeLessDecayWastageBreakage() {
		return species2CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies2CuVolumeLessDecayWastageBreakage(String species2CuVolumeLessDecayWastageBreakage) {
		this.species2CuVolumeLessDecayWastageBreakage = species2CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies3WholeStemVolume() {
		return species3WholeStemVolume;
	}

	public void setSpecies3WholeStemVolume(String species3WholeStemVolume) {
		this.species3WholeStemVolume = species3WholeStemVolume;
	}

	public String getSpecies3CloseUtilizationVolume() {
		return species3CloseUtilizationVolume;
	}

	public void setSpecies3CloseUtilizationVolume(String species3CloseUtilizationVolume) {
		this.species3CloseUtilizationVolume = species3CloseUtilizationVolume;
	}

	public String getSpecies3CuVolumeLessDecay() {
		return species3CuVolumeLessDecay;
	}

	public void setSpecies3CuVolumeLessDecay(String species3CuVolumeLessDecay) {
		this.species3CuVolumeLessDecay = species3CuVolumeLessDecay;
	}

	public String getSpecies3CuVolumeLessDecayWastage() {
		return species3CuVolumeLessDecayWastage;
	}

	public void setSpecies3CuVolumeLessDecayWastage(String species3CuVolumeLessDecayWastage) {
		this.species3CuVolumeLessDecayWastage = species3CuVolumeLessDecayWastage;
	}

	public String getSpecies3CuVolumeLessDecayWastageBreakage() {
		return species3CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies3CuVolumeLessDecayWastageBreakage(String species3CuVolumeLessDecayWastageBreakage) {
		this.species3CuVolumeLessDecayWastageBreakage = species3CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies4WholeStemVolume() {
		return species4WholeStemVolume;
	}

	public void setSpecies4WholeStemVolume(String species4WholeStemVolume) {
		this.species4WholeStemVolume = species4WholeStemVolume;
	}

	public String getSpecies4CloseUtilizationVolume() {
		return species4CloseUtilizationVolume;
	}

	public void setSpecies4CloseUtilizationVolume(String species4CloseUtilizationVolume) {
		this.species4CloseUtilizationVolume = species4CloseUtilizationVolume;
	}

	public String getSpecies4CuVolumeLessDecay() {
		return species4CuVolumeLessDecay;
	}

	public void setSpecies4CuVolumeLessDecay(String species4CuVolumeLessDecay) {
		this.species4CuVolumeLessDecay = species4CuVolumeLessDecay;
	}

	public String getSpecies4CuVolumeLessDecayWastage() {
		return species4CuVolumeLessDecayWastage;
	}

	public void setSpecies4CuVolumeLessDecayWastage(String species4CuVolumeLessDecayWastage) {
		this.species4CuVolumeLessDecayWastage = species4CuVolumeLessDecayWastage;
	}

	public String getSpecies4CuVolumeLessDecayWastageBreakage() {
		return species4CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies4CuVolumeLessDecayWastageBreakage(String species4CuVolumeLessDecayWastageBreakage) {
		this.species4CuVolumeLessDecayWastageBreakage = species4CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies5WholeStemVolume() {
		return species5WholeStemVolume;
	}

	public void setSpecies5WholeStemVolume(String species5WholeStemVolume) {
		this.species5WholeStemVolume = species5WholeStemVolume;
	}

	public String getSpecies5CloseUtilizationVolume() {
		return species5CloseUtilizationVolume;
	}

	public void setSpecies5CloseUtilizationVolume(String species5CloseUtilizationVolume) {
		this.species5CloseUtilizationVolume = species5CloseUtilizationVolume;
	}

	public String getSpecies5CuVolumeLessDecay() {
		return species5CuVolumeLessDecay;
	}

	public void setSpecies5CuVolumeLessDecay(String species5CuVolumeLessDecay) {
		this.species5CuVolumeLessDecay = species5CuVolumeLessDecay;
	}

	public String getSpecies5CuVolumeLessDecayWastage() {
		return species5CuVolumeLessDecayWastage;
	}

	public void setSpecies5CuVolumeLessDecayWastage(String species5CuVolumeLessDecayWastage) {
		this.species5CuVolumeLessDecayWastage = species5CuVolumeLessDecayWastage;
	}

	public String getSpecies5CuVolumeLessDecayWastageBreakage() {
		return species5CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies5CuVolumeLessDecayWastageBreakage(String species5CuVolumeLessDecayWastageBreakage) {
		this.species5CuVolumeLessDecayWastageBreakage = species5CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies6WholeStemVolume() {
		return species6WholeStemVolume;
	}

	public void setSpecies6WholeStemVolume(String species6WholeStemVolume) {
		this.species6WholeStemVolume = species6WholeStemVolume;
	}

	public String getSpecies6CloseUtilizationVolume() {
		return species6CloseUtilizationVolume;
	}

	public void setSpecies6CloseUtilizationVolume(String species6CloseUtilizationVolume) {
		this.species6CloseUtilizationVolume = species6CloseUtilizationVolume;
	}

	public String getSpecies6CuVolumeLessDecay() {
		return species6CuVolumeLessDecay;
	}

	public void setSpecies6CuVolumeLessDecay(String species6CuVolumeLessDecay) {
		this.species6CuVolumeLessDecay = species6CuVolumeLessDecay;
	}

	public String getSpecies6CuVolumeLessDecayWastage() {
		return species6CuVolumeLessDecayWastage;
	}

	public void setSpecies6CuVolumeLessDecayWastage(String species6CuVolumeLessDecayWastage) {
		this.species6CuVolumeLessDecayWastage = species6CuVolumeLessDecayWastage;
	}

	public String getSpecies6CuVolumeLessDecayWastageBreakage() {
		return species6CuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies6CuVolumeLessDecayWastageBreakage(String species6CuVolumeLessDecayWastageBreakage) {
		this.species6CuVolumeLessDecayWastageBreakage = species6CuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies1MoFBiomassWholeStemVolume() {
		return species1MoFBiomassWholeStemVolume;
	}

	public void setSpecies1MoFBiomassWholeStemVolume(String species1MoFBiomassWholeStemVolume) {
		this.species1MoFBiomassWholeStemVolume = species1MoFBiomassWholeStemVolume;
	}

	public String getSpecies1MoFBiomassCloseUtilizationVolume() {
		return species1MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies1MoFBiomassCloseUtilizationVolume(String species1MoFBiomassCloseUtilizationVolume) {
		this.species1MoFBiomassCloseUtilizationVolume = species1MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecay() {
		return species1MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecay(String species1MoFBiomassCuVolumeLessDecay) {
		this.species1MoFBiomassCuVolumeLessDecay = species1MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecayWastage() {
		return species1MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecayWastage(String species1MoFBiomassCuVolumeLessDecayWastage) {
		this.species1MoFBiomassCuVolumeLessDecayWastage = species1MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies1MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species1MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies1MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species1MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species1MoFBiomassCuVolumeLessDecayWastageBreakage = species1MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies2MoFBiomassWholeStemVolume() {
		return species2MoFBiomassWholeStemVolume;
	}

	public void setSpecies2MoFBiomassWholeStemVolume(String species2MoFBiomassWholeStemVolume) {
		this.species2MoFBiomassWholeStemVolume = species2MoFBiomassWholeStemVolume;
	}

	public String getSpecies2MoFBiomassCloseUtilizationVolume() {
		return species2MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies2MoFBiomassCloseUtilizationVolume(String species2MoFBiomassCloseUtilizationVolume) {
		this.species2MoFBiomassCloseUtilizationVolume = species2MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecay() {
		return species2MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecay(String species2MoFBiomassCuVolumeLessDecay) {
		this.species2MoFBiomassCuVolumeLessDecay = species2MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecayWastage() {
		return species2MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecayWastage(String species2MoFBiomassCuVolumeLessDecayWastage) {
		this.species2MoFBiomassCuVolumeLessDecayWastage = species2MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies2MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species2MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies2MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species2MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species2MoFBiomassCuVolumeLessDecayWastageBreakage = species2MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies3MoFBiomassWholeStemVolume() {
		return species3MoFBiomassWholeStemVolume;
	}

	public void setSpecies3MoFBiomassWholeStemVolume(String species3MoFBiomassWholeStemVolume) {
		this.species3MoFBiomassWholeStemVolume = species3MoFBiomassWholeStemVolume;
	}

	public String getSpecies3MoFBiomassCloseUtilizationVolume() {
		return species3MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies3MoFBiomassCloseUtilizationVolume(String species3MoFBiomassCloseUtilizationVolume) {
		this.species3MoFBiomassCloseUtilizationVolume = species3MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecay() {
		return species3MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecay(String species3MoFBiomassCuVolumeLessDecay) {
		this.species3MoFBiomassCuVolumeLessDecay = species3MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecayWastage() {
		return species3MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecayWastage(String species3MoFBiomassCuVolumeLessDecayWastage) {
		this.species3MoFBiomassCuVolumeLessDecayWastage = species3MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies3MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species3MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies3MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species3MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species3MoFBiomassCuVolumeLessDecayWastageBreakage = species3MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies4MoFBiomassWholeStemVolume() {
		return species4MoFBiomassWholeStemVolume;
	}

	public void setSpecies4MoFBiomassWholeStemVolume(String species4MoFBiomassWholeStemVolume) {
		this.species4MoFBiomassWholeStemVolume = species4MoFBiomassWholeStemVolume;
	}

	public String getSpecies4MoFBiomassCloseUtilizationVolume() {
		return species4MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies4MoFBiomassCloseUtilizationVolume(String species4MoFBiomassCloseUtilizationVolume) {
		this.species4MoFBiomassCloseUtilizationVolume = species4MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecay() {
		return species4MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecay(String species4MoFBiomassCuVolumeLessDecay) {
		this.species4MoFBiomassCuVolumeLessDecay = species4MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecayWastage() {
		return species4MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecayWastage(String species4MoFBiomassCuVolumeLessDecayWastage) {
		this.species4MoFBiomassCuVolumeLessDecayWastage = species4MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies4MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species4MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies4MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species4MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species4MoFBiomassCuVolumeLessDecayWastageBreakage = species4MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies5MoFBiomassWholeStemVolume() {
		return species5MoFBiomassWholeStemVolume;
	}

	public void setSpecies5MoFBiomassWholeStemVolume(String species5MoFBiomassWholeStemVolume) {
		this.species5MoFBiomassWholeStemVolume = species5MoFBiomassWholeStemVolume;
	}

	public String getSpecies5MoFBiomassCloseUtilizationVolume() {
		return species5MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies5MoFBiomassCloseUtilizationVolume(String species5MoFBiomassCloseUtilizationVolume) {
		this.species5MoFBiomassCloseUtilizationVolume = species5MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecay() {
		return species5MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecay(String species5MoFBiomassCuVolumeLessDecay) {
		this.species5MoFBiomassCuVolumeLessDecay = species5MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecayWastage() {
		return species5MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecayWastage(String species5MoFBiomassCuVolumeLessDecayWastage) {
		this.species5MoFBiomassCuVolumeLessDecayWastage = species5MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies5MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species5MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies5MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species5MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species5MoFBiomassCuVolumeLessDecayWastageBreakage = species5MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public String getSpecies6MoFBiomassWholeStemVolume() {
		return species6MoFBiomassWholeStemVolume;
	}

	public void setSpecies6MoFBiomassWholeStemVolume(String species6MoFBiomassWholeStemVolume) {
		this.species6MoFBiomassWholeStemVolume = species6MoFBiomassWholeStemVolume;
	}

	public String getSpecies6MoFBiomassCloseUtilizationVolume() {
		return species6MoFBiomassCloseUtilizationVolume;
	}

	public void setSpecies6MoFBiomassCloseUtilizationVolume(String species6MoFBiomassCloseUtilizationVolume) {
		this.species6MoFBiomassCloseUtilizationVolume = species6MoFBiomassCloseUtilizationVolume;
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecay() {
		return species6MoFBiomassCuVolumeLessDecay;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecay(String species6MoFBiomassCuVolumeLessDecay) {
		this.species6MoFBiomassCuVolumeLessDecay = species6MoFBiomassCuVolumeLessDecay;
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecayWastage() {
		return species6MoFBiomassCuVolumeLessDecayWastage;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecayWastage(String species6MoFBiomassCuVolumeLessDecayWastage) {
		this.species6MoFBiomassCuVolumeLessDecayWastage = species6MoFBiomassCuVolumeLessDecayWastage;
	}

	public String getSpecies6MoFBiomassCuVolumeLessDecayWastageBreakage() {
		return species6MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	public void setSpecies6MoFBiomassCuVolumeLessDecayWastageBreakage(
			String species6MoFBiomassCuVolumeLessDecayWastageBreakage
	) {
		this.species6MoFBiomassCuVolumeLessDecayWastageBreakage = species6MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	@Override
	public String getCfsBiomassStem() {
		return cfsBiomassStem;
	}

	@Override
	public void setCfsBiomassStem(String cfsBiomassStem) {
		this.cfsBiomassStem = cfsBiomassStem;
	}

	@Override
	public String getCfsBiomassBark() {
		return cfsBiomassBark;
	}

	@Override
	public void setCfsBiomassBark(String cfsBiomassBark) {
		this.cfsBiomassBark = cfsBiomassBark;
	}

	@Override
	public String getCfsBiomassBranch() {
		return cfsBiomassBranch;
	}

	@Override
	public void setCfsBiomassBranch(String cfsBiomassBranch) {
		this.cfsBiomassBranch = cfsBiomassBranch;
	}

	@Override
	public String getCfsBiomassFoliage() {
		return cfsBiomassFoliage;
	}

	@Override
	public void setCfsBiomassFoliage(String cfsBiomassFoliage) {
		this.cfsBiomassFoliage = cfsBiomassFoliage;
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
			MultiFieldPrefixes prefix, //
			int speciesNumber, //
			MultiFieldSuffixes suffix //
	) {
		@Override
		public String toString() {
			return prefix.name() + speciesNumber + suffix.name();
		}
	}

	static private final Map<MultiFieldKey, Method> getters = new HashMap<>();
	static private final Map<MultiFieldKey, Method> setters = new HashMap<>();

	static {
		Class<TextYieldTableRowValuesBean> clazz = TextYieldTableRowValuesBean.class;
		try {
			for (int i = 1; i <= 6; i++) {
				getters.put(new MultiFieldKey(Species, i, Code), clazz.getDeclaredMethod("getSpecies" + i + "Code"));
				getters.put(
						new MultiFieldKey(Species, i, Percent), clazz.getDeclaredMethod("getSpecies" + i + "Percent")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, WholeStemVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "WholeStemVolume")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, CloseUtilizationVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "CloseUtilizationVolume")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecay),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecay")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecayWastage")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("getSpecies" + i + "CuVolumeLessDecayWastageBreakage")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassWholeStemVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassWholeStemVolume")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCloseUtilizationVolume),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCloseUtilizationVolume")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecay),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecay")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecayWastage")
				);
				getters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("getSpecies" + i + "MoFBiomassCuVolumeLessDecayWastageBreakage")
				);

				setters.put(
						new MultiFieldKey(Species, i, Code),
						clazz.getDeclaredMethod("setSpecies" + i + "Code", String.class)
				);
				setters.put(
						new MultiFieldKey(Species, i, Percent),
						clazz.getDeclaredMethod("setSpecies" + i + "Percent", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, WholeStemVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "WholeStemVolume", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, CloseUtilizationVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "CloseUtilizationVolume", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecay),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecay", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecayWastage", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, CuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod("setSpecies" + i + "CuVolumeLessDecayWastageBreakage", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassWholeStemVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassWholeStemVolume", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCloseUtilizationVolume),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCloseUtilizationVolume", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecay),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCuVolumeLessDecay", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastage),
						clazz.getDeclaredMethod("setSpecies" + i + "MoFBiomassCuVolumeLessDecayWastage", String.class)
				);
				setters.put(
						new MultiFieldKey(SpeciesProjection, i, MoFBiomassCuVolumeLessDecayWastageBreakage),
						clazz.getDeclaredMethod(
								"setSpecies" + i + "MoFBiomassCuVolumeLessDecayWastageBreakage", String.class
						)
				);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix) {

		var key = new MultiFieldKey(prefix, speciesNumber, suffix);

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
		var key = new MultiFieldKey(prefix, speciesNumber, suffix);
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
