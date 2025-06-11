package ca.bc.gov.nrs.vdyp.ecore.projection.output;

import java.io.InputStream;

public interface IMessageLog {

	void addMessage(String message, Object... arguments);

	InputStream getAsStream();

}
