package ca.bc.gov.nrs.vdyp.backend.messaging.message;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BatchRequestMessage(UUID projectionID, String parameterJSON) {
}
