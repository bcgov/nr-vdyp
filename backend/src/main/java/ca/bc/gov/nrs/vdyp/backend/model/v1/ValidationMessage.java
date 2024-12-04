package ca.bc.gov.nrs.vdyp.backend.model.v1;

import java.text.MessageFormat;

public class ValidationMessage {

	/** the kind of this ValidationMessage - an enum value and a template */
	private final ValidationMessageKind kind;
	/** the arguments that, when applied to the template of kind, produce the message */
	private final Object[] args;

	/** the resultion message, calculated from <code>kind</code> and <code>args</code> */
	private final String message;

	public ValidationMessage(ValidationMessageKind kind, Object[] args) {
		this.kind = kind;
		this.args = args;

		this.message = MessageFormat.format(kind.template, args);
	}

	/** the kind of this ValidationMessage - an enum value and a template */
	public ValidationMessageKind getKind() {
		return kind;
	}

	/** the arguments that, when applied to the template of kind, produce the message */
	public Object[] getArgs() {
		return args;
	}

	/** the resultion message, calculated from <code>kind</code> and <code>args</code> */
	public String getMessage() {
		return message;
	}
}
