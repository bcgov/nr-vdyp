package ca.bc.gov.nrs.vdyp.batch.messaging.message;

import java.util.UUID;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import com.fasterxml.jackson.annotation.JsonProperty;

@RegisterReflectionForBinding
public record BatchRequestMessage(@JsonProperty UUID projectionID, @JsonProperty String parameterJSON) {
}
