package ca.bc.gov.nrs.vdyp.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.IllegalFormatConversionException;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FixedWidthFormatTest {

	private static void assertSameAsFormatter(String pattern, Object... args) {
		assertThat(
				pattern + " " + java.util.Arrays.toString(args), FixedWidthFormat.compile(pattern).format(args),
				is(String.format(Locale.ROOT, pattern, args))
		);
	}

	@Test
	void testLiteralsAndPercent() {
		assertSameAsFormatter("abc");
		assertSameAsFormatter("100%% sure\n");
		assertSameAsFormatter("");
	}

	@Test
	void testStrings() {
		assertSameAsFormatter("%s|%5s|%-5s|%-1s|%2s", "a", "b", "c", "toolong", null);
		assertSameAsFormatter("%.2s|%-6.3s|", "abcdef", "abcdef");
		assertSameAsFormatter("%-25s  \n", "082E004    615       1988");
	}

	@Test
	void testIntegers() {
		assertSameAsFormatter(
				"%d|%2d|%3d|%-4d|%6d|%d|%d|%d", 7, -9, 100000, 3, (short) -12, Long.MIN_VALUE, (byte) -1, null
		);
		assertSameAsFormatter("%d", new java.math.BigInteger("123456789012345678901234567890"));
	}

	@Test
	void testFloatSpecialValues() {
		for (float f : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f, -0f,
				Float.MIN_VALUE, -Float.MIN_VALUE, Float.MIN_NORMAL, Float.MAX_VALUE, -Float.MAX_VALUE, -9f, 0.5f, 1.5f,
				2.5f, -2.5f, 0.05f, 0.15f, 0.25f, 0.35f, 2.03125f, -0.00001f, 99999.995f }) {
			for (String p : new String[] { "%9.5f", "%9.2f", "%9.4f", "%6.1f", "%5.1f", "%6.2f", "%4.0f", "%f",
					"%-9.3f" }) {
				assertSameAsFormatter(p, f);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2, 4, 5 })
	void testRandomFloatBitPatterns(int precision) {
		var format = FixedWidthFormat.compile("%9." + precision + "f");
		var pattern = format.pattern();
		var random = new Random(precision);
		for (int i = 0; i < 200_000; i++) {
			float f = Float.intBitsToFloat(random.nextInt());
			assertThat(Float.toString(f), format.format(f), is(String.format(Locale.ROOT, pattern, f)));
		}
	}

	/**
	 * Values close to and exactly on the half way points of each precision, which is where the rounding of the shortest
	 * representation matters.
	 */
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 4, 5 })
	void testFloatsNearRoundingBoundaries(int precision) {
		var format = FixedWidthFormat.compile("%9." + precision + "f");
		var pattern = format.pattern();
		var random = new Random(100 + precision);
		BigDecimal half = BigDecimal.ONE.movePointLeft(precision).divide(BigDecimal.valueOf(2));
		for (int i = 0; i < 50_000; i++) {
			BigDecimal boundary = BigDecimal.valueOf(random.nextInt(2_000_000) - 1_000_000).movePointLeft(precision)
					.add(half);
			float f = boundary.floatValue();
			for (float g : new float[] { f, Math.nextUp(f), Math.nextDown(f) }) {
				assertThat(Float.toString(g), format.format(g), is(String.format(Locale.ROOT, pattern, g)));
			}
		}
	}

	@Test
	void testRealisticRecordValues() {
		var pattern = "%-25s %-1s %2d %-2s%3d%9.5f%9.2f%9.4f%9.4f%9.4f%9.4f%9.4f%9.4f%6.1f\n";
		var format = FixedWidthFormat.compile(pattern);
		var random = new Random(42);
		for (int i = 0; i < 100_000; i++) {
			Object[] args = { "082E004    615       1988", "P", random.nextInt(16), "PL", random.nextInt(6) - 1,
					random.nextFloat() * 80, random.nextFloat() * 5000, random.nextFloat() * 50,
					random.nextFloat() * 900, random.nextFloat() * 900, random.nextFloat() * 900,
					random.nextFloat() * 900, random.nextFloat() * 900, random.nextFloat() * 60 };
			assertThat(format.format(args), is(String.format(Locale.ROOT, pattern, args)));
		}
	}

	@Test
	void testOtherNumericTypesUseFormatter() {
		assertSameAsFormatter("%9.4f|%6.1f", 0.15d, new BigDecimal("2.25"));
	}

	@Test
	void testFormatToAppends() {
		var sb = new StringBuilder("x");
		FixedWidthFormat.compile("%3d|%-3s|").formatTo(sb, 5, "a");
		assertThat(sb.toString(), is("x  5|a  |"));
	}

	@Test
	void testArgumentErrors() {
		var format = FixedWidthFormat.compile("%d %s");
		assertThat(format.argumentCount(), is(2));
		assertThrows(MissingFormatArgumentException.class, () -> format.format(1));
		assertThrows(IllegalFormatConversionException.class, () -> format.format("x", "y"));
		assertThrows(IllegalFormatConversionException.class, () -> FixedWidthFormat.compile("%f").format(1));
	}

	@ParameterizedTest
	@ValueSource(strings = { "%", "%x", "%05d", "%+d", "%,d", "%.2d", "%-s", "%n", "%1$s", "%5.f" })
	void testUnsupportedPatterns(String pattern) {
		assertThrows(IllegalArgumentException.class, () -> FixedWidthFormat.compile(pattern));
	}
}
