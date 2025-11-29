package ca.bc.gov.nrs.vdyp.io.parse.projection;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isPolyId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class VdypPolygonDescriptionParserTest {

	@Test
	void testParse() throws ResourceParseException {
		PolygonIdentifier result = VdypPolygonDescriptionParser.parse("Test                 2025");
		assertThat(result, isPolyId("Test", 2025));
	}

	@Test
	void testParseStreamNoEntries() throws ResourceParseException, IOException {
		var is = TestUtils.makeInputStream();
		var parser = new VdypPolygonDescriptionParser();
		var fileName = "test.dat";
		var resolver = TestUtils.fileResolver(fileName, is);
		var control = TestUtils.loadControlMap();

		var stream = parser.map(fileName, resolver, control).get();

		assertFalse(stream.hasNext(), "Has first entry");
	}

	@Test
	void testParseStreamOneEntry() throws ResourceParseException, IOException {
		var is = TestUtils.makeInputStream("Test                 2025");
		var parser = new VdypPolygonDescriptionParser();
		var fileName = "test.dat";
		var resolver = TestUtils.fileResolver(fileName, is);
		var control = TestUtils.loadControlMap();

		var stream = parser.map(fileName, resolver, control).get();

		assertTrue(stream.hasNext(), "Doesn't have first entry");
		var result1 = stream.next();
		assertThat(result1, isPolyId("Test", 2025));
		assertFalse(stream.hasNext(), "Has second entry");
	}

	@Test
	void testParseStreamMultipleEntries() throws ResourceParseException, IOException {
		var is = TestUtils.makeInputStream("Test1                2025", "Test2                2027");
		var parser = new VdypPolygonDescriptionParser();
		var fileName = "test.dat";
		var resolver = TestUtils.fileResolver(fileName, is);
		var control = TestUtils.loadControlMap();

		var stream = parser.map(fileName, resolver, control).get();

		assertTrue(stream.hasNext(), "Doesn't have first entry");
		var result1 = stream.next();
		assertThat(result1, isPolyId("Test1", 2025));
		assertTrue(stream.hasNext(), "Doesn't have second entry");
		var result2 = stream.next();
		assertThat(result2, isPolyId("Test2", 2027));
		assertFalse(stream.hasNext(), "Has third entry");
	}
}
