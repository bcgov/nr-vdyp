package ca.bc.gov.nrs.vdyp.ecore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CsvRecordBeanHelperTest {

	@Test
	void parseLongAcceptNull_nullValue_returnsNull() {
		assertNull(CsvRecordBeanHelper.parseLongAcceptNull(null));
	}

	@Test
	void parseLongAcceptNull_standardInteger_returnsLong() {
		assertEquals(23000000L, CsvRecordBeanHelper.parseLongAcceptNull("23000000"));
	}

	@Test
	void parseLongAcceptNull_eNotationLowerCase_returnsLong() {
		assertEquals(23000000L, CsvRecordBeanHelper.parseLongAcceptNull("2.3e+07"));
	}

	@Test
	void parseLongAcceptNull_eNotationUpperCase_returnsLong() {
		assertEquals(23000000L, CsvRecordBeanHelper.parseLongAcceptNull("2.3E+07"));
	}

	@Test
	void parseLongAcceptNull_invalidValue_throwsNumberFormatException() {
		assertThrows(NumberFormatException.class, () -> CsvRecordBeanHelper.parseLongAcceptNull("abc"));
	}
}
