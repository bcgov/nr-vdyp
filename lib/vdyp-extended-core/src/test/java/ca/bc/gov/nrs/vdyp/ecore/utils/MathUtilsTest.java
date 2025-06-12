package ca.bc.gov.nrs.vdyp.ecore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.projection.input.BeanValidatorHelper;

public class MathUtilsTest {

	@Test
	public void testRound() {

		assertNull(BeanValidatorHelper.round((Double) null, 0));
		assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10d, 0));
		assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10.0, 0));
		assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10.1, 0));
		assertEquals(Double.valueOf(10.1), BeanValidatorHelper.round(10.11, 1));
		assertEquals(Double.valueOf(0.1), BeanValidatorHelper.round(0.11, 1));
		assertEquals(Double.valueOf(0.11), BeanValidatorHelper.round(0.11, 5));

		assertNull(BeanValidatorHelper.round((String) null, 0));
		assertEquals("10", BeanValidatorHelper.round("10", 0));
		assertEquals("10", BeanValidatorHelper.round("10.0", 0));
		assertEquals("10", BeanValidatorHelper.round("10.1", 0));
		assertEquals("10.1", BeanValidatorHelper.round("10.11", 1));
		assertEquals("0.1", BeanValidatorHelper.round("0.11", 1));
		assertEquals("0.11000", BeanValidatorHelper.round("0.11", 5));
		assertEquals("not a number", BeanValidatorHelper.round("not a number", 5));
	}
}
