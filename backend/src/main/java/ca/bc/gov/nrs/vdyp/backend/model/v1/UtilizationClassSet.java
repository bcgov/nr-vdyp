package ca.bc.gov.nrs.vdyp.backend.model.v1;

import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.*;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;

/**
 * Gets or Sets value
 */
public enum UtilizationClassSet {

	EXCL("Excl", Set.of()), _4_0("4.0+", Set.of(SMALL, U75TO125, U125TO175, U175TO225, OVER225)),
	_7_5("7.5+", Set.of(U75TO125, U125TO175, U175TO225, OVER225)),
	_12_5("12.5+", Set.of(U125TO175, U175TO225, OVER225)), _17_5("17.5+", Set.of(U175TO225, OVER225)),
	_22_5("22.5+", Set.of(OVER225));

	private String value;
	private Set<UtilizationClass> utilizationClassSet;

	UtilizationClassSet(String value, Set<UtilizationClass> ucSet) {
		this.value = value;
		this.utilizationClassSet = ucSet;
	}

	public Set<UtilizationClass> getUtilizationClassSet() {
		return utilizationClassSet;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static UtilizationClassSet fromValue(String value) {
		for (UtilizationClassSet b : UtilizationClassSet.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}

	public double sumOf(UtilizationVector v) {
		return utilizationClassSet.stream().map(uc -> v.get(uc)).reduce((s, value) -> s + value).get();
	}

	@Override
	public String toString() {
		return getValue();
	}
}