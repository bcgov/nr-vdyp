package ca.bc.gov.nrs.vdyp.ecore.projection.output;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.event.Level;

public class NullMessageLog implements IMessageLog {

	public NullMessageLog(Level loggerLevel) {
		/* do nothing */
	}

	@Override
	public void addMessage(String message, Object... arguments) {
		/* do nothing */
	}

	@Override
	public InputStream getAsStream() {
		return new ByteArrayInputStream(new byte[0]);
	}
}
