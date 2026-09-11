package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * JSON response body used by every exception mapper. Needs RegisterForReflection, or GraalVM native-image builds strip
 * its accessors and Jackson fails to serialize it, turning every error response into a 500.
 */
@RegisterForReflection
@Schema(description = "Standard error response.")
public record ApiError(
		@Schema(example = "BAD_REQUEST", description = "Stable machine-readable error code.") String code,
		@Schema(description = "Human-readable error detail.") String message
) {
}
