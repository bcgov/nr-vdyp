package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParserTest;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class FipPolygonParserTest {

	@Test
	public void testParseEmpty() throws Exception {

		var parser = new FipPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(FipPolygonParser.CONTROL_KEY, "test.dat");
		BecDefinitionParserTest.populateControlMapReal(controlMap);

		var fileResolver = TestUtils.fileResolver("test.dat", TestUtils.makeStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(FipPolygonParser.CONTROL_KEY);

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<FipPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	public void testParsePolygon() throws Exception {

		var parser = new FipPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(FipPolygonParser.CONTROL_KEY, "test.dat");
		BecDefinitionParserTest.populateControlMapReal(controlMap);

		// Polygon ID FIZ BEC PctF mode ND Yld
		// AAAAAAAAAAAAAAAAAAAAAAAAA A AAAA FFFF II AAAAAFFFFF
		var fileResolver = TestUtils
				.fileResolver("test.dat", TestUtils.makeStream("Test Polygon              1 CWH  90.0  1 BLAH  0.95"));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(FipPolygonParser.CONTROL_KEY);

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<FipPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertEmpty(stream);
	}

	@Test
	public void testParsePolygonWithBlanks() throws Exception {

		var parser = new FipPolygonParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(FipPolygonParser.CONTROL_KEY, "test.dat");
		BecDefinitionParserTest.populateControlMapReal(controlMap);

		// Polygon ID FIZ BEC PctF mode ND Yld
		// AAAAAAAAAAAAAAAAAAAAAAAAA A AAAA FFFF II AAAAAFFFFF
		var fileResolver = TestUtils
				.fileResolver("test.dat", TestUtils.makeStream("01002 S000001 00     1970 A CWH                1.00"));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(FipPolygonParser.CONTROL_KEY);

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<FipPolygon>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var poly = assertNext(stream);

		assertEmpty(stream);
	}

	static <T> T assertNext(StreamingParser<T> stream) throws IOException, ResourceParseException {
		assertThat(stream, hasNext(true));
		var next = assertDoesNotThrow(() -> stream.next());
		assertThat(next, notNullValue());
		return next;
	}

	static <T> void assertEmpty(StreamingParser<T> stream) throws IOException, ResourceParseException {
		assertThat(stream, hasNext(false));
		assertThrows(IllegalStateException.class, () -> stream.next());
	}

	static <T> Matcher<StreamingParser<T>> hasNext(boolean value) {
		return new TypeSafeDiagnosingMatcher<StreamingParser<T>>() {

			@Override
			public void describeTo(Description description) {

				description.appendText("StreamingParser with hasNext() ").appendValue(value);

			}

			@Override
			protected boolean matchesSafely(StreamingParser<T> item, Description mismatchDescription) {
				try {
					var hasNext = item.hasNext();
					if (hasNext == value) {
						return true;
					}
					mismatchDescription.appendText("hasNext() returned ").appendValue(hasNext);
					return false;
				} catch (IOException | ResourceParseException e) {
					mismatchDescription.appendText("hasNext() threw ").appendValue(e.getMessage());
					return false;
				}
			}

		};
	}
}
