package ca.bc.gov.nrs.vdyp.batch.messaging.message;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import com.fasterxml.jackson.annotation.JsonProperty;

@RegisterReflectionForBinding
public record StopReplyMessage(
		@JsonProperty String status, @JsonProperty String message, @JsonProperty Long executionId
) {
}
