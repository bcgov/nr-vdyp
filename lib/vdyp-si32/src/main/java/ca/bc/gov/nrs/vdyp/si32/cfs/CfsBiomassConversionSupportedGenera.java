package ca.bc.gov.nrs.vdyp.si32.cfs;

import java.text.MessageFormat;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.EnumIterator;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SI32Enum;

/**
 * Lists the indices for CFS Genus for looking up CFS Conversion Params at the Genus level.
 * <p>
 * These values are used as an index into the {@link CfsBiomassConversionCoefficientsForGenus} array.
 */
public enum CfsBiomassConversionSupportedGenera implements SI32Enum<CfsBiomassConversionSupportedGenera> {
	INVALID(-1),

	/* 0 */
	AC(0),
	/* 1 */
	B(1),
	/* 2 */
	C(2),
	/* 3 */
	D(3),
	/* 4 */
	E(4),
	/* 5 */
	F(5),
	/* 6 */
	G(6),
	/* 7 */
	H(7),
	/* 8 */
	L(8),
	/* 9 */
	M(9),
	/* 10 */
	PL(10),
	/* 11 */
	Q(11),
	/* 12 */
	R(12),
	/* 13 */
	S(13),
	/* 14 */
	U(14),
	/* 15 */
	V(15),
	/* 16 */
	W(16),
	/* 17 */
	XH(17),
	/* 18 */
	ZC(18),
	/* 19 */
	ZH(19);

	private final int index;

	private CfsBiomassConversionSupportedGenera(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public int getOffset() {
		if (this.equals(INVALID)) {
			throw new UnsupportedOperationException(
					MessageFormat.format(
							"Cannot call getIndex on {0} as it's not a standard member of the enumeration", this
					)
			);
		}

		return index;
	}

	@Override
	public String getText() {
		if (this.equals(INVALID)) {
			return "INV";
		} else {
			return this.toString();
		}
	}

	/**
	 * Returns the enumeration constant with the given index.
	 *
	 * @param index the value in question
	 * @return the enumeration value, unless no enumeration constant has the given <code>index</code> in which case
	 *         <code>null</code> is returned.
	 */
	public static CfsBiomassConversionSupportedGenera forIndex(int index) {
		for (CfsBiomassConversionSupportedGenera e : CfsBiomassConversionSupportedGenera.values()) {
			if (index == e.index)
				return e;
		}

		return null;
	}

	/**
	 * Returns the genus for the given ecozone and species code.
	 *
	 * @param ecoZone  Cfs Biomass Conversion Supported EcoZone
	 * @param sp64Code MoF Species Code (SP64)
	 * @return Genus for the given ecozone and species code, or INVALID if not found.
	 */
	public static CfsBiomassConversionSupportedGenera
			fromEcoZoneAndSpecies(CfsBiomassConversionSupportedEcoZone ecoZone, String sp64Code) {

		if (ecoZone == null || ecoZone == CfsBiomassConversionSupportedEcoZone.UNKNOWN || sp64Code == null
				|| sp64Code.isEmpty())
			return CfsBiomassConversionSupportedGenera.INVALID;

		if (universalSpeciesToGenusMap.containsKey(sp64Code)) {
			return universalSpeciesToGenusMap.get(sp64Code);
		} else if (ecoZoneToSpeciesGenusMap.containsKey(ecoZone)) {
			Map<String, CfsBiomassConversionSupportedGenera> speciesGenusMap = ecoZoneToSpeciesGenusMap.get(ecoZone);
			if (speciesGenusMap.containsKey(sp64Code)) {
				return speciesGenusMap.get(sp64Code);
			}
		}

		return CfsBiomassConversionSupportedGenera.INVALID;
	}

	/**
	 * @return the number of non-housekeeping entries in the enumeration
	 */
	public static int size() {
		return ZH.index - AC.index + 1;
	}

	public static class Iterator extends EnumIterator<CfsBiomassConversionSupportedGenera> {
		public Iterator() {
			super(values(), AC, ZH);
		}
	}

	static final Map<String, CfsBiomassConversionSupportedGenera> universalSpeciesToGenusMap = Map.ofEntries(
			Map.entry("AC", AC), //
			Map.entry("ACT", AC), //
			Map.entry("B", B), //
			Map.entry("C", C), //
			Map.entry("JR", C), //
			Map.entry("OA", C), //
			Map.entry("T", C), //
			Map.entry("TW", C), //
			Map.entry("Y", C), //
			Map.entry("D", D), //
			Map.entry("DG", D), //
			Map.entry("DM", D), //
			Map.entry("E", E), //
			Map.entry("F", F), //
			Map.entry("G", G), //
			Map.entry("GP", G), //
			Map.entry("GR", G), //
			Map.entry("H", H), //
			Map.entry("L", L), //
			Map.entry("M", M), //
			Map.entry("MV", M), //
			Map.entry("Q", Q), //
			Map.entry("QE", Q), //
			Map.entry("QG", Q), //
			Map.entry("R", R), //
			Map.entry("RA", R), //
			Map.entry("S", S), //
			Map.entry("U", U), //
			Map.entry("UA", U), //
			Map.entry("UP", U), //
			Map.entry("V", V), //
			Map.entry("VB", V), //
			Map.entry("VP", V), //
			Map.entry("W", W), //
			Map.entry("WS", W), //
			Map.entry("WT", W), //
			Map.entry("XH", XH), //
			Map.entry("ZC", ZC), //
			Map.entry("ZH", ZH)
	);

	static final Map<CfsBiomassConversionSupportedEcoZone, Map<String, CfsBiomassConversionSupportedGenera>> ecoZoneToSpeciesGenusMap = Map
			.of(
					CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS, Map.ofEntries(
							//
							Map.entry("AD", AC), //
							Map.entry("AX", AC), //
							Map.entry("BA", B), //
							Map.entry("BP", B), //
							Map.entry("ES", E), //
							Map.entry("EW", E), //
							Map.entry("LA", L), //
							Map.entry("LT", L), //
							Map.entry("LW", L), //
							Map.entry("P", PL), //
							Map.entry("PJ", PL), //
							Map.entry("PL", PL), //
							Map.entry("PM", PL), //
							Map.entry("SS", S), //
							Map.entry("SXL", S), //
							Map.entry("XC", ZC), //
							Map.entry("YC", ZC), //
							Map.entry("WA", ZH), //
							Map.entry("WB", ZH), //
							Map.entry("WP", ZH)//
					), CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS,
					Map.ofEntries(
							//
							Map.entry("AX", AC), //
							Map.entry("BA", B), //
							Map.entry("BB", B), //
							Map.entry("EA", E), //
							Map.entry("EP", E), //
							Map.entry("EXP", E), //
							Map.entry("LA", L), //
							Map.entry("LT", L), //
							Map.entry("LW", L), //
							Map.entry("PJ", PL), //
							Map.entry("PL", PL), //
							Map.entry("SX", S), //
							Map.entry("SXL", S), //
							Map.entry("SXW", S), //
							Map.entry("XC", ZC), //
							Map.entry("WB", ZH), //
							Map.entry("WP", ZH)//
					), CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA,
					Map.ofEntries(
							//
							Map.entry("AX", AC), //
							Map.entry("B", B), //
							Map.entry("BA", B), //
							Map.entry("BG", B), //
							Map.entry("BP", B), //
							Map.entry("LA", C), //
							Map.entry("EA", E), //
							Map.entry("EP", E), //
							Map.entry("EXP", E), //
							Map.entry("P", PL), //
							Map.entry("PL", PL), //
							Map.entry("SXL", S), //
							Map.entry("XC", ZC), //
							Map.entry("Z", ZC), //
							Map.entry("WD", ZH)//
					), CfsBiomassConversionSupportedEcoZone.PACIFIC_MARITIME,
					Map.ofEntries(
							//
							Map.entry("AX", AC), //
							Map.entry("BB", B), //
							Map.entry("BP", B), //
							Map.entry("LA", C), //
							Map.entry("EW", E), //
							Map.entry("P", PL), //
							Map.entry("PF", PL), //
							Map.entry("PL", PL), //
							Map.entry("PR", PL), //
							Map.entry("SA", S), //
							Map.entry("SXL", S), //
							Map.entry("SXS", S), //
							Map.entry("X", ZC), //
							Map.entry("XC", ZC)//
					), CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA,
					Map.ofEntries(
							//
							Map.entry("AD", AC), //
							Map.entry("AX", AC), //
							Map.entry("BB", B), //
							Map.entry("BM", B), //
							Map.entry("BP", B), //
							Map.entry("EB", E), //
							Map.entry("EE", E), //
							Map.entry("ES", E), //
							Map.entry("EW", E), //
							Map.entry("HXM", H), //
							Map.entry("LA", L), //
							Map.entry("P", PL), //
							Map.entry("PF", PL), //
							Map.entry("PJ", PL), //
							Map.entry("PL", PL), //
							Map.entry("PLC", PL), //
							Map.entry("PM", PL), //
							Map.entry("PR", PL), //
							Map.entry("SN", S), //
							Map.entry("SXB", S), //
							Map.entry("SXE", S), //
							Map.entry("SXL", S), //
							Map.entry("SXS", S), //
							Map.entry("SXW", S), //
							Map.entry("XC", ZC)//
					)
			);
}
