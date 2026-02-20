package ca.bc.gov.nrs.vdyp.backend.config;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionExpiryConfig {

	@ConfigProperty(name = "vdyp.projection.expiry.days", defaultValue = "30")
	int daysUntilExpiry;

	public int daysUntilExpiry() {
		return daysUntilExpiry;
	}

	public OffsetDateTime expiryFrom(OffsetDateTime lastUpdatedDate) {
		var base = lastUpdatedDate == null ? OffsetDateTime.now() : lastUpdatedDate;
		return base.plusDays(daysUntilExpiry);
	}
}
