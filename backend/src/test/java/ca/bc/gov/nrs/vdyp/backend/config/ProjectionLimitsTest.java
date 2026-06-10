package ca.bc.gov.nrs.vdyp.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectionLimitsTest {
	@Test
	void testInvalidMaximum() {
		assertThrows(IllegalArgumentException.class, () -> new ProjectionLimitsConfig(0));
	}

	@Test
	void testValidMaximum() {
		ProjectionLimitsConfig config = new ProjectionLimitsConfig(30);
		assertEquals(30, config.maximumPolygons());
	}
}
