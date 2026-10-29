package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class ErrorMessageUtils {
	/**
	 * A regular expression that allows us to extract the message from a string possibly prefixed with a Java class name
	 * and possibly suffixed with a "." Group 3 applies if the message is suffixed with "." and group 4 otherwise. All
	 * messages -should- match this pattern, but if cases have been missed the method will simply take the whole
	 * message.
	 */
	private static final Pattern messagePattern = Pattern.compile("(^[a-zA-Z_0-9\\.]+: )?+((.+)\\.|(.*[^\\.]))$");

	public static StringBuffer serializeCauses(StringBuffer s, List<String> messageList, Throwable e) {

		if (e.getMessage() != null) {
			String message = e.getMessage();
			var matcher = messagePattern.matcher(message);

			if (matcher.matches()) {
				if (matcher.group(3) != null) {
					message = matcher.group(3);
				} else {
					message = matcher.group(4);
				}
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
