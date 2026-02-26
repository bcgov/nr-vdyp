package ca.bc.gov.nrs.vdyp.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ProjectionExpiryConfigTest {
	@Test
	void testDaysUntilExpiry() {
		ProjectionExpiryConfig config = new ProjectionExpiryConfig(30);
		int days = config.daysUntilExpiry();
		assertEquals(30, days);
	}

	@Test
	void testExpiryFrom() {
		ProjectionExpiryConfig config = new ProjectionExpiryConfig(30);
		var now = OffsetDateTime.now();
		var expiry = config.expiryFrom(now);
		assertEquals(now.plusDays(30), expiry);
	}
}
