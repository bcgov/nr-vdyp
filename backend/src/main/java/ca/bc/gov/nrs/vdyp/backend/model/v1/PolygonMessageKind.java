package ca.bc.gov.nrs.vdyp.backend.model.v1;

public enum PolygonMessageKind {
	ASSIGNING_ESTIMATED_SITE_INDEX(
			"directly assigning estimated site index of {0} to species \"{1}\" and recomputing input height"
	), //
	BAD_STAND_DEFINITION("stand definition insufficient for FIPSTART Processing"), //
	BREAST_HEIGHT_AGE_TOO_YOUNG(
			"breast height age too young to generate a stand description. FIPSTART return code: {0}"
	), //
	COPIED_BASAL_AREA_FROM_SUPPLIED_LAYER(
			"starting values for basal area copied over for QA and alternative model used for layer"
	), //
	COPIED_TPH_FROM_SUPPLIED_LAYER(
			"starting values for trees-per-hectare copied over for QA and alternative model used for layer"
	), //
	DUPLICATE_SPECIES("species \"{0}\" appears more than once in the layer definition"),
	ESTIMATE_APPLIED_FROM_OTHER_SPECIES("estimated site index applied from older species {0} to younger species {1}"), //
	ESTIMATED_SI_UNAVAILABLE(
			"Estimated site index required but unavailable for species \"{0}\" at age {1}. Site index computed directly from age and height"
	), //
	GENERIC_FIPSTART_ERROR("an error occurred processing the FIPSTART model. FIPSTART return code: {0}"), //
	GENERIC_VRISTART_ERROR("an error occurred processing the VRISTART model. VRISTART return code: {0}"), //
	INCONSISTENT_SITE_INFO("species definitions for species \"{0}\" have inconsistent site information"),
	LAYER_DETAILS_MISSING(
			"VRISTART reported that basal area or trees-per-hectare was not supplied or quad-mean-diameter"
					+ " is less than 7.5 cm. Projected volumes will be suppressed."
	),
	LAYER_NOT_COMPLETELY_DEFINED(
			"layer is not completely, or is not consistently, defined; projection of layer disabled"
	), //
	LAYER_NOT_PROJECTED("both FORWARD and BACK not executed. A yield table will not be produced for this layer"), //
	LAYER_PERCENTAGES_TOO_INACCURATE(
			"total percent is different from 100% by more than 1%; can't project (percent = {0})"
	), //
	LAYER_STOCKABILITY_EXCEEDS_POLYGON_STOCKABILITY(
			"the stockability percentage \"{0}\" of layer exceeds that of the polygon \"{1}\""
	), //
	LOW_SITE("primary layer height was too short to generate a stand description. FIPSTART return code: {0}"), //
	LOW_SITE_INDEX_WARNING(
			"very low or null site index {0} for species \"{1}\". {2} set to species {2} value prior to projection"
	), //
	LOW_SITE_INDEX_WARNING_2("very low site index {0}. Site index not calculated"),
	LOW_SITE_INDEX_WARNING_3("site index could not be calculated"),
	NO_CC(
			"crown closure was not supplied and there is no leading sp64 from which it can be"
					+ " determined. Disabling projection of layer"
	), //
	NO_PRIMARY_LAYER_SUPPLIED("no primary layer supplied in the input; choosing layer \"{0}\""), //
	NO_VIABLE_STAND_DESCRIPTION("stand description not possible after 80 years of projection. {0} return code: {1}"), //
	MISSING_YEARS_TO_BREAST_HEIGHT("species missing years-to-breast-height value."), //
	NO_LEADING_SPECIES("no leading site species could be found"), //
	NO_YIELD_TABLE_FOR_DEAD_LAYER("suppressing CFS Biomass output for dead layer. No yield table will be produced."), //
	NO_PROJECTED_DATA("projected data for the {0} layer was not generated at calendar year {1,number,#}"), //
	POLYGON_ALREADY_HAS_RANK_ONE_LAYER("polygon already has a rank 1 layer"),
	PREDICATED_BASAL_AREA_TOO_SMALL("predicted basal area too small to be used. VRISTART return code: {0}"), //
	REASSIGNED_HEIGHT("based on estimated site index of {0}, recomputed input height at age {1} to be {2}"), //
	UNRECOGNIZED_GROWTH_MODEL("attempt to process an unrecognized growth model {0}"), //
	USING_DEFAULT_CC("CC was not supplied. Using a default CC of {0}% for this (leading) species ({1})"), //
	;

	public enum Category {
		ERROR, WARNING, INFO
	};

	public final String template;

	PolygonMessageKind(String template) {
		this.template = template;
	}

	public String getTemplate() {
		return template;
	}
}
