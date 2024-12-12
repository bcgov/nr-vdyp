package ca.bc.gov.nrs.vdyp.backend.projection.input;

public enum InventoryStandardCode {
	
	/** Silviculture. TODO: definition */
	I,

	/** The inventory data was captured using VRI standards. */
	V,
	
	/** The inventory data was captured using FIP standards. */
	F,

	/**
	 * Indicates the inventory standard is not known. In this case, an attempt to determine Inventory Standard will be
	 * made based on supplied inventory attributes.
	 */
	Unknown;
}
