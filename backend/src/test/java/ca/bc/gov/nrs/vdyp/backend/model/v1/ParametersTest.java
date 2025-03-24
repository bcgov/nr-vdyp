package ca.bc.gov.nrs.vdyp.backend.model.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationParameter.UtilizationClass;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedUtilizationParameter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

public class ParametersTest {

	@Test
	public void testParametersProvider() throws WebApplicationException, IOException {
		Parameters op = new Parameters();

		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES);
		op.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS);

		op.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED);
		op.addSelectedExecutionOptionsItem(
				ExecutionOption.DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION
		);
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

		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("AL").utilizationClass(UtilizationClass._12_5));
		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("C").utilizationClass(UtilizationClass._17_5));
		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("D").utilizationClass(UtilizationClass._22_5));
		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("E").utilizationClass(UtilizationClass._4_0));
		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("F").utilizationClass(UtilizationClass._7_5));
		op.addUtilsItem(new ValidatedUtilizationParameter().speciesName("H").utilizationClass(UtilizationClass.EXCL));

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

		Assert.assertTrue(
				provider.isReadable(Parameters.class, Parameters.class, null, MediaType.APPLICATION_JSON_TYPE)
		);

		Parameters np = provider.readFrom(
				Parameters.class, Parameters.class, null, MediaType.APPLICATION_JSON_TYPE, null,
				new ByteArrayInputStream(json)
		);

		Assert.assertTrue(op.equals(np));

		Assert.assertFalse(provider.isReadable(Object.class, null, null, MediaType.APPLICATION_JSON_TYPE));
		Assert.assertFalse(provider.isReadable(Parameters.class, null, null, MediaType.APPLICATION_OCTET_STREAM_TYPE));

		np.setProgressFrequency(FrequencyKind.POLYGON.getValue());

		Assert.assertFalse(op.equals(np));
	}

	@Test
	void testProgressFrequency() {

		Assert.assertNull(new ProgressFrequency().getIntValue());
		Assert.assertNull(new ProgressFrequency().getEnumValue());
		Assert.assertEquals(Integer.valueOf(12), new ProgressFrequency(12).getIntValue());
		Assert.assertNull(new ProgressFrequency(12).getEnumValue());
		Assert.assertNull(new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET).getIntValue());
		Assert.assertEquals(
				ProgressFrequency.FrequencyKind.MAPSHEET,
				new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET).getEnumValue()
		);

		Assert.assertThrows(
				IllegalArgumentException.class, () -> ProgressFrequency.FrequencyKind.fromValue("not a value")
		);

		ProgressFrequency pf1 = new ProgressFrequency(12);
		ProgressFrequency pf2 = new ProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);
		Assert.assertTrue(pf1.equals(pf1));
		Assert.assertEquals(Integer.valueOf(12).hashCode(), pf1.hashCode());
		Assert.assertEquals(ProgressFrequency.FrequencyKind.MAPSHEET.hashCode(), pf2.hashCode());
		Assert.assertEquals(17, new ProgressFrequency().hashCode());

		Assert.assertTrue(pf1.toString().indexOf("12") != -1);
		Assert.assertTrue(pf2.toString().indexOf("mapsheet") != -1);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	void testUtilizationParameter() {
		Assert.assertEquals(
				"AL",
				new ValidatedUtilizationParameter().speciesName("AL").utilizationClass(UtilizationClass._12_5)
						.getSpeciesName()
		);
		Assert.assertEquals(
				UtilizationClass._17_5,
				new ValidatedUtilizationParameter().speciesName("AL").utilizationClass(UtilizationClass._17_5)
						.getUtilizationClass()
		);

		Assert.assertThrows(IllegalArgumentException.class, () -> UtilizationClass.fromValue("ZZZ"));

		var up1 = new ValidatedUtilizationParameter().speciesName("AL").utilizationClass(UtilizationClass._12_5);
		var up2 = new ValidatedUtilizationParameter().speciesName("C").utilizationClass(UtilizationClass._12_5);
		var up3 = new ValidatedUtilizationParameter().speciesName("C").utilizationClass(UtilizationClass._22_5);

		Assert.assertTrue(up1.equals(up1));
		Assert.assertTrue(up1.hashCode() == up1.hashCode());
		Assert.assertFalse(up2.equals(up3));
		Assert.assertFalse(up2.equals("C"));

		Assert.assertTrue(up1.toString().indexOf("speciesName: AL") != -1);
		Assert.assertTrue(up1.toString().indexOf("utilizationClass: 12.5") != -1);
	}
}
