package ca.bc.gov.nrs.vdyp.batch.messaging.message;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import com.fasterxml.jackson.annotation.JsonProperty;

@RegisterReflectionForBinding
public record PrioritizeRequestMessage(@JsonProperty String jobGuid, @JsonProperty String projectionGuid) {
}
