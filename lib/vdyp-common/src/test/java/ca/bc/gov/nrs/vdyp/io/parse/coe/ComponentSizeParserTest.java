package ca.bc.gov.nrs.vdyp.io.parse.coe;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseLineException;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ComponentSizeParserTest {

	@Test
	void testParseSimpleP1() throws Exception {

		var is = TestUtils.makeInputStream(
				"S1 C  49.4 153.3 0.726 3.647", //
				"S1 C  49.4 153.3 0.726 3.647", //
				"S2 I  49.4 153.3 0.726 3.647", //
				"S2 I  49.4 153.3 0.726 3.647"
		);

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenus(controlMap);

		var parser = new ComponentSizeParser();

		var result = parser.parse(is, controlMap);

		assertThat(
				result, mmHasEntry(is(new ComponentSizeLimits(49.4f, 153.3f, 0.726f, 3.647f)), "S1", Region.COASTAL)
		);
	}

	@Test
	void testParseBadSpecies() {

		var is = TestUtils.makeInputStream("SX C  49.4 153.3 0.726 3.647");

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenus(controlMap);

		var parser = new ComponentSizeParser();

		@SuppressWarnings("unused")
		var ex = assertThrows(ResourceParseLineException.class, () -> parser.parse(is, controlMap));

	}

	@Test
	void testParseBadRegion() {

		var is = TestUtils.makeInputStream("S1 X  49.4 153.3 0.726 3.647");

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenus(controlMap);

		var parser = new ComponentSizeParser();

		@SuppressWarnings("unused")
		var ex = assertThrows(ResourceParseLineException.class, () -> parser.parse(is, controlMap));

	}

}
