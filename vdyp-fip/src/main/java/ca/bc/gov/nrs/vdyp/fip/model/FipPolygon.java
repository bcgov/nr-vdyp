package ca.bc.gov.nrs.vdyp.fip.model;

public class FipPolygon {

	String polygonIdentifier; // POLYDESC
	String forestInventoryZone; // FIZ
	String biogeoclimaticZone; // BEC
	float percentAvailable; // PCTFLAND
	int modeFip; // MODEfip // Maybe make this an enum
	String nonproductiveDescription; // NPDESC
	float yieldFactor; // YLDFACT

	public FipPolygon(
			String polygonIdentifier, String fiz, String becIdentifier, float percentAvailable, int modeFip,
			String nonproductiveDescription, float yieldFactor
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.forestInventoryZone = fiz;
		this.biogeoclimaticZone = becIdentifier;
		this.percentAvailable = percentAvailable;
		this.modeFip = modeFip;
		this.nonproductiveDescription = nonproductiveDescription;
		this.yieldFactor = yieldFactor;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public void setPolygonIdentifier(String polygonIdentifier) {
		this.polygonIdentifier = polygonIdentifier;
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

	public float getPercentAvailable() {
		return percentAvailable;
	}

	public void setPercentAvailable(float percentAvailable) {
		this.percentAvailable = percentAvailable;
	}

	public int getModeFip() {
		return modeFip;
	}

	public void setModeFip(int modeFip) {
		this.modeFip = modeFip;
	}

	public String getNonproductiveDescription() {
		return nonproductiveDescription;
	}

	public void setNonproductiveDescription(String nonproductiveDescription) {
		this.nonproductiveDescription = nonproductiveDescription;
	}

	public float getYieldFactor() {
		return yieldFactor;
	}

	public void setYieldFactor(float yieldFactor) {
		this.yieldFactor = yieldFactor;
	}

}
