package ca.bc.gov.nrs.vdyp.backend.model.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionRequestParametersValidator;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.ParametersProvider;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.AgeYearRangeCombinationKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.MetadataToOutputDirective;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency.FrequencyKind;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import static org.junit.jupiter.api.Assertions.*;

public class ParametersTest {

	@Test
	public void testParametersProvider() throws WebApplicationException, IOException {
		Parameters op = new Parameters();

		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS);

		op.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_ERROR_LOGGING);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_FILE_HEADER);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_SAVE_INTERMEDIATE_FILES);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER);
		op.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON);
		op.addSelectedExecutionOptionsItem(ExecutionOption.FORWARD_GROW_ENABLED);

		op.addUtilsItem(new UtilizationParameter().speciesName("AL").utilizationClass(UtilizationClassSet._12_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("C").utilizationClass(UtilizationClassSet._17_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("D").utilizationClass(UtilizationClassSet._22_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("E").utilizationClass(UtilizationClassSet._4_0));
		op.addUtilsItem(new UtilizationParameter().speciesName("F").utilizationClass(UtilizationClassSet._7_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("H").utilizationClass(UtilizationClassSet.EXCL));

		op.ageEnd(2030) //
				.ageIncrement(1) //
				.ageStart(2020) //
				.combineAgeYearRange(AgeYearRangeCombinationKind.DIFFERENCE) //
				.yearForcedIntoYieldTable(2020);

		var filters = new FilterParameters().maintainer("maintainer").mapsheet("mapsheet").polygon("polygon")
				.polygonId("polygonId");
		op.filters(filters);

		op.progressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);

		op.metadataToOutput(MetadataToOutputDirective.ALL) //
				.outputFormat(OutputFormat.CSV_YIELD_TABLE) //
				.yearEnd(2030) //
				.yearStart(2020);

		var objectMapper = new ObjectMapper();
		byte[] json = objectMapper.writeValueAsBytes(op);

		ParametersProvider provider = new ParametersProvider();

		assertTrue(provider.isReadable(Parameters.class, Parameters.class, null, MediaType.APPLICATION_JSON_TYPE));

		Parameters np = provider.readFrom(
				Parameters.class, Parameters.class, null, MediaType.APPLICATION_JSON_TYPE, null,
				new ByteArrayInputStream(json)
		);

		assertTrue(op.equals(op));
		assertNotEquals(null, op);
        assertFalse(op.equals(provider));
        assertEquals(op, np);

		assertEquals(op.hashCode(), np.hashCode());
		assertEquals(op.toString(), np.toString());

		assertFalse(provider.isReadable(Object.class, null, null, MediaType.APPLICATION_JSON_TYPE));
		assertFalse(provider.isReadable(Parameters.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE));

		np.setProgressFrequency(FrequencyKind.POLYGON.getValue());

        assertNotEquals(op, np);
	}

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
