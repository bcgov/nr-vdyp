package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * A value was lower than expected or was missing
 * 
 * @param <V>
 */
public abstract class LayerValueLowException extends LayerValidationException {

	private static final long serialVersionUID = -7583524977329450918L;

	static final String TEMPLATE_LOW = "{0} {1} was too low";
	static final String TEMPLATE_MISSING = "Required {0} was missing";

	final private Optional<Number> value;

	static String getMessage(String name, Optional<? extends Number> value) {
		return value.map(v -> MessageFormat.format(TEMPLATE_LOW, name, v)).orElse(
				MessageFormat.format(TEMPLATE_MISSING, name)
		);
	}

	protected LayerValueLowException(
			RuntimeStandProcessingException cause, Class<? extends LayerValueLowException> klazz
	) {
		super(cause, klazz);
		this.value = unwrap(cause, klazz).getValue();
	}

	LayerValueLowException(LayerType layer, String name, Optional<? extends Number> value) {
		super(layer, getMessage(name, value));
		this.value = value.map(Number.class::cast);
	}

	public Optional<Number> getValue() {
		return value;
	}

}
