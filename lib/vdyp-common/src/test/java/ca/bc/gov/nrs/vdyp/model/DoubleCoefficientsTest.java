package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class DoubleCoefficientsTest {

	@Test
	void testGetCoe() {
		var unit = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		assertThat(unit.getCoe(-1), is(2d));
		assertThat(unit.getCoe(0), is(3d));
		assertThat(unit.getCoe(1), is(4d));
	}

	@Test
	void testGetCoeOutOfBounds() {
		var unit = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> unit.getCoe(-2));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> unit.getCoe(2));
	}

	@Test
	void testPairwiseInPlace() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, -1);
		unit1.pairwiseInPlace(unit2, (x, y) -> x + y);
		assertThat(unit1, VdypMatchers.dcoe(-1, 7.0, 9.0, 11.0));
	}

	@Test
	void testPairwiseInPlaceIndexMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwiseInPlace(unit2, (x, y) -> x + y));
	}

	@Test
	void testPairwiseInPlaceSizeMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwiseInPlace(unit2, (x, y) -> x + y));
	}

	@Test
	void testPairwiseInPlaceIndexed() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, -1);
		unit1.pairwiseInPlace(unit2, (x, y, i) -> x + y + i);
		assertThat(unit1, VdypMatchers.dcoe(-1, 6.0, 9.0, 12.0));
	}

	@Test
	void testPairwiseInPlaceIndexedIndexMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwiseInPlace(unit2, (x, y, i) -> x + y + i));
	}

	@Test
	void testPairwiseInPlaceIndexedSizeMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwiseInPlace(unit2, (x, y, i) -> x + y + i));
	}

	@Test
	void testPairwise() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, -1);
		var result = unit1.pairwise(unit2, (x, y) -> x + y);
		assertThat(result, VdypMatchers.dcoe(-1, 7d, 9d, 11d));
		assertThat(unit1, VdypMatchers.dcoe(-1, 2d, 3d, 4d));
	}

	@Test
	void testPairwiseIndexMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwise(unit2, (x, y) -> x + y));
	}

	@Test
	void testPairwiseSizeMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d }, -1);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwise(unit2, (x, y) -> x + y));
	}

	@Test
	void testPairwiseIndexed() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, -1);
		var result = unit1.pairwise(unit2, (x, y, i) -> x + y + i);
		assertThat(result, VdypMatchers.dcoe(-1, 6d, 9d, 12d));
		assertThat(unit1, VdypMatchers.dcoe(-1, 2d, 3d, 4d));
	}

	@Test
	void testPairwiseIndexedIndexMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5d, 6d, 7d }, 0);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwise(unit2, (x, y, i) -> x + y + i));
	}

	@Test
	void testPairwiseIndexedSizeMissmatch() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var unit2 = new DoubleCoefficients(new double[] { 5f, 6f }, -1);
		assertThrows(IllegalArgumentException.class, () -> unit1.pairwise(unit2, (x, y, i) -> x + y + i));
	}

	@Test
	void testScalarInPlace() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		unit1.scalarInPlace(x -> x * 2);
		assertThat(unit1, VdypMatchers.dcoe(-1, 4d, 6d, 8d));
	}

	@Test
	void testScalarInPlaceIndexed() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		unit1.scalarInPlace((x, i) -> x * 2 + i);
		assertThat(unit1, VdypMatchers.dcoe(-1, 3d, 6d, 9d));
	}

	@Test
	void testScalar() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var result = unit1.scalar(x -> x * 2);
		assertThat(result, VdypMatchers.dcoe(-1, 4d, 6d, 8d));
		assertThat(unit1, VdypMatchers.dcoe(-1, 2d, 3d, 4d));
	}

	@Test
	void testScalarIndexed() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		var result = unit1.scalar((x, i) -> x * 2 + i);
		assertThat(result, VdypMatchers.dcoe(-1, 3d, 6d, 9d));
		assertThat(unit1, VdypMatchers.dcoe(-1, 2d, 3d, 4d));
	}

	@Test
	void testScalarInPlaceSpecificIndex() {
		var unit1 = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		unit1.scalarInPlace(1, x -> x * 2);
		assertThat(unit1, VdypMatchers.dcoe(-1, 2d, 3d, 8d));
	}

	@Test
	void testScalarInPlaceSpecificIndexOutOfBounds() {
		var unit = new DoubleCoefficients(new double[] { 2d, 3d, 4d }, -1);
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> unit.scalarInPlace(-2, x -> x * 2));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> unit.scalarInPlace(2, x -> x * 2));
	}

}
