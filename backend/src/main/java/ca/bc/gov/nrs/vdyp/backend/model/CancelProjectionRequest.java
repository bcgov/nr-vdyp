package ca.bc.gov.nrs.vdyp.backend.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@Schema(description = "Optional administrative reason for cancelling a projection.")
public record CancelProjectionRequest(
		@Schema(description = "Reason recorded when an administrator cancels the projection.") String adminCancelReason
) {
}
