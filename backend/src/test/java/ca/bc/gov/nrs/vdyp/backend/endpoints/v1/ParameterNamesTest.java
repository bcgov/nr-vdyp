package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;

/** test ParameterNamesTest for Sonar coverage purposes only */
public class ParameterNamesTest {

	@Test
	void test() {
		Assert.assertEquals("historyInputData", ParameterNames.HISTORY_INPUT_DATA);
	}
}
