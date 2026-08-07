package ca.bc.gov.nrs.vdyp.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ProjectionStuckConfigTest {
	@Test
	void testThresholdMinutes() {
		ProjectionStuckConfig config = new ProjectionStuckConfig(120);
		assertEquals(120, config.thresholdMinutes());
	}

	@Test
	void testThreshold() {
		ProjectionStuckConfig config = new ProjectionStuckConfig(120);
		OffsetDateTime before = OffsetDateTime.now().minusMinutes(120);
		OffsetDateTime threshold = config.threshold();
		OffsetDateTime after = OffsetDateTime.now().minusMinutes(120);

		assertTrue(!threshold.isBefore(before) && !threshold.isAfter(after));
	}
}
