package ca.bc.gov.nrs.vdyp.exceptions;

import ca.bc.gov.nrs.vdyp.model.LayerType;

public abstract class LayerValidationException extends StandProcessingException {

	private static final long serialVersionUID = -4273805289141234254L;

	final LayerType layer;

	protected LayerValidationException(
			RuntimeStandProcessingException cause, Class<? extends LayerValidationException> klazz
	) {
		super(cause);
		this.layer = unwrap(cause, klazz).getLayer();
	}

	protected LayerValidationException(LayerType layer, String message) {
		super(message);
		this.layer = layer;
	}

	protected LayerValidationException(LayerType layer, String message, Throwable cause) {
		super(message, cause);
		this.layer = layer;
	}

	public LayerType getLayer() {
		return layer;
	}

}
