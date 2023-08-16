package ca.bc.gov.nrs.vdyp.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

public class Utils {

	/**
	 * Returns a singleton set containing the value if it's not null, otherwise an
	 * empty set
	 *
	 * @param <T>
	 * @param value
	 * @return
	 */
	public static <T> Set<T> singletonOrEmpty(@Nullable T value) {
		if (Objects.isNull(value)) {
			return Collections.emptySet();
		}
		return Collections.singleton(value);
	}

	@SuppressWarnings("unchecked")
	public static <U> U expectParsedControl(Map<String, Object> control, String key, Class<? super U> clazz) {
		var value = control.get(key);
		if (value == null) {
			throw new IllegalStateException("Expected control map to have " + key);
		}
		if (clazz != String.class && value instanceof String) {
			throw new IllegalStateException(
					"Expected control map entry " + key + " to be parsed but was still a String " + value
			);
		}
		if (!clazz.isInstance(value)) {
			throw new IllegalStateException(
					"Expected control map entry " + key + " to be a " + clazz.getSimpleName() + " but was a "
							+ value.getClass()
			);
		}
		return (U) value;
	}

	/**
	 * Creates a Comparator that compares two objects by applying the given accessor
	 * function to get comparable values that are then compared.
	 *
	 * @param <T>      type to be compared with the Comparator
	 * @param <V>      Comparable type
	 * @param accessor Function getting a V from a T
	 */
	public static <T, V extends Comparable<V>> Comparator<T> compareUsing(Function<T, V> accessor) {
		return (x, y) -> accessor.apply(x).compareTo(accessor.apply(y));
	}
}
