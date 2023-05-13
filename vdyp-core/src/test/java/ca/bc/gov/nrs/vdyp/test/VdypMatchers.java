package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.Matchers.hasProperty;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ca.bc.gov.nrs.vdyp.io.parse.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;

/**
 * Custom Hamcrest Matchers
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VdypMatchers {
	
	/**
	 * Matches a string if when parsed by the parser method it matches the given matcher
	 * @param parsedMatcher matcher for the parsed value
	 * @param parser parser
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
	 * @param causeMatcher
	 * @return
	 */
	public static Matcher<Throwable> causedBy(Matcher<? extends Throwable> causeMatcher) {
		
		return new BaseMatcher<Throwable>() {

			@Override
			public boolean matches(Object actual) {
				return causeMatcher.matches(((Throwable) actual).getCause());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("was caused by exception that ").appendDescriptionOf(causeMatcher);
			}
			
		};
	}
}
