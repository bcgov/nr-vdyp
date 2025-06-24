package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ValidationMessageTest {

	@Test
	void objectMethodTests() {
		var m1 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_HIGH, "1", "field", "0");
		var m2 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW, "1", "field", "2");
		var m3 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW);
		var m4 = new ValidationMessage(ValidationMessageKind.INTEGER_VALUE_TOO_LOW, "2", "field");

		assertEquals(m1, m1);
		assertTrue(m1.compareTo(m2) < 0);
		assertTrue(m2.compareTo(m3) > 0);
		assertTrue(m3.compareTo(m4) < 0);
	}
}
