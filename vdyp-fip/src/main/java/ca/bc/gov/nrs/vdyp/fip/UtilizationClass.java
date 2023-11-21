package ca.bc.gov.nrs.vdyp.fip;

import java.util.Optional;

public enum UtilizationClass {
	SMALL(-1, "<7.5 cm", 0f, 7.5f), //
	ALL(0, ">7.5 cm", 7.5f, 10000f), //
	U75TO125(1, "7.5 - 12.5 cm", 7.5f, 12.5f), //
	U125TO175(2, "12.5 - 17.5 cm", 12.5f, 17.5f), //
	U175TO225(3, "17.5 - 22.5 cm", 17.5f, 22.5f), //
	OVER225(4, ">22.5 cm", 22.5f, 10000f);

	public final int index;
	public final String name;
	public final float lowBound;
	public final float highBound;

	private Optional<UtilizationClass> next = Optional.empty();
	private Optional<UtilizationClass> previous = Optional.empty();

	static {
		for (int i = 1; i < UtilizationClass.values().length; i++) {
			UtilizationClass.values()[i].previous = Optional.of(UtilizationClass.values()[i - 1]);
			UtilizationClass.values()[i - 1].next = Optional.of(UtilizationClass.values()[i]);
		}
	}

	UtilizationClass(int index, String name, float lowBound, float highBound) {
		this.index = index;
		this.name = name;
		this.lowBound = lowBound;
		this.highBound = highBound;
	}

	Optional<UtilizationClass> next() {
		return this.next;
	}

	Optional<UtilizationClass> previous() {
		return this.previous;
	}
}