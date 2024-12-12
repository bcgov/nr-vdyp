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
	INTEGER_VALUE_TOO_HIGH("Field {1}''s value \"{0}\" is not an integer", Category.ERROR),
	
	EXPECTED_STREAMS_NOT_SUPPLIED("The following input file types were not supplied: {0}", Category.ERROR),
	UNEXPECTED_STREAMS_SUPPLIED("The following input file types were supplied, but not expected: {0}", Category.ERROR),
	
	MISSING_INVENTORY_STANDARD_CODE("Polygon {0}: missing inventory standard code", Category.ERROR),
	INVALID_PERCENT_STOCKABLE("Polygon {0}: percent stockable value {1} is not a percentage", Category.ERROR),
	INVALID_PERCENT_STOCKABLE_DEAD("Polygon {0}: percent stockable dead value {1} is not a percentage", Category.ERROR),
	MISSING_BEC_ZONE("Polygon {0}: bec zone missing", Category.ERROR), 
	YIELD_FACTOR_OUT_OF_RANGE("Polygon {0}: yield factor {} is out of range. Must be between 0.0 and 10.0, inclusive", Category.ERROR),
	INVALID_REFERENCE_YEAR("Polygon {0}: reference year must be between {1} and {2}, inclusive. The value {3} is out of range", Category.ERROR),
	INVALID_YEAR_OF_DEATH("Polygon {0}: year of death must be between {1} and {2}, inclusive. The value {3} is out of range", Category.ERROR), 
	INVALID_CFS_ECO_ZONE("Polygon {0}: CFS Eco Zone code {1} does not identify a Cfs Eco Zone", Category.ERROR),
	;

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
