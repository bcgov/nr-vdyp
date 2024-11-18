package ca.bc.gov.nrs.vdyp.io.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.coe.CompVarAdjustmentsParser;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

/**
 * See {@link BasalAreaGrowthFiatParserTest} for other tests, the two parsers being identical.
 *
 * @author Michael Junkin, Vivid Solutions
 */
class CompVarAdjustmentsParserTest {

	@Test
	void testParseSimple() throws Exception {

		var parser = new CompVarAdjustmentsParser();

		Map<String, Object> controlMap = new HashMap<>();

		var is = TestUtils.makeInputStream(
				" 01    0.50   Small utl class, BA   CVADJ.prm  Comp Var adjustment & related parms", //
				" 02    0.51                    DQ", //
				" 03    0.52                    HL", //
				" 04    0.53                    VOL", //
				" 05    0.54                    BA Adj, Utl Class 1 (7.5-12.5 cm)", //
				" 06    0.55                            Utl Class 2", //
				" 07    0.56                            Utl Class 3", //
				" 08    0.57                            Utl Class 4 (22.5+ cm)", //
				" 15    0.58                    DQ Adj, Utl Class 1", //
				" 16    0.59                            Utl Class 2", //
				" 17    0.60                            Utl Class 3", //
				" 18    0.61                            Utl Class 4", //
				" 11    0.91   Util Class 1 ( 7.5-12.5), Whole-stem vol ", //
				" 12    0.92                             Close Util Vol", //
				" 13    0.93                             Close U, less decay", //
				" 14    0.94                             Close U, less waste and decay", //
				" 21    0.92   Util Class 2 (12.5-17.5), Whole-stem volume ", //
				" 22    0.93                             ", //
				" 23    0.94                             ", //
				" 24    0.95                             ", //
				" 31    0.93   Util Class 3 (17.5-22.5), Whole-stem volume ", //
				" 32    0.94                             ", //
				" 33    0.95                             ", //
				" 34    0.96                             ", //
				" 41    0.94   Util Class 4 (22.5+    ), Whole-stem volume ", //
				" 42    0.95                             Close Util vol", //
				" 43    0.96                             Close U vol, less decay", //
				" 44    0.97                             Close U vol, less decay and waste", //
				" 51    0.80  Lorey height, primary species", //
				" 52    0.81  Lorey height, other species", //
				"999                 End of usuable info" //
		);

		TestUtils.populateControlMapFromStream(controlMap, parser, is);

		CompVarAdjustments m = (CompVarAdjustments) controlMap.get(ControlKey.PARAM_ADJUSTMENTS.name());

		assertThat(m.getValue(UtilizationClass.SMALL, UtilizationClassVariable.BASAL_AREA), is(0.50f));
		assertThat(m.getValue(UtilizationClass.SMALL, UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.51f));
		assertThat(m.getValue(UtilizationClass.SMALL, UtilizationClassVariable.LOREY_HEIGHT), is(0.52f));
		assertThat(m.getValue(UtilizationClass.SMALL, UtilizationClassVariable.WHOLE_STEM_VOL), is(0.53f));
		assertThat(m.getLoreyHeightPrimaryParam(), is(0.80f));
		assertThat(m.getLoreyHeightOther(), is(0.81f));

		assertThat(m.getValue(UtilizationClass.U75TO125, UtilizationClassVariable.BASAL_AREA), is(0.54f));
		assertThat(m.getValue(UtilizationClass.U125TO175, UtilizationClassVariable.BASAL_AREA), is(0.55f));
		assertThat(m.getValue(UtilizationClass.U175TO225, UtilizationClassVariable.BASAL_AREA), is(0.56f));
		assertThat(m.getValue(UtilizationClass.OVER225, UtilizationClassVariable.BASAL_AREA), is(0.57f));

		assertThat(m.getValue(UtilizationClass.U75TO125, UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.58f));
		assertThat(m.getValue(UtilizationClass.U125TO175, UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.59f));
		assertThat(m.getValue(UtilizationClass.U175TO225, UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.60f));
		assertThat(m.getValue(UtilizationClass.OVER225, UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.61f));

		assertThrows(
				IllegalArgumentException.class,
				() -> m.getValue(UtilizationClass.OVER225, UtilizationClassVariable.LOREY_HEIGHT)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> m.getValue(UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> m.getValue(UtilizationClass.ALL, UtilizationClassVariable.BASAL_AREA)
		);

		assertThat(m.getVolumeValue(UtilizationClass.U75TO125, UtilizationClassVariable.WHOLE_STEM_VOL), is(0.91f));
		assertThat(m.getVolumeValue(UtilizationClass.U125TO175, UtilizationClassVariable.WHOLE_STEM_VOL), is(0.92f));
		assertThat(m.getVolumeValue(UtilizationClass.U175TO225, UtilizationClassVariable.WHOLE_STEM_VOL), is(0.93f));
		assertThat(m.getVolumeValue(UtilizationClass.OVER225, UtilizationClassVariable.WHOLE_STEM_VOL), is(0.94f));

		assertThat(m.getVolumeValue(UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL), is(0.92f));
		assertThat(m.getVolumeValue(UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL), is(0.93f));
		assertThat(m.getVolumeValue(UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL), is(0.94f));
		assertThat(m.getVolumeValue(UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL), is(0.95f));

		assertThat(
				m.getVolumeValue(UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY),
				is(0.93f)
		);
		assertThat(
				m.getVolumeValue(UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY),
				is(0.94f)
		);
		assertThat(
				m.getVolumeValue(UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY),
				is(0.95f)
		);
		assertThat(
				m.getVolumeValue(UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY),
				is(0.96f)
		);

		assertThat(
				m.getVolumeValue(
						UtilizationClass.U75TO125, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
				), is(0.94f)
		);
		assertThat(
				m.getVolumeValue(
						UtilizationClass.U125TO175, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
				), is(0.95f)
		);
		assertThat(
				m.getVolumeValue(
						UtilizationClass.U175TO225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
				), is(0.96f)
		);
		assertThat(
				m.getVolumeValue(
						UtilizationClass.OVER225, UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
				), is(0.97f)
		);
	}

	@Test
	void testParseBadIndex() throws Exception {

		var parser = new CompVarAdjustmentsParser();

		Map<String, Object> controlMap = new HashMap<>();

		var is1 = TestUtils.makeInputStream(" 99    0.50");

		try {
			parser.modify(controlMap, is1);
			Assertions.fail();
		} catch (Exception e) {
			assertThat(e, hasProperty("message", is("Error at line 1: Index 99 not in the range 1 to 98 inclusive")));
		}

		var is2 = TestUtils.makeInputStream("  0    0.50");

		try {
			parser.modify(controlMap, is2);
			Assertions.fail();
		} catch (Exception e) {
			assertThat(e, hasProperty("message", is("Error at line 1: Index 0 not in the range 1 to 98 inclusive")));
		}
	}
}
