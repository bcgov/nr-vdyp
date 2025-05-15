package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.StringSelection;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UnsupportedYieldTableRecordBeanTest {

	static Stream<String> properties() throws IntrospectionException {
		return Arrays.stream(Introspector.getBeanInfo(UnsupportedYieldTableRecordBean.class).getPropertyDescriptors())
				.map(pd -> pd.getName())
				.filter(name -> !name.equals("class"));
	}

	static Stream<Arguments> propertiesWithExamples() throws IntrospectionException {
		return Stream.of(
				Arguments.of("mode", "Test"),
				Arguments.of("cfsBiomassFoliage", (Double) 42.0),
				Arguments.of("cfsBiomassBranch", (Double) 42.0),
				Arguments.of("cfsBiomassBark", (Double) 42.0),
				Arguments.of("cfsBiomassStem", (Double) 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecayWastageBreakage", (Double) 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecayWastage", (Double) 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecay", (Double) 42.0),
				Arguments.of("moFBiomassCloseUtilizationVolume", (Double) 42.0),
				Arguments.of("moFBiomassWholeStemVolume", (Double) 42.0),
				Arguments.of("cuVolumeLessDecayWastageBreakage", (Double) 42.0),
				Arguments.of("cuVolumeLessDecayWastage", (Double) 42.0),
				Arguments.of("cuVolumeLessDecay", (Double) 42.0),
				Arguments.of("closeUtilizationVolume", (Double) 42.0),
				Arguments.of("wholeStemVolume", (Double) 42.0),
				Arguments.of("basalArea", (Double) 42.0),
				Arguments.of("treesPerHectare", (Double) 42.0),
				Arguments.of("diameter", (Double) 42.0),
				Arguments.of("loreyHeight", (Double) 42.0),
				Arguments.of("secondaryHeight", (Double) 42.0),
				Arguments.of("dominantHeight", (Double) 42.0),
				Arguments.of("siteIndex", (Double) 42.0),
				Arguments.of("percentStockable", (Double) 42.0),
				Arguments.of("totalAge", (Integer) 42),
				Arguments.of("projectionYear", (Integer) 42),
				Arguments.of("layerId", "Test"),
				Arguments.of("mapId", "Test"),
				Arguments.of("district", "Test"),
				Arguments.of("featureId", (Long) 42l)
		);
	}

	@ParameterizedTest
	@MethodSource("properties")
	void testGetters(String property) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		YieldTableRowBean b = new UnsupportedYieldTableRecordBean();
		var ex = assertThrows(InvocationTargetException.class, () -> BeanUtils.getSimpleProperty(b, property));
		assertThat(ex, causedBy(instanceOf(UnsupportedOperationException.class)));
	}

	@ParameterizedTest
	@MethodSource("propertiesWithExamples")
	void testSetters(String property, Object example) throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IntrospectionException {
		UnsupportedYieldTableRecordBean b = new UnsupportedYieldTableRecordBean();

		final Method setter = UnsupportedYieldTableRecordBean.class.getMethod(
				"set" + StringUtils.capitalise(property), example.getClass()
		);
		;
		var ex = assertThrows(
				InvocationTargetException.class, () -> setter.invoke(b, example)
		);
		assertThat(ex, causedBy(instanceOf(UnsupportedOperationException.class)));
	}

	@Test
	void testTableNumberSetter() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		YieldTableRowBean b = new UnsupportedYieldTableRecordBean();
		assertThrows(UnsupportedOperationException.class, () -> b.setTableNumber(42));
	}

	@Test
	void testPolygonIdSetter() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		YieldTableRowBean b = new UnsupportedYieldTableRecordBean();
		assertThrows(UnsupportedOperationException.class, () -> b.setPolygonId(42l));
	}

	@Test
	void testSpeciesFieldValueSetter() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		YieldTableRowBean b = new UnsupportedYieldTableRecordBean();
		assertThrows(UnsupportedOperationException.class, () -> b.setSpeciesFieldValue(null, 0, null, "Test"));
	}

	@Test
	void testSpeciesFieldValueGetter() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		YieldTableRowBean b = new UnsupportedYieldTableRecordBean();
		assertThrows(UnsupportedOperationException.class, () -> b.getSpeciesFieldValue(null, 0, null));
	}

}
