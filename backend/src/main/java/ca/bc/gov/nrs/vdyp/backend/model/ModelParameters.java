package ca.bc.gov.nrs.vdyp.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelParameters(
		@JsonProperty("species") List<ModelSpecies> species, @JsonProperty("derivedBy") String derivedBy,
		@JsonProperty("becZone") String becZone, @JsonProperty("ecoZone") String ecoZone,
		@JsonProperty("siteIndex") String siteIndex, @JsonProperty("siteSpecies") String siteSpecies,
		@JsonProperty("compute") String compute, @JsonProperty("ageYears") String ageYears,
		@JsonProperty("speciesAge") String speciesAge, @JsonProperty("speciesHeight") String speciesHeight,
		@JsonProperty("bha50SiteIndex") String bha50SiteIndex, @JsonProperty("compute2") String compute2,
		@JsonProperty("ageYears2") String ageYears2, @JsonProperty("age2") String age2,
		@JsonProperty("height2") String height2, @JsonProperty("si2") String si2,
		@JsonProperty("compute3") String compute3, @JsonProperty("ageYears3") String ageYears3,
		@JsonProperty("age3") String age3, @JsonProperty("height3") String height3, @JsonProperty("si3") String si3,
		@JsonProperty("compute4") String compute4, @JsonProperty("ageYears4") String ageYears4,
		@JsonProperty("age4") String age4, @JsonProperty("height4") String height4, @JsonProperty("si4") String si4,
		@JsonProperty("compute5") String compute5, @JsonProperty("ageYears5") String ageYears5,
		@JsonProperty("age5") String age5, @JsonProperty("height5") String height5, @JsonProperty("si5") String si5,
		@JsonProperty("compute6") String compute6, @JsonProperty("ageYears6") String ageYears6,
		@JsonProperty("age6") String age6, @JsonProperty("height6") String height6, @JsonProperty("si6") String si6,
		@JsonProperty("stockable") Float stockable, @JsonProperty("cc") Float crownClosure,
		@JsonProperty("BA") Float basalArea, @JsonProperty("TPH") Float tph,
		@JsonProperty("minDBHLimit") String minDBHLimit, @JsonProperty("currentDiameter") String currentDiameter
) {
	public record ModelSpecies(@JsonProperty("code") String speciesCode, @JsonProperty("percent") Float percent) {
	}
}
