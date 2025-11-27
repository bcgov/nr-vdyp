package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DebugSettingsTest {

	@Test
	void testNullArray() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(null);
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
	}

	@Test
	void testEmptyArray() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(new Integer[0]);
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
	}

	@Test
	void testSizeOneArray() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(new Integer[] { 43 });
		assertThat("Entry " + 1, ds.getValue(1), is(43));
		for (int i = 2; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
	}

	@SuppressWarnings("rawtypes")
	@Test
	void testTooLargeArray() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new DebugSettings(new Integer[DebugSettings.MAX_DEBUG_SETTINGS + 1])
		);
	}

	@Test
	void testValuesRecordedCorrectly() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
		for (int i = 0; i < 10; i++) {
			assertThat(ds.getValue(i + 1), is(i + 1));
		}
	}

	@Test
	void testDefaultCorrectly() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(new Integer[] { 1 });
		assertThat(ds.getValue(1), is(1));
		for (int i = 2; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat(ds.getValue(i), is(0));
		}
	}

	@Test
	void testOutOfBoundsRequest() {
		@SuppressWarnings("rawtypes")
		DebugSettings ds = new DebugSettings(new Integer[] { 1 });
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
	}
}
