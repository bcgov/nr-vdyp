package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

/**
 * All fields (interim and final) for Calculated Cfs Biomass Volume.
 *
 * @param cfsBiomassMerch    Merchantable biomass volume
 * @param cfsBiomassNonMerch Non-Merchantable biomass volume
 * @param cfsBiomassSapling  Sapling biomass volume
 * @param propStemwood       Proportion of biomass that is stemwood
 * @param propBark           Proportion of biomass that is bark
 * @param propBranches       Proportion of biomass that is branches
 * @param propFoliage        Proportion of biomass that is foliage
 * @param bioStemwood        Biomass volume of stemwood
 * @param bioBark            Biomass volume of bark
 * @param bioBranches        Biomass volume of branches
 * @param bioFoliage         Biomass volume of foliage
 * @param bioDead            Biomass volume of dead material
 */
public record CfsBiomassVolumeDetails(
		Double cfsBiomassMerch, Double cfsBiomassNonMerch, Double cfsBiomassSapling, Double propStemwood,
		Double propBark, Double propBranches, Double propFoliage, Double bioStemwood, Double bioBark,
		Double bioBranches, Double bioFoliage, Double bioDead
) {
	public static final CfsBiomassVolumeDetails EMPTY = new CfsBiomassVolumeDetails(
			-9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0, -9.0
	);
	public static final CfsBiomassVolumeDetails ZERO_VOLUME = new CfsBiomassVolumeDetails(
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
	);
}
