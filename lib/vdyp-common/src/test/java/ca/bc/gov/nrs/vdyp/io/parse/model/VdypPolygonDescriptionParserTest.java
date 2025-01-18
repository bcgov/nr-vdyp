package ca.bc.gov.nrs.vdyp.io.parse.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;

class VdypPolygonDescriptionParserTest {

	@Test
	void testEmpty() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream("")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());
		}
	}

	@Test
	void testOnePoly() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream("01002 S000001 00     1970")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonDescriptionParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = stream.next();

			assertThat(result, isPolyId("01002 S000001 00", 1970));

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
						"01002 S000001 00     1970", "01002 S000002 00     1970", "01002 S000003 00     1973"

				)
		) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonDescriptionParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = stream.next();

			assertThat(result, isPolyId("01002 S000001 00", 1970));

			assertTrue(stream.hasNext(), "stream is empty");

			result = stream.next();

			assertThat(result, isPolyId("01002 S000002 00", 1970));

			assertTrue(stream.hasNext(), "stream is empty");

			result = stream.next();

			assertThat(result, isPolyId("01002 S000003 00", 1973));

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());

		}
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "01002 S000001 00     XXXX", "01002 S000001 00     199X", "01002 S000001 00      999",
					"01002 S000001 00     999 ", "01002 S000001 00     19 9", "01002 S000001 00         " }
	)
	void testIDsWithoutAValidYear(String id) throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream(id)) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonDescriptionParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var ex = assertThrows(ResourceParseException.class, () -> stream.next());

		}

	}

	@ParameterizedTest
	@ValueSource(
			strings = { "01002 S000001 00    2024", // short (24)
					"01002 S000001 00      2024" // long (26)
			}
	)
	void testIdsWithWrongLength(String id) throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream(id)) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonDescriptionParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var ex = assertThrows(ResourceParseException.class, () -> stream.next());

		}

	}

	@ParameterizedTest
	@ValueSource(
			strings = { "01002 S000001 00     XXXX", // bad year
					"01002 S000001 00    2024", // short
					"01002 S000001 00      2024" // long
			}
	)
	void testCanProgressToNextLineAfterError(String id) throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream(id, "01002 S000002 00     1970")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonDescriptionParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var ex = assertThrows(ResourceParseException.class, () -> stream.next());

			assertTrue(stream.hasNext(), "stream is empty");

			var result = stream.next();

			assertThat(result, isPolyId("01002 S000002 00", 1970));

		}

	}

}
