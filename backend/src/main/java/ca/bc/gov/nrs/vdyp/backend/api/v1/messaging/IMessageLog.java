package ca.bc.gov.nrs.vdyp.backend.api.v1.messaging;

import java.io.InputStream;

public interface IMessageLog {

	void addMessage(String message, Object... arguments);

	InputStream getAsStream();

}