package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import ca.bc.gov.nrs.vdyp.backend.responses.v1.ResourceTypes;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

@JsonSubTypes({ @Type(value = ValidationMessageResource.class, name = ResourceTypes.VALIDATION_MESSAGE_RESOURCE) })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class ValidationMessageResource implements Serializable, Comparable<ValidationMessageResource> {

	private static final long serialVersionUID = -2783912229763612724L;

	private ValidationMessageKind kind;

	private String message;

	private String messageTemplate;

	private Object[] messageArguments;

	public ValidationMessageResource() {
		super();
	}

	public ValidationMessageResource(String message) {
		this.message = message;
	}

	public ValidationMessageResource(ValidationMessage validationMessage) {
		this.kind = validationMessage.getKind();
		this.message = validationMessage.getMessage();
		this.messageTemplate = validationMessage.getKind().template;
		this.messageArguments = validationMessage.getArgs();
	}

	public ValidationMessageKind getKind() {
		return kind;
	}

	public void setKind(ValidationMessageKind kind) {
		this.kind = kind;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageTemplate() {
		return messageTemplate;
	}

	public void setMessageTemplate(String messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	public Object[] getMessageArguments() {
		return messageArguments;
	}

	public void setMessageArguments(Object[] messageArguments) {
		this.messageArguments = messageArguments;
	}

	@Override
	public int compareTo(ValidationMessageResource that) {
		if (that == null) {
			return -1;
		} else {
			if (this.kind.ordinal() != that.kind.ordinal()) {
				return this.kind.ordinal() - that.kind.ordinal();
			}
			if (this.messageArguments.length != that.messageArguments.length) {
				return this.messageArguments.length - that.messageArguments.length;
			}
			for (int i = 0; i < this.messageArguments.length; i++) {
				if (!this.messageArguments[i].equals(that.messageArguments[i])) {
					return this.messageArguments[i].hashCode() - that.messageArguments[i].hashCode();
				}
			}
		}

		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ValidationMessageResource that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return kind.hashCode() * 17 + Arrays.hashCode(messageArguments);
	}
}
