package ca.bc.gov.nrs.vdyp.model;

public class BecDefinition extends AbstractSpeciesDefinition {

	final Region region;

	public BecDefinition(String alias, Region region, String name) {
		super(alias, name);
		this.region = region;
	}

	public Region getRegion() {
		return region;
	}
}
