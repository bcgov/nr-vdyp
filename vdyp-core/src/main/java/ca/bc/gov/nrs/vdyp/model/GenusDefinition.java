package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

public class GenusDefinition extends AliasedEntity {

	final Optional<Integer> preference;

	public GenusDefinition(String alias, Optional<Integer> preference, String name) {
		super(alias, name);
		this.preference = preference;
	}

	public Optional<Integer> getPreference() {
		return preference;
	}
}
