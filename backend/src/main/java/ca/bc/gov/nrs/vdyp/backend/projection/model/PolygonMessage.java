package ca.bc.gov.nrs.vdyp.backend.projection.model;

import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
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
	private Stand stand;

	/** The error code for the operation to which this message is associated. */
	private ReturnCode errorCode;

	/** The message associated with the text of the message, including the template, arguments and severity */
	private ValidationMessage message;

	private PolygonMessage() {
	}

	/**
	 * @return the specific layer to which this message applies. <code>null</code> indicates this a polygon level
	 *         message and does not apply to a particular layer.
	 */
	public Layer getLayer() {
		return layer;
	}

	/**
	 * @return the specific Species Group (SP0) to which this message is associated. If not known or not applicable,
	 *         this member is to be <code>null</code>.
	 */
	public Stand getStand() {
		return stand;
	}

	/** @return the error code for the operation to which this message is associated. */
	public ReturnCode getErrorCode() {
		return errorCode;
	}

	/** @return the message code associated with the text of the message. */
	public ValidationMessage getMessage() {
		return message;
	}

	public static class Builder {
		private PolygonMessage polygonMessage = new PolygonMessage();

		public Builder setLayer(Layer layer) {
			polygonMessage.layer = layer;
			return this;
		}

		public Builder setStand(Stand stand) {
			polygonMessage.stand = stand;
			return this;
		}

		public Builder setErrorCode(ReturnCode errorCode) {
			polygonMessage.errorCode = errorCode;
			return this;
		}

		public Builder setMessage(ValidationMessage message) {
			polygonMessage.message = message;
			return this;
		}

		public PolygonMessage build() {
			return polygonMessage;
		}
	}
}
