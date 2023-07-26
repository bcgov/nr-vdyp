package ca.bc.gov.nrs.vdyp.common;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
}
