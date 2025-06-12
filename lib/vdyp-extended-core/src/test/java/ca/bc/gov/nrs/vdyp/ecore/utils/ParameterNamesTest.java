package ca.bc.gov.nrs.vdyp.ecore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** test ParameterNamesTest for Sonar coverage purposes only */
public class ParameterNamesTest {

	@Test
	void test() {
		assertEquals("SCSV-History", ParameterNames.SCSV_HISTORY_INPUT_DATA);
	}
}
