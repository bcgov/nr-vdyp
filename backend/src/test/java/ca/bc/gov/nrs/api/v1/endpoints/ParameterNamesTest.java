package ca.bc.gov.nrs.api.v1.endpoints;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.v1.gen.api.impl.ParameterNames;

/** test ParameterNamesTest for Sonar coverage purposes only */
public class ParameterNamesTest {

	@Test
	void test() {
		Assert.assertEquals("historyInputData", ParameterNames.HISTORY_INPUT_DATA);
	}
}
