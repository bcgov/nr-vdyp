package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;

import java.util.Optional;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ca.bc.gov.nrs.vdyp.io.parse.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;

/**
 * Custom Hamcrest Matchers
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VdypMatchers {

	/**
	 * Matches a string if when parsed by the parser method it matches the given
	 * matcher
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
	 * @return
	 */
	public static Matcher<Optional<?>> notPresent() {
		return new BaseMatcher<Optional<?>>() {

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
				if (! (item instanceof Optional)) {
					description.appendText("was not an Optional");
					return;
				}
				if (! ((Optional<?>) item).isPresent()) {
					description.appendText("was not present");
					return;
				}
			}

		};
	}

	public static <T> Matcher<MatrixMap<T>> mmHasEntry(Matcher<Optional<T>> valueMatcher, Object... keys) {
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
}
