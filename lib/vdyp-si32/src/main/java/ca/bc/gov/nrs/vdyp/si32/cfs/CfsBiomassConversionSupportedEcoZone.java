package ca.bc.gov.nrs.vdyp.si32.cfs;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.model.EnumIterator;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZone;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZoneMethods;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SI32Enum;

/**
 * Lists indices to Eco Zone conversion factors for the supported CFS Biomass conversion factors.
 * <ul>
 * <li>UNKNOWN: Indicates an uninitialized value or error condition. This should never be used to indicate a valid Eco
 * Zone Index.
 * <li>other: Indices into the {@link CfsBiomassConversionCoefficientsDetails} array corresponding to the identified CFS
 * Eco Zone.
 * </ul>
 * For the BC implementation of the CFS Conversion Factors, only a subset Eco Zones are supported. This enumeration
 * lists each of those Eco Zones and their corresponding index into the Cfs*BiomassConversionCoefficients arrays.
 * <p>
 * The list of enumeration constants is automatically generated and copy and pasted into this enum definition from the:
 * <ol>
 * <li>'Conversion Param Enum Defn' column of the
 * <li>'DeadConversionFactorsTable' found on the
 * <li>'Derived C Species Table' tab in the
 * <li>'BC_Inventory_updates_by_CBMv2bs.xlsx' located in the
 * <li>'Documents/CFS-Biomass' folder.
 * </ol>
 */
public enum CfsBiomassConversionSupportedEcoZone implements SI32Enum<CfsBiomassConversionSupportedEcoZone> {
	UNKNOWN(-1),

	/* 4 */
	TAIGA_PLAINS(0),
	/* 9 */
	BOREAL_PLAINS(1),
	/* 12 */
	BOREAL_CORDILLERA(2),
	/* 13 */
	PACIFIC_MARITIME(3),
	/* 14 */
	MONTANE_CORDILLERA(4);

	private final int index;

	private CfsBiomassConversionSupportedEcoZone(int index) {
		this.index = index;
	}

	public static CfsBiomassConversionSupportedEcoZone of(short cfsEcoZone) {
		return switch (cfsEcoZone) {
		case 4 -> TAIGA_PLAINS;
		case 9 -> BOREAL_PLAINS;
		case 12 -> BOREAL_CORDILLERA;
		case 13 -> PACIFIC_MARITIME;
		case 14 -> MONTANE_CORDILLERA;
		default -> UNKNOWN;
		};
	}

	public static CfsBiomassConversionSupportedEcoZone
			fromBecZoneData(String becZoneCode, String becSubZone, String becVariant, String mapSheet) {
		BecZone becZone = BecZoneMethods.becZoneToIndex(becZoneCode);

		CfsBiomassConversionSupportedEcoZone ecoZone = UNKNOWN;
		switch (becZone) {
		case BWBS:
			ecoZone = TAIGA_PLAINS; /* 4. Default for BWBS */
			if (becSubZone != null) {
				if (becSubZone.equalsIgnoreCase("dk") && (becVariant.equals("1") || becVariant.equals("2"))) {
					ecoZone = BOREAL_CORDILLERA;
				} else if (becSubZone.equalsIgnoreCase("mw") && (becVariant.equals("1"))) {
					ecoZone = BOREAL_PLAINS;
				} else if (becSubZone.equalsIgnoreCase("wk")) {
					ecoZone = BOREAL_PLAINS;
				} else if (mapSheet != null && mapSheet.startsWith("093")) {
					ecoZone = BOREAL_PLAINS; /* 9 */
				} else if (mapSheet != null && mapSheet.startsWith("104")) {
					ecoZone = BOREAL_CORDILLERA; /* 12 */
				}
			}
			break;

		case AT:
			ecoZone = MONTANE_CORDILLERA; /* 14. Default for AT */
			if (mapSheet != null && mapSheet.startsWith("092"))
				ecoZone = PACIFIC_MARITIME;
			break;

		case CDF:
		case CWH:
		case MH:
			ecoZone = PACIFIC_MARITIME; /* 13 */
			break;

		case ICH:
			ecoZone = MONTANE_CORDILLERA; /* 14 */
			if (becSubZone != null && becSubZone.equalsIgnoreCase("mc")) {
				ecoZone = PACIFIC_MARITIME; /* 13 */
			}
			break;

		case BG:
		case ESSF:
		case IDF:
		case MS:
		case PP:
		case SBPS:
		case SBS:
			ecoZone = MONTANE_CORDILLERA; /* 14 */
			break;

		case SWB:
			ecoZone = BOREAL_CORDILLERA; /* 12 */
			break;

		default:
			/* Invalid BEC Zone */
			break;

		}
		return ecoZone;
	}

	@Override
	public int getIndex() {
		return index;
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

		return index;
	}

	@Override
	public String getText() {
		if (this.equals(UNKNOWN)) {
			return "UNK";
		} else {
			return this.toString();
		}
	}

	/**
	 * @return the number of non-housekeeping entries in the enumeration
	 */
	public static int size() {
		return MONTANE_CORDILLERA.index - TAIGA_PLAINS.index + 1;
	}

	public static class Iterator extends EnumIterator<CfsBiomassConversionSupportedEcoZone> {
		public Iterator() {
			super(values(), TAIGA_PLAINS, MONTANE_CORDILLERA);
		}
	}
}
