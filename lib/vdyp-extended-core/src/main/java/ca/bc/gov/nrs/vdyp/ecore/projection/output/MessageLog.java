package ca.bc.gov.nrs.vdyp.ecore.projection.output;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class MessageLog implements IMessageLog {

	private static final Logger logger = LoggerFactory.getLogger(MessageLog.class);

	private List<String> messages = new ArrayList<>();
	private final Level loggerLevel;

	public MessageLog(Level loggerLevel) {
		this.loggerLevel = loggerLevel;
	}

	@Override
	public void addMessage(String message, Object... arguments) {
		if (arguments.length > 0) {
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = arguments[i] == null ? "null" : arguments[i].toString();
			}
			message = MessageFormat.format(message, arguments);
		}
		messages.add(message);
		logger.atLevel(loggerLevel).log(loggerLevel + " message: " + message);
	}

	@Override
	public InputStream getAsStream() {
		StringBuffer sb = new StringBuffer();
		messages.stream().forEach(s -> sb.append(s).append('\n'));
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
}
