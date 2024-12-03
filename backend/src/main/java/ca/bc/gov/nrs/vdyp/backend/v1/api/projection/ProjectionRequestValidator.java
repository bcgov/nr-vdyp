package ca.bc.gov.nrs.vdyp.backend.v1.api.projection;

import static ca.bc.gov.nrs.vdyp.backend.v1.api.projection.ValidatedParameters.DEFAULT;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters.OutputFormatEnum;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters.SelectedDebugOptionsEnum;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.Parameters.SelectedExecutionOptionsEnum;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

public class ProjectionRequestValidator {

	private List<String> validationErrorMessages = new ArrayList<>();

	public static void validate(ProjectionState state, Map<String, InputStream> streams)
			throws ProjectionRequestValidationException {

		var validator = new ProjectionRequestValidator();

		validator.validateState(state);
		validator.validateStreams(streams);

		if (validator.validationErrorMessages.size() > 0) {
			throw new ProjectionRequestValidationException(validator.validationErrorMessages);
		}
	}

	private void validateStreams(Map<String, InputStream> streams) throws ProjectionRequestValidationException {
	}

	private void validateState(ProjectionState state) throws ProjectionRequestValidationException {

		validateRequestParametersIndividually(state);
		validateRequestParametersCollectively(state);
	}

	private void validateRequestParametersIndividually(ProjectionState state)
			throws ProjectionRequestValidationException {

		Parameters params = state.getParams();
		ValidatedParameters vparams = new ValidatedParameters();

		// Parameters.JSON_PROPERTY_OUTPUT_FORMAT

		if (params.getOutputFormatText() == null) {
			vparams.outputFormat(DEFAULT.getOutputFormat());
		} else {
			try {
				var outputFormat = OutputFormatEnum.fromValue(params.getOutputFormatText());
				vparams.setOutputFormat(outputFormat);
			} catch (IllegalArgumentException e) {
				recordValidationError("{0} is not a recognized output format", params.getOutputFormatText());
			}
		}

		// Parameters.JSON_PROPERTY_SELECTED_EXECUTION_OPTIONS

		if (params.getSelectedExecutionOptionsText() == null) {
			vparams.selectedExecutionOptions(DEFAULT.getSelectedExecutionOptions());
		} else {
			List<SelectedExecutionOptionsEnum> selectedOptions = new ArrayList<>();
			for (String optionText : params.getSelectedExecutionOptionsText()) {
				try {
					var e = Parameters.SelectedExecutionOptionsEnum.fromValue(optionText);
					selectedOptions.add(e);
				} catch (IllegalArgumentException e) {
					recordValidationError("{0} is not a recognized execution option", optionText);
				}
			}
			vparams.selectedExecutionOptions(selectedOptions);
		}

		// Parameters.JSON_PROPERTY_SELECTED_DEBUG_OPTIONS

		if (params.getSelectedDebugOptionsText() == null) {
			vparams.selectedDebugOptions(DEFAULT.getSelectedDebugOptions());
		} else {
			List<SelectedDebugOptionsEnum> selectedOptions = new ArrayList<>();
			for (String optionText : params.getSelectedDebugOptionsText()) {
				try {
					var e = Parameters.SelectedDebugOptionsEnum.fromValue(optionText);
					selectedOptions.add(e);
				} catch (IllegalArgumentException e) {
					recordValidationError("{0} is not a recognized debug option", optionText);
				}
			}
			vparams.selectedDebugOptions(selectedOptions);
		}

		vparams.setMinAgeStart(DEFAULT.getMinAgeEnd());
		vparams.setMaxAgeStart(DEFAULT.getMaxAgeStart());

		// Parameters.JSON_PROPERTY_AGE_START
		vparams.setAgeStart(
				getIntegerValue(
						params.getAgeStartText(), DEFAULT.getAgeStart(), vparams.getMinAgeStart(),
						vparams.getMinAgeEnd(), "ageEnd"
				)
		);

		vparams.setMinAgeEnd(DEFAULT.getMinAgeEnd());
		vparams.setMaxAgeEnd(DEFAULT.getMaxAgeEnd());

		// Parameters.JSON_PROPERTY_AGE_END
		vparams.setAgeEnd(
				getIntegerValue(
						params.getAgeEndText(), DEFAULT.getAgeEnd(), vparams.getMinAgeEnd(), vparams.getMaxAgeEnd(),
						"ageEnd"
				)
		);

		vparams.setMinYearStart(DEFAULT.getMinYearStart());
		vparams.setMaxYearStart(DEFAULT.getMaxYearStart());

		// Parameters.JSON_PROPERTY_YEAR_START
		vparams.setYearStart(
				getIntegerValue(
						params.getYearStartText(), DEFAULT.getYearStart(), vparams.getMinYearStart(),
						vparams.getMaxYearStart(), "yearStart"
				)
		);

		vparams.setMinYearEnd(DEFAULT.getMinYearEnd());
		vparams.setMaxYearEnd(DEFAULT.getMaxYearEnd());

		// Parameters.JSON_PROPERTY_YEAR_END
		vparams.setYearEnd(
				getIntegerValue(
						params.getYearEndText(), DEFAULT.getYearEnd(), vparams.getMinYearEnd(), vparams.getMaxYearEnd(),
						"yearEnd"
				)
		);

		vparams.setMinAgeIncrement(DEFAULT.getMinAgeIncrement());
		vparams.setMaxAgeIncrement(DEFAULT.getMaxAgeIncrement());

		// Parameters.JSON_PROPERTY_AGE_INCREMENT
		vparams.setAgeIncrement(
				getIntegerValue(
						params.getAgeIncrementText(), DEFAULT.getAgeIncrement(), vparams.getMinAgeIncrement(),
						vparams.getMaxAgeIncrement(), "ageIncrement"
				)
		);

		// Parameters.JSON_PROPERTY_FORCE_YEAR
		vparams.setForceYear(
				getIntegerValue(params.getForceYearText(), DEFAULT.getForceYear(), null, null, "forceYear")
		);

		// Parameters.JSON_PROPERTY_COMBINE_AGE_YEAR_RANGE
		if (params.getCombineAgeYearRangeText() == null) {
			vparams.setCombineAgeYearRange(DEFAULT.getCombineAgeYearRange());
		} else {
			String rangeValue = params.getCombineAgeYearRangeText();
			try {
				var e = Parameters.CombineAgeYearRangeEnum.fromValue(rangeValue);
				vparams.setCombineAgeYearRange(e);
			} catch (IllegalArgumentException e) {
				recordValidationError("{0} is not a recognized CombineAgeYearRangeEnum value", rangeValue);
			}
		}

		// Parameters.JSON_PROPERTY_PROGRESS_FREQUENCY
		if (params.getProgressFrequencyText() == null) {
			vparams.setProgressFrequency(DEFAULT.getProgressFrequency());
		} else {
			String text = params.getProgressFrequencyText();
			try {
				var frequency = new ProgressFrequency(text);
				vparams.setProgressFrequency(frequency);
			} catch (IllegalArgumentException e) {
				recordValidationError(
						"\"{0}\" is not a valid ProgressFrequency value - must be one of"
								+ " \"never\", \"polygon\", \"mapsheet\", or an integer",
						text
				);
			}
		}

		// Parameters.JSON_PROPERTY_METADATA_TO_OUTPUT
		if (params.getMetadataToOutputText() == null) {
			vparams.setMetadataToOutput(DEFAULT.getMetadataToOutput());
		} else {
			String text = params.getMetadataToOutputText();
			try {
				var e = Parameters.MetadataToOutputEnum.fromValue(text);
				vparams.setMetadataToOutput(e);
			} catch (IllegalArgumentException e) {
				recordValidationError("{0} is not a recognized MetadataToOutputEnum value", text);
			}
		}

		// Parameters.JSON_PROPERTY_FILTERS
		if (params.getFilters() == null) {
			params.setFilters(DEFAULT.getFilters().copy());
		}

		// Parameters.JSON_PROPERTY_UTILS
		if (params.getUtilsText() == null) {
			vparams.setUtils(DEFAULT.getUtils());
		} else {
			List<UtilizationParameter> upList = new ArrayList<>();
			for (var up : params.getUtilsText()) {
				boolean isValidUtilizationParameter = true;

				if (SP0Name.forText(up.getSpeciesName()).equals(SP0Name.UNKNOWN)) {
					recordValidationError(
							"Species Group name \"{0}\" is not a known Species Group name", up.getSpeciesName()
					);
					isValidUtilizationParameter = false;
				}

				UtilizationParameter.ValueEnum value = null;
				try {
					UtilizationParameter.ValueEnum.fromValue(up.getValue());
				} catch (IllegalArgumentException e) {
					recordValidationError(
							"Utilization Class \"{0}\" is not a known Utilization Class name", up.getValue()
					);
					isValidUtilizationParameter = false;
				}

				if (isValidUtilizationParameter) {
					upList.add(new UtilizationParameter().speciesName(up.getSpeciesName()).value(value));
				}
			}
		}

		state.setValidatedParams(vparams);
	}

	private void validateRequestParametersCollectively(ProjectionState state)
			throws ProjectionRequestValidationException {

		if (state.getParams() instanceof ValidatedParameters vparams) {

			if (!vparams.getOutputFormat().equals(OutputFormatEnum.DCSV) //
					&& vparams.getAgeStart() == null && vparams.getYearStart() == null //
					&& vparams.getForceYear() == null //
					&& !vparams.containsOption(
							SelectedExecutionOptionsEnum.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
					) //
					&& !vparams.containsOption(
							SelectedExecutionOptionsEnum.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
					)) {

				recordValidationError("At least one of \"ageStart\" or \"yearStart\" must be given");
			}

			if (!vparams.getOutputFormat().equals(OutputFormatEnum.DCSV) //
					&& vparams.getAgeEnd() == null && vparams.getYearEnd() == null //
					&& vparams.getForceYear() == null //
					&& !vparams.containsOption(
							SelectedExecutionOptionsEnum.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES
					) //
					&& !vparams.containsOption(
							SelectedExecutionOptionsEnum.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
					)) {

				recordValidationError("At least one of \"ageEnd\" or \"yearEnd\" must be given");
			}

			if (state.getKind() == ProjectionRequestKind.DCSV && vparams.getOutputFormat() != OutputFormatEnum.DCSV) {
				recordValidationError(
						"DCSV output Format can be selected when, and only when, the input format is DCSV .\n"
				);
			}

			if (vparams.getOutputFormat() == OutputFormatEnum.DCSV) {
				if (vparams.getAgeStart() != null || vparams.getAgeEnd() != null //
						|| vparams.getYearStart() != null || vparams.getYearEnd() != null) {

					recordValidationWarning(
							"Age range yield table parameters are ignored when DCSV output format is requested"
					);
					vparams.ageStart(null).ageEnd(null).yearStart(null).yearEnd(null).ageIncrement(null);
				}

				int forceParamCount = 0;
				if (vparams.getForceYear() != null) {
					forceParamCount += 1;
				}
				if (vparams
						.containsOption(SelectedExecutionOptionsEnum.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {
					forceParamCount += 1;
				}
				if (vparams.containsOption(
						SelectedExecutionOptionsEnum.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
				)) {
					forceParamCount += 1;
				}

				if (forceParamCount != 1) {
					recordValidationError(
							"Exactly one of \"{0}\", \"{1}\" and \"{2}\" must be specified with the DCSV Output Format." //
							, SelectedExecutionOptionsEnum.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES //
							, SelectedExecutionOptionsEnum.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES //
							, "forceYear"
					);
				}
			}
		} else {
			throw new IllegalStateException("Expecting parameters have already been validated");
		}

		if (vparams.containsOption(SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
				&& vparams.getOutputFormat() != OutputFormatEnum.CSV_YIELD_TABLE //
				&& vparams.getOutputFormat() != OutputFormatEnum.YIELD_TABLE) {

			recordValidationError(
					"CFS Biomass output is only supported for {0} and {1} output formats",
					OutputFormatEnum.CSV_YIELD_TABLE, OutputFormatEnum.YIELD_TABLE
			);
		}

		// Prevent selection of both MoF Biomass and CFS Biomass because
		// both require different selections of Species Utilization
		// Levels. Once we get to being able to reset Utilization Levels
		// on the same projection, then we will be able to select both
		// MoF and CFS Biomass output selections.

		if (vparams.containsOption(SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
				&& vparams.containsOption(SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

			recordValidationError("MoF and CFS Biomass Output can not be selected at the same time.");
		}

		// When selecting CSV Output for CFS Biomass, ensure neither of the MoF Volume
		// nor MoF Biomass have also been selected as the CFS Biomass has different
		// output columns than for MoF Volume/Biomass.

		if (vparams.getOutputFormat() == OutputFormatEnum.CSV_YIELD_TABLE //
				&& vparams.containsOption(SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_CFS_BIOMASS) //
				&& vparams.containsOption(SelectedExecutionOptionsEnum.DO_INCLUDE_PROJECTED_MOF_BIOMASS)) {

			recordValidationError(
					"For CSV Yield Table of CFS Biomass, neither MoF Volume or Biomass may also be selected."
			);
		}
	}

	private void recordValidationError(String message, Object... args) {
		String formattedMessage = MessageFormat.format("Error: " + message, args);
		validationErrorMessages.add(formattedMessage);
	}

	private void recordValidationWarning(String message, Object... args) {
		String formattedMessage = MessageFormat.format("Warning: " + message, args);
		validationErrorMessages.add(formattedMessage);
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
			recordValidationError("Field {1}'s value \"{0}\" is not an integer", text, fieldName);
			return null;
		}

		if (value != null && minValue != null && value < minValue) {
			recordValidationError("Field {1}'s value \"{0}\" must be at least {2}", text, fieldName, minValue);
			return null;
		}

		if (value != null && maxValue != null && value < maxValue) {
			recordValidationError("Field {1}'s value \"{0}\" must be no more than {2}", text, fieldName, maxValue);
			return null;
		}

		return value;
	}
}
