package ca.bc.gov.nrs.vdyp.model.builders;

import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Interface for a builder for a model object keyed by a layer
 */
public interface LayerIdentifiedBuilder extends PolygonIdentifiedBuilder {

	void layerType(LayerType type);
}
