package ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;

/**
 * An enumeration of the different Canadian Forest Service ECO Zones.
 *
 * Definitions found in 'Volume_to_Biomass.doc' found in the 'Documents/CFS-Biomass' folder.
 *
 * The 'Ext' version provides a fixed size variable without the advantages of language specific enumeration. This data
 * type would be useful in cross language interfaces where data types size must be fixed and not dependent on compiler
 * settings.
 *
 * Elements for this table are automatically generated and copy and pasted from the:
 * <ul>
 * <li>'Eco Zone C Enum Definition' column of the
 * <li>'EcoZoneTable' found on the
 * <li>'Lookups' tab in the
 * <li>'BC_Inventory_updates_by_CBMv2bs.xlsx' located in the
 * <li>'Documents/CFS-Biomass' folder.
 * </ol>
 */
public enum CfsEcoZoneCode {

	ArcticCordillera(1), NorthernArctic(2), SouthernArctic(3), TaigaPlains(4), TaigaShield(5), BorealShield(6),
	AtlanticMaritime(7), MixedwoodPlains(8), BorealPlains(9), Prairies(10), TaigaCordillera(11), BorealCordillera(12),
	PacificMaritime(13), MontaneCordillera(14),

	Unknown(Vdyp7Constants.EMPTY_INT);

	private final short code;

	CfsEcoZoneCode(int code) {
		this.code = (short) code;
	}

	public static CfsEcoZoneCode fromCode(Short code) {
		if (code != null) {
			for (CfsEcoZoneCode e : values()) {
				if (e.code == code) {
					return e;
				}
			}
		}

		throw new IllegalArgumentException(MessageFormat.format("{0} is not a known CfsEcoZone", code));
	}

	public short getCode() {
		return code;
	}

	public static CfsEcoZoneCode getDefault() {
		return Unknown;
	}
}
