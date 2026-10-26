package ca.bc.gov.nrs.vdyp.backend.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionStuckConfig {

	private final int thresholdMinutes;

	public ProjectionStuckConfig(
			@ConfigProperty(name = "vdyp.projection.stuck.threshold-minutes", defaultValue = "120") int thresholdMinutes
	) {
		this.thresholdMinutes = thresholdMinutes;
	}

	public int thresholdMinutes() {
		return thresholdMinutes;
	}

	public OffsetDateTime threshold() {
		return OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(thresholdMinutes);
	}
}
