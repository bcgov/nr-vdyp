package ca.bc.gov.nrs.vdyp.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelParameters(
		@JsonProperty("species") List<ModelSpecies> species, @JsonProperty("derivedBy") String derivedBy,
		@JsonProperty("becZone") String becZone, @JsonProperty("ecoZone") String ecoZone,
		@JsonProperty("siteIndex") String siteIndex, @JsonProperty("siteSpecies") String siteSpecies,
		@JsonProperty("computedValue") String computedValue, @JsonProperty("ageYears") String ageYears,
		@JsonProperty("speciesAge") String speciesAge, @JsonProperty("speciesHeight") String speciesHeight,
		@JsonProperty("bha50SiteIndex") String bha50SiteIndex, @JsonProperty("computedValue2") String computedValue2,
		@JsonProperty("ageYears2") String ageYears2, @JsonProperty("speciesAge2") String speciesAge2,
		@JsonProperty("speciesHeight2") String speciesHeight2, @JsonProperty("bha50SiteIndex2") String bha50SiteIndex2,
		@JsonProperty("computedValue3") String computedValue3, @JsonProperty("ageYears3") String ageYears3,
		@JsonProperty("speciesAge3") String speciesAge3, @JsonProperty("speciesHeight3") String speciesHeight3,
		@JsonProperty("bha50SiteIndex3") String bha50SiteIndex3, @JsonProperty("computedValue4") String computedValue4,
		@JsonProperty("ageYears4") String ageYears4, @JsonProperty("speciesAge4") String speciesAge4,
		@JsonProperty("speciesHeight4") String speciesHeight4, @JsonProperty("bha50SiteIndex4") String bha50SiteIndex4,
		@JsonProperty("computedValue5") String computedValue5, @JsonProperty("ageYears5") String ageYears5,
		@JsonProperty("speciesAge5") String speciesAge5, @JsonProperty("speciesHeight5") String speciesHeight5,
		@JsonProperty("bha50SiteIndex5") String bha50SiteIndex5, @JsonProperty("computedValue6") String computedValue6,
		@JsonProperty("ageYears6") String ageYears6, @JsonProperty("speciesAge6") String speciesAge6,
		@JsonProperty("speciesHeight6") String speciesHeight6, @JsonProperty("bha50SiteIndex6") String bha50SiteIndex6,
		@JsonProperty("stockable") Float stockable, @JsonProperty("cc") Float crownClosure,
		@JsonProperty("BA") Float basalArea, @JsonProperty("TPH") Float tph,
		@JsonProperty("minDBHLimit") String minDBHLimit, @JsonProperty("currentDiameter") String currentDiameter
) {
	public record ModelSpecies(@JsonProperty("code") String speciesCode, @JsonProperty("percent") Float percent) {
	}
}
