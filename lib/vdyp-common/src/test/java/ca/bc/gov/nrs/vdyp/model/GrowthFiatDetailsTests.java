package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

public class GrowthFiatDetailsTests {

	@Test
	void test0Ages() {
		try {
			GrowthFiatDetails details = new GrowthFiatDetails(
					1, List.of(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.2f, 0.3f)
			);
			Assertions.assertTrue(details.getNAgesSupplied() == 0);
			assertThat(details.getAges(), arrayContaining(0.0f, 0.0f, 0.0f, 0.0f));
			assertThat(details.getCoefficients(), arrayContaining(0.0f, 0.0f, 0.0f, 0.0f));
			assertThat(details.getMixedCoefficients(), arrayContaining(0.1f, 0.2f, 0.3f));
		} catch (ResourceParseException e) {
			fail();
		}
	}

	@Test
	void test4Ages() {
		try {
			GrowthFiatDetails details = new GrowthFiatDetails(
					1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 4.0f, 4.0f, 0.1f, 0.2f, 0.3f)
			);
			Assertions.assertTrue(details.getNAgesSupplied() == 4);
			assertThat(details.getAges(), arrayContaining(1.0f, 2.0f, 3.0f, 4.0f));
			assertThat(details.getCoefficients(), arrayContaining(1.0f, 2.0f, 3.0f, 4.0f));
			assertThat(details.getMixedCoefficients(), arrayContaining(0.1f, 0.2f, 0.3f));
		} catch (ResourceParseException e) {
			fail();
		}
	}

	@Test
	void test3Ages() {
		try {
			GrowthFiatDetails details = new GrowthFiatDetails(
					1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 0.0f, 0.0f, 0.1f, 0.2f, 0.3f)
			);
			Assertions.assertTrue(details.getNAgesSupplied() == 3);
			assertThat(details.getAges(), arrayContaining(1.0f, 2.0f, 3.0f, 0.0f));
			assertThat(details.getCoefficients(), arrayContaining(1.0f, 2.0f, 3.0f, 0.0f));
			assertThat(details.getMixedCoefficients(), arrayContaining(0.1f, 0.2f, 0.3f));
		} catch (ResourceParseException e) {
			fail();
		}
	}

	@Test
	void testInterpolation() {
		try {
			GrowthFiatDetails details = new GrowthFiatDetails(
					1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 0.0f, 0.0f, 0.1f, 0.2f, 0.3f)
			);
			Assertions.assertTrue(details.getNAgesSupplied() == 3);
			assertThat(details.calculateCoefficient(0.5f), is(1.0f));
			assertThat(details.calculateCoefficient(1.0f), is(1.0f));
			assertThat(details.calculateCoefficient(1.5f), is(1.5f));
			assertThat(details.calculateCoefficient(2.0f), is(2.0f));
			assertThat(details.calculateCoefficient(2.5f), is(2.5f));
			assertThat(details.calculateCoefficient(3.0f), is(3.0f));
			assertThat(details.calculateCoefficient(3.5f), is(3.0f));
		} catch (ResourceParseException e) {
			fail();
		}
	}

	@Test
	void testAgeListNotEndedProperly() {
		assertThrows(
				ResourceParseException.class,
				() -> new GrowthFiatDetails(
						1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 0.0f, 0.0f, 4.0f, 4.0f, 0.1f, 0.2f, 0.3f)
				)
		);
	}

	@Test
	void testBadCoefficient() {
		assertThrows(
				ResourceParseException.class,
				() -> new GrowthFiatDetails(
						1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 4.0f, 0.1f, 0.2f, 0.3f)
				)
		);
	}

	@Test
	void testAgesNotMonatonicallyIncreasing() {
		assertThrows(
				ResourceParseException.class,
				() -> new GrowthFiatDetails(
						1, List.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 2.5f, 4.0f, 0.1f, 0.2f, 0.3f)
				)
		);
	}

	@Test
	void testRegionValueNot1Or2() {
		assertThrows(
				ResourceParseException.class,
				() -> new GrowthFiatDetails(
						0, List.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f, 1.5f, 4.0f, 4.0f, 0.1f, 0.2f, 0.3f)
				)
		);
	}
}
