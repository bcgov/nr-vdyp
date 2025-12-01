package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.application.test.TestDebugSettings;
import ca.bc.gov.nrs.vdyp.model.DebugSettings.Math77MessagesLevel;
import ca.bc.gov.nrs.vdyp.model.DebugSettings.SpeciesGroupPreference;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class DebugSettingsTest {

	@Test
	void testNullArray() {
		DebugSettings ds = new DebugSettings(null);
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
	}

	@Test
	void testEmptyArray() {
		DebugSettings ds = new DebugSettings(new Integer[0]);
		for (int i = 1; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
	}

	@Test
	void testSizeOneArray() {
		DebugSettings ds = new DebugSettings(new Integer[] { 43 });
		assertThat("Entry " + 1, ds.getValue(1), is(43));
		for (int i = 2; i <= DebugSettings.MAX_DEBUG_SETTINGS; i++) {
			assertThat("Entry " + i, ds.getValue(i), is(0));
		}
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(DebugSettings.MAX_DEBUG_SETTINGS + 1));
		assertThrows(IllegalArgumentException.class, () -> ds.getValue(0));
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
	void testSetValue() {
		DebugSettings ds = new DebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
		ds.setValue(3, 42);
		for (int i = 0; i < 10; i++) {
			if (i + 1 == 3) {
				assertThat(ds.getValue(i + 1), is(42));
			} else {
				assertThat(ds.getValue(i + 1), is(i + 1));
			}
		}

	}

	@Test
	void testSetOutOfBounds() {
		DebugSettings ds = new DebugSettings(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
		assertThrows(IllegalArgumentException.class, () -> ds.setValue(0, 42));
		assertThrows(IllegalArgumentException.class, () -> ds.setValue(-1, 42));
		assertThrows(IllegalArgumentException.class, () -> ds.setValue(DebugSettings.MAX_DEBUG_SETTINGS + 1, 42));
		for (int i = 0; i < 10; i++) {
			assertThat(ds.getValue(i + 1), is(i + 1));
		}

	}

	public static List<Arguments> speciesGroupPreferences() {
		return List.of(
				Arguments.of(0, SpeciesGroupPreference.DEFAULT),
				Arguments.of(1, SpeciesGroupPreference.USE_PREFERRED_WITHIN_TOLERANCE),

				Arguments.of(-1, SpeciesGroupPreference.DEFAULT), Arguments.of(2, SpeciesGroupPreference.DEFAULT),
				Arguments.of(Integer.MAX_VALUE, SpeciesGroupPreference.DEFAULT),
				Arguments.of(Integer.MIN_VALUE, SpeciesGroupPreference.DEFAULT)
		);
	}

	@ParameterizedTest
	@MethodSource("speciesGroupPreferences")
	void testGetSpeciesGroupPreference(int index, SpeciesGroupPreference expected) {
		DebugSettings ds = TestUtils.debugSettingsSingle(TestDebugSettings.class, 22, index);
		assertThat(ds.getSpeciesGroupPreference(), is(expected));
	}

	public static List<Arguments> math77MessagesLevel() {
		return List.of(
				Arguments.of(0, Math77MessagesLevel.NONE), Arguments.of(1, Math77MessagesLevel.SOME),
				Arguments.of(2, Math77MessagesLevel.ALL),

				Arguments.of(-1, Math77MessagesLevel.NONE), Arguments.of(3, Math77MessagesLevel.NONE),
				Arguments.of(Integer.MAX_VALUE, Math77MessagesLevel.NONE),
				Arguments.of(Integer.MIN_VALUE, Math77MessagesLevel.NONE)
		);
	}

	@ParameterizedTest
	@MethodSource("math77MessagesLevel")
	void testGetMath77Messages(int index, Math77MessagesLevel expected) {
		DebugSettings ds = TestUtils.debugSettingsSingle(TestDebugSettings.class, 5, index);
		assertThat(ds.getMath77MessagesLevel(), is(expected));
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
