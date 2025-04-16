package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * A value was lower than expected or was missing
 *
 * @param <V>
 */
public abstract class LayerValueLowException extends LayerValidationException {

	private static final long serialVersionUID = -7583524977329450918L;

	static final String TEMPLATE_LOW = "{0} of {1} was lower than expected {2}";
	static final String TEMPLATE_MISSING = "Required {0} was missing";

	@Nullable
	private final Number value;
	@Nullable
	private final Number threshold;

	static String getMessage(String name, Optional<? extends Number> value, Optional<? extends Number> threshold) {
		if (threshold.isEmpty() && !value.isEmpty()) {
			throw new IllegalArgumentException("If value is present, threshold must be present");
		}
		return value.map(v -> MessageFormat.format(TEMPLATE_LOW, name, v, threshold.get()))
				.orElse(MessageFormat.format(TEMPLATE_MISSING, name));
	}

	protected LayerValueLowException(RuntimeProcessingException cause, Class<? extends LayerValueLowException> klazz) {
		super(cause, klazz);
		this.value = unwrap(cause, klazz).getValue().orElse(null);
		this.threshold = unwrap(cause, klazz).getThreshold().orElse(null);
	}

	protected LayerValueLowException(
			LayerType layer, String name, Optional<? extends Number> value, Optional<? extends Number> threshold,
			Throwable cause
	) {
		super(layer, getMessage(name, value, threshold), cause);
		this.value = value.map(Number.class::cast).orElse(null);
		this.threshold = threshold.map(Number.class::cast).orElse(null);
	}

	protected LayerValueLowException(
			LayerType layer, String name, Optional<? extends Number> value, Optional<? extends Number> threshold
	) {
		super(layer, getMessage(name, value, threshold));
		this.value = value.map(Number.class::cast).orElse(null);
		this.threshold = threshold.map(Number.class::cast).orElse(null);
	}

	public Optional<Number> getValue() {
		return Optional.ofNullable(value);
	}

	public Optional<Number> getThreshold() {
		return Optional.ofNullable(threshold);
	}

	@FunctionalInterface
	protected static interface Constructor<T extends LayerValueLowException, N extends Number> {

		T build(LayerType layer, String name, Optional<N> value, Optional<N> threshold);

	}

	private static <T extends LayerValueLowException, N extends Number> Optional<T> check(
			LayerType layer, String name, Optional<N> value, N threshold, boolean inclusive,
			Constructor<T, N> constructor
	) {
		final Optional<Double> castValue = value.map(Number::doubleValue);
		final double castThreshold = threshold.doubleValue();
		final Function<Double, Boolean> test = inclusive ? v -> v < castThreshold : v -> v <= castThreshold;
		final boolean fail = castValue.map(test).orElse(true);
		if (fail) {
			return Optional.of(constructor.build(layer, name, value, Optional.of(threshold)));
		}
		return Optional.empty();
	}

	protected static <T extends LayerValueLowException, N extends Number> Optional<T> checkInclusive(
			LayerType layer, String name, Optional<N> value, N threshold, Constructor<T, N> constructor
	) {
		return check(layer, name, value, threshold, true, constructor);
	}

	protected static <T extends LayerValueLowException, N extends Number> Optional<T> checkExclusive(
			LayerType layer, String name, Optional<N> value, N threshold, Constructor<T, N> constructor
	) {
		return check(layer, name, value, threshold, false, constructor);
	}

}
