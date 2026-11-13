package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class HcsvLineFilterTest {

	private static final String[] HEADER_LINE = { "FEATURE_ID", "TREE_COVER_LAYER_ESTIMATED_ID", "MAP_ID" };
	private static final String[] DATA_LINE = { "13919428", "14321067", "093C090" };
	private static final String[] BLANK_LINE = { "", " ", "" };
	private static final String[] UNSET_FEATURE_ID_LINE = { "-9", "14321067", "093C090" };

	@Test
	void testHeaderLineFilteredWhenSeenFirst() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(HEADER_LINE), is(false));
		assertThat(filter.allowLine(DATA_LINE), is(true));
	}

	@Test
	void testHeaderLineFilteredWhenSeenAfterDataLine() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(DATA_LINE), is(true));
		assertThat(filter.allowLine(HEADER_LINE), is(false));
	}

	@Test
	void testHeaderLineWithSurroundingWhitespaceFiltered() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(new String[] { " FEATURE_ID ", "MAP_ID" }), is(false));
	}

	@Test
	void testDataLinesAllowedWhenNoHeaderPresent() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(DATA_LINE), is(true));
		assertThat(filter.allowLine(DATA_LINE), is(true));
	}

	@Test
	void testBlankLineFiltered() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(BLANK_LINE), is(false));
		assertThat(filter.allowLine(new String[0]), is(false));
	}

	@Test
	void testUnsetFeatureIdLineFiltered() {
		var filter = new HcsvLineFilter(true, true);

		assertThat(filter.allowLine(UNSET_FEATURE_ID_LINE), is(false));
	}
}
