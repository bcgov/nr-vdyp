package ca.bc.gov.nrs.vdyp.backend.v1.api.impl.messaging;

import java.io.InputStream;

public interface IMessageLog {

	void addMessage(String message, Object... arguments);

	InputStream getAsStream();

}