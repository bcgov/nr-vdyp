package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.fip.model.FipDebugSettings;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class FipDebugSettingsTest {

	static List<Arguments> standardBoolean() {
		return List.of(
				Arguments.of(0, false), Arguments.of(1, true), Arguments.of(2, true), Arguments.of(-1, true),
				Arguments.of(Integer.MIN_VALUE, true), Arguments.of(Integer.MAX_VALUE, true)
		);
	}

	@ParameterizedTest
	@MethodSource("standardBoolean")
	void testNoBasalAreaLimit(int value, boolean expected) {
		FipDebugSettings unit = TestUtils
				.debugSettingsSingle(FipDebugSettings.class, FipDebugSettings.NO_BA_LIMIT, value);
		assertThat(unit, hasProperty("noBasalAreaLimit", is(expected)));
	}

	@ParameterizedTest
	@MethodSource("standardBoolean")
	void testNoQuadraticMeanDiameterLimit(int value, boolean expected) {
		FipDebugSettings unit = TestUtils
				.debugSettingsSingle(FipDebugSettings.class, FipDebugSettings.NO_DQ_LIMIT, value);
		assertThat(unit, hasProperty("noQuadraticMeanDiameterLimit", is(expected)));
	}

}
