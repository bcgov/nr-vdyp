package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import ca.bc.gov.nrs.vdyp.backend.responses.v1.ResourceTypes;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

@JsonSubTypes(
	{ @Type(value = ValidationMessageListResource.class, name = ResourceTypes.VALIDATION_MESSAGE_LIST_RESOURCE) }
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class ValidationMessageListResource implements Serializable {

	private static final long serialVersionUID = -1081880501343373315L;

	private List<ValidationMessageResource> messages = new ArrayList<ValidationMessageResource>();

	public ValidationMessageListResource() {
	}

	public ValidationMessageListResource(List<ValidationMessage> messages) {

		if (messages != null) {
			for (var message : messages) {
				this.messages.add(new ValidationMessageResource(message));
			}
		}
	}

	public ValidationMessageListResource(ValidationMessage message) {

		if (message != null) {
			this.messages.add(new ValidationMessageResource(message));
		}
	}

	public ValidationMessageListResource(String message) {
		this(new ValidationMessage(ValidationMessageKind.GENERIC, message));
	}

	public List<ValidationMessageResource> getMessages() {
		return messages;
	}

	public boolean hasMessages() {
		return messages.size() > 0;
	}
}
