package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractYieldTableRecordBeanTest {

	abstract YieldTableRowBean createInstance();

	static Stream<Arguments> propertiesWithExamples() throws IntrospectionException {
		return Stream.of(
				Arguments.of("mode", "Test"), Arguments.of("polygonId", 1029301L),
				Arguments.of("cfsBiomassFoliage", 42.0), Arguments.of("cfsBiomassStem", 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecayWastageBreakage", 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecayWastage", 42.0),
				Arguments.of("moFBiomassCuVolumeLessDecay", 42.0), Arguments.of("moFBiomassWholeStemVolume", 42.0),
				Arguments.of("cuVolumeLessDecayWastageBreakage", 42.0), Arguments.of("cuVolumeLessDecayWastage", 42.0),
				Arguments.of("cuVolumeLessDecay", 42.0), Arguments.of("closeUtilizationVolume", 42.0),
				Arguments.of("wholeStemVolume", 42.0), Arguments.of("basalArea", 42.0),
				Arguments.of("treesPerHectare", 42.0), Arguments.of("diameter", 42.0),
				Arguments.of("loreyHeight", 42.0), Arguments.of("secondaryHeight", 42.0),
				Arguments.of("dominantHeight", 42.0), Arguments.of("siteIndex", 42.0),
				Arguments.of("percentStockable", 42.0), Arguments.of("totalAge", 42),
				Arguments.of("projectionYear", 42), Arguments.of("layerId", "Test"), Arguments.of("mapId", "Test"),
				Arguments.of("district", "Test"), Arguments.of("featureId", 42L)
		);
	}

	@ParameterizedTest
	@MethodSource("propertiesWithExamples")
	void testLoadingSingleValues(String property, Object example) throws Exception {
		YieldTableRowBean bean = createInstance();
		final Method setter = bean.getClass().getMethod("set" + StringUtils.capitalise(property), example.getClass());
		setter.invoke(bean, example);
		final Method getter = bean.getClass().getMethod("get" + StringUtils.capitalise(property));
		Object value = getter.invoke(bean);

		switch (example.getClass().toString()) {
		case "Integer" -> assertThat(value, is(FieldFormatter.format((Integer) example)));
		case "Double" -> assertThat(value, is(FieldFormatter.format((Double) example)));
		case "Long" -> assertThat(value, is(FieldFormatter.format((Long) example)));
		case "String" -> assertThat(value, is(FieldFormatter.format((String) example)));
		}
	}

	@Test
	void testTableNumber() {
		YieldTableRowBean bean = createInstance();
		bean.setTableNumber(4);
		assertThat(bean.getTableNumber(), is("4"));
	}

	@Test
	void testLoading() {
		YieldTableRowBean b = createInstance();

		double nextSetValue = 0.0;
		String nextCodeValue = "a";
		for (var p : YieldTableRowBean.MultiFieldPrefixes.values()) {
			for (var s : YieldTableRowBean.MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					switch (s) {
					case Code:
						for (var i = 1; i <= 6; i++) {
							b.setSpeciesFieldValue(p, i, s, nextCodeValue);
							nextCodeValue = new String(new byte[] { (byte) (nextCodeValue.charAt(0) + 1) });
						}
						break;
					default:
						for (var i = 1; i <= 6; i++) {
							b.setSpeciesFieldValue(p, i, s, nextSetValue);
							nextSetValue += 1;
						}
						break;
					}
				} else {
					assertThrows(IllegalArgumentException.class, () -> b.setSpeciesFieldValue(p, 1, s, ""));
				}
			}
		}

		double nextGetDoubleValue = 0;
		String nextGetStringValue = "a";
		for (var p : YieldTableRowBean.MultiFieldPrefixes.values()) {
			for (var s : YieldTableRowBean.MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					switch (s) {
					case Code:
						for (var i = 1; i <= 6; i++) {
							assertThat(b.getSpeciesFieldValue(p, i, s), is(nextGetStringValue));
							nextGetStringValue = new String(new byte[] { (byte) (nextGetStringValue.charAt(0) + 1) });
						}
						break;
					default:
						for (var i = 1; i <= 6; i++) {
							String expectedValue = FieldFormatter.format(nextGetDoubleValue);
							assertEquals(expectedValue, b.getSpeciesFieldValue(p, i, s));
							nextGetDoubleValue += 1;
						}
						break;
					}
				} else {
					assertThrows(IllegalArgumentException.class, () -> b.getSpeciesFieldValue(p, 1, s));
				}
			}
		}
	}
}
