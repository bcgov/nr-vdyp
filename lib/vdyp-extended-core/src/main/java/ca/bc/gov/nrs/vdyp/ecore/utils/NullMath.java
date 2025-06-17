package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class NullMath {

	public static <T extends Number> boolean op(T a, T b, BiFunction<T, T, Boolean> f, T valueIfNull) {
		if (a == null) {
			a = valueIfNull;
		}
		if (b == null) {
			b = valueIfNull;
		}

		return f.apply(a, b);
	}

	public static <T extends Number> T max(T a, T b, BinaryOperator<T> maxf, T valueIfNull) {
		if (a == null && b == null) {
			return null;
		}
		if (a == null) {
			a = valueIfNull;
		}
		if (b == null) {
			b = valueIfNull;
		}

		return maxf.apply(a, b);
	}

	public static <T extends Number> T max(T a, T b, T c, BinaryOperator<T> maxf, T valueIfNull) {
		if (a == null && b == null && c == null) {
			return null;
		}

		return max(a, max(b, c, maxf, valueIfNull), maxf, valueIfNull);
	}
}
