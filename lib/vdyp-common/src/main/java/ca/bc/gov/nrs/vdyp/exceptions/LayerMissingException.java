package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * With layer == PRIMARY this is equivalent to IPASS=-1 in both VRI and FIP
 */
public class LayerMissingException extends LayerValidationException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String TEMPLATE = "Expected layer {0} but it does not exist";

	LayerMissingException(LayerType layer, Throwable cause) {
		super(layer, MessageFormat.format(TEMPLATE, layer), cause);
	}

	public LayerMissingException(LayerType layer) {
		super(layer, MessageFormat.format(TEMPLATE, layer));
	}

	public LayerMissingException(RuntimeProcessingException cause) {
		super(cause, LayerMissingException.class);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app.isStart() && getLayer() == LayerType.PRIMARY)
			return Optional.of(-1);
		return Optional.empty();
	}

}
