package ca.bc.gov.nrs.vdyp.backend.projection;

import static ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationParameter.UtilizationClass;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import jakarta.ws.rs.WebApplicationException;

public class ParameterValidationTest {

	@Test
	public void testNoParametersSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA, MISSING_END_CRITERIA);
	}

	@Test
	public void testOnlyAgeStartParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageStart(1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA);
	}

	@Test
	public void testAgeStartTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMinAgeStart() - 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);
	}

	@Test
	public void testAgeStartTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageEnd(400).ageStart(ValidatedParameters.DEFAULT.getMaxAgeStart() + 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);
	}

	@Test
	public void testOnlyYearStartParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().yearStart(ValidatedParameters.DEFAULT.getMinYearStart());
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA);
	}

	@Test
	public void testYearStartTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMinYearStart() - 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_LOW);
	}

	@Test
	public void testYearStartTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageEnd(400).yearStart(ValidatedParameters.DEFAULT.getMaxYearStart() + 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA, INTEGER_VALUE_TOO_HIGH);
	}

	@Test
	public void testOnlyAgeEndParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageEnd(400);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA);
	}

	@Test
	public void testAgeEndTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMinAgeEnd() - 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);
	}

	@Test
	public void testAgeEndTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().ageStart(1).ageEnd(ValidatedParameters.DEFAULT.getMaxAgeEnd() + 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);
	}

	@Test
	public void testOnlyYearEndParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().yearEnd(1500);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_START_CRITERIA);
	}

	@Test
	public void testYearEndTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMinYearEnd() - 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_LOW);
	}

	@Test
	public void testYearEndTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().yearStart(1500).yearEnd(ValidatedParameters.DEFAULT.getMaxYearEnd() + 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), MISSING_END_CRITERIA, INTEGER_VALUE_TOO_HIGH);
	}

	@Test
	public void testAgeIncrementTooLowParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject().ageIncrement(ValidatedParameters.DEFAULT.getMinAgeIncrement() - 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INTEGER_VALUE_TOO_LOW);
	}

	@Test
	public void testAgeIncrementTooHighParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject().ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement() + 1);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INTEGER_VALUE_TOO_HIGH);
	}

	@Test
	public void testValidAgeIncrementSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject().ageIncrement(ValidatedParameters.DEFAULT.getMaxAgeIncrement());
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testValidAgeStartAndEndParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testValidYearStartAndEndParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = new Parameters().yearStart(1600).yearEnd(2100);
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testInvalidOutputFormatOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setOutputFormat("bad output format");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNRECOGNIZED_OUTPUT_FORMAT);
	}

	@Test
	public void testValidOutputFormatOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setOutputFormat(Parameters.OutputFormat.CSV_YIELD_TABLE);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testInvalidCombineAgeYearRangeOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setCombineAgeYearRange("bad option");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION);
	}

	@Test
	public void testValidCombineAgeYearRangeOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setCombineAgeYearRange(Parameters.AgeYearRangeCombinationKind.INTERSECT);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testInvalidProcessFrequencySupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setProgressFrequency("bad option");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INVALID_PROCESS_FREQUENCY_VALUE);
	}

	@Test
	public void testValidProcessFrequencySupplied1() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setProgressFrequency(ProgressFrequency.FrequencyKind.MAPSHEET);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testValidProcessFrequencySupplied2() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setProgressFrequency(100);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testInvalidMetadataToOutputValueSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.setMetadataToOutput("bad option");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INVALID_METADATA_TO_OUTPUT_VALUE);
	}

	@Test
	public void testInvalidExecutionOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.addSelectedExecutionOptionsItem(ExecutionOption.BACK_GROW_ENABLED)
				.addSelectedExecutionOptionsItem("bad option");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNRECOGNIZED_EXECUTION_OPTION);
	}

	@Test
	public void testInvalidDebugOptionSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT)
				.addSelectedDebugOptionsItem("bad option");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNRECOGNIZED_DEBUG_OPTION);
	}

	@Test
	public void testBadUtilizationParameterSpeciesSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var up = new UtilizationParameter().speciesName("bad species name")
				.utilizationClass(UtilizationParameter.UtilizationClass._12_5.getValue());
		p1.addUtilsItem(up);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNKNOWN_SPECIES_GROUP_NAME);
	}

	@Test
	public void testBadUtilizationParameterUtilizationClassSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var up = new UtilizationParameter().speciesName("D").utilizationClass("bad utilization class");
		p1.addUtilsItem(up);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), UNKNOWN_UTILIZATION_CLASS_NAME);
	}

	@Test
	public void testBadUtilizationParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var up = new UtilizationParameter().speciesName("bad species name").utilizationClass("bad utilization class");
		p1.addUtilsItem(up);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(
				validator.getValidationMessages(), UNKNOWN_SPECIES_GROUP_NAME, UNKNOWN_UTILIZATION_CLASS_NAME
		);
	}

	@Test
	public void testValidUtilizationParameterSupplied() throws WebApplicationException, IOException {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		var up = new UtilizationParameter().speciesName("D").utilizationClass(UtilizationClass._12_5);
		p1.addUtilsItem(up);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testInvalidForceYear() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.forceYear("bad year");

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INVALID_INTEGER_VALUE);
	}

	@Test
	public void testValidForceYear() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.forceYear(2020);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages());
	}

	@Test
	public void testDCSVIssues() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.outputFormat(OutputFormat.DCSV);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(
				validator.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT,
				MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
		);
	}

	@Test
	public void testDCSVIssues2() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.outputFormat(OutputFormat.DCSV).forceYear(2020);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(
				validator.getValidationMessages(), AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT, MISMATCHED_INPUT_OUTPUT_TYPES
		);
	}

	@Test
	public void testCSFBiomassIssues1() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(
				validator.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT,
				CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS
		);
	}

	@Test
	public void testCSFBiomassIssues2() {

		Parameters p1 = buildValidParametersObject();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.HCSV, "id", p1);

		p1.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS);
	}

	@Test
	public void testCSFBiomassIssues3() {

		Parameters p1 = new Parameters();
		ProjectionState s1 = new ProjectionState(ProjectionRequestKind.DCSV, "id", p1);

		p1.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)
				.outputFormat(OutputFormat.DCSV).forceYear(2020);

		var validator = new ProjectionRequestValidator();
		validator.validateState(s1);
		verifyMessageSetIs(validator.getValidationMessages(), INVALID_CFS_BIOMASS_OUTPUT_FORMAT);
	}

	// Helpers

	private void verifyMessageSetIs(List<ValidationMessage> validationMessages, ValidationMessageKind... kinds) {
		Set<ValidationMessageKind> expectedKinds = Set.of(kinds);
		Set<ValidationMessageKind> presentKinds = new HashSet<>();

		for (var message : validationMessages) {
			presentKinds.add(message.getKind());
		}

		Assert.assertEquals(expectedKinds, presentKinds);
	}

	private Parameters buildValidParametersObject() {
		return new Parameters().ageEnd(400).ageStart(1);
	}
}
