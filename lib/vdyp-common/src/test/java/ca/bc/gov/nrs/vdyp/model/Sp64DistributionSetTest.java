package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class Sp64DistributionSetTest {

	private static final String aAlias = "A";
	private static final String bAlias = "B";
	private static final String cAlias = "C";

	@Test
	void testSp64DistributionConstruction() {
		assertThrows(IllegalArgumentException.class, () -> new Sp64Distribution(1, aAlias, -8.0f));
		assertThrows(IllegalArgumentException.class, () -> new Sp64Distribution(1, aAlias, 120.0f));
		assertThrows(IllegalArgumentException.class, () -> new Sp64Distribution(1, null, 50.0f));
	}

	@Test
	void testSp64DistributionComparison1() {

		var a = new Sp64Distribution(1, aAlias, 75f);
		var b = new Sp64Distribution(1, bAlias, 25f);

		assertNotEquals(a, null);

		assertNotEquals(a, b);
		assertTrue(a.compareTo(a) == 0);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
		assertTrue(a.compareTo(null) > 0);
	}

	@Test
	void testSp64DistributionComparison2() {
		var gd1 = new Sp64Distribution(0, aAlias, 70.0f);
		var gd2 = new Sp64Distribution(0, bAlias, 30.0f);

		assertNotEquals(gd1, gd2);
		assertNotEquals(gd1, null);
		assertNotEquals(null, gd2);

		assertTrue(gd1.compareTo(gd1) == 0);
		assertTrue(gd1.compareTo(gd2) < 0);
		assertTrue(gd2.compareTo(gd1) > 0);
		assertTrue(gd1.compareTo(null) > 0);

		assertEquals(gd1.hashCode(), gd1.hashCode());
	}

	@Test
	void testSp64DistributionAccessors() {
		var gd1 = new Sp64Distribution(0, aAlias, 70.0f);
		assertThat(gd1.getGenusAlias(), is(aAlias));
		assertThat(gd1.getIndex(), is(0));
		assertThat(gd1.getPercentage(), is(70.0f));
	}

	@Test
	void testSp64DistributionSetConstruction() {
		var gd1 = new Sp64Distribution(1, aAlias, 70.0f);
		var gd1a = new Sp64Distribution(2, aAlias, 30.0f);
		var gd2 = new Sp64Distribution(2, bAlias, 30.0f);
		var gd2a = new Sp64Distribution(2, bAlias, 25.0f);
		var gd3 = new Sp64Distribution(1, cAlias, 20.0f);

		assertNotNull(new Sp64DistributionSet(2, List.of(gd1, gd2)));
		var sp64Set1 = new Sp64DistributionSet(3, List.of(gd1, gd2a));
		assertNotNull(sp64Set1);
		assertTrue(sp64Set1.getSpeciesDistribution(1).get().equals(gd1));
		assertTrue(sp64Set1.getSpeciesDistribution(2).get().equals(gd2a));

		// Test copy constructor
		var sp64Set1Copy = sp64Set1.copy();
		assertNotNull(sp64Set1Copy);
		assertTrue(sp64Set1Copy.getSpeciesDistribution(1).get().equals(gd1));
		assertTrue(sp64Set1Copy.getSpeciesDistribution(2).get().equals(gd2a));

		// Same sp64 aliases
		assertThrows(IllegalArgumentException.class, () -> new Sp64DistributionSet(2, List.of(gd1, gd1a)));

		// maxIndex too low
		assertThrows(IllegalArgumentException.class, () -> new Sp64DistributionSet(1, List.of(gd1, gd2)));

		// same indicies
		assertThrows(IllegalArgumentException.class, () -> new Sp64DistributionSet(2, List.of(gd1a, gd2a)));

		// percentages not decreasing
		assertThrows(IllegalArgumentException.class, () -> new Sp64DistributionSet(2, List.of(gd3, gd2)));
	}

	@Test
	void testSp64DistributionSetAccessor() {
		var sd1 = new Sp64Distribution(1, aAlias, 70.0f);
		var sd2 = new Sp64Distribution(2, bAlias, 30.0f);

		var set = new Sp64DistributionSet(4, List.of(sd1, sd2));

		assertThat(set.getSpeciesDistribution(1), isA(Optional.class));
		assertThat(set.getSpeciesDistribution(1).get(), is(sd1));
		assertThat(set.getSpeciesDistribution(2), isA(Optional.class));
		assertThat(set.getSpeciesDistribution(2).get(), is(sd2));
		assertThat(set.getSpeciesDistribution(3), is(Optional.empty()));
		assertThat(set.getSpeciesDistribution(4), is(Optional.empty()));
		assertThrows(IllegalArgumentException.class, () -> set.getSpeciesDistribution(5));

		assertThat(set.getSp64DistributionList().size(), is(set.getSize()));
		assertThat(set.getSp64DistributionMap().size(), is(set.getSize()));
	}

	@Test
	void testSp64DistributionSetComparison() {
		var sd1 = new Sp64Distribution(1, aAlias, 70.0f);
		var sd2 = new Sp64Distribution(2, bAlias, 30.0f);
		var sd3 = new Sp64Distribution(3, cAlias, 30.0f);
		var sd4 = new Sp64Distribution(3, bAlias, 30.0f);
		var sd5 = new Sp64Distribution(2, bAlias, 20.0f);

		{
			// maxIndex differences
			var set1 = new Sp64DistributionSet(4, List.of(sd1, sd2));
			var set2 = new Sp64DistributionSet(3, List.of(sd1, sd2));
			assertThat(set1.compareTo(set2), is(1));
			assertThat(set2.compareTo(set1), is(-1));
			assertThat(set1.compareTo(set1), is(0));
		}

		{
			// maxIndex same, length differences
			var set1 = new Sp64DistributionSet(4, List.of(sd1, sd2));
			var set2 = new Sp64DistributionSet(4, List.of(sd1));
			assertThat(set1.compareTo(set2), is(1));
		}

		{
			// maxIndex same, length same, element differences

			// lower index
			var set1 = new Sp64DistributionSet(4, List.of(sd1, sd2));
			var set2 = new Sp64DistributionSet(4, List.of(sd1, sd4));
			assertThat(set1.compareTo(set2), is(-1));

			// lower alias
			var set3 = new Sp64DistributionSet(4, List.of(sd1, sd2));
			var set4 = new Sp64DistributionSet(4, List.of(sd1, sd3));
			assertThat(set3.compareTo(set4), is(-1));

			// lower percentage
			var set5 = new Sp64DistributionSet(4, List.of(sd1, sd2));
			var set6 = new Sp64DistributionSet(4, List.of(sd1, sd5));
			assertThat(set5.compareTo(set6), is(1));
		}
	}
}
