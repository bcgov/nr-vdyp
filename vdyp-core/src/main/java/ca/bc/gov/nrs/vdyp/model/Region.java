package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;
import java.util.Optional;

public enum Region {
	COASTAL('C'), INTERIOR('I');

	final char characterAlias;

	private Region(char characterAlias) {
		this.characterAlias = characterAlias;
	}

	public static Optional<Region> fromAlias(char alias) {
		return Arrays.stream(Region.values()).filter(x -> x.getCharacterAlias() == alias).findFirst();
	}

	public char getCharacterAlias() {
		return characterAlias;
	}

}
