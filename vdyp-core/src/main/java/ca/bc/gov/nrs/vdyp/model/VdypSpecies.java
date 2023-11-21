package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;

public class VdypSpecies extends BaseVdypSpecies implements VdypUtilizationHolder {

	Coefficients baseAreaByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/BA
	Coefficients loreyHeightByUtilization = new Coefficients(Arrays.asList(0f, 0f), -1); // LVCOM/HL
	Coefficients quadraticMeanDiameterByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/DQ
	Coefficients treesPerHectareByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/TPH

	Coefficients wholeStemVolumeByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/VOLWS
	Coefficients closeUtilizationVolumeByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/VOLCU
	Coefficients closeUtilizationNetVolumeOfDecayByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1
	); // LVCOM/VOL_D
	Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1
	); // LVCOM/VOL_DW
	Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = new Coefficients(
			Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1
	); // LVCOM/VOL_DWB

	int volumeGroup;
	int decayGroup;
	int breakageGroup;

	public VdypSpecies(String polygonIdentifier, Layer layer, String genus) {
		super(polygonIdentifier, layer, genus);
	}

	public VdypSpecies(BaseVdypSpecies toCopy) {
		super(toCopy);
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

	public int getVolumeGroup() {
		return volumeGroup;
	}

	public void setVolumeGroup(int volumeGroup) {
		this.volumeGroup = volumeGroup;
	}

	public int getDecayGroup() {
		return decayGroup;
	}

	public void setDecayGroup(int decayGroup) {
		this.decayGroup = decayGroup;
	}

	public int getBreakageGroup() {
		return breakageGroup;
	}

	public void setBreakageGroup(int breakageGroup) {
		this.breakageGroup = breakageGroup;
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
		return closeUtilizationNetVolumeOfDecayByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayByUtilization(
			Coefficients closeUtilizationNetVolumeOfDecayByUtilization
	) {
		this.closeUtilizationNetVolumeOfDecayByUtilization = closeUtilizationNetVolumeOfDecayByUtilization;
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
