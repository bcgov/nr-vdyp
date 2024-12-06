package ca.bc.gov.nrs.vdyp.backend.projection;

import static ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind.*;
import static ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters.DEFAULT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

public class ProjectionRequestParametersValidator {

	private List<ValidationMessage> validationErrorMessages = new ArrayList<>();

	public static void validate(ProjectionState state)
			throws ProjectionRequestValidationException {

		var validator = new ProjectionRequestParametersValidator();

		validator.validateState(state);

		if (validator.validationErrorMessages.size() > 0) {
			throw new ProjectionRequestValidationException(validator.validationErrorMessages);
		}
	}

	public List<ValidationMessage> getValidationMessages() {
		return validationErrorMessages;
	}

	void validateState(ProjectionState state) {

		validateRequestParametersIndividually(state);
		validateRequestParametersCollectively(state);
	}

	private void validateRequestParametersIndividually(ProjectionState state) {

		Parameters params = state.getParams();
		ValidatedParameters vparams = new ValidatedParameters();

		// Parameters.JSON_PROPERTY_OUTPUT_FORMAT

		if (params.getOutputFormat() == null) {
			vparams.outputFormat(DEFAULT.getOutputFormat());
		} else {
			try {
				var outputFormat = OutputFormat.fromValue(params.getOutputFormat());
				vparams.setOutputFormat(outputFormat);
			} catch (IllegalArgumentException e) {
				recordValidationMessage(UNRECOGNIZED_OUTPUT_FORMAT, params.getOutputFormat());
			}
		}

		// Parameters.JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS

		if (params.getSelectedExecutionOptions() == null) {
			vparams.selectedExecutionOptions(DEFAULT.getSelectedExecutionOptions());
		} else {
			List<ExecutionOption> selectedOptions = new ArrayList<>();
			for (String optionText : params.getSelectedExecutionOptions()) {
				try {
					var e = Parameters.ExecutionOption.fromValue(optionText);
					selectedOptions.add(e);
				} catch (IllegalArgumentException e) {
					recordValidationMessage(UNRECOGNIZED_EXECUTION_OPTION, optionText);
				}
			}
			vparams.selectedExecutionOptions(selectedOptions);
		}

		// Parameters.JSON_PROPERTY_SELECTED_DEBUG_OPTIONS

		if (params.getSelectedDebugOptions() == null) {
			vparams.selectedDebugOptions(DEFAULT.getSelectedDebugOptions());
		} else {
			List<DebugOption> selectedOptions = new ArrayList<>();
			for (String optionText : params.getSelectedDebugOptions()) {
				try {
					var e = Parameters.DebugOption.fromValue(optionText);
					selectedOptions.add(e);
				} catch (IllegalArgumentException e) {
					recordValidationMessage(UNRECOGNIZED_DEBUG_OPTION, optionText);
				}
			}
			vparams.selectedDebugOptions(selectedOptions);
		}

		vparams.setMinAgeStart(DEFAULT.getMinAgeEnd());
		vparams.setMaxAgeStart(DEFAULT.getMaxAgeStart());

		// Parameters.JSON_PROPERTY_AGE_START
		vparams.setAgeStart(
				getIntegerValue(
						params.getAgeStart(), DEFAULT.getAgeStart(), vparams.getMinAgeStart(), vparams.getMaxAgeStart(),
						"ageEnd"
				)
		);

		vparams.setMinAgeEnd(DEFAULT.getMinAgeEnd());
		vparams.setMaxAgeEnd(DEFAULT.getMaxAgeEnd());

		// Parameters.JSON_PROPERTY_AGE_END
		vparams.setAgeEnd(
				getIntegerValue(
						params.getAgeEnd(), DEFAULT.getAgeEnd(), vparams.getMinAgeEnd(), vparams.getMaxAgeEnd(),
						"ageEnd"
				)
		);

		vparams.setMinYearStart(DEFAULT.getMinYearStart());
		vparams.setMaxYearStart(DEFAULT.getMaxYearStart());

		// Parameters.JSON_PROPERTY_YEAR_START
		vparams.setYearStart(
				getIntegerValue(
						params.getYearStart(), DEFAULT.getYearStart(), vparams.getMinYearStart(),
						vparams.getMaxYearStart(), "yearStart"
				)
		);

		vparams.setMinYearEnd(DEFAULT.getMinYearEnd());
		vparams.setMaxYearEnd(DEFAULT.getMaxYearEnd());

		// Parameters.JSON_PROPERTY_YEAR_END
		vparams.setYearEnd(
				getIntegerValue(
						params.getYearEnd(), DEFAULT.getYearEnd(), vparams.getMinYearEnd(), vparams.getMaxYearEnd(),
						"yearEnd"
				)
		);

		vparams.setMinAgeIncrement(DEFAULT.getMinAgeIncrement());
		vparams.setMaxAgeIncrement(DEFAULT.getMaxAgeIncrement());

		// Parameters.JSON_PROPERTY_AGE_INCREMENT
		vparams.setAgeIncrement(
				getIntegerValue(
						params.getAgeIncrement(), DEFAULT.getAgeIncrement(), vparams.getMinAgeIncrement(),
						vparams.getMaxAgeIncrement(), "ageIncrement"
				)
		);

		// Parameters.JSON_PROPERTY_FORCE_YEAR
		vparams.setForceYear(getIntegerValue(params.getForceYear(), DEFAULT.getForceYear(), null, null, "forceYear"));

		// Parameters.JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE
		if (params.getCombineAgeYearRange() == null) {
			vparams.setCombineAgeYearRange(DEFAULT.getCombineAgeYearRange());
		} else {
			String rangeValue = params.getCombineAgeYearRange();
			try {
				var e = Parameters.AgeYearRangeCombinationKind.fromValue(rangeValue);
				vparams.setCombineAgeYearRange(e);
			} catch (IllegalArgumentException e) {
				recordValidationMessage(UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION, rangeValue);
			}
		}

		// Parameters.JSON_PROPERTY_PROGRESS_FREQUENCY
		if (params.getProgressFrequency() == null) {
			vparams.setProgressFrequency(DEFAULT.getProgressFrequency());
		} else {
			String text = params.getProgressFrequency();
			try {
				var frequency = new ProgressFrequency(text);
				vparams.setProgressFrequency(frequency);
			} catch (IllegalArgumentException e) {
				recordValidationMessage(INVALID_PROCESS_FREQUENCY_VALUE, text);
			}
		}

		// Parameters.JSON_PROPERTY_METADATA_TO_OUTPUT
		if (params.getMetadataToOutput() == null) {
			vparams.setMetadataToOutput(DEFAULT.getMetadataToOutput());
		} else {
			String text = params.getMetadataToOutput();
			try {
				var e = Parameters.MetadataToOutputDirective.fromValue(text);
				vparams.setMetadataToOutput(e);
			} catch (IllegalArgumentException e) {
				recordValidationMessage(INVALID_METADATA_TO_OUTPUT_VALUE, text);
			}
		}

		// Parameters.JSON_PROPERTY_FILTERS
		if (params.getFilters() == null) {
			vparams.setFilters(DEFAULT.getFilters().copy());
		} else {
			vparams.setFilters(params.getFilters());
		}

		// Parameters.JSON_PROPERTY_UTILS
		if (params.getUtils() == null) {
			vparams.setUtils(DEFAULT.getUtils());
		} else {
			List<ValidatedUtilizationParameter> upList = new ArrayList<>();
			for (var up : params.getUtils()) {
				boolean isValidUtilizationParameter = true;

				if (SP0Name.forText(up.getSpeciesName()).equals(SP0Name.UNKNOWN)) {
					recordValidationMessage(UNKNOWN_SPECIES_GROUP_NAME, up.getSpeciesName());
					isValidUtilizationParameter = false;
				}

				UtilizationParameter.UtilizationClass uc = null;
				try {
					uc = UtilizationParameter.UtilizationClass.fromValue(up.getUtilizationClass());
				} catch (IllegalArgumentException e) {
					recordValidationMessage(UNKNOWN_UTILIZATION_CLASS_NAME, up.getUtilizationClass());
					isValidUtilizationParameter = false;
				}

				if (isValidUtilizationParameter) {
					upList.add(
							new ValidatedUtilizationParameter().speciesName(up.getSpeciesName()).utilizationClass(uc)
					);
				}
			}
			vparams.setUtils(upList);
		}

		state.setValidatedParams(vparams);
	}

	private void validateRequestParametersCollectively(ProjectionState state) {

		if (state.getValidatedParams() == null) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}: parameters have not yet been individually validated.", state.getProjectionId()
					)
			);
		}

		ValidatedParameters vparams = state.getValidatedParams();
		if (vparams.getOutputFormat() != null) {

			if (!vparams.getOutputFormat().equals(OutputFormat.DCSV) //
					&& vparams.getAgeStart() == null //
					&& vparams.getYearStart() == null //
					&& vparams.getForceYear() == null //
					&& !vparams.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES) //
					&& !vparams.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				recordValidationMessage(MISSING_START_CRITERIA);
			}

			if (!vparams.getOutputFormat().equals(OutputFormat.DCSV) //
					&& vparams.getAgeEnd() == null //
					&& vparams.getYearEnd() == null //
					&& vparams.getForceYear() == null //
					&& !vparams.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES) //
					&& !vparams.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				recordValidationMessage(MISSING_END_CRITERIA);
			}

			if (state.getRequestKind() == ProjectionRequestKind.DCSV && vparams.getOutputFormat() != OutputFormat.DCSV
					|| state.getRequestKind() != ProjectionRequestKind.DCSV
							&& vparams.getOutputFormat() == OutputFormat.DCSV) {
				recordValidationMessage(MISMATCHED_INPUT_OUTPUT_TYPES);
			}

			if (vparams.getOutputFormat() == OutputFormat.DCSV) {
				if (vparams.getAgeStart() != null || vparams.getAgeEnd() != null //
						|| vparams.getYearStart() != null || vparams.getYearEnd() != null) {

					recordValidationMessage(AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT);
					vparams.ageStart(null).ageEnd(null).yearStart(null).yearEnd(null).ageIncrement(null);
				}

				int forceParamCount = 0;
				if (vparams.getForceYear() != null) {
					forceParamCount += 1;
				}
				if (vparams.containsOption(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {
					forceParamCount += 1;
				}
				if (vparams.containsOption(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {
					forceParamCount += 1;
				}

				if (forceParamCount != 1) {
					recordValidationMessage(MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT //
							, ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES //
							, ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES //
							, "forceYear"
					);
				}
			}

			// When selecting CSV Output for CFS Biomass, ensure neither of the MoF Volume
			// nor MoF Biomass have also been selected as the CFS Biomass has different
			// output columns than for MoF Volume/Biomass.

			if (vparams.getOutputFormat() == OutputFormat.CSV_YIELD_TABLE //
					&& vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
					&& (vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)
							|| vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES))) {

				recordValidationMessage(CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS);
			}
		}

		if (vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
				&& vparams.getOutputFormat() != OutputFormat.CSV_YIELD_TABLE //
				&& vparams.getOutputFormat() != OutputFormat.YIELD_TABLE) {

			recordValidationMessage(
					INVALID_CFS_BIOMASS_OUTPUT_FORMAT, OutputFormat.CSV_YIELD_TABLE, OutputFormat.YIELD_TABLE
			);
		}

		// Prevent selection of both MoF Biomass and CFS Biomass because
		// both require different selections of Species Utilization
		// Levels. Once we get to being able to reset Utilization Levels
		// on the same projection, then we will be able to select both
		// MoF and CFS Biomass output selections.

		if (vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
				&& vparams.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

			recordValidationMessage(CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT);
		}
	}

	private void recordValidationMessage(ValidationMessageKind kind, Object... args) {
		ValidationMessage message = new ValidationMessage(kind, args);
		validationErrorMessages.add(message);
	}

	private Integer
			getIntegerValue(String text, Integer defaultValue, Integer minValue, Integer maxValue, String fieldName) {

		Integer value;

		try {
			if (text == null) {
				value = defaultValue;
			} else {
				value = Integer.valueOf(text);
				if (value == Parameters.LEGACY_NULL_VALUE) {
					value = defaultValue;
				}
			}
		} catch (NumberFormatException e) {
			recordValidationMessage(INVALID_INTEGER_VALUE, text, fieldName);
			return null;
		}

		if (value != null && minValue != null && value < minValue) {
			recordValidationMessage(INTEGER_VALUE_TOO_LOW, text, fieldName, minValue);
			return null;
		}

		if (value != null && maxValue != null && value > maxValue) {
			recordValidationMessage(INTEGER_VALUE_TOO_HIGH, text, fieldName, maxValue);
			return null;
		}

		return value;
	}
}
