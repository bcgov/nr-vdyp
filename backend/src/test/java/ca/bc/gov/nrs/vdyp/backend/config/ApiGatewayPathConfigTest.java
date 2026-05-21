package ca.bc.gov.nrs.vdyp.backend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiGatewayPathConfigTest {

	@ParameterizedTest
	@CsvSource(
		{ "'/api/gateway', '/api/gateway'", "'api/gateway', '/api/gateway'", "' /api/gateway/ ', 'api/gateway'",
				"'/api/gateway/', '/api/gateway/'" }
	)
	void testMatchesNormalizedConfiguredPaths(String configuredPath, String requestPath) {
		ApiGatewayPathConfig config = new ApiGatewayPathConfig(configuredPath);

		assertTrue(config.isApiGatewayPath(requestPath));
	}

	@Test
	void testIgnoresBlankConfiguredEntries() {
		ApiGatewayPathConfig config = new ApiGatewayPathConfig(" /api/token, , /api/jobs/ , ");

		assertTrue(config.isApiGatewayPath("/api/token"));
		assertTrue(config.isApiGatewayPath("/api/jobs"));
		assertFalse(config.isApiGatewayPath(""));
		assertFalse(config.isApiGatewayPath(" "));
	}

	@Test
	void testDoesNotMatchNullOrUnknownPath() {
		ApiGatewayPathConfig config = new ApiGatewayPathConfig("/api/token");

		assertFalse(config.isApiGatewayPath(null));
		assertFalse(config.isApiGatewayPath("/api/other"));
	}

	@Test
	void testRootPathRemainsRootPath() {
		ApiGatewayPathConfig config = new ApiGatewayPathConfig("/");

		assertTrue(config.isApiGatewayPath("/"));
		assertFalse(config.isApiGatewayPath(""));
	}
}
