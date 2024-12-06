package ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations;

public enum InventoryStandard {
	/** TODO: definition */
	Silviculture,

	/** The inventory data was captured using VRI standards. */
	VRI,
	
	/** The inventory data was captured using FIP standards. */
	FIP,

	/**
	 * Indicates the inventory standard is not known. In this case, an attempt to determine Inventory Standard will be
	 * made based on supplied inventory attributes.
	 */
	Unknown
}
