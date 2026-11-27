package ca.bc.gov.nrs.vdyp.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Formattable;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Locale;
import java.util.MissingFormatArgumentException;

/**
 * A pre-parsed replacement for {@link String#format(String, Object...)} for the fixed-width record formats used to
 * write VDYP7 data files.
 * <p>
 * {@link String#format} parses its pattern with a regular expression and looks up locale data on every call, which
 * dominates the cost of writing projection output. This class parses the pattern once and formats each field directly.
 * It supports the subset of the {@link java.util.Formatter} syntax that the record formats use:
 * <ul>
 * <li><code>%[-][width][.precision]s</code></li>
 * <li><code>%[-][width]d</code></li>
 * <li><code>%[-][width][.precision]f</code></li>
 * <li><code>%%</code> and literal text, including line feeds</li>
 * </ul>
 * Any other conversion or flag is rejected when the pattern is compiled.
 * <p>
 * The output is identical to {@link String#format} with {@link Locale#ROOT}, that is, with ASCII digits and a period as
 * the decimal separator regardless of the default locale. {@link String#format} without a locale uses the default
 * locale instead, so in a locale whose decimal separator is a comma it writes files that VDYP cannot read back.
 * <p>
 * <code>%f</code> arguments of type {@link Float} are formatted directly. The {@link java.util.Formatter} rounds the
 * shortest decimal representation of the value half up. For a float, the true value is either exactly on a rounding
 * boundary, in which case its shortest representation is exact, or it is separated from the boundary by far more than
 * the error of a 17 significant digit representation. Rounding the {@link Double#toString(double)} digits half up
 * therefore gives the same result. Other numeric types are passed to the {@link java.util.Formatter}.
 * <p>
 * Instances are immutable and thread safe.
 */
public final class FixedWidthFormat {

	private static final char LITERAL = 0;

	/**
	 * One element of a compiled pattern: either literal text (conversion {@link #LITERAL}) or a conversion of the next
	 * argument.
	 */
	private record Segment(char conversion, String literal, boolean leftJustify, int width, int precision) {
	}

	private final String pattern;
	private final Segment[] segments;
	private final int argumentCount;
	private final int estimatedLength;

	private FixedWidthFormat(String pattern, List<Segment> segments) {
		this.pattern = pattern;
		this.segments = segments.toArray(Segment[]::new);
		int args = 0;
		int length = 0;
		for (var segment : this.segments) {
			if (segment.conversion() == LITERAL) {
				length += segment.literal().length();
			} else {
				args++;
				length += Math.max(segment.width(), 1);
			}
		}
		this.argumentCount = args;
		this.estimatedLength = length;
	}

	/**
	 * Parse a pattern.
	 *
	 * @param pattern a {@link java.util.Formatter} pattern using only the supported subset of the syntax
	 * @return the compiled pattern
	 * @throws IllegalArgumentException if the pattern uses syntax outside the supported subset
	 */
	public static FixedWidthFormat compile(String pattern) {
		List<Segment> segments = new ArrayList<>();
		StringBuilder literal = new StringBuilder();
		int i = 0;
		int n = pattern.length();
		while (i < n) {
			char c = pattern.charAt(i++);
			if (c != '%') {
				literal.append(c);
				continue;
			}
			if (i >= n) {
				throw unsupported(pattern, "dangling %");
			}
			if (pattern.charAt(i) == '%') {
				literal.append('%');
				i++;
				continue;
			}
			if (!literal.isEmpty()) {
				segments.add(new Segment(LITERAL, literal.toString(), false, -1, -1));
				literal.setLength(0);
			}

			boolean leftJustify = false;
			if (pattern.charAt(i) == '-') {
				leftJustify = true;
				i++;
			}
			int width = -1;
			int start = i;
			while (i < n && Character.isDigit(pattern.charAt(i))) {
				i++;
			}
			if (i > start) {
				if (pattern.charAt(start) == '0') {
					throw unsupported(pattern, "zero padding");
				}
				width = Integer.parseInt(pattern, start, i, 10);
			}
			int precision = -1;
			if (i < n && pattern.charAt(i) == '.') {
				start = ++i;
				while (i < n && Character.isDigit(pattern.charAt(i))) {
					i++;
				}
				if (i == start) {
					throw unsupported(pattern, "missing precision");
				}
				precision = Integer.parseInt(pattern, start, i, 10);
			}
			if (i >= n) {
				throw unsupported(pattern, "missing conversion");
			}
			char conversion = pattern.charAt(i++);
			switch (conversion) {
			case 's':
				break;
			case 'd':
				if (precision >= 0) {
					throw unsupported(pattern, "precision on %d");
				}
				break;
			case 'f':
				if (precision < 0) {
					precision = 6;
				}
				break;
			default:
				throw unsupported(pattern, "conversion %" + conversion);
			}
			if (leftJustify && width < 0) {
				throw unsupported(pattern, "- flag without width");
			}
			segments.add(new Segment(conversion, null, leftJustify, width, precision));
		}
		if (!literal.isEmpty()) {
			segments.add(new Segment(LITERAL, literal.toString(), false, -1, -1));
		}
		return new FixedWidthFormat(pattern, segments);
	}

	private static IllegalArgumentException unsupported(String pattern, String what) {
		return new IllegalArgumentException(
				"Unsupported format pattern \"" + pattern + "\": " + what + "; FixedWidthFormat supports only"
						+ " %[-][width][.precision]s, %[-][width]d, %[-][width][.precision]f and %%"
		);
	}

	/**
	 * @return the pattern this instance was compiled from
	 */
	public String pattern() {
		return pattern;
	}

	/**
	 * Format the arguments.
	 *
	 * @param args the arguments referenced by the pattern, in order
	 * @return the formatted text
	 * @throws MissingFormatArgumentException   if there are fewer arguments than conversions
	 * @throws IllegalFormatConversionException if an argument does not suit its conversion
	 */
	public String format(Object... args) {
		StringBuilder sb = new StringBuilder(estimatedLength);
		formatTo(sb, args);
		return sb.toString();
	}

	/**
	 * Format the arguments, appending them to the given builder.
	 *
	 * @param sb   the builder to append to
	 * @param args the arguments referenced by the pattern, in order
	 * @throws MissingFormatArgumentException   if there are fewer arguments than conversions
	 * @throws IllegalFormatConversionException if an argument does not suit its conversion
	 */
	public void formatTo(StringBuilder sb, Object... args) {
		int argIndex = 0;
		for (var segment : segments) {
			if (segment.conversion() == LITERAL) {
				sb.append(segment.literal());
				continue;
			}
			if (argIndex >= args.length) {
				throw new MissingFormatArgumentException(specifier(segment));
			}
			Object arg = args[argIndex++];
			switch (segment.conversion()) {
			case 's' -> appendString(sb, segment, arg);
			case 'd' -> appendInteger(sb, segment, arg);
			default -> appendFloat(sb, segment, arg);
			}
		}
	}

	/**
	 * @return the number of arguments the pattern consumes
	 */
	public int argumentCount() {
		return argumentCount;
	}

	private static void appendString(StringBuilder sb, Segment segment, Object arg) {
		if (arg instanceof Formattable) {
			sb.append(String.format(Locale.ROOT, specifier(segment), arg));
			return;
		}
		String s = String.valueOf(arg);
		if (segment.precision() >= 0 && s.length() > segment.precision()) {
			s = s.substring(0, segment.precision());
		}
		appendField(sb, segment, s);
	}

	private static void appendInteger(StringBuilder sb, Segment segment, Object arg) {
		if (arg == null) {
			appendField(sb, segment, "null");
		} else if (arg instanceof Integer || arg instanceof Long || arg instanceof Short || arg instanceof Byte) {
			appendField(sb, segment, Long.toString( ((Number) arg).longValue()));
		} else if (arg instanceof BigInteger) {
			appendField(sb, segment, arg.toString());
		} else {
			throw new IllegalFormatConversionException('d', arg.getClass());
		}
	}

	private static void appendField(StringBuilder sb, Segment segment, String text) {
		int padding = segment.width() - text.length();
		if (!segment.leftJustify()) {
			appendSpaces(sb, padding);
		}
		sb.append(text);
		if (segment.leftJustify()) {
			appendSpaces(sb, padding);
		}
	}

	private static void appendFloat(StringBuilder sb, Segment segment, Object arg) {
		if (arg == null) {
			appendField(sb, segment, "null");
			return;
		}
		if (! (arg instanceof Float f)) {
			if (arg instanceof Double || arg instanceof BigDecimal) {
				sb.append(String.format(Locale.ROOT, specifier(segment), arg));
				return;
			}
			throw new IllegalFormatConversionException('f', arg.getClass());
		}

		double value = f.doubleValue();
		if (Double.isNaN(value)) {
			appendField(sb, segment, "NaN");
			return;
		}
		// Formatter treats -0.0 as negative
		boolean negative = Double.compare(value, 0.0) < 0;
		if (Double.isInfinite(value)) {
			appendField(sb, segment, negative ? "-Infinity" : "Infinity");
			return;
		}
		appendRounded(sb, segment, negative ? -value : value, negative);
	}

	/**
	 * Append a finite, non-negative value rounded half up to the segment's precision, as {@link java.util.Formatter}
	 * does, together with its sign and padding.
	 * <p>
	 * The digits of {@link Double#toString(double)} are rounded in place. Its output is either <code>ddd.ddd</code> or,
	 * outside 10<sup>-3</sup> to 10<sup>7</sup>, <code>d.dddE[-]n</code>.
	 */
	private static void appendRounded(StringBuilder sb, Segment segment, double magnitude, boolean negative) {
		String s = Double.toString(magnitude);
		int length = s.length();

		// digits[0] is a leading zero that absorbs a carry out of the most significant digit
		char[] digits = new char[length + 1];
		digits[0] = '0';
		int count = 1;
		int pointIndex = 0;
		int exponent = 0;
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (c == '.') {
				pointIndex = count;
			} else if (c == 'E') {
				exponent = Integer.parseInt(s, i + 1, length, 10);
				break;
			} else {
				digits[count++] = c;
			}
		}

		// digits[0, integerDigits) are before the decimal point. Positions outside [0, count) are zeros.
		int integerDigits = pointIndex + exponent;
		int precision = segment.precision();
		int keep = integerDigits + precision;
		if (keep < count) {
			// keep >= 1 whenever the dropped digit is non-zero, because digits[0] is zero
			boolean roundUp = keep >= 0 && digits[keep] >= '5';
			count = Math.max(keep, 0);
			if (roundUp) {
				int i = keep - 1;
				while (digits[i] == '9') {
					digits[i--] = '0';
				}
				digits[i]++;
			}
		}

		int first = 0;
		while (first < integerDigits - 1 && digitAt(digits, count, first) == '0') {
			first++;
		}
		int integerLength = Math.max(integerDigits - first, 1);
		int fieldLength = (negative ? 1 : 0) + integerLength + (precision > 0 ? precision + 1 : 0);

		int padding = segment.width() - fieldLength;
		if (!segment.leftJustify()) {
			appendSpaces(sb, padding);
		}
		if (negative) {
			sb.append('-');
		}
		if (integerDigits <= 0) {
			sb.append('0');
		} else {
			for (int i = first; i < integerDigits; i++) {
				sb.append(digitAt(digits, count, i));
			}
		}
		if (precision > 0) {
			sb.append('.');
			for (int i = integerDigits; i < keep; i++) {
				sb.append(digitAt(digits, count, i));
			}
		}
		if (segment.leftJustify()) {
			appendSpaces(sb, padding);
		}
	}

	private static char digitAt(char[] digits, int count, int index) {
		return index >= 0 && index < count ? digits[index] : '0';
	}

	private static void appendSpaces(StringBuilder sb, int count) {
		while (count > 0) {
			int chunk = Math.min(count, SPACES.length());
			sb.append(SPACES, 0, chunk);
			count -= chunk;
		}
	}

	private static final String SPACES = "                                ";

	private static String specifier(Segment segment) {
		StringBuilder sb = new StringBuilder("%");
		if (segment.leftJustify()) {
			sb.append('-');
		}
		if (segment.width() >= 0) {
			sb.append(segment.width());
		}
		if (segment.precision() >= 0) {
			sb.append('.').append(segment.precision());
		}
		return sb.append(segment.conversion()).toString();
	}

	@Override
	public String toString() {
		return pattern;
	}
}
