package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

public abstract class AbstractProjectionRequestException extends Exception {

	private static final long serialVersionUID = -3349545755843821804L;
	private final List<ValidationMessage> validationMessages;

	/**
	 * The polygon/layer/species context (e.g. "Polygon 123 Layer 1") this exception was raised against, if any. Only
	 * populated by the AbstractProjectionRequestException(String, List) constructor, since the other constructors
	 * already bake any context directly into their single GENERIC validation message.
	 */
	private final String contextPrefix;

	public AbstractProjectionRequestException(List<ValidationMessage> validationMessages) {
		super(buildMessage(validationMessages));
		this.validationMessages = validationMessages;
		this.contextPrefix = null;
	}

	public AbstractProjectionRequestException(String message, List<ValidationMessage> validationMessages) {
		super( (message != null ? (message + ": ") : "") + buildMessage(validationMessages));
		this.validationMessages = validationMessages;
		this.contextPrefix = message;
	}

	public AbstractProjectionRequestException(Throwable cause) {
		super(cause);
		validationMessages = new ArrayList<ValidationMessage>();
		if (cause.getMessage() != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, cause.getMessage()));
		}
		this.contextPrefix = null;
	}

	public AbstractProjectionRequestException(String message, Throwable e) {
		super(message, e);
		validationMessages = new ArrayList<ValidationMessage>();
		if (message != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, message));
		}
		this.contextPrefix = null;
	}

	public AbstractProjectionRequestException(String message) {
		super(message);
		validationMessages = new ArrayList<ValidationMessage>();
		if (message != null) {
			validationMessages.add(new ValidationMessage(ValidationMessageKind.GENERIC, message));
		}
		this.contextPrefix = null;
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}

	/**
	 * @return the polygon/layer/species context this exception was raised against (e.g. "Polygon 123 Layer 1"), or null
	 *         if this exception's validation messages already carry that context individually.
	 */
	public String getContextPrefix() {
		return contextPrefix;
	}

	/**
	 * Prepends contextPrefix to message, e.g. "Polygon 123: some problem". If message already starts with that same
	 * prefix (some validation message templates already embed the polygon/layer identity on their own), the prefix is
	 * not repeated.
	 */
	public static String prefixMessage(String contextPrefix, String message) {
		if (message != null && message.startsWith(contextPrefix + ": ")) {
			return message;
		}
		return contextPrefix + ": " + message;
	}

	protected static String buildContextPrefix(long featureId) {
		return "Polygon " + featureId;
	}

	protected static String buildContextPrefix(long featureId, String layerId) {
		if (layerId == null) {
			return buildContextPrefix(featureId);
		}
		return buildContextPrefix(featureId) + " Layer " + layerId;
	}

	protected static String buildContextPrefix(long featureId, String layerId, String speciesCode) {
		if (speciesCode == null) {
			return buildContextPrefix(featureId, layerId);
		}
		return buildContextPrefix(featureId, layerId) + " Species " + speciesCode;
	}

	protected static String withContext(String contextPrefix, String message) {
		return contextPrefix + (message != null ? ": " + message : "");
	}

	private static String buildMessage(List<ValidationMessage> validationMessages) {
		if (validationMessages.size() > 0) {
			StringBuffer sb = new StringBuffer(validationMessages.get(0).toString());
			for (int i = 1; i < validationMessages.size(); i++) {
				sb.append("; ").append(validationMessages.get(i).getMessage());
			}
			return sb.toString();
		} else {
			return "";
		}
	}
}
