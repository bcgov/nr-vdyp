package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl.ParametersProvider;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.FilterParameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationClassSet;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationParameter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

public class ParametersProviderTest {

	@Test
	public void testParametersProvider() throws WebApplicationException, IOException {
		Parameters op = new Parameters();

		op.addSelectedDebugOptionsItem(Parameters.DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT);
		op.addSelectedDebugOptionsItem(Parameters.DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS);
		op.addSelectedDebugOptionsItem(Parameters.DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES);
		op.addSelectedDebugOptionsItem(Parameters.DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS);

		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.BACK_GROW_ENABLED);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES);
		op.addSelectedExecutionOptionsItem(
				Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
		);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_FILE_HEADER);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(
				Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
		);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SAVE_INTERMEDIATE_FILES);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON);
		op.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.FORWARD_GROW_ENABLED);

		op.addUtilsItem(new UtilizationParameter().speciesName("AL").utilizationClass(UtilizationClassSet._12_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("C").utilizationClass(UtilizationClassSet._17_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("D").utilizationClass(UtilizationClassSet._22_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("E").utilizationClass(UtilizationClassSet._4_0));
		op.addUtilsItem(new UtilizationParameter().speciesName("F").utilizationClass(UtilizationClassSet._7_5));
		op.addUtilsItem(new UtilizationParameter().speciesName("H").utilizationClass(UtilizationClassSet.EXCL));

		op.ageEnd(2030) //
				.ageIncrement(1) //
				.ageStart(2020) //
				.combineAgeYearRange(Parameters.AgeYearRangeCombinationKind.DIFFERENCE) //
				.yearForcedIntoYieldTable(2020);

		var filters = new FilterParameters().maintainer("maintainer").mapsheet("mapsheet").polygon("polygon")
				.polygonId("polygonId");
		op.filters(filters);

		op.progressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);

		op.metadataToOutput(Parameters.MetadataToOutputDirective.ALL) //
				.outputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE) //
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

		np.setProgressFrequency(ProgressFrequency.FrequencyKind.POLYGON.getValue());

		assertNotEquals(op, np);
	}
}
