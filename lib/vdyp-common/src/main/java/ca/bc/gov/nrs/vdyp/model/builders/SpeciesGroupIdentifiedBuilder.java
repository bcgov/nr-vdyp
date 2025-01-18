package ca.bc.gov.nrs.vdyp.model.builders;

/**
 * Interface for a builder for a model object keyed by a species group
 */
public interface SpeciesGroupIdentifiedBuilder extends LayerIdentifiedBuilder {
	void genus(String speciesGroup);
}
