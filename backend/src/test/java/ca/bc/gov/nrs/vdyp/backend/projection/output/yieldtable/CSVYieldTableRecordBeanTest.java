package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.CSVYieldTableRecordBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.CSVYieldTableRecordBean.MultiFieldSuffixes;

public class CSVYieldTableRecordBeanTest {

	@Test
	void testLoading() {
		CSVYieldTableRecordBean b = new CSVYieldTableRecordBean();

		int nextSetValue = 0;
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (CSVYieldTableRecordBean.isValidPrefixSuffixPair(p, s)) {
					for (var i = 1; i <= 6; i++) {
						b.setSpeciesField(p, i, s, Integer.valueOf(nextSetValue).toString());
					}
				} else {
					try {
						b.setSpeciesField(p, 1, s, "");
						Assert.fail();
					} catch (AssertionError e) {
						// expected
					}
				}
			}
		}

		int nextGetValue = 0;
		for (var p : MultiFieldPrefixes.values()) {
			for (var s : MultiFieldSuffixes.values()) {
				if (CSVYieldTableRecordBean.isValidPrefixSuffixPair(p, s)) {
					for (var i = 1; i <= 6; i++) {
						Assert.assertEquals(Integer.valueOf(nextGetValue).toString(), b.getSpeciesField(p, i, s));
					}
				} else {
					try {
						b.getSpeciesField(p, 1, s);
						Assert.fail();
					} catch (AssertionError e) {
						// expected
					}
				}
			}
		}
	}
}
