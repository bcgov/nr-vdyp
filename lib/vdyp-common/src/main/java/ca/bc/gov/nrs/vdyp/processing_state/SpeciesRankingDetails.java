package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Optional;

public record SpeciesRankingDetails(
		int primarySpeciesIndex, Optional<Integer> secondarySpeciesIndex, int inventoryTypeGroup, int basalAreaGroup1,
		int basalAreaGroup3
) {
}