package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;

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
	 * does not apply to a particular layer (layer).
	 */
	private Layer layer;

	/**
	 * The specific Species Group to which this message is associated. If not known or not applicable, this member is to
	 * be <code>null</code> (standComponent).
	 */
	private Stand stand;

	/**
	 * The specific Species (SP0) to which this message is associated. If not known or not applicable, this member is to
	 * be <code>null</code> (speciesComponent)
	 */
	private Species species;

	/** The severity code associated with the message (severity) */
	private MessageSeverityCode severity;

	/** The return code for the operation with which this message is associated (errorCode) */
	private ReturnCode returnCode;

	/** The template of the message */
	private PolygonMessageKind kind;

	/** The template arguments */
	private Object[] args;

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

	/** @return the return code for the operation with which this message is associated (iErrorCode) */
	public ReturnCode getReturnCode() {
		return returnCode;
	}

	/** @return the severity code associated with the message. */
	public MessageSeverityCode getSeverity() {
		return severity;
	}

	public PolygonMessageKind getKind() {
		return kind;
	}

	public String getSimpleMessageText() {
		StringBuilder sb = new StringBuilder();
		sb.append(severity.getText()).append(": ");

		sb.append(MessageFormat.format(kind.getTemplate(), args));

		return sb.toString();

	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		if (species != null) {
			sb.append(species.toString());
		} else if (layer != null) {
			sb.append(layer.toString());
		} else if (stand != null) {
			sb.append(stand.toString());
		} else if (polygon != null) {
			sb.append(polygon.toString());
		}

		sb.append(' ').append(severity.getText()).append(": ");

		sb.append(MessageFormat.format(kind.getTemplate(), args));
		sb.append(" ").append(getSimpleMessageText());

		return sb.toString();
	}

	public static class Builder {
		private PolygonMessage polygonMessage = new PolygonMessage();

		public Builder polygon(Polygon polygon) {
			polygonMessage.polygon = polygon;
			return this;
		}

		public Builder layer(Layer layer) {
			if (polygonMessage.polygon != null || polygonMessage.stand != null || polygonMessage.species != null) {
				throw new IllegalArgumentException("PolygonMessage: species one of polygon, layer, stand or species");
			}

			polygonMessage.layer = layer;
			polygonMessage.polygon = layer.getPolygon();

			return this;
		}

		public Builder stand(Stand stand) {
			if (polygonMessage.polygon != null || polygonMessage.stand != null || polygonMessage.species != null) {
				throw new IllegalArgumentException("PolygonMessage: species one of polygon, layer, stand or species");
			}

			polygonMessage.stand = stand;
			polygonMessage.layer = stand.getLayer();
			polygonMessage.polygon = stand.getLayer().getPolygon();

			return this;
		}

		public Builder species(Species species) {
			if (polygonMessage.polygon != null || polygonMessage.stand != null || polygonMessage.species != null) {
				throw new IllegalArgumentException("PolygonMessage: species one of polygon, layer, stand or species");
			}

			polygonMessage.species = species;
			polygonMessage.stand = species.getStand();
			polygonMessage.layer = polygonMessage.stand.getLayer();
			polygonMessage.polygon = polygonMessage.layer.getPolygon();

			return this;
		}

		public Builder
				details(ReturnCode returnCode, MessageSeverityCode severity, PolygonMessageKind kind, Object... args) {
			polygonMessage.returnCode = returnCode;
			polygonMessage.severity = severity;
			polygonMessage.kind = kind;
			polygonMessage.args = args;

			return this;
		}

		public PolygonMessage build() {
			if (polygonMessage.polygon == null) {
				throw new IllegalStateException("PolygonMessage: polygon not specified at build time");
			}
			if (polygonMessage.kind == null) {
				throw new IllegalStateException("PolygonMessage: template not specified at build time");
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
