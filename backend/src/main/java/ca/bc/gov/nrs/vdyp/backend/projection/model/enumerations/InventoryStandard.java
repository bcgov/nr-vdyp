package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.backend.projection.input.InventoryStandardCode;

public enum InventoryStandard {
	
	/** TODO: definition */
	Silviculture(InventoryStandardCode.I),

	/** The inventory data was captured using VRI standards. */
	VRI(InventoryStandardCode.V),
	
	/** The inventory data was captured using FIP standards. */
	FIP(InventoryStandardCode.F),

	/**
	 * Indicates the inventory standard is not known. In this case, an attempt to determine Inventory Standard will be
	 * made based on supplied inventory attributes.
	 */
	Unknown(InventoryStandardCode.Unknown);

	private InventoryStandardCode code;
	
	InventoryStandard(InventoryStandardCode inventoryStandardCode) {
		this.code = inventoryStandardCode;
	}
	
	public static InventoryStandard getFromCode(InventoryStandardCode code) {
		
		if (code != null) {
			for (InventoryStandard is: values()) {
				if (code == is.code) {
					return is;
				}
			}
		}
		
		throw new IllegalArgumentException(MessageFormat.format("Code {0} is not a known Inventory Standard Code", code));
	}

	public static InventoryStandard getDefault() {
		return VRI;
	}
}
