package ca.bc.gov.nrs.vdyp.backend.projection.model;

/**
 * When selecting a Species (Sp64) or Stand (Sp0) from a Layer by index, this enumeration is used to specify the sort
 * order to be used when evaluating the index.
 */
public enum SpeciesSelectionCriteria {
	BY_PERCENT, BY_NAME, AS_SUPPLIED
}
