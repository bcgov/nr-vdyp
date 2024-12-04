package ca.bc.gov.nrs.vdyp.backend.model.v1;

public enum ValidationMessageKind {
	UNRECOGNIZED_OUTPUT_FORMAT("{0} is not a recognized output format", Category.ERROR),
	UNRECOGNIZED_EXECUTION_OPTION("{0} is not a recognized execution option", Category.ERROR),
	UNRECOGNIZED_DEBUG_OPTION("{0} is not a recognized debug option", Category.ERROR),
	UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION("{0} is not a recognized CombineAgeYearRangeEnum value", Category.ERROR),
	INVALID_PROCESS_FREQUENCY_VALUE("{0} is not a recognized CombineAgeYearRangeEnum value", Category.ERROR),
	INVALID_METADATA_TO_OUTPUT_VALUE("{0} is not a recognized MetadataToOutputEnum value", Category.ERROR),
	UNKNOWN_SPECIES_GROUP_NAME("Species Group name \"{0}\" is not a known Species Group name", Category.ERROR),
	UNKNOWN_UTILIZATION_CLASS_NAME("Utilization Class \"{0}\" is not a known Utilization Class name", Category.ERROR),
	MISSING_START_CRITERIA("At least one of \"ageStart\" or \"yearStart\" must be given", Category.ERROR),
	MISSING_END_CRITERIA("At least one of \"ageEnd\" or \"yearEnd\" must be given", Category.ERROR),
	MISMATCHED_INPUT_OUTPUT_TYPES(
			"DCSV output Format can be selected when, and only when, the input format is DCSV", Category.ERROR
	),
	AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT(
			"Age range yield table parameters are ignored when DCSV output format is requested", Category.WARNING
	),
	MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT(
			"Exactly one of \"{0}\", \"{1}\" and \"{2}\" must be specified with the DCSV Output Format", Category.ERROR
	),
	INVALID_CFS_BIOMASS_OUTPUT_FORMAT(
			"CFS Biomass output is only supported for {0} and {1} output formats", Category.ERROR
	),
	CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT(
			"MoF and CFS Biomass Output can not be selected at the same time", Category.ERROR
	),
	CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS(
			"For CSV Yield Table of CFS Biomass, neither MoF Volume or Biomass may also be selected.", Category.ERROR
	), INVALID_INTEGER_VALUE("Field {1}''s value \"{0}\" is not an integer", Category.ERROR),
	INTEGER_VALUE_TOO_LOW("Field {1}''s value \"{0}\" is not an integer", Category.ERROR),
	INTEGER_VALUE_TOO_HIGH("Field {1}''s value \"{0}\" is not an integer", Category.ERROR),;

	public enum Category {
		ERROR, WARNING, INFO
	};

	public final String template;
	public final Category category;

	ValidationMessageKind(String template, Category category) {
		this.template = template;
		this.category = category;
	}
}
