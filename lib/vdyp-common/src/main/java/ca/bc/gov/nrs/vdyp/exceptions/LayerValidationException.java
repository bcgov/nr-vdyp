package ca.bc.gov.nrs.vdyp.exceptions;

import ca.bc.gov.nrs.vdyp.model.LayerType;

public abstract class LayerValidationException extends StandProcessingException {

	private static final long serialVersionUID = -4273805289141234254L;

	final LayerType layer;

	protected LayerValidationException(
			RuntimeStandProcessingException cause, Class<? extends LayerValidationException> klazz
	) {
		this(cause, unwrap(cause, klazz));
	}

	protected LayerValidationException(LayerType layer, String message) {
		super(message);
		this.layer = layer;
	}

	private LayerValidationException(RuntimeStandProcessingException cause, LayerValidationException unwrapped) {
		super(unwrapped.getMessage(), cause);
		this.layer = unwrapped.getLayer();
	}

	protected LayerValidationException(LayerType layer, String message, Throwable cause) {
		super(message, cause);
		this.layer = layer;
	}

	public LayerType getLayer() {
		return layer;
	}

}
