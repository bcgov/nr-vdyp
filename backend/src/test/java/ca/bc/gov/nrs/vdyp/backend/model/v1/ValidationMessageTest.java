package ca.bc.gov.nrs.vdyp.backend.model.v1;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class ValidationMessageTest {

	@Test
	void objectMethodTests() {
		var m1 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_HIGH, "1", "field");
		var m2 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW, "1", "field");
		var m3 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW);
		var m4 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW, "2", "field");
		
		Assert.assertEquals(m1, m1);
		Assert.assertTrue(m1.compareTo(m2) > 0);
		Assert.assertTrue(m2.compareTo(m3) > 0);
		Assert.assertTrue(m3.compareTo(m4) < 0);
	}
}
