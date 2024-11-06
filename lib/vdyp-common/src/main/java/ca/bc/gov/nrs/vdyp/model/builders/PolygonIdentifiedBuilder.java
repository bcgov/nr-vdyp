package ca.bc.gov.nrs.vdyp.model.builders;

import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;

/**
 * Interface for a builder for a model object keyed by a polygon
 */
public interface PolygonIdentifiedBuilder {
	void polygonIdentifier(PolygonIdentifier polygonIdentifier);

	default void polygonIdentifier(String string) {
		polygonIdentifier(PolygonIdentifier.split(string));
	}

	default void polygonIdentifier(String base, int year) {
		polygonIdentifier(new PolygonIdentifier(base, year));
	}
}
