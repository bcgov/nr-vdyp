package ca.bc.gov.nrs.vdyp.backend.utils;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.input.BeanValidatorHelper;

public class MathUtilsTest {
	
	@Test
	public void testRound() {

		Assert.assertNull(BeanValidatorHelper.round((Double)null, 0));
		Assert.assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10d, 0));
		Assert.assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10.0, 0));
		Assert.assertEquals(Double.valueOf(10), BeanValidatorHelper.round(10.1, 0));
		Assert.assertEquals(Double.valueOf(10.1), BeanValidatorHelper.round(10.11, 1));
		Assert.assertEquals(Double.valueOf(0.1), BeanValidatorHelper.round(0.11, 1));
		Assert.assertEquals(Double.valueOf(0.11), BeanValidatorHelper.round(0.11, 5));

		Assert.assertNull(BeanValidatorHelper.round((String)null, 0));
		Assert.assertEquals("10", BeanValidatorHelper.round("10", 0));
		Assert.assertEquals("10", BeanValidatorHelper.round("10.0", 0));
		Assert.assertEquals("10", BeanValidatorHelper.round("10.1", 0));
		Assert.assertEquals("10.1", BeanValidatorHelper.round("10.11", 1));
		Assert.assertEquals("0.1", BeanValidatorHelper.round("0.11", 1));
		Assert.assertEquals("0.11000", BeanValidatorHelper.round("0.11", 5));
		Assert.assertEquals("not a number", BeanValidatorHelper.round("not a number", 5));
	}
}
