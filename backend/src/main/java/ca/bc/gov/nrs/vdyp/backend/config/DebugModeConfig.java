package ca.bc.gov.nrs.vdyp.backend.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DebugModeConfig {

	private final int maximumPolygons;

	public DebugModeConfig(
			@ConfigProperty(name = "vdyp.debug-mode.maximum.polygons", defaultValue = "1") int maximumPolygons
	) {
		if (maximumPolygons < 1) {
			throw new IllegalArgumentException("vdyp.debug-mode.maximum.polygons must be greater than zero");
		}
		this.maximumPolygons = maximumPolygons;
	}

	public int maximumPolygons() {
		return maximumPolygons;
	}
}
