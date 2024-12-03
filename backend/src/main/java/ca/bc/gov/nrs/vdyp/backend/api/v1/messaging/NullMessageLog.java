package ca.bc.gov.nrs.vdyp.backend.api.v1.messaging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class NullMessageLog implements IMessageLog {

	private static final Logger logger = LoggerFactory.getLogger(NullMessageLog.class);

	private final Level loggerLevel;

	public NullMessageLog(Level loggerLevel) {
		this.loggerLevel = loggerLevel;
	}

	@Override
	public void addMessage(String message, Object... arguments) {
		if (arguments.length > 0) {
			message = MessageFormat.format(message, arguments);
		}
		logger.atLevel(loggerLevel).log(loggerLevel + " message: " + message);
	}

	@Override
	public InputStream getAsStream() {
		return new ByteArrayInputStream(new byte[0]);
	}
}
