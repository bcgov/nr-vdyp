package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.model.BaseDebugSettings;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.DoubleCoefficients;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.ReadOnlyCoefficients;
import ca.bc.gov.nrs.vdyp.model.ReadOnlyDoubleCoefficients;
import ca.bc.gov.nrs.vdyp.model.ReadOnlyMatrixMap;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;
import ca.bc.gov.nrs.vdyp.model.StockingClassFactor;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariables;

/**
 * Makes the values of a control map read only, so that the map can be shared between runs (see
 * {@link ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser#parseSharedByName}). Each value is replaced by a read
 * only form that behaves the same for reading, and throws {@link UnsupportedOperationException} on any attempt to
 * change it:
 * <ul>
 * <li>{@link MatrixMap}: a {@link ReadOnlyMatrixMap} view with the same MatrixMapN interface</li>
 * <li>{@link Coefficients} and {@link DoubleCoefficients}: a {@link ReadOnlyCoefficients} or
 * {@link ReadOnlyDoubleCoefficients} copy</li>
 * <li>{@link Map} and {@link List}: an unmodifiable view, so a map subclass that overrides {@code get} keeps its
 * behaviour</li>
 * <li>{@link BaseDebugSettings} and {@link ControlVariables}: the same object, made read only, since code depends on
 * their exact subclasses</li>
 * </ul>
 * Values nested in these are made read only too. Types that are already immutable are kept as they are. A value of any
 * other type causes an {@link IllegalArgumentException}, so that a new type of parsed value can not be shared without
 * first deciding how to protect it.
 * <p>
 * The conversion changes the given objects in place (for example, it replaces the values held by a matrix with their
 * read only forms). It must only be applied to a freshly parsed map that has not yet been shared.
 */
public final class SharedControlMapValues {

	/** Types whose instances can not be changed after construction */
	private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
			String.class, Integer.class, Long.class, Short.class, Byte.class, Float.class, Double.class, Boolean.class,
			Character.class, BecLookup.class, BecDefinition.class, GenusDefinitionMap.class, GenusDefinition.class,
			StockingClassFactor.class, SiteCurveAgeMaximum.class, ComponentSizeLimits.class, CompVarAdjustments.class,
			GrowthFiatDetails.class
	);

	/** Read only form of each object already converted, so shared references stay shared */
	private final Map<Object, Object> converted = new IdentityHashMap<>();

	private SharedControlMapValues() {
	}

	/**
	 * Replace every value of {@code controlMap} with its read only form.
	 *
	 * @param controlMap a freshly parsed control map; it is changed in place
	 * @throws IllegalArgumentException if a value has a type that has no read only form
	 */
	public static void makeReadOnly(Map<String, Object> controlMap) {
		var conversion = new SharedControlMapValues();
		controlMap.replaceAll((key, value) -> {
			try {
				return conversion.readOnly(value);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Control map entry " + key + ": " + e.getMessage(), e);
			}
		});
	}

	/**
	 * @return the read only form of a single value
	 * @throws IllegalArgumentException if the value has a type that has no read only form
	 */
	public static Object readOnlyValue(Object value) {
		return new SharedControlMapValues().readOnly(value);
	}

	private Object readOnly(Object value) {
		if (value == null || value instanceof Enum<?> || IMMUTABLE_TYPES.contains(value.getClass())) {
			return value;
		}
		var previous = converted.get(value);
		if (previous != null) {
			return previous;
		}
		var result = convert(value);
		converted.put(value, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object convert(Object value) {
		if (value instanceof MatrixMap<?> matrix) {
			return readOnlyMatrix((MatrixMap<Object>) matrix);
		}
		// Coefficients and DoubleCoefficients are Lists, so they must be handled before plain Lists
		if (value instanceof Coefficients coefficients) {
			return ReadOnlyCoefficients.of(coefficients);
		}
		if (value instanceof DoubleCoefficients coefficients) {
			return ReadOnlyDoubleCoefficients.of(coefficients);
		}
		if (value instanceof ModelCoefficients modelCoefficients) {
			return new ModelCoefficients(
					modelCoefficients.getModel(), (Coefficients) readOnly(modelCoefficients.getCoefficients())
			);
		}
		if (value instanceof Map<?, ?> map) {
			return readOnlyMap((Map<Object, Object>) map);
		}
		// Only JDK lists become unmodifiable lists; a VDYP List subclass would lose its type
		if (value instanceof List<?> list && value.getClass().getName().startsWith("java.")) {
			var copy = new ArrayList<Object>(list.size());
			for (var element : list) {
				copy.add(readOnly(element));
			}
			return Collections.unmodifiableList(copy);
		}
		if (value instanceof Optional<?> optional) {
			return optional.map(this::readOnly);
		}
		if (value instanceof BaseDebugSettings debugSettings) {
			debugSettings.makeReadOnly();
			return debugSettings;
		}
		if (value instanceof ControlVariables controlVariables) {
			controlVariables.makeReadOnly();
			return controlVariables;
		}
		throw new IllegalArgumentException(
				"No read only form is defined for a shared control map value of type " + value.getClass().getName()
						+ "; add one to " + SharedControlMapValues.class.getSimpleName()
		);
	}

	private MatrixMap<Object> readOnlyMatrix(MatrixMap<Object> matrix) {
		// The matrix is not yet shared, so its cells can be replaced with their read only forms
		matrix.eachKey(key -> {
			var cell = matrix.getM(key);
			var readOnlyCell = readOnly(cell);
			if (readOnlyCell != cell) {
				matrix.putM(readOnlyCell, key);
			}
		});
		return ReadOnlyMatrixMap.of(matrix);
	}

	private Map<Object, Object> readOnlyMap(Map<Object, Object> map) {
		Map<Object, Object> changes = new LinkedHashMap<>();
		for (var entry : map.entrySet()) {
			var value = entry.getValue();
			var readOnlyValue = readOnly(value);
			if (readOnlyValue != value) {
				changes.put(entry.getKey(), readOnlyValue);
			}
		}
		if (changes.isEmpty()) {
			// Keep the original map, so a subclass that overrides get keeps its behaviour
			return Collections.unmodifiableMap(map);
		}
		try {
			// The map is not yet shared, so its values can be replaced in place
			map.putAll(changes);
			return Collections.unmodifiableMap(map);
		} catch (UnsupportedOperationException e) {
			// Already unmodifiable, so copy it with the read only values
			Map<Object, Object> copy = new LinkedHashMap<>(map);
			copy.putAll(changes);
			return Collections.unmodifiableMap(copy);
		}
	}
}
