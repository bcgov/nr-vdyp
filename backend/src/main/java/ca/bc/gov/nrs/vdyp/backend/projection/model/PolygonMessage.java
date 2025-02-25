package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.backend.model.v1.SeverityCode;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;

/**
 * Holds a single polygon message that can be reported back to the calling application.
 */
public class PolygonMessage {

	/**
	 * The polygon with which this message is associated.
	 */
	private Polygon polygon;

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

	/** The severity code associated with the message. */
	private SeverityCode severity;

	/** The return code for the operation with which this message is associated. */
	private ReturnCode returnCode;

	/** The message associated with the text of the message, including the template, arguments and severity */
	private ValidationMessage message;

	private PolygonMessage() {
	}

	/**
	 * @return the polygon with which this message is associated.
	 */
	public Polygon getPolygon() {
		return polygon;
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

	/** @return the return code for the operation with which this message is associated. */
	public ReturnCode getReturnCode() {
		return returnCode;
	}

	/** @return the message code associated with the text of the message. */
	public ValidationMessage getMessage() {
		return message;
	}

	/** @return the severity code associated with the message. */
	public SeverityCode getSeverity() {
		return severity;
	}

	public static class Builder {
		private PolygonMessage polygonMessage = new PolygonMessage();

		public Builder polygon(Polygon polygon) {
			polygonMessage.polygon = polygon;
			return this;
		}

		public Builder layer(Layer layer) {
			if (polygonMessage.polygon != null && layer.getPolygon() != polygonMessage.polygon) {
				throw new IllegalArgumentException(
						MessageFormat.format(
								"PolygonMessage: layer {0}'s polygon {1} does not match polygon {2}", layer,
								layer.getPolygon(), polygonMessage.polygon
						)
				);
			}

			polygonMessage.layer = layer;
			polygonMessage.polygon = layer.getPolygon();
			
			return this;
		}

		public Builder stand(Stand stand) {
			if (polygonMessage.layer != null && stand.getLayer() != polygonMessage.layer) {
				throw new IllegalArgumentException(
						MessageFormat.format(
								"PolygonMessage: stand {0}'s layer {1} does not match layer {2}", stand,
								stand.getLayer(), polygonMessage.layer
						)
				);
			}

			polygonMessage.stand = stand;
			polygonMessage.layer = stand.getLayer();
			polygonMessage.polygon = stand.getLayer().getPolygon();

			return this;
		}

		public Builder returnCode(ReturnCode errorCode) {
			polygonMessage.returnCode = errorCode;
			return this;
		}

		public Builder message(ValidationMessage message) {
			polygonMessage.message = message;
			return this;
		}

		public Builder severity(SeverityCode severity) {
			polygonMessage.severity = severity;
			return null;
		}

		public PolygonMessage build() {
			if (polygonMessage.polygon == null) {
				throw new IllegalStateException("PolygonMessage: polygon not specified at build time");
			}
			if (polygonMessage.message == null) {
				throw new IllegalStateException("PolygonMessage: message not specified at build time");
			}
			if (polygonMessage.returnCode == null) {
				throw new IllegalStateException("PolygonMessage: returnCode not specified at build time");
			}
			if (polygonMessage.severity == null) {
				throw new IllegalStateException("PolygonMessage: severity not specified at build time");
			}

			return polygonMessage;
		}
	}
}
