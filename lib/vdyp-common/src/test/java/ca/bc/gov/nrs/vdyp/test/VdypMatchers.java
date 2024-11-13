package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.model.builders.ModelClassBuilder;

/**
 * Custom Hamcrest Matchers
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VdypMatchers {

	static final float EPSILON = 0.001f;

	private static float currentEpsilon = EPSILON;

	/**
	 * Change the <code>closeTo</code> tolerance. Recommended usage:
	 *
	 * <pre>
	 * float originalValue = VdypMatchers.setEpsilon(... new value ...);
	 * try {
	 *     ... closeTo invocations use the new value ...
	 * } finally {
	 *     VdypMatchers.setEpsilon(originalValue);
	 * }
	 * </pre>
	 *
	 * @param newValue the new tolerance value
	 * @return the tolerance value at the time this method was called
	 */
	public static float setEpsilon(float newValue) {
		var originalValue = currentEpsilon;
		currentEpsilon = newValue;
		return originalValue;
	}

	/**
	 * Matches a string if when parsed by the parser method it matches the given matcher
	 *
	 * @param parsedMatcher matcher for the parsed value
	 * @param parser        parser
	 * @return
	 */
	public static <T> Matcher<String> parseAs(Matcher<? super T> parsedMatcher, ValueParser<T> parser) {
		return new BaseMatcher<String>() {

			@Override
			public boolean matches(Object actual) {
				try {
					return parsedMatcher.matches(parser.parse(actual.toString()));
				} catch (ValueParseException e) {
					return false;
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("parses as ");
				parsedMatcher.describeTo(description);
			}

		};
	}

	/**
	 * Matcher for the cause of an exception
	 *
	 * @param causeMatcher
	 * @return
	 */
	public static Matcher<Throwable> causedBy(Matcher<? extends Throwable> causeMatcher) {

		return new BaseMatcher<Throwable>() {

			@Override
			public boolean matches(Object actual) {
				return causeMatcher.matches( ((Throwable) actual).getCause());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("was caused by exception that ").appendDescriptionOf(causeMatcher);
			}

		};
	}

	public static <T> Matcher<Optional<T>> present() {
		return present(anything());
	}

	/**
	 * Matches an Optional if it is present and its value matches the given matcher
	 *
	 * @param delegate matcher for the optional's value
	 * @return
	 */
	public static <T> Matcher<Optional<T>> present(Matcher<? super T> delegate) {
		return new BaseMatcher<Optional<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean matches(Object actual) {
				if (! (actual instanceof Optional)) {
					return false;
				}
				if (! ((Optional<?>) actual).isPresent()) {
					return false;
				}
				return delegate.matches( ((Optional<T>) actual).get());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Optional that ");
				delegate.describeTo(description);
			}

			@Override
			public void describeMismatch(Object item, Description description) {
				if (! (item instanceof Optional)) {
					description.appendText("Not an Optional");
					return;
				}
				if (! ((Optional<?>) item).isPresent()) {
					description.appendText("Not present");
					return;
				}
				delegate.describeMismatch(item, description);
			}

		};
	}

	/**
	 * Matches an Optional if it is not present
	 *
	 * @param <T>
	 *
	 * @return
	 */
	public static <T> Matcher<Optional<T>> notPresent() {
		return new BaseMatcher<Optional<T>>() {

			@Override
			public boolean matches(Object actual) {
				if (! (actual instanceof Optional)) {
					return false;
				}
				return ! ((Optional<?>) actual).isPresent();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Optional that is empty");
			}

			@Override
			public void describeMismatch(Object item, Description description) {
				if (item == null) {
					description.appendText("was null");
					return;
				}
				if (! (item instanceof Optional)) {
					description.appendText("was not an Optional");
					return;
				}
				if ( ((Optional<?>) item).isPresent()) {
					description.appendText("had value ").appendValue( ((Optional<?>) item).get());
					return;
				}
			}

		};
	}

	public static <T> Matcher<MatrixMap<T>> mmHasEntry(Matcher<T> valueMatcher, Object... keys) {
		return new BaseMatcher<MatrixMap<T>>() {

			@Override
			public boolean matches(Object actual) {
				if (! (actual instanceof MatrixMap)) {
					return false;
				}
				return valueMatcher.matches( ((MatrixMap<?>) actual).getM(keys));
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Matrix map with entry ").appendValueList("[", ", ", "]", keys)
						.appendText(" that ");
				valueMatcher.describeTo(description);

			}

			@Override
			public void describeMismatch(Object item, Description description) {
				if (! (item instanceof MatrixMap)) {
					description.appendText("was not a MatrixMap");
					return;
				}
				// TODO give better feedback if keys don't match the map
				var value = ((MatrixMap<?>) item).getM(keys);

				description.appendText("entry ").appendValueList("[", ", ", "]", keys).appendText(" ");
				valueMatcher.describeMismatch(value, description);
			}

		};

	}

	public static <T> Matcher<MatrixMap<Optional<T>>> mmEmpty() {
		return mmAll(notPresent());
	}

	/**
	 * Match a MatrixMap if all of its values match the given matcher
	 *
	 * @param <T>
	 * @param valueMatcher
	 * @return
	 */
	public static <T> Matcher<MatrixMap<T>> mmAll(Matcher<T> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<MatrixMap<T>>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("MatrixMap with all values ").appendDescriptionOf(valueMatcher);
			}

			@Override
			protected boolean matchesSafely(MatrixMap<T> item, Description mismatchDescription) {
				if (item.all(valueMatcher::matches)) {
					return true;
				}
				// TODO This could stand to be more specific
				mismatchDescription.appendText("Not all values were ").appendDescriptionOf(valueMatcher);
				return false;
			}

		};
	}

	/**
	 * Match a MatrixMap if its dimensions match the given matchers
	 *
	 * @param <T>
	 * @param <T>
	 * @param valueMatcher
	 * @return
	 */
	@SafeVarargs
	public static <T> Matcher<MatrixMap<T>> mmDimensions(Matcher<? super Set<?>>... dimensionMatchers) {
		return new TypeSafeDiagnosingMatcher<MatrixMap<T>>() {

			@Override
			public void describeTo(Description description) {
				description.appendList("MatrixMap with dimensions that ", ", ", "", Arrays.asList(dimensionMatchers));
			}

			@Override
			protected boolean matchesSafely(MatrixMap<T> item, Description mismatchDescription) {
				var dimensions = item.getDimensions();
				if (dimensionMatchers.length != dimensions.size()) {
					mismatchDescription.appendText("Expected ").appendValue(dimensionMatchers.length)
							.appendText(" dimensions but had ").appendValue(dimensions.size());
					return false;
				}
				var result = true;
				for (int i = 0; i < dimensionMatchers.length; i++) {
					if (!dimensionMatchers[i].matches(dimensions.get(i))) {
						if (!result) {
							mismatchDescription.appendText(", ");
						}
						result = false;
						mismatchDescription.appendText("dimension ").appendValue(i).appendText(" ");
						dimensionMatchers[i].describeMismatch(dimensions.get(i), mismatchDescription);
					}
				}

				return result;
			}

		};
	}

	/**
	 * Equivalent to {@link Matchers.hasEntry} with a simple equality check on the key. Does not show the full map
	 * contents on a mismatch, just the requested entry if it's present.
	 */
	public static <K, V> Matcher<Map<K, V>> hasSpecificEntry(K key, Matcher<V> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Map<K, V>>() {

			@Override
			protected boolean matchesSafely(Map<K, V> map, Description mismatchDescription) {
				V result = map.get(key);
				if (Objects.isNull(result)) {
					mismatchDescription.appendText("entry for ").appendValue(key).appendText(" was not present");
					return false;
				}
				if (!valueMatcher.matches(result)) {
					mismatchDescription.appendText("entry for ").appendValue(key).appendText(" was present but ");
					valueMatcher.describeMismatch(result, mismatchDescription);
					return false;
				}

				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("A map with an entry for ").appendValue(key).appendText(" that ")
						.appendDescriptionOf(valueMatcher);
			}

		};

	}

	public static <V> Matcher<Map<String, V>> controlMapHasEntry(ControlKey key, Matcher<V> valueMatcher) {
		return hasSpecificEntry(key.name(), valueMatcher);
	}

	/**
	 * Matches a BecLookup that contains a bec with the specified alias that matches the given matcher.
	 */
	public static Matcher<BecLookup> hasBec(String alias, Matcher<Optional<BecDefinition>> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<BecLookup>() {

			@Override
			protected boolean matchesSafely(BecLookup map, Description mismatchDescription) {
				var result = map.get(alias);
				if (Objects.isNull(result)) {
					mismatchDescription.appendText("entry for ").appendValue(alias).appendText(" was not present");
					return false;
				}
				if (!valueMatcher.matches(result)) {
					mismatchDescription.appendText("entry for ").appendValue(alias).appendText(" was present but ");
					valueMatcher.describeMismatch(result, mismatchDescription);
					return false;
				}

				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("A BEC Lookup with an entry for ").appendValue(alias).appendText(" that ")
						.appendDescriptionOf(valueMatcher);
			}

		};

	}

	/**
	 * Matches a ValueOrMarker with a value
	 */
	public static <V> Matcher<ValueOrMarker<V, ?>> isValue(Matcher<V> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<ValueOrMarker<V, ?>>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("ValueOrMarker with a value ").appendDescriptionOf(valueMatcher);

			}

			@Override
			protected boolean matchesSafely(ValueOrMarker<V, ?> item, Description mismatchDescription) {
				if (item.isMarker()) {
					mismatchDescription.appendText("isMarker() was true");
					return false;
				}
				if (!item.isValue()) {
					mismatchDescription.appendText("isValue() was false");
					return false;
				}
				if (item.getMarker().isPresent()) {
					mismatchDescription.appendText("getMarker() was present with value ")
							.appendValue(item.getMarker().get());
					return false;
				}
				if (!item.getValue().isPresent()) {
					mismatchDescription.appendText("getValue() was not present");
					return false;
				}
				if (valueMatcher.matches(item.getValue().get())) {
					return true;
				}
				mismatchDescription.appendText("Value was present but ");
				valueMatcher.describeMismatch(item.getValue().get(), mismatchDescription);
				return false;
			}
		};
	}

	/**
	 * Matches a ValueOrMarker with a marker
	 */
	public static <M> Matcher<ValueOrMarker<?, M>> isMarker(Matcher<M> markerMatcher) {
		return new TypeSafeDiagnosingMatcher<ValueOrMarker<?, M>>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("ValueOrMarker with a value ").appendDescriptionOf(markerMatcher);

			}

			@Override
			protected boolean matchesSafely(ValueOrMarker<?, M> item, Description mismatchDescription) {
				if (item.isValue()) {
					mismatchDescription.appendText("isValue() was true");
					return false;
				}
				if (!item.isMarker()) {
					mismatchDescription.appendText("isMarker() was false");
					return false;
				}
				if (item.getValue().isPresent()) {
					mismatchDescription.appendText("getValue() was present with value ")
							.appendValue(item.getValue().get());
					return false;
				}
				if (!item.getMarker().isPresent()) {
					mismatchDescription.appendText("getMarker() was not present");
					return false;
				}
				if (markerMatcher.matches(item.getMarker().get())) {
					return true;
				}
				mismatchDescription.appendText("Marker was present but ");
				markerMatcher.describeMismatch(item.getMarker().get(), mismatchDescription);
				return false;
			}
		};
	}

	public static <T> T assertNext(StreamingParser<T> stream) throws IOException, ResourceParseException {
		var hasNext = assertDoesNotThrow(() -> stream.hasNext());
		assertThat(hasNext, is(true));
		var next = assertDoesNotThrow(() -> stream.next());
		assertThat(next, notNullValue());
		return next;
	}

	public static <T> void assertEmpty(StreamingParser<T> stream) throws IOException, ResourceParseException {
		var hasNext = assertDoesNotThrow(() -> stream.hasNext());
		assertThat(hasNext, is(false));
		assertThrows(NoSuchElementException.class, () -> stream.next());
	}

	public static Matcher<Coefficients> coe(int indexFrom, Matcher<? super List<Float>> contentsMatcher) {
		return describedAs(
				"A Coefficients indexed from %0 that %1", //
				allOf(
						isA(Coefficients.class), //
						hasProperty("indexFrom", is(indexFrom)), //
						contentsMatcher
				), //
				indexFrom, //
				contentsMatcher
		);
	}

	@SafeVarargs
	public static Matcher<Coefficients> coe(int indexFrom, Matcher<Float>... contentsMatchers) {
		return coe(indexFrom, contains(contentsMatchers));
	}

	public static Matcher<Coefficients>
			coe(int indexFrom, Function<Float, Matcher<? super Float>> matcherGenerator, Float... contents) {
		List<Matcher<? super Float>> contentsMatchers = Arrays.stream(contents).map(matcherGenerator).toList();
		return coe(indexFrom, contains(contentsMatchers));
	}

	public static Matcher<Coefficients> coe(int indexFrom, Float... contents) {
		return coe(indexFrom, VdypMatchers::closeTo, contents);
	}

	public static Matcher<Float> asFloat(Matcher<Double> doubleMatcher) {
		return new TypeSafeDiagnosingMatcher<Float>() {

			@Override
			public void describeTo(Description description) {
				doubleMatcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(Float item, Description mismatchDescription) {
				if (!doubleMatcher.matches((double) item)) {
					doubleMatcher.describeMismatch(item, mismatchDescription);
					return false;
				}
				return true;
			}

		};
	}

	public static Matcher<Float> closeTo(float expected) {
		return closeTo(expected, currentEpsilon);
	}

	public static Matcher<Float> closeTo(float expected, float threshold) {
		float epsilon = Float.max(threshold * FloatMath.abs(expected), Float.MIN_VALUE);
		return asFloat(Matchers.closeTo(expected, epsilon));
	}

	public static Matcher<String> hasLines(String... expectedLines) {
		return hasLines(Matchers.contains(expectedLines));
	}

	public static Matcher<String> hasLines(Matcher<Iterable<? extends String>> lineMatcher) {
		return new TypeSafeDiagnosingMatcher<String>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("A string with lines that ");
				lineMatcher.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				var lines = List.of(item.split("\n"));

				if (lineMatcher.matches(lines)) {
					return true;
				} else {
					lineMatcher.describeMismatch(lines, mismatchDescription);
					return false;
				}
			}

		};

	}

	public static <T extends ModelClassBuilder<U>, U> Matcher<T> builds(Matcher<U> builtMatcher) {
		return new TypeSafeDiagnosingMatcher<T>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("is a ModelClassBuilder which builds and object that ");
			}

			@Override
			protected boolean matchesSafely(T item, Description mismatchDescription) {
				var result = item.build();
				if (builtMatcher.matches(result)) {
					return true;
				}
				mismatchDescription.appendText("built object was ");
				builtMatcher.describeMismatch(result, mismatchDescription);
				return false;
			}

		};
	}

	public static Matcher<PolygonIdentifier> isPolyId(String base, int year) {
		return allOf(instanceOf(PolygonIdentifier.class), hasProperty("base", is(base)), hasProperty("year", is(year)));
	}

	public static Matcher<BecDefinition> isBec(String alias) {
		return allOf(instanceOf(BecDefinition.class), hasProperty("alias", is(alias)));
	}

	public static Matcher<Coefficients>
			utilization(Coefficients expected) {
		if (expected.size() == 2) {
			return utilizationHeight(expected.get(0), expected.get(1));
		}
		return utilization(
				expected.get(0), expected.get(1), expected.get(2), expected.get(3), expected.get(4), expected.get(5)
		);
	}

	public static Matcher<Coefficients>
			utilization(float small, float all, float util1, float util2, float util3, float util4) {
		return new TypeSafeDiagnosingMatcher<Coefficients>() {

			boolean matchesComponent(Description description, float expected, float result) {
				boolean matches = closeTo(expected).matches(result);
				description.appendText(String.format(matches ? "%f" : "[[%f]]", result));
				return matches;
			}

			@Override
			public void describeTo(Description description) {
				String utilizationRep = String.format(
						"[Small: %f, All: %f, 7.5cm: %f, 12.5cm: %f, 17.5cm: %f, 22.5cm: %f]", small, all, util1, util2,
						util3, util4
				);
				description.appendText("A utilization vector ").appendValue(utilizationRep);
			}

			@Override
			protected boolean matchesSafely(Coefficients item, Description mismatchDescription) {
				if (item.size() != 6 || item.getIndexFrom() != -1) {
					mismatchDescription.appendText("Was not a utilization vector");
					return false;
				}
				boolean matches = true;
				mismatchDescription.appendText("Was [Small: ");
				matches &= matchesComponent(mismatchDescription, small, item.getCoe(UtilizationClass.SMALL.index));
				mismatchDescription.appendText(", All: ");
				matches &= matchesComponent(mismatchDescription, all, item.getCoe(UtilizationClass.ALL.index));
				mismatchDescription.appendText(", 7.5cm: ");
				matches &= matchesComponent(mismatchDescription, util1, item.getCoe(UtilizationClass.U75TO125.index));
				mismatchDescription.appendText(", 12.5cm: ");
				matches &= matchesComponent(mismatchDescription, util2, item.getCoe(UtilizationClass.U125TO175.index));
				mismatchDescription.appendText(", 17.5cm: ");
				matches &= matchesComponent(mismatchDescription, util3, item.getCoe(UtilizationClass.U175TO225.index));
				mismatchDescription.appendText(", 22.5cm: ");
				matches &= matchesComponent(mismatchDescription, util4, item.getCoe(UtilizationClass.OVER225.index));
				mismatchDescription.appendText("]");
				return matches;
			}

		};
	}

	public static Matcher<Coefficients> utilizationAllAndBiggest(float all) {
		return utilization(0f, all, 0f, 0f, 0f, all);
	}

	public static Matcher<Coefficients> utilizationHeight(float small, float all) {
		return new TypeSafeDiagnosingMatcher<Coefficients>() {

			boolean matchesComponent(Description description, float expected, float result) {
				boolean matches = closeTo(expected).matches(result);
				description.appendText(String.format(matches ? "%f" : "[[%f]]", result));
				return matches;
			}

			@Override
			public void describeTo(Description description) {
				String utilizationRep = String.format("[Small: %f, All: %f]", small, all);
				description.appendText("A lorey height vector ").appendValue(utilizationRep);
			}

			@Override
			protected boolean matchesSafely(Coefficients item, Description mismatchDescription) {
				if (item.size() != 2 || item.getIndexFrom() != -1) {
					mismatchDescription.appendText("Was not a lorey height vector");
					return false;
				}
				boolean matches = true;
				mismatchDescription.appendText("Was [Small: ");
				matches &= matchesComponent(mismatchDescription, small, item.getCoe(UtilizationClass.SMALL.index));
				mismatchDescription.appendText(", All: ");
				matches &= matchesComponent(mismatchDescription, all, item.getCoe(UtilizationClass.ALL.index));
				mismatchDescription.appendText("]");
				return matches;
			}

		};
	}

	public static <T> Matcher<T>
			compatibilityVariable(String name, Matcher<Float> expected, Class<T> klazz, Object... params) {
		return new TypeSafeDiagnosingMatcher<>(klazz) {

			@Override
			public void describeTo(Description description) {
				description.appendText(name).appendValueList("(", ", ", ") ", params);
				description.appendDescriptionOf(expected);
			}

			@Override
			protected boolean matchesSafely(T item, Description mismatchDescription) {
				Method method;
				try {
					method = klazz.getMethod(
							name,
							(Class<?>[]) Arrays.stream(params)
									.map(o -> o instanceof Integer ? Integer.TYPE : o.getClass()).toArray(Class[]::new)
					);
				} catch (NoSuchMethodException e) {
					mismatchDescription.appendText("Method " + name + " does not exist");
					return false;
				}

				float result;
				try {
					result = (float) method.invoke(item, params);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					mismatchDescription.appendText(e.getMessage());
					return false;
				}

				if (expected.matches(result)) {
					return true;
				}

				expected.describeMismatch(result, mismatchDescription);
				return false;
			}

		};
	}

	static private boolean sep(boolean match, Description mismatchDescription) {
		if (match) {
			mismatchDescription.appendText(", ");
		}
		return false;
	}

	static private <T> boolean matchValue(
			boolean match, String name, T value, T expected, Description mismatchDescription
	) {
		if (!expected.equals(value)) {
			match = sep(match, mismatchDescription);
			mismatchDescription
					.appendText(name)
					.appendText(" was ")
					.appendValue(value)
					.appendText(" but expected ")
					.appendValue(expected);
		}
		return match;
	}

	static private boolean matchValue(
			boolean match, String name, UtilizationVector value, UtilizationVector expected,
			Description mismatchDescription
	) {
		return matchValue(match, name, value, utilization(expected), mismatchDescription);
	}

	static private <T> boolean matchValue(
			boolean match, String name, T value, Matcher<T> expected, Description mismatchDescription
	) {
		if (!expected.matches(value)) {
			match = sep(match, mismatchDescription);
			mismatchDescription
					.appendText(name)
					.appendText(" was ");
			expected.describeMismatch(expected, mismatchDescription);
		}
		return match;
	}

	Matcher<VdypPolygon> deepEquals(final VdypPolygon expected) {
		return new TypeSafeDiagnosingMatcher<VdypPolygon>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("matches VDYPPolygon ").appendValue(expected.getPolygonIdentifier().toString());
			}

			@Override
			protected boolean matchesSafely(VdypPolygon item, Description mismatchDescription) {
				boolean match = true;

				match = matchValue(
						match, "PolygonIdentifier",
						expected.getPolygonIdentifier(), item.getPolygonIdentifier(),
						mismatchDescription
				);

				match = matchValue(
						match, "BiogeoclimaticZone",
						expected.getBiogeoclimaticZone(), item.getBiogeoclimaticZone(),
						mismatchDescription
				);

				match = matchValue(
						match, "ForestInventoryZone",
						expected.getForestInventoryZone(), item.getForestInventoryZone(),
						mismatchDescription
				);

				match = matchValue(
						match, "InventoryTypeGroup",
						expected.getInventoryTypeGroup(), item.getInventoryTypeGroup(),
						mismatchDescription
				);

				match = matchValue(
						match, "Mode",
						expected.getMode(), item.getMode(),
						mismatchDescription
				);

				match = matchValue(
						match, "PercentAvailable",
						expected.getPercentAvailable(), item.getPercentAvailable(),
						mismatchDescription
				);

				match = matchValue(
						match, "TargetYear",
						expected.getTargetYear(), item.getTargetYear(),
						mismatchDescription
				);

				match = matchValue(
						match, "Layers",
						expected.getLayers().keySet(), item.getLayers().keySet(),
						mismatchDescription
				);

				for (var layerType : expected.getLayers().keySet()) {
					if (item.getLayers().keySet().contains(layerType)) {
						var itemLayer = item.getLayers().get(layerType);
						var expectedLayer = expected.getLayers().get(layerType);
						var layerMatcher = deepEquals(expectedLayer);

						if (!layerMatcher.matches(itemLayer)) {
							match = sep(match, mismatchDescription);
							mismatchDescription.appendText("mismatch in layer ");
							mismatchDescription.appendValue(layerType);
							mismatchDescription.appendText(": ");
							layerMatcher.describeMismatch(itemLayer, mismatchDescription);
						}
					}
				}

				return match;
			}

		};
	}

	public static Matcher<VdypLayer> deepEquals(VdypLayer expected) {
		return new TypeSafeDiagnosingMatcher<VdypLayer>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("matches VDYPLayer ").appendValue(expected.getPolygonIdentifier()).appendValue(
						expected.getLayerType()
				);
			}

			@Override
			protected boolean matchesSafely(VdypLayer item, Description mismatchDescription) {
				boolean match = true;

				match = matchValue(
						match, "PolygonIdentifier",
						expected.getPolygonIdentifier(), item.getPolygonIdentifier(),
						mismatchDescription
				);
				match = matchValue(
						match, "LayerType",
						expected.getLayerType(), item.getLayerType(),
						mismatchDescription
				);

				match = matchValue(
						match, "InventoryTypeGroup",
						expected.getInventoryTypeGroup(), item.getInventoryTypeGroup(),
						mismatchDescription
				);
				match = matchValue(
						match, "PrimaryGenus",
						expected.getPrimaryGenus(), item.getPrimaryGenus(),
						mismatchDescription
				);
				match = matchValue(
						match, "EmpiricalRelationshipParameterIndex",
						expected.getEmpiricalRelationshipParameterIndex(), item
								.getEmpiricalRelationshipParameterIndex(),
						mismatchDescription
				);

				match = matchValue(
						match, "Species Groups",
						expected.getSpecies().keySet(), item.getSpecies().keySet(),
						mismatchDescription
				);

				for (var speciesGroupId : expected.getSpecies().keySet()) {
					if (item.getSpecies().keySet().contains(speciesGroupId)) {
						var itemSpecGroup = item.getSpecies().get(speciesGroupId);
						var expectedSpecGroup = expected.getSpecies().get(speciesGroupId);
						var specGroupMatcher = deepEquals(expectedSpecGroup);

						if (!specGroupMatcher.matches(itemSpecGroup)) {
							match = sep(match, mismatchDescription);
							mismatchDescription.appendText("mismatch in species group ");
							mismatchDescription.appendValue(speciesGroupId);
							mismatchDescription.appendText(": ");
							specGroupMatcher.describeMismatch(itemSpecGroup, mismatchDescription);
						}
					}
				}

				match = matchValue(
						match, "getLoreyHeightByUtilization",
						expected.getLoreyHeightByUtilization(),
						item.getLoreyHeightByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getBaseAreaByUtilization",
						expected.getBaseAreaByUtilization(),
						item.getBaseAreaByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getQuadraticMeanDiameterByUtilization",
						expected.getQuadraticMeanDiameterByUtilization(),
						item.getQuadraticMeanDiameterByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getTreesPerHectareByUtilization",
						expected.getTreesPerHectareByUtilization(),
						item.getTreesPerHectareByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getWholeStemVolumeByUtilization",
						expected.getWholeStemVolumeByUtilization(),
						item.getWholeStemVolumeByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getCloseUtilizationVolumeByUtilization",
						expected.getCloseUtilizationVolumeByUtilization(),
						item.getCloseUtilizationVolumeByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getCloseUtilizationVolumeNetOfDecayByUtilization",
						expected.getCloseUtilizationVolumeNetOfDecayByUtilization(),
						item.getCloseUtilizationVolumeNetOfDecayByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization",
						expected.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
						item.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
						mismatchDescription
				);
				match = matchValue(
						match, "getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization",
						expected.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(),
						item.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(),
						mismatchDescription
				);

				return match;
			}

		};
	}

	public static Matcher<VdypSpecies> deepEquals(VdypSpecies expected) {
		return new TypeSafeDiagnosingMatcher<VdypSpecies>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("matches VDYPSpecies ")
						.appendValue(expected.getPolygonIdentifier())
						.appendValue(expected.getLayerType())
						.appendValue(expected.getGenus());
			}

			@Override
			protected boolean matchesSafely(VdypSpecies item, Description mismatchDescription) {
				boolean match = true;

				match = matchValue(
						match, "PolygonIdentifier",
						expected.getPolygonIdentifier(),
						item.getPolygonIdentifier(),
						mismatchDescription
				);
				match = matchValue(
						match, "LayerType",
						expected.getLayerType(),
						item.getLayerType(),
						mismatchDescription
				);
				match = matchValue(
						match, "Genus",
						expected.getGenus(),
						item.getGenus(),
						mismatchDescription
				);
				match = matchValue(
						match, "GenusIndex",
						expected.getGenusIndex(),
						item.getGenusIndex(),
						mismatchDescription
				);

				match = matchValue(
						match, "BreakageGroup",
						expected.getBreakageGroup(),
						item.getBreakageGroup(),
						mismatchDescription
				);
				match = matchValue(
						match, "VolumeGroup",
						expected.getVolumeGroup(),
						item.getVolumeGroup(),
						mismatchDescription
				);
				match = matchValue(
						match, "DecayGroup",
						expected.getDecayGroup(),
						item.getDecayGroup(),
						mismatchDescription
				);

				match = matchValue(
						match, "PercentGenus",
						expected.getPercentGenus(),
						item.getPercentGenus(),
						mismatchDescription
				);
				match = matchValue(
						match, "FractionGenus",
						expected.getFractionGenus(),
						item.getFractionGenus(),
						mismatchDescription
				);

				if (expected.getSite().isEmpty() && item.getSite().isPresent()) {
					match = sep(match, mismatchDescription);
					mismatchDescription.appendText("expected not to have a Site but one was present");
				} else if (expected.getSite().isPresent() && item.getSite().isEmpty()) {
					match = sep(match, mismatchDescription);
					mismatchDescription.appendText("expected to have a Site but none was present");
				}
				match = Utils.flatMapBoth(expected.getSite(), item.getSite(), (expectedSite, itemSite) -> {
					var siteMatcher = deepEquals(expectedSite);
					if (!siteMatcher.matches(itemSite)) {
						mismatchDescription.appendText("mismatch in site ");
						mismatchDescription.appendValue(expected.getGenus());
						mismatchDescription.appendText(": ");
						siteMatcher.describeMismatch(itemSite, mismatchDescription);
						return Optional.of(false);
					}
					return Optional.empty();
				}).orElse(match);

				if (expected.getSite().isEmpty() && item.getSite().isPresent()) {
					match = sep(match, mismatchDescription);
					mismatchDescription.appendText(
							"expected not to have Compatibility Variables but they were present"
					);
				} else if (expected.getSite().isPresent() && item.getSite().isEmpty()) {
					match = sep(match, mismatchDescription);
					mismatchDescription.appendText("expected to have Compatibility Variables but none were present");
				}

				Utils.flatMapBoth(
						expected.getCompatibilityVariables(), item.getCompatibilityVariables(), (
								expectedCv, itemCv
						) -> {
							var cvMatcher = deepEquals(expectedCv);

							if (!cvMatcher.matches(itemCv)) {
								mismatchDescription.appendText("mismatch in Compatibility Variables ");
								mismatchDescription.appendValue(expected.getGenus());
								mismatchDescription.appendText(": ");
								cvMatcher.describeMismatch(itemCv, mismatchDescription);
								return Optional.of(false);
							}
							return Optional.empty();

						}
				);
				expected.getSp64DistributionSet();

				return match;

			}

		};
	}

	public static Matcher<VdypCompatibilityVariables> deepEquals(VdypCompatibilityVariables expected) {
		return new TypeSafeDiagnosingMatcher<VdypCompatibilityVariables>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("Matches given VDYPCompatibilityVariables");
			}

			@Override
			protected boolean matchesSafely(VdypCompatibilityVariables item, Description mismatchDescription) {
				boolean match = true;

				match = matchValue(
						match, "CvBasalArea",
						item.getCvVolume(),
						mmEquals(expected.getCvVolume(), VdypMatchers::closeTo),
						mismatchDescription
				);
				match = matchValue(
						match, "CvBasalArea",
						item.getCvBasalArea(),
						mmEquals(expected.getCvBasalArea(), VdypMatchers::closeTo),
						mismatchDescription
				);
				match = matchValue(
						match, "CvBasalArea",
						item.getCvQuadraticMeanDiameter(),
						mmEquals(expected.getCvQuadraticMeanDiameter(), VdypMatchers::closeTo),
						mismatchDescription
				);
				match = matchValue(
						match, "CvBasalArea",
						item.getCvPrimaryLayerSmall(),
						mapEquals(expected.getCvPrimaryLayerSmall(), VdypMatchers::closeTo),
						mismatchDescription
				);

				return match;
			}
		};
	}

	public static <K, V> Matcher<Map<? extends K, ? extends V>> mapEquals(
			Map<K, V> expected, Function<V, Matcher<V>> valueMatcherGenerator
	) {
		if (expected.isEmpty()) {
			return Matchers.anEmptyMap();
		}
		return new TypeSafeDiagnosingMatcher<Map<? extends K, ? extends V>>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("map with contents: ").appendValue(expected);

			}

			@Override
			protected boolean matchesSafely(
					Map<? extends K, ? extends V> item, Description mismatchDescription
			) {
				if (item.isEmpty()) {
					mismatchDescription.appendText("map was empty");
					return false;
				}
				if (!expected.keySet().equals(item.keySet())) {
					mismatchDescription.appendText("expected keys ").appendValue(expected.keySet()).appendText(
							" but were "
					).appendValue(item.keySet());
					return false;
				}
				List<String> failures = new LinkedList<>();
				for (var key : expected.keySet()) {
					final V expectedValue = expected.get(key);
					var valueMatcher = valueMatcherGenerator.apply(expectedValue);
					V actualValue = item.get(key);
					if (!valueMatcher.matches(item.get(key))) {
						var failureDescription = new StringDescription();
						failureDescription.appendText("at ");
						failureDescription.appendValue(key);
						failureDescription.appendText(" expected ");
						failureDescription.appendValue(expectedValue);
						failureDescription.appendText(" but it ");
						valueMatcher.describeMismatch(actualValue, failureDescription);
						failures.add(failureDescription.toString());
					}
				}

				if (!failures.isEmpty()) {
					var first = failures.iterator().next();
					mismatchDescription.appendText(first);

					if (failures.size() > 1) {
						mismatchDescription
								.appendText(" and there were ")
								.appendText(Integer.toString(failures.size() - 1))
								.appendText(" other mismatches");
					}
					return false;
				}

				return true;
			}

		};
	}

	public static <M extends MatrixMap<T>, T> Matcher<M> mmEquals(
			M expected, Function<T, Matcher<T>> valueMatcherGenerator
	) {
		return new TypeSafeDiagnosingMatcher<M>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a matrix map identical to that given");
			}

			@Override
			protected boolean matchesSafely(M item, Description mismatchDescription) {
				if (item.getNumDimensions() != expected.getNumDimensions()) {
					mismatchDescription
							.appendText("matrix map had ")
							.appendText(Integer.toString(item.getNumDimensions()))
							.appendText(" dimensions but expected ")
							.appendText(Integer.toString(expected.getNumDimensions()));

					return false;
				}
				if (!item.getDimensions().equals(expected.getDimensions())) {
					mismatchDescription
							.appendText("matrix map had dimensions ")
							.appendText(item.getDimensions().toString())
							.appendText(" but expected ")
							.appendText(expected.getDimensions().toString());

					return false;
				}

				List<String> failures = new LinkedList<>();

				expected.eachKey(key -> {
					T expectedValue = expected.getM(key);
					T actualValue = item.getM(key);
					var valueMatcher = valueMatcherGenerator.apply(expectedValue);
					if (!valueMatcher.matches(actualValue)) {
						var failureDescription = new StringDescription();
						failureDescription.appendText("at ");
						failureDescription.appendValueList("[", ", ", "]", key);
						failureDescription.appendText(" expected ");
						failureDescription.appendValue(expectedValue);
						failureDescription.appendText(" but it ");
						valueMatcher.describeMismatch(actualValue, failureDescription);
						failures.add(failureDescription.toString());
					}

				});

				if (!failures.isEmpty()) {
					var first = failures.iterator().next();
					mismatchDescription.appendText(first);

					if (failures.size() > 1) {
						mismatchDescription
								.appendText(" and there were ")
								.appendText(Integer.toString(failures.size() - 1))
								.appendText(" other mismatches");
					}
					return false;
				}

				return true;
			}

		};
	}

	public static Matcher<VdypSite> deepEquals(VdypSite expected) {
		return new TypeSafeDiagnosingMatcher<VdypSite>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("matches VDYPSite ")
						.appendValue(expected.getPolygonIdentifier())
						.appendValue(expected.getLayerType())
						.appendValue(expected.getSiteGenus());
			}

			@Override
			protected boolean matchesSafely(VdypSite item, Description mismatchDescription) {
				boolean match = true;
				match = matchValue(
						match, "PolygonIdentifier",
						expected.getPolygonIdentifier(),
						item.getPolygonIdentifier(),
						mismatchDescription
				);
				match = matchValue(
						match, "LayerType",
						expected.getLayerType(),
						item.getLayerType(),
						mismatchDescription
				);
				match = matchValue(
						match, "Genus",
						expected.getSiteGenus(),
						item.getSiteGenus(),
						mismatchDescription
				);

				match = matchValue(
						match, "LayerType",
						expected.getLayerType(),
						item.getLayerType(),
						mismatchDescription
				);

				match = matchValue(
						match, "AgeTotal",
						expected.getAgeTotal(),
						item.getAgeTotal(),
						mismatchDescription
				);
				match = matchValue(
						match, "Height",
						expected.getHeight(),
						item.getHeight(),
						mismatchDescription
				);
				match = matchValue(
						match, "SiteCurveNumber",
						expected.getSiteCurveNumber(),
						item.getSiteCurveNumber(),
						mismatchDescription
				);
				match = matchValue(
						match, "SiteIndex",
						expected.getSiteIndex(),
						item.getSiteIndex(),
						mismatchDescription
				);
				match = matchValue(
						match, "YearsAtBreastHeight",
						expected.getYearsAtBreastHeight(),
						item.getYearsAtBreastHeight(),
						mismatchDescription
				);
				match = matchValue(
						match, "YearsToBreastHeight",
						expected.getYearsToBreastHeight(),
						item.getYearsToBreastHeight(),
						mismatchDescription
				);

				return match;
			}
		};
	}
}
