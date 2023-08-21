package ca.bc.gov.nrs.vdyp.model;

public class AliasedEntity {

	final String alias;
	final String name;

	protected AliasedEntity(String alias, String name) {
		this.alias = alias;
		this.name = name;
	}

	/**
	 * @return The short alias for the entity
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @return The full, human readable name for the entity
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("%s (%s)", getAlias(), getName());
	}

}
