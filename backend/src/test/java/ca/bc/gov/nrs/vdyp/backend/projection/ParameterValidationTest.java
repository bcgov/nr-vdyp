package ca.bc.gov.nrs.vdyp.backend.projection;

import static ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
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

class ParameterValidationTest {

	@Test
	void testNoParametersSupplied() throws WebApplicationException, IOException {

		Parameters p = new Parameters();
		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, MISSING_END_CRITERIA);
		}
	}

	@Test
	void testOnlyAgeStartParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = new Parameters().ageStart(1);
		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA);
		}
	}

	@Test
	void testAgeStartTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMinAgeStart() - 1);
		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);
		}
	}

	@Test
	void testAgeStartTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMaxAgeStart() + 1);
		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);
		}
	}

	@Test
	void testOnlyYearStartParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = new Parameters().yearStart(ValidatedParameters.DEFAULT.getMinYearStart());
		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA);
		}
	}

	@Test
	void testYearStartTooLowParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMinYearStart() - 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);
		}
	}

	@Test
	void testYearStartTooHighParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMaxYearStart() + 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);
		}
	}

	@Test
	void testOnlyAgeEndParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().ageEnd(400);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {

			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA);
		}
	}

	@Test
	void testAgeEndTooLowParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMinAgeEnd() - 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {

			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);
		}
	}

	@Test
	void testAgeEndTooHighParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMaxAgeEnd() + 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {

			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);
		}
	}

	@Test
	void testOnlyYearEndParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().yearEnd(1500);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_START_CRITERIA);
		}
	}

	@Test
	void testYearEndTooLowParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMinYearEnd() - 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);
		}
	}

	@Test
	void testYearEndTooHighParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMaxYearEnd() + 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);
		}
	}

	@Test
	void testAgeIncrementTooLowParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = TestHelper.buildValidParametersObject()
					.ageIncrement(ValidatedParameters.DEFAULT.getMinAgeIncrement() - 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INTEGER_VALUE_TOO_LOW);
		}
	}

	@Test
	void testAgeIncrementTooHighParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = TestHelper.buildValidParametersObject()
					.ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement() + 1);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INTEGER_VALUE_TOO_HIGH);
		}
	}

	@Test
	void testValidAgeIncrementSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = TestHelper.buildValidParametersObject()
					.ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement());
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testValidAgeStartAndEndParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = TestHelper.buildValidParametersObject();
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testValidYearStartAndEndParameterSupplied() throws WebApplicationException, IOException {

		try {
			Parameters p = new Parameters().yearStart(1600).yearEnd(2100);
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testInvalidOutputFormatOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setOutputFormat("bad output format");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_OUTPUT_FORMAT);
		}
	}

	@Test
	void testValidOutputFormatOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setOutputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testInvalidCombineAgeYearRangeOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setCombineAgeYearRange("bad option");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION);
		}
	}

	@Test
	void testValidCombineAgeYearRangeOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setCombineAgeYearRange(Parameters.AgeYearRangeCombinationKind.INTERSECT);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testInvalidProcessFrequencySupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency("bad option");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_PROCESS_FREQUENCY_VALUE);
		}
	}

	@Test
	void testValidProcessFrequencySupplied1() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testValidProcessFrequencySupplied2() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setProgressFrequency(100);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testInvalidMetadataToOutputValueSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.setMetadataToOutput("bad option");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_METADATA_TO_OUTPUT_VALUE);
		}
	}

	@Test
	void testInvalidExecutionOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED)
				.addSelectedExecutionOptionsItem("bad option");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_EXECUTION_OPTION);
		}
	}

	@Test
	void testInvalidDebugOptionSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
				.addSelectedDebugOptionsItem("bad option");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_DEBUG_OPTION);
		}
	}

	@Test
	void testBadUtilizationParameterSpeciesSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("bad species name").utilizationClass(UtilizationClassSet._12_5);
		p.addUtilsItem(up);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_SPECIES_GROUP_NAME);
		}
	}

	@Test
	void testBadUtilizationParameterUtilizationClassSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("D").utilizationClass("bad utilization class");
		p.addUtilsItem(up);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), UNRECOGNIZED_UTILIZATION_CLASS_NAME);
		}
	}

	@Test
	void testBadUtilizationParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("bad species name").utilizationClass("bad utilization class");
		p.addUtilsItem(up);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), UNRECOGNIZED_SPECIES_GROUP_NAME, UNRECOGNIZED_UTILIZATION_CLASS_NAME
			);
		}
	}

	@Test
	void testValidUtilizationParameterSupplied() throws WebApplicationException, IOException {

		Parameters p = TestHelper.buildValidParametersObject();
		var up = new UtilizationParameter().speciesName("D").utilizationClass(UtilizationClassSet._12_5);
		p.addUtilsItem(up);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testInvalidForceYear() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.yearForcedIntoYieldTable("bad year");

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_INTEGER_VALUE);
		}
	}

	@Test
	void testValidForceYear() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.yearForcedIntoYieldTable(2020);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}
	}

	@Test
	void testDCSVIssues() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.outputFormat(OutputFormat.DCSV);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT,
					MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
			);
		}
	}

	@Test
	void testDCSVIssues2() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.outputFormat(OutputFormat.DCSV).yearForcedIntoYieldTable(2020);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
			);
		}
	}

	@Test
	void testCSFBiomassIssues1() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT,
					CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS
			);
		}
	}

	@Test
	void testCSFBiomassIssues2() {

		Parameters p = TestHelper.buildValidParametersObject();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);

		try {
			new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS
			);
		}
	}

	@Test
	void testCSFBiomassIssues3() {

		Parameters p = new Parameters();
		p.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.outputFormat(OutputFormat.DCSV).yearForcedIntoYieldTable(2020);

		try {
			new ProjectionContext(ProjectionRequestKind.DCSV, "id", p, false);
			Assert.fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), INVALID_CFS_BIOMASS_OUTPUT_FORMAT);
		}
	}

	@Test
	void testValidFullParameterCreation() {

		ProjectionContext s1 = null;

		try {
			Parameters p = new Parameters().addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
					.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS)
					.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES)
					.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS)
					.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED)
					.addSelectedExecutionOptionsItem(
							ExecutionOption.DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION
					).addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)
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
					.combineAgeYearRange(AgeYearRangeCombinationKind.DIFFERENCE) //
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
		} catch (AbstractProjectionRequestException e) {
			Assert.fail();
		}

		var vp = s1.getParams();

		Assert.assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT));
		Assert.assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS));
		Assert.assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES));
		Assert.assertTrue(vp.containsOption(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS));
		Assert.assertTrue(vp.containsOption(ExecutionOption.BACK_GROW_ENABLED));
		Assert.assertTrue(
				vp.containsOption(ExecutionOption.DO_ALLOW_BASAL_AREA_AND_TREES_PER_HECTARE_VALUE_SUBSTITUTION)
		);
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_FILE_HEADER));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE));
		Assert.assertTrue(
				vp.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)
		);
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_SAVE_INTERMEDIATE_FILES));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER));
		Assert.assertTrue(vp.containsOption(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON));
		Assert.assertTrue(vp.containsOption(ExecutionOption.FORWARD_GROW_ENABLED));

		Assert.assertEquals(Integer.valueOf(10), vp.getAgeEnd());
		Assert.assertEquals(Integer.valueOf(3), vp.getAgeIncrement());
		Assert.assertEquals(Integer.valueOf(20), vp.getAgeStart());
		Assert.assertEquals(AgeYearRangeCombinationKind.DIFFERENCE, vp.getCombineAgeYearRange());
		Assert.assertEquals(Integer.valueOf(2020), vp.getYearForcedIntoYieldTable());
		Assert.assertEquals(MetadataToOutputDirective.ALL, vp.getMetadataToOutput());
		Assert.assertEquals(OutputFormat.YIELD_TABLE, vp.getOutputFormat());
		Assert.assertEquals(FrequencyKind.MAPSHEET, vp.getProgressFrequency().getEnumValue());
		Assert.assertEquals(Integer.valueOf(2024), vp.getYearEnd());
		Assert.assertEquals(Integer.valueOf(2015), vp.getYearStart());

		Assert.assertEquals("maintainer", vp.getFilters().getMaintainer());
		Assert.assertEquals("mapsheet", vp.getFilters().getMapsheet());
		Assert.assertEquals("polygon", vp.getFilters().getPolygon());
		Assert.assertEquals("polygonId", vp.getFilters().getPolygonId());

		var expectedMap = new HashMap<SP0Name, UtilizationClassSet>();
		expectedMap.put(SP0Name.forText("C"), UtilizationClassSet._4_0);
		expectedMap.put(SP0Name.forText("D"), UtilizationClassSet._12_5);
		for (var entry : expectedMap.entrySet()) {
			assertThat(vp.getUtils(), Matchers.hasEntry(entry.getKey(), entry.getValue()));
		}
		for (var sp0Name : SP0Name.values()) {
			Assert.assertTrue(
					expectedMap.containsKey(sp0Name) || vp.getUtils().get(sp0Name).equals(UtilizationClassSet._12_5)
			);
		}
	}
}
