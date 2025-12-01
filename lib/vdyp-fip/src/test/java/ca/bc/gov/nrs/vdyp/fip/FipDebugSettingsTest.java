package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.fip.model.FipDebugSettings;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.DebugSettings.Math77MessagesLevel;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class FipDebugSettingsTest {

	public static List<Arguments> math77MessagesLevel() {
		return List.of(
				Arguments.of(0, Math77MessagesLevel.NONE), Arguments.of(1, Math77MessagesLevel.SOME),
				Arguments.of(2, Math77MessagesLevel.ALL),

				Arguments.of(-1, Math77MessagesLevel.NONE), Arguments.of(3, Math77MessagesLevel.NONE),
				Arguments.of(Integer.MAX_VALUE, Math77MessagesLevel.NONE),
				Arguments.of(Integer.MIN_VALUE, Math77MessagesLevel.NONE)
		);
	}

	/*
	 * In FipStart use debug setting 9 instead of 5 based on inline docs in FIPSTART.CTL
	 */
	@ParameterizedTest
	@MethodSource("math77MessagesLevel")
	void testGetMath77Messages(int index, Math77MessagesLevel expected) {
		DebugSettings ds = TestUtils.debugSettingsSingle(FipDebugSettings.class, 9, index);
		assertThat(ds.getMath77MessagesLevel(), is(expected));
	}

}
