package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.Matchers.is;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

class ForwardDebugSettingsTest {

	@Test
	void testNoSpecialActions() {
		ForwardDebugSettings fs = new ForwardDebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.SPECIES_DYNAMICS), is(1));
		MatcherAssert.assertThat(fs.getSpeciesDynamics(), is(ForwardDebugSettings.SpeciesDynamics.NONE));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.MAX_BREAST_HEIGHT_AGE), is(2));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.BA_GROWTH_MODEL), is(3));
		MatcherAssert.assertThat(fs.getValue(DebugSettings.UPPER_BOUNDS_MODE), is(4));
		MatcherAssert.assertThat(fs.getValue(DebugSettings.MATH77_MESSAGE_LEVEL), is(5));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.DQ_GROWTH_MODEL), is(6));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.LOREY_HEIGHT_CHANGE_STRATEGY), is(8));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.DO_LIMIT_BA_WHEN_DQ_LIMITED), is(9));

		MatcherAssert.assertThat(fs.getFillInValues(), is(new Integer[0]));
	}

	@Test
	void testOneSpecialAction() {
		ForwardDebugSettings fs = new ForwardDebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 0 });

		MatcherAssert.assertThat(fs.getFillInValues(), is(new Integer[] { 12 }));
	}

	@Test
	void testAllSpecialActions() {
		ForwardDebugSettings fs = new ForwardDebugSettings(
				new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
						111, 112, 113, 114 }
		);

		MatcherAssert.assertThat(
				fs.getFillInValues(),
				is(new Integer[] { 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114 })
		);
	}

	@Test
	void testAllSomeActions() {
		ForwardDebugSettings fs = new ForwardDebugSettings(
				new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100, 101, 102, 103, 104, 105, 106, 107 }
		);

		MatcherAssert.assertThat(fs.getFillInValues(), is(new Integer[] { 100, 101, 102, 103, 104, 105, 106, 107 }));
	}
}
