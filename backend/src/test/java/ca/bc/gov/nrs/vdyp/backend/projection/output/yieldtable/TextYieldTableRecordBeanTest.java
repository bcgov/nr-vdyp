package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowValues.MultiFieldSuffixes;

public class TextYieldTableRecordBeanTest {

	@Test
	void testLoading() {
		YieldTableRowValues b = new TextYieldTableRowValuesBean();

		int nextSetValue = 0;
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					for (var i = 1; i <= 6; i++) {
						b.setSpeciesFieldValue(p, i, s, Integer.valueOf(nextSetValue).toString());
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

		int nextGetValue = 0;
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (b.isValidPrefixSuffixPair(p, s)) {
					for (var i = 1; i <= 6; i++) {
						Assert.assertEquals(Integer.valueOf(nextGetValue).toString(), b.getSpeciesFieldValue(p, i, s));
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
