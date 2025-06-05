package ca.bc.gov.nrs.vdyp.backend.projection;

import static ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.FilterParameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.AgeYearRangeCombinationKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.MetadataToOutputDirective;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency.FrequencyKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationClassSet;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;
import jakarta.ws.rs.WebApplicationException;
import static org.junit.jupiter.api.Assertions.*;

class ParameterValidationTest {
	@Test
	void testNoParametersSupplied() throws WebApplicationException {

		Parameters p = new Parameters();
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, MISSING_END_CRITERIA);
	}

	@Test
	void testOnlyAgeStartParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageStart(1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA);

	}

	@Test
	void testAgeStartTooLowParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMinAgeStart() - 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);

	}

	@Test
	void testAgeStartTooHighParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMaxAgeStart() + 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);

	}

	@Test
	void testOnlyYearStartParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().yearStart(ValidatedParameters.DEFAULT.getMinYearStart());
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA);

	}

	@Test
	void testYearStartTooLowParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMinYearStart() - 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);

	}

	@Test
	void testYearStartTooHighParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMaxYearStart() + 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);

	}

	@Test
	void testOnlyAgeEndParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageEnd(400);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);

		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA);

	}

	@Test
	void testAgeEndTooLowParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMinAgeEnd() - 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);

		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);

	}

	@Test
	void testAgeEndTooHighParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMaxAgeEnd() + 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);

		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);

	}

	@Test
	void testOnlyYearEndParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().yearEnd(1500);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA);

	}

	@Test
	void testYearEndTooLowParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMinYearEnd() - 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);

	}

	@Test
	void testYearEndTooHighParameterSupplied() throws WebApplicationException {

		Parameters p = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMaxYearEnd() + 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);

	}

	@Test
	void testAgeIncrementTooLowParameterSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject()
				.ageIncrement(ValidatedParameters.DEFAULT.getMinAgeIncrement() - 1);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INTEGER_VALUE_TOO_LOW);
	}

	@Test
	void testAgeIncrementTooHighParameterSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject()
				.ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement() + 1);
		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INTEGER_VALUE_TOO_HIGH);

	}

	@Test
	void testValidAgeIncrementSupplied() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject()
				.ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement());
        ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);

        assertThat(context.getParams().getAgeIncrement(), is(ValidatedParameters.DEFAULT.getMaxAgeIncrement()));
	}

	@Test
	void testValidAgeStartAndEndParameterSupplied() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
        ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);

        assertThat(context.getParams().getAgeStart(), is(1));
        assertThat(context.getParams().getAgeEnd(), is(400));
	}

	@Test
	void testValidYearStartAndEndParameterSupplied()
			throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = new Parameters().yearStart(1600).yearEnd(2100);
        ProjectionContext context =new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);

        assertThat(context.getParams().getYearStart(), is(1600));
        assertThat(context.getParams().getYearEnd(), is(2100));
	}

	@Test
	void testInvalidOutputFormatOptionSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setOutputFormat("bad output format");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_OUTPUT_FORMAT);

	}

	@Test
	void testValidOutputFormatOptionSupplied() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setOutputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE);

		ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getOutputFormat(), is(OutputFormat.CSV_YIELD_TABLE));
	}

	@Test
	void testInvalidCombineAgeYearRangeOptionSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setCombineAgeYearRange("bad option");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION);

	}

	@Test
	void testValidCombineAgeYearRangeOptionSupplied()
			throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setCombineAgeYearRange(Parameters.AgeYearRangeCombinationKind.INTERSECT);

		ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getCombineAgeYearRange(), is(AgeYearRangeCombinationKind.INTERSECT));
	}

	@Test
	void testInvalidProgressFrequencySupplied() throws WebApplicationException{

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency("bad option");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_PROCESS_FREQUENCY_VALUE);

	}

	@Test
	void testValidProgressFrequencySupplied1() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);

		ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getProgressFrequency().getEnumValue(), is(FrequencyKind.MAPSHEET));
	}

	@Test
	void testValidProgressFrequencySupplied2() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency(100);

        ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getProgressFrequency().getIntValue(), is(100));

	}

	@Test
	void testInvalidMetadataToOutputValueSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setMetadataToOutput("bad option");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_METADATA_TO_OUTPUT_VALUE);

	}

	@Test
	void testInvalidExecutionOptionSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED)
				.addSelectedExecutionOptionsItem("bad option");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_EXECUTION_OPTION);

	}

	@Test
	void testInvalidDebugOptionSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
				.addSelectedDebugOptionsItem("bad option");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_DEBUG_OPTION);

	}

	@Test
	void testBadUtilizationParameterSpeciesSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("bad species name").utilizationClass(UtilizationClassSet._12_5);
		p.addUtilsItem(up);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_SPECIES_GROUP_NAME);

	}

	@Test
	void testBadUtilizationParameterUtilizationClassSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("D").utilizationClass("bad utilization class");
		p.addUtilsItem(up);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_UTILIZATION_CLASS_NAME);

	}

	@Test
	void testBadUtilizationParameterSupplied() throws WebApplicationException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("bad species name").utilizationClass("bad utilization class");
		p.addUtilsItem(up);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(
				e.getValidationMessages(), UNRECOGNIZED_SPECIES_GROUP_NAME, UNRECOGNIZED_UTILIZATION_CLASS_NAME
		);

	}

	@Test
	void testValidUtilizationParameterSupplied() throws WebApplicationException, AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("D").utilizationClass(UtilizationClassSet._12_5);
		p.addUtilsItem(up);

        ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getUtils().get(SP0Name.D), is(UtilizationClassSet._12_5));
	}

	@Test
	void testInvalidForceYear() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.yearForcedIntoYieldTable("bad year");

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_INTEGER_VALUE);

	}

	@Test
	void testValidForceYear() throws AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.yearForcedIntoYieldTable(2020);

		ProjectionContext context = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
        assertThat(context.getParams().getYearForcedIntoYieldTable(), is(2020));
	}

	@Test
	void testDCSVIssues() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.outputFormat(OutputFormat.DCSV);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(
				e.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT,
				MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
		);

	}

	@Test
	void testDCSVIssues2() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.outputFormat(OutputFormat.DCSV).yearForcedIntoYieldTable(2020);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(
				e.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
		);

	}

	@Test
	void testCSFBiomassIssues1() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(
				e.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT,
				CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS
		);
	}

	@Test
	void testCSFBiomassIssues2() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false)
		);
		TestHelper
				.verifyMessageSetIs(e.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS);
	}

	@Test
	void testCSFBiomassIssues3() {

		Parameters p = new Parameters();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.outputFormat(OutputFormat.DCSV).yearForcedIntoYieldTable(2020);

		AbstractProjectionRequestException e = assertThrows(
				AbstractProjectionRequestException.class,
				() -> new ProjectionContext(ProjectionRequestKind.DCSV, "id", p, false)
		);
		TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_CFS_BIOMASS_OUTPUT_FORMAT);
	}

	@Test
	void testValidFullParameterCreation() throws AbstractProjectionRequestException {

		ProjectionContext s1 = null;
		ProjectionContext s2 = null;

		Parameters p = new Parameters().addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS)
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES)
				.addExcludedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS)
				.addExcludedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_DELAY_EXECUTION_FOLDER_DELETION)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_ERROR_LOGGING)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_FILE_HEADER)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(
						ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
				).addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_SAVE_INTERMEDIATE_FILES)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON)
				.addSelectedExecutionOptionsItem(ExecutionOption.FORWARD_GROW_ENABLED)
				.addUtilsItem(
						new UtilizationParameter().speciesName("D")
								.utilizationClass(UtilizationClassSet._12_5.getValue())
				)
				.addUtilsItem(
						new UtilizationParameter().speciesName("C")
								.utilizationClass(UtilizationClassSet._4_0.getValue())
				).ageEnd(10) //
				.ageIncrement(3) //
				.ageStart(20) //
				.combineAgeYearRange(AgeYearRangeCombinationKind.INTERSECT) //
				.filters(
						new FilterParameters().maintainer("maintainer").mapsheet("mapsheet").polygon("polygon")
								.polygonId("polygonId")
				).yearForcedIntoYieldTable(2020) //
				.metadataToOutput(MetadataToOutputDirective.ALL) //
				.outputFormat(OutputFormat.YIELD_TABLE) //
				.progressFrequency(FrequencyKind.MAPSHEET) //
				.yearEnd(2024) //
				.yearStart(2015);

		s1 = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		s2 = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);

		var vp = s1.getParams();
		var v2 = s2.getParams();

		assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT));
		assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS));
		assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES));
		assertFalse(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS));
		assertFalse(vp.containsOption(ExecutionOption.BACK_GROW_ENABLED));
		assertTrue(vp.containsOption(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION));
		assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING));
		assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING));
		assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING));
		assertTrue(vp.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES));
		assertTrue(vp.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_FILE_HEADER));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION));
		assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE));
		assertTrue(vp.containsOption(ExecutionOption.DO_SAVE_INTERMEDIATE_FILES));
		assertTrue(vp.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER));
		assertTrue(vp.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON));
		assertTrue(vp.containsOption(ExecutionOption.FORWARD_GROW_ENABLED));

		assertEquals(Integer.valueOf(10), vp.getAgeEnd());
		assertEquals(Integer.valueOf(3), vp.getAgeIncrement());
		assertEquals(Integer.valueOf(20), vp.getAgeStart());
		assertEquals(AgeYearRangeCombinationKind.INTERSECT, vp.getCombineAgeYearRange());
		assertEquals(Integer.valueOf(2020), vp.getYearForcedIntoYieldTable());
		assertEquals(MetadataToOutputDirective.ALL, vp.getMetadataToOutput());
		assertEquals(OutputFormat.YIELD_TABLE, vp.getOutputFormat());
		assertEquals(FrequencyKind.MAPSHEET, vp.getProgressFrequency().getEnumValue());
		assertEquals(Integer.valueOf(2024), vp.getYearEnd());
		assertEquals(Integer.valueOf(2015), vp.getYearStart());

		assertEquals("maintainer", vp.getFilters().getMaintainer());
		assertEquals("mapsheet", vp.getFilters().getMapsheet());
		assertEquals("polygon", vp.getFilters().getPolygon());
		assertEquals("polygonId", vp.getFilters().getPolygonId());

		var expectedMap = new HashMap<SP0Name, UtilizationClassSet>();
		expectedMap.put(SP0Name.forText("C"), UtilizationClassSet._4_0);
		expectedMap.put(SP0Name.forText("D"), UtilizationClassSet._12_5);
		for (var entry : expectedMap.entrySet()) {
			assertThat(vp.getUtils(), Matchers.hasEntry(entry.getKey(), entry.getValue()));
		}
		for (var sp0Name : SP0Name.values()) {
			assertTrue(
					expectedMap.containsKey(sp0Name) || vp.getUtils().get(sp0Name).equals(UtilizationClassSet._12_5)
			);
		}

        assertEquals(vp, vp);
		assertEquals(vp, v2);
		assertEquals(vp.hashCode(), v2.hashCode());
		assertEquals(vp.toString(), v2.toString());
	}
}
