package ca.bc.gov.nrs.vdyp.backend.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApiGatewayPathConfig {

	private final Set<String> apiGatewayPaths;

	public ApiGatewayPathConfig(@ConfigProperty(name = "vdyp.api-gateway.paths") String configuredPaths) {
		this.apiGatewayPaths = Arrays.stream(configuredPaths.split(",")).map(String::trim)
				.filter(path -> !path.isBlank()).map(ApiGatewayPathConfig::normalizePath)
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isApiGatewayPath(String path) {
		return apiGatewayPaths.contains(normalizePath(path));
	}

	private static String normalizePath(String path) {
		if (path == null || path.isBlank()) {
			return "";
		}

		String normalized = path.trim();

		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}

		// Avoid trailing-slash mismatch except for root.
		if (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		return normalized;
	}
}
