package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/** test ParameterNamesTest for Sonar coverage purposes only */
public class ParameterNamesTest {

	@Test
	void test() {
		Assert.assertEquals("SCSV-History", ParameterNames.SCSV_HISTORY_INPUT_DATA);
	}
}
