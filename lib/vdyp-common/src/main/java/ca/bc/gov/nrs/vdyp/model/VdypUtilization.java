package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

/**
 * Instance of this class are parsed out of the Utilization input files supplied to Forward and other applications. They
 * are used to solely for the purpose of transporting this information into code that builds the UtilizationHolders
 * (that is, VdypLayer and VdypSpecies) from sets of them (each set will contain one entry per UtilizationClass.)
 */
public class VdypUtilization implements VdypEntity {

	// See IPSJF155.doc

	private PolygonIdentifier polygonId; // POLYDESC
	private final LayerType layerType; // LAYERG
	private final int genusIndex; // ISP
	private final Optional<String> genus; // SP0
	private final UtilizationClass ucIndex; // J - utilization index

	// The following are not final because post construction the values
	// may be scaled by the scale method below.

	private float basalArea;
	private float liveTreesPerHectare;
	private float loreyHeight;
	private float wholeStemVolume;
	private float closeUtilizationVolume;
	private float cuVolumeMinusDecay;
	private float cuVolumeMinusDecayWastage;
	private float cuVolumeMinusDecayWastageBreakage;
	private float quadraticMeanDiameterAtBH;

	// Set after construction
	private VdypSpecies parent;

	public VdypUtilization(
			PolygonIdentifier polygonId, LayerType layerType, int genusIndex, Optional<String> genus,
			UtilizationClass ucIndex, float basalArea, float liveTreesPerHectare, float loreyHeight,
			float wholeStemVolume, float closeUtilizationVolume, float cuVolumeMinusDecay,
			float cuVolumeMinusDecayWastage, float cuVolumeMinusDecayWastageBreakage, float quadraticMeanDiameterAtBH
	) {
		this.polygonId = polygonId;
		this.layerType = layerType;
		this.genusIndex = genusIndex;
		this.genus = genus;
		this.ucIndex = ucIndex;
		this.basalArea = basalArea;
		this.liveTreesPerHectare = liveTreesPerHectare;
		this.loreyHeight = loreyHeight;
		this.wholeStemVolume = wholeStemVolume;
		this.closeUtilizationVolume = closeUtilizationVolume;
		this.quadraticMeanDiameterAtBH = quadraticMeanDiameterAtBH;
		this.cuVolumeMinusDecay = cuVolumeMinusDecay;
		this.cuVolumeMinusDecayWastage = cuVolumeMinusDecayWastage;
		this.cuVolumeMinusDecayWastageBreakage = cuVolumeMinusDecayWastageBreakage;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(polygonId).append(' ').append(layerType).append(' ').append(genusIndex).append(' ')
				.append(ucIndex.index);

		return sb.toString();
	}

	public void setParent(VdypSpecies parent) {
		this.parent = parent;
	}

	public PolygonIdentifier getPolygonId() {
		return polygonId;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public Integer getGenusIndex() {
		return genusIndex;
	}

	public Optional<String> getGenus() {
		return genus;
	}

	public UtilizationClass getUcIndex() {
		return ucIndex;
	}

	public float getBasalArea() {
		return basalArea;
	}

	public float getLiveTreesPerHectare() {
		return liveTreesPerHectare;
	}

	public float getLoreyHeight() {
		return loreyHeight;
	}

	public float getWholeStemVolume() {
		return wholeStemVolume;
	}

	public float getCloseUtilizationVolume() {
		return closeUtilizationVolume;
	}

	public float getCuVolumeMinusDecay() {
		return cuVolumeMinusDecay;
	}

	public float getCuVolumeMinusDecayWastage() {
		return cuVolumeMinusDecayWastage;
	}

	public float getCuVolumeMinusDecayWastageBreakage() {
		return cuVolumeMinusDecayWastageBreakage;
	}

	public float getQuadraticMeanDiameterAtBH() {
		return quadraticMeanDiameterAtBH;
	}

	public VdypSpecies getParent() {
		return parent;
	}
}
