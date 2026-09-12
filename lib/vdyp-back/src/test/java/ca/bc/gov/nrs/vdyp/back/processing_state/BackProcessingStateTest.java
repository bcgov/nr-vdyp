package ca.bc.gov.nrs.vdyp.back.processing_state;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasIndexedPropertyAt;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.beanutils.ConvertUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class BackProcessingStateTest {

	static List<Arguments> scalarAccessors() {
		return List.of(
				Arguments.of("convergenceYear", Integer.TYPE, 1995), Arguments.of("baseAreaVeteran", Float.TYPE, 42f),
				Arguments.of("convergenceAge", Float.TYPE, 42f),
				Arguments.of("convergenceDominantHeight", Float.TYPE, 42f),
				Arguments.of("convergenceBasalArea", Float.TYPE, 42f),
				Arguments.of("convergenceQuadraticMeanDiameter", Float.TYPE, 42f),
				Arguments.of("dominantHeightBackupFactor", Float.TYPE, 42f),
				Arguments.of("basalAreaBackupFactor", Float.TYPE, 42f),
				Arguments.of("quadMeanDiameterBackupFactor", Float.TYPE, 42f),
				Arguments.of("quadMeanDiameterBackupFactorMinimum", Float.TYPE, 42f)
		);
	}

	static List<Arguments> indexedAccessors() {
		return List.of(
				Arguments.of("speciesConvergenceLoreyHeight", 1),
				Arguments.of("speciesConvergenceQuadraticMeanDiameter", 1),
				Arguments.of("finalQuadraticMeanDiameter", 0), Arguments.of("speciesLoreyHeightBackupFactor", 1),
				Arguments.of("speciesQuadMeanDiameterBackupFactor", 1),
				Arguments.of("speciesQuadMeanDiameterBackupFactorMinimum", 1),
				Arguments.of("speciesLoreyHeightBackupFactorMaximum", 1)
		);
	}

	@ParameterizedTest
	@MethodSource("scalarAccessors")
	void testScalarAccessors(String property, Class<? extends Number> type, Number value) throws Exception {
		Map<String, Object> rawControlMap = TestUtils
				.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));
		var unit = new BackProcessingState(rawControlMap);

		var pd = new PropertyDescriptor(property, BackProcessingState.class);

		var boxedType = ConvertUtils.primitiveToWrapper(type);
		var writeMethod = pd.getWriteMethod();

		assertThat(unit, hasProperty(property, notPresent()));

		// Set the value

		writeMethod.invoke(unit, Optional.of(boxedType.cast(value)));

		assertThat(unit, hasProperty(property, present(is(boxedType.cast(value)))));

		// Set to empty
		writeMethod.invoke(unit, Optional.empty());

		assertThat(unit, hasProperty(property, notPresent()));

	}

	@Disabled("Not implemented")
	@ParameterizedTest
	@MethodSource("scalarAccessors")
	void testConveninenceScalarSetter(String property, Class<? extends Number> type, Number value) throws Exception {
		Map<String, Object> rawControlMap = TestUtils
				.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));
		var unit = new BackProcessingState(rawControlMap);

		var pd = new PropertyDescriptor(property, BackProcessingState.class);

		var writeMethod = pd.getWriteMethod();

		assertThat(unit, hasProperty(property, notPresent()));

		// Convenience setter without Optional

		writeMethod = BackProcessingState.class.getMethod(writeMethod.getName(), type);

		writeMethod.invoke(unit, type.cast(value));

		assertThat(unit, hasProperty(property, present(is(type.cast(value)))));

	}

	float readIndexed(Object obj, Method accessor, int index) throws Throwable {
		try {
			return (float) accessor.invoke(obj, index);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	@ParameterizedTest
	@MethodSource("indexedAccessors")
	void testScalarAccessors(String property, int from) throws Exception {
		Map<String, Object> rawControlMap = TestUtils
				.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));
		var unit = new BackProcessingState(rawControlMap);

		var upcaseProperty = property.substring(0, 1).toUpperCase(Locale.ENGLISH) + property.substring(1);

		var writeMethod = BackProcessingState.class.getMethod("set" + upcaseProperty, float[].class);

		var readMethod = BackProcessingState.class.getMethod("get" + upcaseProperty, int.class);

		assertThrows(IllegalStateException.class, () -> readIndexed(unit, readMethod, 1));

		writeMethod.invoke(unit, new float[] {});

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, -1));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 1));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 2));

		writeMethod.invoke(unit, new float[] { 100f });

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, -1));
		if (from <= 0) {
			assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 0, is(100f)));
		} else {
			assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 0));
		}
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 1));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 2));

		writeMethod.invoke(unit, new float[] { 101f, 111f });

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, -1));
		if (from <= 0) {
			assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 0, is(101f)));
		} else {
			assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 0));
		}
		assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 1, is(111f)));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 2));

		writeMethod.invoke(unit, new float[] { 102f, 112f, 122f });

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, -1));
		if (from <= 0) {
			assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 0, is(102f)));
		} else {
			assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 0));
		}
		assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 1, is(112f)));
		assertThat(unit, hasIndexedPropertyAt(readMethod.getName(), 2, is(122f)));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> readIndexed(unit, readMethod, 3));

	}

}
