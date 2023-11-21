package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;

public class VdypLayer extends BaseVdypLayer<VdypSpecies> implements VdypUtilizationHolder {

	float breastHeightAge; // LVCOM3/AGEBHLV +

	Coefficients baseAreaByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/BA species 0
	Coefficients loreyHeightByUtilization = new Coefficients(
			Arrays.asList(0f, 0f), -1 //
	); // LVCOM/HL species 0
	Coefficients quadraticMeanDiameterByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/DQ species 0
	Coefficients treesPerHectareByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/TPH species 0

	Coefficients wholeStemVolumeByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/VOLWS species 0
	Coefficients closeUtilizationVolumeByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/VOLCU species 0
	Coefficients closeUtilizationVolumeNetOfDecayByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/VOL_D species 0
	Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/VOL_DW species 0
	Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1 //
	); // LVCOM/VOL_DWB species 0

	public VdypLayer(String polygonIdentifier, Layer layer) {
		super(polygonIdentifier, layer);
	}

	public float getBreastHeightAge() {
		return breastHeightAge;
	}

	public void setBreastHeightAge(float breastHeightAge) {
		this.breastHeightAge = breastHeightAge;
	}

	@Override
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	@Override
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	@Override
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	@Override
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

	@Override
	public Coefficients getQuadraticMeanDiameterByUtilization() {
		return quadraticMeanDiameterByUtilization;
	}

	@Override
	public void setQuadraticMeanDiameterByUtilization(Coefficients quadraticMeanDiameterByUtilization) {
		this.quadraticMeanDiameterByUtilization = quadraticMeanDiameterByUtilization;
	}

	@Override
	public Coefficients getTreesPerHectareByUtilization() {
		return treesPerHectareByUtilization;
	}

	@Override
	public void setTreesPerHectareByUtilization(Coefficients treesPerHectareByUtilization) {
		this.treesPerHectareByUtilization = treesPerHectareByUtilization;
	}

	@Override
	public Coefficients getWholeStemVolumeByUtilization() {
		return wholeStemVolumeByUtilization;
	}

	@Override
	public void setWholeStemVolumeByUtilization(Coefficients wholeStemVolumeByUtilization) {
		this.wholeStemVolumeByUtilization = wholeStemVolumeByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeByUtilization() {
		return closeUtilizationVolumeByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeByUtilization(Coefficients closeUtilizationVolumeByUtilization) {
		this.closeUtilizationVolumeByUtilization = closeUtilizationVolumeByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayByUtilization() {
		return closeUtilizationVolumeNetOfDecayByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayByUtilization(
			Coefficients closeUtilizationNetVolumeOfDecayByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayByUtilization = closeUtilizationNetVolumeOfDecayByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization() {
		return closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
			Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization() {
		return closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
			Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

}
