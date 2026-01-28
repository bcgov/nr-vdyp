package ca.bc.gov.nrs.vdyp.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelParameters(
		@JsonProperty("species") List<ModelSpecies> species, @JsonProperty("derivedBy") String derivedBy,
		@JsonProperty("becZone") String becZone, @JsonProperty("ecoZone") String ecoZone,
		@JsonProperty("siteIndex") String siteIndex, @JsonProperty("siteSpecies") String siteSpecies,
		@JsonProperty("ageYears") String ageYears, @JsonProperty("speciesAge") String speciesAge,
		@JsonProperty("speciesHeight") String speciesHeight, @JsonProperty("bha50SiteIndex") String bha50SiteIndex,
		@JsonProperty("stockable") Float stockable, @JsonProperty("cc") Float crownClosure,
		@JsonProperty("BA") Float basalArea, @JsonProperty("TPH") Float tph,
		@JsonProperty("minDBHLimit") String minDBHLimit, @JsonProperty("currentDiameter") String currentDiameter
) {
	public record ModelSpecies(@JsonProperty("code") String speciesCode, @JsonProperty("percent") Float percent) {
	}
}
