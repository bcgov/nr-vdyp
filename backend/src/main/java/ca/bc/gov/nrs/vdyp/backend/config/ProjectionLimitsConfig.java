package ca.bc.gov.nrs.vdyp.backend.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionLimitsConfig {

	private final int maximumPolygons;

	public ProjectionLimitsConfig(
			@ConfigProperty(name = "vdyp.projection.maximum.polygons", defaultValue = "300") int maximumPolygons
	) {
		if (maximumPolygons < 1) {
			throw new IllegalArgumentException("vdyp.projection.maximum.polygons must be greater than zero");
		}
		this.maximumPolygons = maximumPolygons;
	}

	public int maximumPolygons() {
		return maximumPolygons;
	}
}
