package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.text.MessageFormat;

public class Exceptions {

	public static String getMessage(Throwable t) {
		return getMessage(t, null);
	}

	public static String getMessage(Throwable t, String context) {

		String message;

		if (context == null) {
			context = "";
		} else {
			context = context.stripTrailing() + " ";
		}

		if (t.getCause() != null) {

			message = MessageFormat.format(
					"{0}saw {1} exception with cause {2}{3}", context, t.getClass().getName(),
					t.getCause().getClass().getName(),
					t.getCause().getMessage() != null ? "; reason: " + t.getCause().getMessage() : ""
			);

		} else {

			message = MessageFormat.format(
					"{0}saw {1}{2}", context, t.getClass().getName(),
					t.getMessage() != null ? "; reason: " + t.getMessage() : ""
			);
		}

		return message;
	}
}
