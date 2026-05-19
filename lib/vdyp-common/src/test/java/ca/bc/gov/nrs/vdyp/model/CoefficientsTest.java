package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.test.CoefficientsProvider;
import ca.bc.gov.nrs.vdyp.test.DoubleCoefficientsProvider;
import ca.bc.gov.nrs.vdyp.test.FloatCoefficientsProvider;

/**
 * Ensure that Coefficients and Double Coefficients are tests
 */
class CoefficientsTest {
	private static final double EPSILION = 0.000001d;

	static Stream<Arguments> coefficientTypes() {
		// MAKE SURE BINARY AND UNARY OPERATORS MATCH
		return Stream.of(
				Arguments.of(
						"float coefficients", new FloatCoefficientsProvider(
								(x, y) -> x + y, //
								x -> x * 2, //
								(x, y, i) -> x + y + i, //
								(x, i) -> x * 2 + i//
						)
				),
				Arguments.of(
						"double coefficients", new DoubleCoefficientsProvider(
								(x, y) -> x + y, //
								x -> x * 2, //
								(x, y, i) -> x + y + i, //
								(x, i) -> x * 2 + i
						)
				)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testGetCoe(String name, CoefficientsProvider<T> provider) {
		var startIndex = -1;
		var inputs = new double[] { 2d, 3d, 4d };
		var unit = provider.create(inputs, startIndex);
		assertThat(provider.getCoe(unit, -1), closeTo(2d, EPSILION));
		assertThat(provider.getCoe(unit, 0), closeTo(3d, EPSILION));
		assertThat(provider.getCoe(unit, 1), closeTo(4d, EPSILION));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testGetCoeOutOfBounds(String name, CoefficientsProvider<T> provider) {
		var startIndex = -1;
		var inputs = new double[] { 2d, 3d, 4d };
		var unit = provider.create(inputs, startIndex);
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> provider.getCoe(unit, -2));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> provider.getCoe(unit, 2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlace(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, -1);
		provider.pairwiseInPlace(unit1, unit2);
		provider.assertValues(unit1, -1, 7d, 9d, 11d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlaceIndexMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> provider.pairwiseInPlace(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlaceSizeMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> provider.pairwiseInPlace(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlaceIndexed(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, -1);
		provider.indexedPairwiseInPlace(unit1, unit2);
		provider.assertValues(unit1, -1, 6d, 9d, 12d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlaceIndexedIndexMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> provider.indexedPairwiseInPlace(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseInPlaceIndexedSizeMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> provider.indexedPairwiseInPlace(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwise(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, -1);
		var result = provider.pairwise(unit1, unit2);
		provider.assertValues(result, -1, 7d, 9d, 11d);
		provider.assertValues(unit1, -1, 2d, 3d, 4d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseIndexMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> provider.pairwise(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseSizeMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> provider.pairwise(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseIndexed(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, -1);
		var result = provider.indexedPairwise(unit1, unit2);
		provider.assertValues(result, -1, 6d, 9d, 12d);
		provider.assertValues(unit1, -1, 2d, 3d, 4d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseIndexedIndexMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> provider.indexedPairwise(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testPairwiseIndexedSizeMismatch(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = provider.create(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> provider.indexedPairwise(unit1, unit2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalarInPlace(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		provider.scalarInPlace(unit1);
		provider.assertValues(unit1, -1, 4d, 6d, 8d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalarInPlaceIndexed(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		provider.indexedScalarInPlace(unit1);
		provider.assertValues(unit1, -1, 3d, 6d, 9d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalar(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2d, 3d, 4d }, -1);
		var result = provider.scalar(unit1);
		provider.assertValues(result, -1, 4d, 6d, 8d);
		provider.assertValues(unit1, -1, 2d, 3d, 4d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalarIndexed(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2f, 3f, 4f }, -1);
		var result = provider.indexedScalar(unit1);
		provider.assertValues(result, -1, 3d, 6d, 9d);
		provider.assertValues(unit1, -1, 2d, 3d, 4d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalarInPlaceSpecificIndex(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2f, 3f, 4f }, -1);
		provider.scalarInPlace(1, unit1);
		provider.assertValues(unit1, -1, 2d, 3d, 8d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testIndexedScalarInPlaceSpecificIndex(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2f, 3f, 4f }, -1);
		provider.indexedScalarInPlace(1, unit1);
		provider.assertValues(unit1, -1, 2d, 3d, 9d);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("coefficientTypes")
	<T> void testScalarInPlaceSpecificIndexOutOfBounds(String name, CoefficientsProvider<T> provider) {
		var unit1 = provider.create(new double[] { 2f, 3f, 4f }, -1);
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> provider.scalarInPlace(-2, unit1));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> provider.scalarInPlace(2, unit1));
	}

}
