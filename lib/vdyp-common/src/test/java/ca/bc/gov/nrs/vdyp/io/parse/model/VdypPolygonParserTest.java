package ca.bc.gov.nrs.vdyp.io.parse.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;

class VdypPolygonParserTest {

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

		try (var is = TestUtils.makeInputStream("01002 S000001 00     1970 CWH  A    99 37  1  1")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = stream.next();

			assertThat(
					result,
					allOf(
							hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)),
							hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH")))
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
						"01002 S000001 00     1970 CWH  A    99 37  1  1",
						"01002 S000002 00     1970 CWH  A    98 15 75  1",
						"01002 S000003 00     1970 IDF  A    99 15 75  1"

				)
		) {

			resolver.addStream("test.dat", is);

			var parser = new VdypPolygonParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = stream.next();

			assertThat(
					result,
					allOf(
							hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)),
							hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH")))
					)
			);

			assertTrue(stream.hasNext(), "stream is empty");

			result = stream.next();

			assertThat(
					result,
					allOf(
							hasProperty("polygonIdentifier", isPolyId("01002 S000002 00", 1970)),
							hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH")))
					)
			);

			assertTrue(stream.hasNext(), "stream is empty");

			result = stream.next();

			assertThat(
					result,
					allOf(
							hasProperty("polygonIdentifier", isPolyId("01002 S000003 00", 1970)),
							hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF")))
					)
			);

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());

		}
	}

}
