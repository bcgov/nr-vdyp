package ca.bc.gov.nrs.vdyp.si32.bec;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.EnumIterator;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SI32Enum;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;

/**
 * Identifies each of the BEC Zones known to the system.
 * <ul>
 * <li>UNKNOWN: Represents an unknown BEC Zone or an error condition.
 * <li>others: Individual BEC Zones recognized.
 */
public enum BecZone implements SI32Enum<BecZone> {
	UNKNOWN(-1, SpeciesRegion.INTERIOR),

	AT(0, SpeciesRegion.INTERIOR), //
	BG(1, SpeciesRegion.INTERIOR), //
	BWBS(2, SpeciesRegion.INTERIOR), //
	CDF(3, SpeciesRegion.COAST), //
	CWH(4, SpeciesRegion.COAST), //
	ESSF(5, SpeciesRegion.INTERIOR), //
	ICH(6, SpeciesRegion.INTERIOR), //
	IDF(7, SpeciesRegion.INTERIOR), //
	MH(8, SpeciesRegion.COAST), //
	MS(9, SpeciesRegion.INTERIOR), //
	PP(10, SpeciesRegion.INTERIOR), //
	SBPS(11, SpeciesRegion.INTERIOR), //
	SBS(12, SpeciesRegion.INTERIOR), //
	SWB(13, SpeciesRegion.INTERIOR); //

	private static final Map<Integer, BecZone> index2EnumMap = new HashMap<>();

	static {
		for (BecZone becZone : BecZone.values()) {
			index2EnumMap.put(becZone.index, becZone);
		}
	}

	private final int index;
	private final SpeciesRegion speciesRegion;

	private BecZone(int index, SpeciesRegion speciesRegion) {
		this.index = index;
		this.speciesRegion = speciesRegion;
	}

	@Override
	public int getIndex() {
		return index;
	}

	public SpeciesRegion getSpeciesRegion() {
		return speciesRegion;
	}

	@Override
	public int getOffset() {
		if (this.equals(UNKNOWN)) {
			throw new UnsupportedOperationException(
					MessageFormat.format(
							"Cannot call getIndex on {0} as it's not a standard member of the enumeration", this
					)
			);
		}

		return ordinal() - 1;
	}

	@Override
	public String getText() {
		if (this.equals(UNKNOWN)) {
			throw new UnsupportedOperationException(
					MessageFormat
							.format("Cannot call getText on {0} as it's not a standard member of the enumeration", this)
			);
		}

		return this.toString();
	}

	/**
	 * Returns the enumeration constant with the given index.
	 *
	 * @param index the value in question
	 * @return the enumeration value, unless no enumeration constant has the given <code>index</code> in which case
	 *         <code>null</code> is returned.
	 */
	public static BecZone forIndex(int index) {
		return index2EnumMap.get(index);
	}

	/**
	 * @return the number of non-housekeeping entries in the enumeration
	 */
	public static int size() {
		return SWB.index - AT.index + 1;
	}

	public static class Iterator extends EnumIterator<BecZone> {
		public Iterator() {
			super(values(), AT, SWB);
		}
	}
}
