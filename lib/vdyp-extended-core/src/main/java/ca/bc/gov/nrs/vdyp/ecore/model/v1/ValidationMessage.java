package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;

import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage;

public class ValidationMessage implements Comparable<ValidationMessage>, Serializable {

	private static final long serialVersionUID = 5071025981103618615L;

	/** the kind of this ValidationMessage - an enum value and a template */
	private final ValidationMessageKind kind;

	/** the arguments that, when applied to the template of kind, produce the message */
	private final Object[] args;

	/** the resultion message */
	private final String message;

	public ValidationMessage(ValidationMessageKind kind, Object... args) {
		this.kind = kind;
		this.args = args;

		this.message = MessageFormat.format(kind.template, args);
	}

	public ValidationMessage(PolygonMessage polygonMessage) {
		this.kind = null;
		this.args = null;
		this.message = polygonMessage.toString();
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

	@Override
	public String toString() {
		return MessageFormat.format(message, args);
	}

	@Override
	public int compareTo(ValidationMessage that) {
		if (that == null) {
			return -1;
		} else {
			if (this.kind.ordinal() != that.kind.ordinal()) {
				return this.kind.ordinal() - that.kind.ordinal();
			}
			if (this.args.length != that.args.length) {
				return this.args.length - that.args.length;
			}
			for (int i = 0; i < this.args.length; i++) {
				if (!this.args[i].equals(that.args[i])) {
					return this.args[i].hashCode() - that.args[i].hashCode();
				}
			}
		}

		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ValidationMessage that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return kind.hashCode() * 17 + Arrays.hashCode(args);
	}
}
