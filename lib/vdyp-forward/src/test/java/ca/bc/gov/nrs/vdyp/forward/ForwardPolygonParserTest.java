package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertEmpty;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.assertNext;
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
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class ForwardPolygonParserTest {

	@Test
	void testParseEmpty() throws Exception {

		var parser = new VdypPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext("test.dat", TestUtils.makeInputStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	void testParsePolygon() throws Exception {

		var parser = new VdypPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils
				.fileResolverContext(
						"test.dat", TestUtils.makeInputStream("01002 S000001 00     1970 CWH  A    99 37  1  1")
				);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly.toString(), is("01002 S000001 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(99.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(37))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonWithBlanks() throws Exception {

		var parser = new VdypPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils
				.fileResolverContext("test.dat", TestUtils.makeInputStream("01002 S000001 00     1970 CWH  A    99"));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly.toString(), is("01002 S000001 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(99.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", notPresent()));
		assertThat(poly, hasProperty("mode", notPresent()));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParseMultiple() throws Exception {

		var parser = new VdypPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat", TestUtils.makeInputStream(
						"01002 S000001 00     1970 CWH  A    99 37  1  1", //
						"01002 S000002 00     1970 CWH  A    98 15 75  1", //
						"01002 S000003 00     1970 CWH  A    99 15 75  1", //
						"01002 S000004 00     1970 MH   A    99 15 75  1", //
						"01003AS000001 00     1953 CWH  B    91 11 31  2", //
						"01003AS000003 00     1953 SBS  B    92 11 31  1", //
						"01004 S000002 00     1953 CWH  B    96 11 31  1", //
						"01004 S000036 00     1957 MH   B    97 11 31", //
						"01004 S000037 00     1957 SBS  B    94 11", //
						"01004 S000038 00     1957 CWH  B    90 ", //
						"                          CWH  B    90 11 31  1" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream); // 1

		assertThat(poly.toString(), is("01002 S000001 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(99.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(37))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 2

		assertThat(poly.toString(), is("01002 S000002 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(98.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(15))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 3

		assertThat(poly.toString(), is("01002 S000003 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(99.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(15))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 4

		assertThat(poly.toString(), is("01002 S000004 00     1970"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1970))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("MH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("A")));
		assertThat(poly, hasProperty("percentAvailable", is(99.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(15))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 5

		assertThat(poly.toString(), is("01003AS000001 00     1953"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1953))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(91.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(11))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.YOUNG))));

		poly = assertNext(stream); // 6

		assertThat(poly.toString(), is("01003AS000003 00     1953"));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1953))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("SBS"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(92.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(11))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 7

		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("name", is("01004 S000002 00"))));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1953))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(96.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(11))));
		assertThat(poly, hasProperty("mode", present(is(PolygonMode.START))));

		poly = assertNext(stream); // 8

		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("name", is("01004 S000036 00"))));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1957))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("MH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(97.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(11))));
		assertThat(poly, hasProperty("mode", notPresent()));

		poly = assertNext(stream); // 9

		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("name", is("01004 S000037 00"))));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1957))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("SBS"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(94.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", present(is(11))));
		assertThat(poly, hasProperty("mode", notPresent()));

		poly = assertNext(stream); // 10

		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("name", is("01004 S000038 00"))));
		assertThat(poly, hasProperty("polygonIdentifier", hasProperty("year", is(1957))));
		assertThat(poly, hasProperty("biogeoclimaticZone", hasProperty("alias", is("CWH"))));
		assertThat(poly, hasProperty("forestInventoryZone", is("B")));
		assertThat(poly, hasProperty("percentAvailable", is(90.0f)));
		assertThat(poly, hasProperty("inventoryTypeGroup", notPresent()));
		assertThat(poly, hasProperty("mode", notPresent()));

		VdypMatchers.assertEmpty(stream);
	}

	@Test
	void testParsePolygonPercentForestLandDefaulting() throws Exception {

		var parser = new VdypPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_POLY.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 CWH  A   0.0", "01002 S000002 00     1970 CWH  A  -1.0"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertThat(poly, hasProperty("percentAvailable", is(90.0f)));

		poly = assertNext(stream);

		assertThat(poly, hasProperty("percentAvailable", is(90.0f)));

		VdypMatchers.assertEmpty(stream);
	}
}
