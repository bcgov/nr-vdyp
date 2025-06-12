package ca.bc.gov.nrs.vdyp.ecore.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency.FrequencyKind;

public class ParametersTest {

	@Test
	void testProgressFrequency() {

		assertNull(new ProgressFrequency().getIntValue());
		assertNull(new ProgressFrequency().getEnumValue());
		assertEquals(Integer.valueOf(12), new ProgressFrequency(12).getIntValue());
		assertNull(new ProgressFrequency(12).getEnumValue());
		assertNull(new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET).getIntValue());
		assertEquals(
				ProgressFrequency.FrequencyKind.MAPSHEET,
				new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET).getEnumValue()
		);

		assertThrows(IllegalArgumentException.class, () -> ProgressFrequency.FrequencyKind.fromValue("not a value"));

		ProgressFrequency pf1 = new ProgressFrequency(12);
		ProgressFrequency pf2 = new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);
		assertTrue(pf1.equals(pf1));
		assertEquals(Integer.valueOf(12).hashCode(), pf1.hashCode());
		assertEquals(ProgressFrequency.FrequencyKind.MAPSHEET.hashCode(), pf2.hashCode());
		assertEquals(17, new ProgressFrequency().hashCode());

		assertTrue(pf1.toString().contains("12"));
		assertTrue(pf2.toString().contains("mapsheet"));

		Parameters p1 = new Parameters();
		p1.progressFrequency(12);
		assertEquals("12", p1.getProgressFrequency());
		p1.progressFrequency("12");
		assertEquals("12", p1.getProgressFrequency());
		p1.progressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);
		assertEquals(FrequencyKind.MAPSHEET.getValue(), p1.getProgressFrequency());
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	void testUtilizationParameter() {
		assertEquals(
				"AL",
				new UtilizationParameter().speciesName("AL").utilizationClass(UtilizationClassSet._12_5)
						.getSpeciesName()
		);
		assertEquals(
				UtilizationClassSet._17_5.getValue(),
				new UtilizationParameter().speciesName("AL").utilizationClass(UtilizationClassSet._17_5)
						.getUtilizationClass()
		);

		assertThrows(IllegalArgumentException.class, () -> UtilizationClassSet.fromValue("ZZZ"));

		var up1 = new UtilizationParameter().speciesName("AL").utilizationClass(UtilizationClassSet._12_5);
		var up2 = new UtilizationParameter().speciesName("C").utilizationClass(UtilizationClassSet._12_5);
		var up3 = new UtilizationParameter().speciesName("C").utilizationClass(UtilizationClassSet._22_5);

		assertTrue(up1.equals(up1));
		assertEquals(up1.hashCode(), up1.hashCode());
		assertNotEquals(up2, up3);
		assertFalse("C".equals(up2));

		assertTrue(up1.toString().contains("speciesName: AL"));
		assertTrue(up1.toString().contains("utilizationClass: 12.5"));
	}

	@Test
	void testExecutionDebugOptions() {
		Parameters op = new Parameters();

		op.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED);
		assertEquals(1, op.getSelectedExecutionOptions().size());
		op.selectedExecutionOptions(
				Arrays.asList(ExecutionOption.BACK_GROW_ENABLED, ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION)
		);
		assertEquals(2, op.getSelectedExecutionOptions().size());

		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING.getValue());
		assertEquals(3, op.getSelectedExecutionOptions().size());

		op.setSelectedExecutionOptions(null);
		// Check that the null wiped out all the execution options
		assertTrue(op.getSelectedExecutionOptions().isEmpty());

		op.excludedExecutionOptions(
				Arrays.asList(ExecutionOption.BACK_GROW_ENABLED, ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION)
		);
		assertEquals(2, op.getExcludedExecutionOptions().size());
		assertTrue(op.getExcludedExecutionOptions().contains(ExecutionOption.BACK_GROW_ENABLED.getValue()));

		op.addExcludedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING);
		op.addExcludedExecutionOptionsItem(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING.getValue());
		assertEquals(4, op.getExcludedExecutionOptions().size());
		assertTrue(op.getExcludedExecutionOptions().contains(ExecutionOption.DO_ENABLE_DEBUG_LOGGING.getValue()));
		assertTrue(op.getExcludedExecutionOptions().contains(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING.getValue()));

		op.excludedExecutionOptions(null);
		assertTrue(op.getExcludedExecutionOptions().isEmpty());

		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS);
		assertEquals(1, op.getSelectedDebugOptions().size());
		op.selectedDebugOptions(
				Arrays.asList(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT, DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES)
		);
		assertEquals(2, op.getSelectedDebugOptions().size());

		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS.getValue());
		assertEquals(3, op.getSelectedDebugOptions().size());

		op.setSelectedDebugOptions(null);
		// Check that the null wiped out all the execution options
		assertNull(op.getSelectedDebugOptions());
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS);
		assertEquals(1, op.getSelectedDebugOptions().size());

		op.addExcludedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS.getValue());
		assertEquals(1, op.getExcludedDebugOptions().size());
		assertTrue(op.getExcludedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS.getValue()));

		op.excludedDebugOptions(
				Arrays.asList(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS, DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
		);
		assertEquals(2, op.getExcludedDebugOptions().size());
		assertTrue(op.getExcludedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS.getValue()));

		op.addExcludedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES);
		assertEquals(3, op.getExcludedDebugOptions().size());
		assertTrue(op.getExcludedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES.getValue()));

		op.excludedDebugOptions(null);
		assertNull(op.getExcludedDebugOptions());
		op.addExcludedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS);
		assertEquals(1, op.getExcludedDebugOptions().size());
	}

	@Test
	void testBuilderStyleInvocation() {
		Parameters op = new Parameters();
		op.outputFormat(OutputFormat.PLOTSY).ageStart("0").ageEnd("100").yearStart("1950").yearEnd("2050")
				.yearForcedIntoYieldTable("2032").ageIncrement("3").combineAgeYearRange("notimportant")
				.metadataToOutput("notimportant2").utils(null);
		assertEquals(OutputFormat.PLOTSY.getValue(), op.getOutputFormat());
		assertEquals("0", op.getAgeStart());
		assertEquals("100", op.getAgeEnd());
		assertEquals("1950", op.getYearStart());
		assertEquals("2050", op.getYearEnd());
		assertEquals("2032", op.getYearForcedIntoYieldTable());
		assertEquals("3", op.getAgeIncrement());
		assertEquals("notimportant", op.getCombineAgeYearRange());
		assertEquals("notimportant2", op.getMetadataToOutput());
		assertNull(op.getUtils());

	}
}
