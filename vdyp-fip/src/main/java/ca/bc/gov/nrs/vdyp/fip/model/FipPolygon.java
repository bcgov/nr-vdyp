package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;

public class FipPolygon extends BaseVdypPolygon<FipLayer, Optional<Float>> {

	String forestInventoryZone; // FIP_P/FIZ
	String biogeoclimaticZone; // FIP_P/BEC
	Optional<FipMode> modeFip; // FIP_P2/MODE / MODEfip
	Optional<String> nonproductiveDescription; // FIP_P3/NPDESC
	float yieldFactor; // FIP_P4/YLDFACT

	public FipPolygon(
			String polygonIdentifier, String fiz, String becIdentifier, Optional<Float> percentAvailable,
			Optional<FipMode> modeFip, Optional<String> nonproductiveDescription, float yieldFactor
	) {
		super(polygonIdentifier, percentAvailable);
		this.forestInventoryZone = fiz;
		this.biogeoclimaticZone = becIdentifier;
		this.modeFip = modeFip;
		this.nonproductiveDescription = nonproductiveDescription;
		this.yieldFactor = yieldFactor;
	}

	public String getForestInventoryZone() {
		return forestInventoryZone;
	}

	public void setForestInventoryZone(String forestInventoryZone) {
		this.forestInventoryZone = forestInventoryZone;
	}

	public String getBiogeoclimaticZone() {
		return biogeoclimaticZone;
	}

	public void setBiogeoclimaticZone(String biogeoclimaticZone) {
		this.biogeoclimaticZone = biogeoclimaticZone;
	}

	public Optional<FipMode> getModeFip() {
		return modeFip;
	}

	public void setModeFip(Optional<FipMode> modeFip) {
		this.modeFip = modeFip;
	}

	public Optional<String> getNonproductiveDescription() {
		return nonproductiveDescription;
	}

	public void setNonproductiveDescription(Optional<String> nonproductiveDescription) {
		this.nonproductiveDescription = nonproductiveDescription;
	}

	public float getYieldFactor() {
		return yieldFactor;
	}

	public void setYieldFactor(float yieldFactor) {
		this.yieldFactor = yieldFactor;
	}

}
