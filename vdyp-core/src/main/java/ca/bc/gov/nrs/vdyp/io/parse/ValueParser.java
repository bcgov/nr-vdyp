package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Parses a string to a value
 * @author Kevin Smith, Vivid Solutions
 *
 * @param <T>
 */
@FunctionalInterface
public interface ValueParser<T> {
	/**
	 * Parse a string to a value
	 * @param string
	 * @return The parsed value
	 * @throws ValueParseException if the string could not be parsed
	 */
	T parse(String string) throws ValueParseException;
	
	public static interface JavaNumberParser <U extends Number> {
		U parse(String s) throws NumberFormatException;
	}
	
	/**
	 * Adapts a parse method that throws NumerFormatException
	 * @param parser method that parses a string into the required type and throws NumerFormatException
	 * @param klazz the type to parse into
	 */
	public static <U extends Number> ValueParser<U> numberParser(JavaNumberParser<U> parser, Class<U> klazz) {
		return s->{
			String stripped = s.strip();
			try {
				return parser.parse(stripped);
			} catch(NumberFormatException ex) {
				throw new ValueParseException(stripped, String.format("\"%s\" is not a valid %s", stripped, klazz.getSimpleName()), ex);
			}
		};
	}
	
	/**
	 * Parser for long integers
	 */
	public static final ValueParser<Long> LONG = numberParser(Long::parseLong, Long.class);
	
	/**
	 * Parser for integers
	 */
	public static final ValueParser<Integer> INTEGER = numberParser(Integer::parseInt, Integer.class);
	
	/**
	 * Parser for short integers
	 */
	public static final ValueParser<Short> SHORT = numberParser(Short::parseShort, Short.class);
	
	/**
	 * Parser for double precision floats
	 */
	public static final ValueParser<Double> DOUBLE = numberParser(Double::parseDouble, Double.class);
	
	/**
	 * Parser for single precision floats
	 */
	public static final ValueParser<Float> FLOAT = numberParser(Float::parseFloat, Float.class);
	
	public static <U> ValueParser<List<U>> list(ValueParser<U> delegate) {
		return s-> {
			var elementStrings = s.strip().split("\s+");
			List<U> result = new ArrayList<>();
			for(String elementString : elementStrings) {
				result.add(delegate.parse(elementString));
			}
			return Collections.unmodifiableList(result);
		};
	}
	
	/**
	 * Wrap a parser with an additional validation step
	 * @param <U>
	 * @param delegate
	 * @param validator Function that returns an error string if the parsed value is invalid
	 * @return
	 */
	public static <U> ValueParser<U> validate(ValueParser<U> delegate, Function<U, Optional<String>> validator) {
		return s-> {
			var value = delegate.parse(s);
			var error = validator.apply(value);
			if(error.isPresent()) {
				throw new ValueParseException(s, error.get());
			}
			return value;
		};
	}
	
	/**
	 * Makes a parser that parses if the string is not blank, and returns an empty Optional otherwise.
	 * @param delegate Parser to use if the string is not blank
	 */
	public static <U> ValueParser<Optional<U>> optional(ValueParser<U> delegate) {
		return (s)->{
			if(!s.isBlank()) {
				return Optional.of(delegate.parse(s));
			}
			return Optional.empty();
		};
	}
}
