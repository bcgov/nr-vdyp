package ca.bc.gov.nrs.vdyp.io.parse.coe;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.coe;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseLineException;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class HLNonprimaryCoefficientParserTest {

	@Test
	void testParseSimple() throws Exception {

		var parser = new HLNonprimaryCoefficientParser();

		var is = TestUtils.makeInputStream("S1 S2 C 1   0.86323   1.00505");

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenus(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(coe(1, 0.86323f, 1.00505f)), "S1", "S2", Region.COASTAL));
	}

	@ParameterizedTest
	@ValueSource(strings = "SX S2 C 1   0.86323   1.00505, S1 SX C 1   0.86323   1.00505, S1 S2 X 1   0.86323   1.00505, S1 S2 C 1   0.86323")
	void testParseBadSpecies1(String line) throws Exception {

		var parser = new HLNonprimaryCoefficientParser();

		var is = TestUtils.makeInputStream(line);

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenus(controlMap);

		assertThrows(ResourceParseLineException.class, () -> parser.parse(is, controlMap));

	}

	@Test
	void testParseMultiple() throws Exception {

		var parser = new HLNonprimaryCoefficientParser();

		var is = TestUtils.makeInputStream("AC AT C 1   0.86323   1.00505", "AC  B C 1   4.44444   5.55555");

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenusReal(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(coe(1, 0.86323f, 1.00505f)), "AC", "AT", Region.COASTAL));
		assertThat(result, mmHasEntry(present(coe(1, 4.44444f, 5.55555f)), "AC", "B", Region.COASTAL));
	}

	@Test
	void testParseBlank() throws Exception {

		var parser = new HLNonprimaryCoefficientParser();

		var is = TestUtils.makeInputStream(
				"AC AT C 1   0.86323   1.00505", "      C 1   6.66666   7.77777", "AC  B C 1   4.44444   5.55555"
		);

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenusReal(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(coe(1, 0.86323f, 1.00505f)), "AC", "AT", Region.COASTAL));
		assertThat(result, mmHasEntry(present(coe(1, 4.44444f, 5.55555f)), "AC", "B", Region.COASTAL));
	}

	@Test
	void testParseEmpty() throws Exception {

		var parser = new HLNonprimaryCoefficientParser();

		var is = TestUtils.makeInputStream("AC AT C 1   0.86323   1.00505", "", "AC  B C 1   4.44444   5.55555");

		Map<String, Object> controlMap = new HashMap<>();

		TestUtils.populateControlMapGenusReal(controlMap);

		var result = parser.parse(is, controlMap);

		assertThat(result, mmHasEntry(present(coe(1, 0.86323f, 1.00505f)), "AC", "AT", Region.COASTAL));
		assertThat(result, mmHasEntry(present(coe(1, 4.44444f, 5.55555f)), "AC", "B", Region.COASTAL));
	}

}
