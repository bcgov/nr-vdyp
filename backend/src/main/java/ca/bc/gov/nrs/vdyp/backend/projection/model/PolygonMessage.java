package ca.bc.gov.nrs.vdyp.backend.projection.model;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.MessageCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.MessageSeverity;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;

/**
 * Holds a single polygon message that can be reported back to the calling application.
 */
public class PolygonMessage {

	/**
	 * The specific layer to which this message applies. <code>null</code> indicates this a polygon level message and
	 * does not apply to a particular layer.
	 */
	private Layer layer;

	/**
	 * The specific Species Group (SP0) to which this message is associated. If not known or not applicable, this member
	 * is to be <code>null</code>.
	 */
	private StandComponent standComponent;

	/** The error code for the operation to which this message is associated. */
	private ReturnCode errorCode;

	/** The severity of the message. */
	private MessageSeverity severity;

	/** The message code associated with the text of the message. */
	private MessageCode messageCode;

	/** The text of the message. */
	private String message;

	/**
	 * @return the specific layer to which this message applies. <code>null</code> indicates this a polygon level message and
	 * does not apply to a particular layer.
	 */
	public Layer getLayer() {
		return layer;
	}

	/**
	 * @return the specific Species Group (SP0) to which this message is associated. If not known or not applicable, this member
	 * is to be <code>null</code>.
	 */
	public StandComponent getStandComponent() {
		return standComponent;
	}

	/** @return the error code for the operation to which this message is associated. */
	public ReturnCode getErrorCode() {
		return errorCode;
	}

	/** @return the severity of the message. */
	public MessageSeverity getSeverity() {
		return severity;
	}

	/** @return the message code associated with the text of the message. */
	public MessageCode getMessageCode() {
		return messageCode;
	}

	/** @return the text of the message. */
	public String getMessage() {
		return message;
	}
}
