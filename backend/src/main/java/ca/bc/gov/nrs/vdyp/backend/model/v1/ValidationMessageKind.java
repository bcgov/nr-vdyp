package ca.bc.gov.nrs.vdyp.backend.model.v1;

public enum ValidationMessageKind {
	UNRECOGNIZED_OUTPUT_FORMAT("{0} is not a recognized output format", Category.ERROR),
	UNRECOGNIZED_EXECUTION_OPTION("{0} is not a recognized execution option", Category.ERROR),
	UNRECOGNIZED_DEBUG_OPTION("{0} is not a recognized debug option", Category.ERROR),
	UNRECOGNIZED_COMBINE_AGE_YEAR_RANGE_OPTION("{0} is not a recognized CombineAgeYearRangeEnum value", Category.ERROR),
	INVALID_PROCESS_FREQUENCY_VALUE("{0} is not a recognized CombineAgeYearRangeEnum value", Category.ERROR),
	INVALID_METADATA_TO_OUTPUT_VALUE("{0} is not a recognized MetadataToOutputEnum value", Category.ERROR),
	UNRECOGNIZED_SPECIES_GROUP_NAME("Species Group name \"{0}\" is not a known Species Group name", Category.ERROR),
	UNRECOGNIZED_UTILIZATION_CLASS_NAME(
			"Utilization Class \"{0}\" is not a known Utilization Class name", Category.ERROR
	), // 
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
	), //
	INVALID_INTEGER_VALUE("Field \"{1}\"'s value \"{0}\" is not an integer", Category.ERROR),
	INTEGER_VALUE_TOO_LOW("Field \"{1}\"'s value \"{0}\" is not an integer", Category.ERROR),
	INTEGER_VALUE_TOO_HIGH("Field \"{1}\"'s value \"{0}\" is not an integer", Category.ERROR),

	EXPECTED_STREAMS_NOT_SUPPLIED("The following input file types were not supplied: {0}", Category.ERROR),
	UNEXPECTED_STREAMS_SUPPLIED("The following input file types were supplied, but not expected: {0}", Category.ERROR),

	MISSING_INVENTORY_STANDARD_CODE("Polygon {0}: missing inventory standard code", Category.ERROR),
	MISSING_BEC_ZONE("Polygon {0}: bec zone missing", Category.ERROR),
	NUMBER_OUT_OF_RANGE(
			"Polygon {0}: field \"{1}\" value \"{2}\" is either not a number or out of range. Must be between {3} and {4}, inclusive",
			Category.ERROR
	), //
	INVALID_CODE("Polygon {0}: field \"{1}\" value \"{2}\" is not a recognized value for this code", Category.ERROR),
	NOT_A_NUMBER("Polygon {0}: field \"{1}\" value \"{2}\" is not a number", Category.ERROR),
	NON_TREE_COVERAGE_PERCENTAGES_EXCEED_100(
			"Polygon {0}: other vegetation coverage percentages, in total, exceed 100%", Category.ERROR
	),
	NON_VEGETATION_COVERAGE_PERCENTAGES_EXCEED_100(
			"Polygon {0}: non-vegetation coverage percentages, in total, exceed 100%", Category.ERROR
	),
	MISSING_LAYER_CODE(
			"Polygon {0}: layer with VDYP7 layer code \"{1}\" is missing the required layer level code", Category.ERROR
	),
	UNRECOGNIZED_SPECIES(
			"Polygon {0}: layer with id \"{1}\" contains an unrecognized species code \"{2}\"", Category.ERROR
	), //
	MISSING_SPECIES_NAME("Polygon {0}: layer with id \"{1}\", species #{2}, is missing a name", Category.ERROR),
	UNRECOGNIZED_INVENTORY_STANDARD_CODE("Polygon {0}: Inventory Standard Code {1} is not recognized", Category.ERROR),
	UNRECOGNIZED_LAYER_SUMMARIZATION_MODE(
			"Polygon {0}: Layer Summarization Mode {1} is not recognized", Category.ERROR
	), //
	NO_PRIMARY_LAYER_SUPPLIED(
			"Polygon {0}: No primary layer supplied in the input; choosing Layer \"{1}\"", Category.WARNING
	), //
	DUPLICATE_LAYER_SUPPLIED("Polygon {0}: Layer \"{1}\" has already been supplied", Category.ERROR),
	LAYER_STOCKABILITY_EXCEEDS_POLYGON_STOCKABILITY(
			"Polygon {0}: the stockability percentage \"{2}\" of layer \"{1}\" exceeds that of the polygon \"{3}\"",
			Category.ERROR
	), //
	POLYGON_ALREADY_HAS_RANK_ONE_LAYER("Polygon {0}: polygon already has a rank 1 layer", Category.ERROR), 
	PERCENTAGES_INCREASING("Polygon {0} Layer {1}: percent {2} at index {3} is larger than a preceding percentage", Category.ERROR), 
	DUPLICATE_SPECIES("Polygon {0} Layer {1}: species '{2}' appears more than once in the layer definition", Category.WARNING), 
	INCONSISTENT_SITE_INFO("Polygon {0} Layer {1}: species definitions for species '{3}' have inconsistent site information", Category.WARNING), 
	LOW_SITE_INDEX_ERROR("Polygon {0} Layer {1}: very low or null site index {2} for species '{3}'. Projected Dominant Height set to input height", Category.ERROR),
	LOW_SITE_INDEX_WARNING("Polygon {0} Layer {1}: very low or null site index {2} for species '{3}'. Projected Dominant Height set to input height", Category.WARNING), 
	NO_LEADING_SPECIES("Polygon {0} Layer {1}: no leading site species could be found", Category.ERROR), 
	SPECIES_WITH_NO_STAND_OR_AGE("Polygon {0} Layer {1}: Species '{2}' has no age but requires that a new Stand be created, which requires the age be known", Category.ERROR),
	PRIMARY_LAYER_NOT_FOUND("Polygon {0}: no primary layer found", Category.ERROR),
	GENERIC("{0}", Category.ERROR);
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
