package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.Matchers.is;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;

class ForwardDebugSettingsTest {

	@Test
	void testNoSpecialActions() {
		ForwardDebugSettings fs = new ForwardDebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.SPECIES_DYNAMICS_1), is(1));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.MAX_BREAST_HEIGHT_AGE_2), is(2));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.BASAL_AREA_GROWTH_MODEL_3), is(3));
		MatcherAssert
				.assertThat(fs.getValue(ForwardDebugSettings.Vars.PER_SPECIES_AND_REGION_MAX_BREAST_HEIGHT_4), is(4));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.MESSAGING_LEVEL_5), is(5));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.DQ_GROWTH_MODEL_6), is(6));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.LOREY_HEIGHT_CHANGE_STRATEGY_8), is(8));
		MatcherAssert.assertThat(fs.getValue(ForwardDebugSettings.Vars.DO_LIMIT_BA_WHEN_DQ_LIMITED_9), is(9));

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
