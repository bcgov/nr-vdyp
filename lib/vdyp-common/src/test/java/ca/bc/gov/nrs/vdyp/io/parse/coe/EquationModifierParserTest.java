package ca.bc.gov.nrs.vdyp.io.parse.coe;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.test.TestUtils;

class EquationModifierParserTest {

	@Test
	void testParseSimple() throws Exception {

		var parser = new EquationModifierParser();

		var is = TestUtils.makeInputStream(" 25  18  26");

		Map<String, Object> controlMap = new HashMap<>();

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(is(26), 25, 18));
		
		// test defaults
		assertThat(result, mmHasEntry(is(EquationModifierParser.DEFAULT_VALUE), 12, 16));
	}

}
