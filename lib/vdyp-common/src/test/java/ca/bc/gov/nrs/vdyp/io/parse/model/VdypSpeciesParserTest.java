package ca.bc.gov.nrs.vdyp.io.parse.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.model.VdypSpeciesParser.Ages;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;

class VdypSpeciesParserTest {

	@Test
	void testEmpty() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream("")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypSpeciesParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());
		}
	}

	@Test
	void testOnePoly() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (
				var is = TestUtils.makeInputStream(
						"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 35.00 35.30  55.0  54.0   1.0 1 13",
						"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970  "
				)
		) {

			resolver.addStream("test.dat", is);

			var parser = new VdypSpeciesParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = new ArrayList<>(stream.next()); // Array list makes it indexable so we can easily sample
															// entries to test. Checking everything would be excessive.

			assertThat(result, hasSize(5));

			assertThat(
					result.get(0),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000001 00", 1970)),
							hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B"))
					)
			);

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());

		}
	}

	@Test
	void testMultiplePoly() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (
				var is = TestUtils.makeInputStream(
						"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 35.00 35.30  55.0  54.0   1.0 1 13",
						"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970  ",
						"01002 S000002 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000002 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000002 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 28.70 24.30  45.0  39.6   5.4 1 34",
						"01002 S000002 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000002 00     1970 V  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000002 00     1970 V  8 H  H  100.0     0.0     0.0     0.0 16.70 26.20 105.0  97.9   7.1 1 -9",
						"01002 S000002 00     1970 V 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000002 00     1970  "

				)
		) {

			resolver.addStream("test.dat", is);

			var parser = new VdypSpeciesParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = new ArrayList<>(stream.next()); // Array list makes it indexable so we can easily sample
															// entries to test. Checking everything would be excessive.

			assertThat(result, hasSize(5));

			assertThat(
					result.get(0),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000001 00", 1970)),
							hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B"))
					)
			);
			assertThat(
					result.get(4),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000001 00", 1970)),
							hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("S"))
					)
			);
			assertTrue(stream.hasNext(), "stream is empty");

			result = new ArrayList<>(stream.next()); // Array list makes it indexable so we can easily sample
														// entries to test. Checking everything would be excessive.

			assertThat(result, hasSize(7));

			assertThat(
					result.get(0),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000002 00", 1970)),
							hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B"))
					)
			);
			assertThat(
					result.get(3),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000002 00", 1970)),
							hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("S"))
					)
			);
			assertThat(
					result.get(4),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000002 00", 1970)),
							hasProperty("layerType", is(LayerType.VETERAN)), hasProperty("genus", is("B"))
					)
			);
			assertThat(
					result.get(6),
					allOf(
							hasProperty("polygonIdentifier", VdypMatchers.isPolyId("01002 S000002 00", 1970)),
							hasProperty("layerType", is(LayerType.VETERAN)), hasProperty("genus", is("S"))
					)
			);

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());

		}
	}

	@Nested
	class InferAges {

		@Test
		void testNoNan() {
			var result = VdypSpeciesParser.inferAges(new Ages(60, 50, 10));
			assertThat(result, equalTo(new Ages(60, 50, 10))); // Leave as is
		}

		@Test
		void testTotalNaN() {
			var result = VdypSpeciesParser.inferAges(new Ages(Float.NaN, 50, 10));
			assertThat(result, equalTo(new Ages(60, 50, 10))); // Fill in total
		}

		@Test
		void testYtbNaN() {
			var result = VdypSpeciesParser.inferAges(new Ages(60, 50, Float.NaN));
			assertThat(result, equalTo(new Ages(60, 50, 10))); // Fill Years to Breast Height
		}

		// TODO maybe implement this the same as the other two
		@Test
		void testYabNaN() {
			var result = VdypSpeciesParser.inferAges(new Ages(60, Float.NaN, 10));
			assertThat(result, equalTo(new Ages(60, Float.NaN, 10))); // Leave as is
		}

		// TODO maybe we should log a warning for these cases?

		@Test
		void testDontAddUp() {
			var result = VdypSpeciesParser.inferAges(new Ages(60, 50, 5));
			assertThat(result, equalTo(new Ages(60, 50, 5))); // Leave as is
		}

		@Test
		void testTotalAbndYabBothNaN() {
			var result = VdypSpeciesParser.inferAges(new Ages(Float.NaN, Float.NaN, 10));
			assertThat(result, equalTo(new Ages(Float.NaN, Float.NaN, 10))); // Leave as is
		}

		@Test
		void testTotalNaNYtbZero() {
			var result = VdypSpeciesParser.inferAges(new Ages(Float.NaN, 50, 0));
			assertThat(result, equalTo(new Ages(Float.NaN, 50, 0))); // Leave as is
		}

		@Test
		void testTotalNaNYabZero() {
			var result = VdypSpeciesParser.inferAges(new Ages(Float.NaN, 0, 10));
			assertThat(result, equalTo(new Ages(Float.NaN, 0, 10))); // Leave as is
		}

		@Test
		void testYtbNaNTotalZero() {
			var result = VdypSpeciesParser.inferAges(new Ages(0, 50, Float.NaN));
			assertThat(result, equalTo(new Ages(0, 50, Float.NaN))); // Leave as is
		}

		@Test
		void testYtbNaNYabZero() {
			var result = VdypSpeciesParser.inferAges(new Ages(60, 0, Float.NaN));
			assertThat(result, equalTo(new Ages(60, 0, Float.NaN))); // Leave as is
		}

	}
}
