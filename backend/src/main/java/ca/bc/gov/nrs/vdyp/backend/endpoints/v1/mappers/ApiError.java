package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.mappers;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * JSON response body used by every exception mapper. Needs RegisterForReflection, or GraalVM native-image
 * builds strip its accessors and Jackson fails to serialize it, turning every error response into a 500.
 */
@RegisterForReflection
public record ApiError(String code, String message) {
}
