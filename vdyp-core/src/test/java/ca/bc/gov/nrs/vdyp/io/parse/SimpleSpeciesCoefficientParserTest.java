package ca.bc.gov.nrs.vdyp.io.parse;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class SimpleSpeciesCoefficientParserTest {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseSimpleP1() throws Exception {

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P1);

		var is = TestUtils.makeStream("S1 I   1.00160   0.20508-0.0013743");

		Map<String, Object> controlMap = new HashMap<>();

		SP0DefinitionParserTest.populateControlMap(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(is(1.00160f)), 1, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry(present(is(0.20508f)), 2, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry(present(is(-0.0013743f)), 3, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 4, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 0, "S1", Region.INTERIOR));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseSimpleP2() throws Exception {

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P2);

		var is = TestUtils.makeStream("S1 C   0.49722   1.18403");

		Map<String, Object> controlMap = new HashMap<>();

		SP0DefinitionParserTest.populateControlMap(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(is(0.49722f)), 1, "S1", Region.COASTAL));
		assertThat(result, mmHasEntry(present(is(1.18403f)), 2, "S1", Region.COASTAL));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 3, "S1", Region.COASTAL));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 0, "S1", Region.COASTAL));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseSimpleP3() throws Exception {

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P3);

		var is = TestUtils.makeStream("S1 I   1.04422   0.93010  -0.05745  -2.50000");

		Map<String, Object> controlMap = new HashMap<>();

		SP0DefinitionParserTest.populateControlMap(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(is(1.04422f)), 1, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry(present(is(0.93010f)), 2, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry(present(is(-0.05745f)), 3, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry(present(is(-2.50000f)), 4, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 5, "S1", Region.INTERIOR));
		assertThat(result, mmHasEntry((Matcher) notPresent(), 0, "S1", Region.INTERIOR));
	}

	@Test
	public void testParseBadSpecies() throws Exception {

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P1);

		var is = TestUtils.makeStream("SX I   1.00160   0.20508-0.0013743");

		Map<String, Object> controlMap = new HashMap<>();

		SP0DefinitionParserTest.populateControlMap(controlMap);

		var ex = assertThrows(ResourceParseLineException.class, () -> parser.parse(is, controlMap));

	}

	@Test
	public void testParseBadRegion() throws Exception {

		var parser = new HLCoefficientParser(HLCoefficientParser.NUM_COEFFICIENTS_P1);

		var is = TestUtils.makeStream("S1 X   1.00160   0.20508-0.0013743");

		Map<String, Object> controlMap = new HashMap<>();

		SP0DefinitionParserTest.populateControlMap(controlMap);

		var ex = assertThrows(ResourceParseLineException.class, () -> parser.parse(is, controlMap));

	}

}
