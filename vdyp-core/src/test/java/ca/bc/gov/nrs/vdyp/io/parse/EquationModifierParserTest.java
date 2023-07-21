package ca.bc.gov.nrs.vdyp.io.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.test.TestUtils;

class EquationModifierParserTest {

	@Test
	void testParseSimple() throws Exception {

		var parser = new EquationModifierParser();

		var is = TestUtils.makeStream(" 25  18  26");

		Map<String, Object> controlMap = new HashMap<>();

		var result = parser.parse(is, controlMap);

		assertThat(result, hasEntry(is(25), hasEntry(is(18), is(26))));
		// indexed
	}

}
