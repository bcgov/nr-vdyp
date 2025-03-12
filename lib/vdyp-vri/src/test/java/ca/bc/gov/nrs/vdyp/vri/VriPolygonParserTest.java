package ca.bc.gov.nrs.vdyp.vri;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertEmpty;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertNext;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isPolyId;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;

class VriPolygonParserTest {

	@Test
	void testParseEmpty() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext("test.dat", TestUtils.makeInputStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	void testParsePolygon() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("082F074/0071         2001 G IDF  90.0  2 BLAH  0.95")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("082F074/0071", 2001)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(90.0f))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.YOUNG))));
		assertThat(poly, hasProperty("nonproductiveDescription", present(is("BLAH"))));
		assertThat(poly, hasProperty("yieldFactor", is(0.95f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonWithBlanks() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("082F074/0071         2001 G IDF                    ")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("082F074/0071", 2001)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParseMultiple() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream(
						"01002 S000001 00     1970 A CWH                1.00", //
						"01002 S000002 00     1970 A CWH                1.00", //
						"01002 S000003 00     1970 A CWH                1.00", //
						"01002 S000004 00     1970 A CWH                1.00", //
						"01003AS000001 00     1953 B CWH                1.00", //
						"01003AS000003 00     1953 B CWH                1.00", //
						"01004 S000002 00     1953 B CWH                1.00", //
						"01004 S000036 00     1957 B CWH                1.00", //
						"01004 S000037 00     1957 B CWH                1.00", //
						"01004 S000038 00     1957 B CWH                1.00" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000002 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000003 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000004 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01003AS000001 00", 1953)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01003AS000003 00", 1953)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01004 S000002 00", 1953)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01004 S000036 00", 1957)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01004 S000037 00", 1957)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01004 S000038 00", 1957)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonZeroAsDefault() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("01002 S000001 00     1970 A CWH   0.0  0       0.00")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonNegativeAsDefault() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("01002 S000001 00     1970 A CWH  -1.0         -1.00")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(85f))));
		assertThat(poly, hasProperty("mode", notPresent()));
		assertThat(poly, hasProperty("nonproductiveDescription", notPresent()));
		assertThat(poly, hasProperty("yieldFactor", is(1.0f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonSetPercentAvailableDefaultInBatcMode() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("082F074/0071         2001 G IDF        4 BLAH  0.95")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("082F074/0071", 2001)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(poly, hasProperty("percentAvailable", notPresent()));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.BATC))));
		assertThat(poly, hasProperty("nonproductiveDescription", present(is("BLAH"))));
		assertThat(poly, hasProperty("yieldFactor", is(0.95f)));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonDontOverridePercentAvailableInBatcMode() throws Exception {

		var parser = new VriPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.VRI_INPUT_YIELD_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream("082F074/0071         2001 G IDF  90.0  4 BLAH  0.95")
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.VRI_INPUT_YIELD_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VriPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("polygonIdentifier", isPolyId("082F074/0071", 2001)));
		assertThat(poly, hasProperty("forestInventoryZone", is(" ")));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(poly, hasProperty("percentAvailable", present(is(90.0f))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.BATC))));
		assertThat(poly, hasProperty("nonproductiveDescription", present(is("BLAH"))));
		assertThat(poly, hasProperty("yieldFactor", is(0.95f)));

		VdypMatchers.assertEmpty(stream);
	}

}
