package ca.bc.gov.nrs.vdyp.forward;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;

public class ForwardLayerProcessingState extends LayerProcessingState<ForwardLayerProcessingState> {

	private static final String COMPATIBILITY_VARIABLES_SET_CAN_BE_SET_ONCE_ONLY = "CompatibilityVariablesSet can be set once only";
	private static final String UNSET_CV_VOLUMES = "unset cvVolumes";
	private static final String UNSET_CV_BASAL_AREAS = "unset cvBasalAreas";
	private static final Logger logger = LoggerFactory.getLogger(ForwardLayerProcessingState.class);

	// L1COM2 - equation groups. From the configuration, narrowed to the
	// polygon's BEC zone.

	// L1COM3 - just shadows of fields of L1COM5
	// AGETOTL1 = wallet.ageTotals[primarySpeciesIndex]
	// AGEBHL1 = wallet.yearsAtBreastHeight[primarySpeciesIndex]
	// YTBHL1 = wallet.yearsToBreastHeight[primarySpeciesIndex]
	// HDL1 = wallet.dominantHeights[primarySpeciesIndex]

	// Calculated data - this data is calculated after construction during processing.

	// FRBASP0 - FR
	// TODO

	// MNSP - MSPL1, MSPLV
	// TODO

	public ForwardLayerProcessingState(ForwardProcessingState fps, VdypLayer layer) throws ProcessingException {

		super(fps, fps.getCurrentPolygon(), layer.getLayerType());

	}

	public static Logger getLogger() {
		return logger;
	}

	/**
	 * Update the cached primary species details after growth. These changes are made to the cached values only at this
	 * time. Later, if Control Variable 6 is > 0, <code>setPrimarySpeciesDetails</code> will be called before the next
	 * growth period is run and the bank values will be updated, too.
	 *
	 * @param newPrimarySpeciesDominantHeight
	 */
	public void updatePrimarySpeciesDetailsAfterGrowth(float newPrimarySpeciesDominantHeight) {

		this.primarySpeciesDominantHeight = newPrimarySpeciesDominantHeight;
		this.primarySpeciesTotalAge += 1;
		this.primarySpeciesAgeAtBreastHeight += 1;

		// primarySpeciesSiteIndex - does this change?
		// primarySpeciesAgeToBreastHeight of course doesn't change.
	}

	/**
	 * CVADJ1 - adjust the values of the compatibility variables after one year of growth.
	 */
	public void updateCompatibilityVariablesAfterGrowth() {

		var compVarAdjustments = ps.controlMap.getCompVarAdjustments();

		updateCompatibilityVariables(
				(val, sucv, i) -> val * compVarAdjustments.getValue(UtilizationClass.SMALL, sucv),
				(val, uc, i) -> val * compVarAdjustments.getValue(uc, UtilizationClassVariable.BASAL_AREA),
				(val, uc, i) -> val * compVarAdjustments.getValue(uc, UtilizationClassVariable.QUAD_MEAN_DIAMETER),
				(val, uc, vv, i) -> val * compVarAdjustments.getVolumeValue(uc, vv)
		);

	}

	@Override
	protected VdypLayer updateLayerFromBank() {

		VdypLayer updatedLayer = getBank().buildLayerFromBank();

		if (getLayerType().equals(LayerType.PRIMARY)) {
			// Inject the compatibility variable values.
			for (int i = 1; i < getNSpecies() + 1; i++) {
				VdypSpecies species = updatedLayer.getSpeciesBySp0(getBank().speciesNames[i]);

				applyCompatibilityVariablesToSpecies(i, species);
			}
		}

		return updatedLayer;
	}

	@Override
	protected Predicate<VdypSpecies> getBankFilter() {
		return s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= ForwardProcessingEngine.MIN_BASAL_AREA;
	}

	@Override
	protected void applyCompatibilityVariables(VdypSpecies species, int i) {
		// TODO Auto-generated method stub
		// Added this to the parent during the initial 443 development but can't remember why. It will probably be
		// important later. Making a note to remember to check at the end and delete it if it's not needed after all.
	}
}
