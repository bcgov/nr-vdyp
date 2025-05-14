package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class DebugSettingsTest {

	@Test
	void testNullArray() {
		DebugSettings ds = new DebugSettings(null);
		assertThat(ds.settings, Matchers.notNullValue());
		assertTrue(ds.settings.length == DebugSettings.MAX_DEBUG_SETTINGS);
	}

	@Test
	void testEmptyArray() {
		DebugSettings ds = new DebugSettings(new Integer[0]);
		assertThat(ds.settings, Matchers.notNullValue());
		assertTrue(ds.settings.length == DebugSettings.MAX_DEBUG_SETTINGS);
	}

	@Test
	void testSizeOneArray() {
		DebugSettings ds = new DebugSettings(new Integer[] { 43 });
		assertThat(ds.settings, Matchers.notNullValue());
		assertTrue(ds.settings.length == DebugSettings.MAX_DEBUG_SETTINGS);
		assertThat(ds.settings[0], is(43));
	}

	@Test
	void testTooLargeArray() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new DebugSettings(new Integer[DebugSettings.MAX_DEBUG_SETTINGS + 1])
		);
	}

	@Test
	void testValuesRecordedCorrectly() {
		DebugSettings ds = new DebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
		for (int i = 0; i < 10; i++) {
			assertThat(ds.getValue(i + 1), is(i + 1));
		}
	}

	@Test
	void testDefaultCorrectly() {
		DebugSettings ds = new DebugSettings(new Integer[] { 1 });
		assertThat(ds.getValue(1), is(1));
		for (int i = 2; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat(ds.getValue(i), is(0));
		}
	}

	@Test
	void testOutOfBoundsRequest() {
		DebugSettings ds = new DebugSettings(new Integer[] { 1 });
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
	}
}
