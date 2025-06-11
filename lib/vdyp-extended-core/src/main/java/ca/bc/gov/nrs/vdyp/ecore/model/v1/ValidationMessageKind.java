package ca.bc.gov.nrs.vdyp.ecore.model.v1;

public enum ValidationMessageKind {
	GENERIC("{0}"), //
	AGE_RANGES_IGNORED_WHEN_DCSV_OUTPUT(
			"Age range yield table parameters are ignored when DCSV output format is requested"
	), CANNOT_SPECIFY_BOTH_CFS_AND_MOF_BIOMASS_OUTPUT("MoF and CFS Biomass output cannot be selected at the same time"),
	CANNOT_SPECIFY_BOTH_CFS_BIOMASS_AND_EITHER_MOF_OPTIONS(
			"For CSV Yield Table of CFS Biomass, neither MoF volume or biomass may also be selected"
	), //
	DUPLICATE_LAYER_SUPPLIED("Polygon {0}: Layer \"{1}\" has already been supplied"),
	EXPECTED_STREAMS_NOT_SUPPLIED("The following input file types were not supplied: {0}"),
	INTEGER_VALUE_TOO_HIGH("Field \"{1}\"''s value \"{0}\" is above the maximum value \"{2}\""),
	INTEGER_VALUE_TOO_LOW("Field \"{1}\"''s value \"{0}\" is below the minimum value \"{2}\""),
	INVALID_CFS_BIOMASS_OUTPUT_FORMAT("CFS Biomass output is only supported for {0} and {1} output formats"),
	INVALID_CODE("Polygon {0}: field \"{1}\" value \"{2}\" is not a recognized value for this code"),
	INVALID_METADATA_TO_OUTPUT_VALUE("{0} is not a recognized metadata-to-output value"),
	INVALID_PROCESS_FREQUENCY_VALUE("{0} is not a recognized progress-frequency value"),
	INVALID_INTEGER_VALUE("Field \"{1}\"''s value \"{0}\" is not an integer"),
	MISMATCHED_INPUT_OUTPUT_TYPES("DCSV output format can be selected when, and only when, the input format is DCSV"),
	MISSING_BEC_ZONE("Polygon {0}: bec zone missing"),
	MISSING_END_CRITERIA("At least one of \"ageEnd\" or \"yearEnd\" must be given"),
	MISSING_LAYER_CODE("Polygon {0}: layer with VDYP7 layer code \"{1}\" is missing the required layer level code"),
	MISSING_SPECIES_NAME("Polygon {0}: layer with id \"{1}\", species #{2}, is missing a name"),
	MISSING_START_CRITERIA("At least one of \"ageStart\" or \"yearStart\" must be given"),
	MUST_BE_EXACTLY_ONE_FORCE_PARAM_WHEN_DCSV_OUTPUT(
			"Exactly one of \"{0}\", \"{1}\" and \"{2}\" must be specified with the DCSV Output Format"
	), //
	NO_LEADING_SPECIES("Layer {0}: no leading site species could be found"), //
	NON_TREE_COVERAGE_PERCENTAGES_EXCEED_100(
			"Polygon {0}: other vegetation coverage percentages, in total, exceed 100%"
	),
	NON_VEGETATION_COVERAGE_PERCENTAGES_EXCEED_100(
			"Polygon {0}: non-vegetation coverage percentages, in total, exceed 100%"
	), //
	NOT_A_NUMBER("Polygon {0}: field \"{1}\" value \"{2}\" is not a number"),
	NUMBER_OUT_OF_RANGE(
			"Polygon {0}: field \"{1}\" value \"{2}\" is either not a number or out of range. Must be between {3} and {4}, inclusive"
	), //
	PERCENTAGES_INCREASING("Polygon {0} Layer {1}: percent {2} at index {3} is larger than a preceding percentage"),
	PRIMARY_LAYER_NOT_FOUND("Polygon {0}: no primary layer found for any projection type"), //
	UNEXPECTED_STREAMS_SUPPLIED("The following input file types were supplied, but not expected: {0}"),
	UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION("{0} is not a recognized combine-year-age-range value"),
	UNSUPPORTED_COMBINE_AGE_YEAR_RANGE_OPTION("only the INTERSECT combine-year-age-range option is supported"),
	UNRECOGNIZED_DEBUG_OPTION("{0} is not a recognized debug option"),
	UNRECOGNIZED_EXECUTION_OPTION("{0} is not a recognized execution option"),
	UNRECOGNIZED_INVENTORY_STANDARD_CODE("Polygon {0}: inventory standard code {1} is not recognized"),
	UNRECOGNIZED_OUTPUT_FORMAT("{0} is not a recognized output format"),
	UNRECOGNIZED_SPECIES("Polygon {0}: layer with id \"{1}\" contains an unrecognized species code \"{2}\""), //
	UNRECOGNIZED_SPECIES_GROUP_NAME("Species group name \"{0}\" is not a known species group"),
	UNRECOGNIZED_UTILIZATION_CLASS_NAME("Utilization class \"{0}\" is not a known utilization class name"), //
	;

	public enum Category {
		ERROR, WARNING, INFO
	};

	public final String template;

	ValidationMessageKind(String template) {
		this.template = template;
	}
}
