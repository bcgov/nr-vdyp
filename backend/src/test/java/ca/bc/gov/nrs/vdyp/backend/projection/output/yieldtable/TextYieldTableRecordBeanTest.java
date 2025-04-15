package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes;

public class TextYieldTableRecordBeanTest {

	@Test
	void testLoading() {
		YieldTableRowBean b = new TextYieldTableRowValuesBean();

		double nextSetValue = 0.0;
		String nextCodeValue = "a";
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					switch (s) {
					case Code:
						for (var i = 1; i <= 6; i++) {
							b.setSpeciesFieldValue(p, i, s, nextCodeValue);
							nextCodeValue = new String(new byte[] { (byte) (nextCodeValue.charAt(0) + 1) });
						}
						break;
					default:
						for (var i = 1; i <= 6; i++) {
							b.setSpeciesFieldValue(p, i, s, nextSetValue);
							nextSetValue += 1;
						}
						break;
					}
				} else {
					var setSuccessfully = false;
					try {
						b.setSpeciesFieldValue(p, 1, s, "");
						setSuccessfully = true;
					} catch (IllegalArgumentException e) {
						// expected
					}
					Assert.assertFalse(setSuccessfully);
				}
			}
		}

		double nextGetDoubleValue = 0;
		String nextGetStringValue = "a";
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					switch (s) {
					case Code:
						for (var i = 1; i <= 6; i++) {
							Assert.assertEquals(nextGetStringValue, b.getSpeciesFieldValue(p, i, s));
							nextGetStringValue = new String(new byte[] { (byte) (nextGetStringValue.charAt(0) + 1) });
						}
						break;
					default:
						for (var i = 1; i <= 6; i++) {
							String expectedValue = FieldFormatter.format(nextGetDoubleValue);
							Assert.assertEquals(expectedValue, b.getSpeciesFieldValue(p, i, s));
							nextGetDoubleValue += 1;
						}
						break;
					}
				} else {
					var setSuccessfully = false;
					try {
						b.getSpeciesFieldValue(p, 1, s);
						setSuccessfully = true;
					} catch (IllegalArgumentException e) {
						// expected
					}
					Assert.assertFalse(setSuccessfully);
				}
			}
		}
	}
}
