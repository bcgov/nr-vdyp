package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;

public class ErrorMessageUtils {
	public static String BuildMessage(VdypApplicationIdentifier appId, Polygon polygon, String verb, Throwable e) {
		return MessageFormat.format(
				"Polygon {0}: encountered error in {1} when {2} polygon{3}", polygon, appId, verb, serializeCauses(e)
		);
	}

	/**
	 * Return the reasons of the cause chain of <code>e</code>, making every effort to remove duplication as well as
	 * class names that prefix the individual exceptions.
	 *
	 * @param e the exception in question
	 * @return as described
	 */
	private static String serializeCauses(Throwable e) {
		var messageList = new ArrayList<String>();
		return serializeCauses(new StringBuffer(), messageList, e).toString();
	}

	/**
	 * A regular expression that allows us to extract the message from a string possibly prefixed with a Java class name
	 * and possibly suffixed with a "." Group 3 applies if the message is suffixed with "." and group 4 otherwise. All
	 * messages -should- match this pattern, but if cases have been missed the method will simply take the whole
	 * message.
	 */
	private static final Pattern messagePattern = Pattern.compile("(^[a-zA-Z_0-9\\.]+: )?+((.+)\\.|(.*[^\\.]))$");

	private static StringBuffer serializeCauses(StringBuffer s, List<String> messageList, Throwable e) {

		if (e.getMessage() != null) {
			var matcher = messagePattern.matcher(e.getMessage());

			String message;
			if (matcher.matches()) {
				if (matcher.group(3) != null) {
					message = matcher.group(3);
				} else {
					message = matcher.group(4);
				}
			} else {
				message = e.getMessage();
			}

			if (!StringUtils.isBlank(message) && !containsSuffix(messageList, message)) {
				s.append(": ").append(message);
				messageList.add(message);
			}
		}

		if (e.getCause() != null) {
			serializeCauses(s, messageList, e.getCause());
		}

		return s;
	}

	private static boolean containsSuffix(List<String> messageList, String message) {
		for (var m : messageList) {
			if (m.endsWith(message)) {
				return true;
			}
		}
		return false;
	}
}
