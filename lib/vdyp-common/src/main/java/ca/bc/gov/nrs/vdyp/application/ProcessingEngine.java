package ca.bc.gov.nrs.vdyp.application;

import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public abstract class ProcessingEngine {

	/**
	 * Run all steps of the engine on the given polygon up to and including the given <code>lastStep</code>.
	 *
	 * @param polygon           the polygon on which to operate
	 * @param lastStepInclusive execute up to and including this step
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public abstract void processPolygon(VdypPolygon polygon, ExecutionStep lastStepInclusive)
			throws ProcessingException;

	public enum ExecutionStep {
		// Must be first
		NONE, //

		CHECK_FOR_WORK, //
		CALCULATE_MISSING_SITE_CURVES, //
		CALCULATE_COVERAGES, //
		DETERMINE_POLYGON_RANKINGS, //
		ESTIMATE_MISSING_SITE_INDICES, //
		ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES, //
		CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX, //
		SET_COMPATIBILITY_VARIABLES, //
		GROW_1_LAYER_DHDELTA, //
		GROW_2_LAYER_BADELTA, //
		GROW_3_LAYER_DQDELTA, //
		GROW_4_LAYER_BA_AND_DQTPH_EST, //
		GROW_5A_LH_EST, //
		GROW_5_SPECIES_BADQTPH, //
		GROW_6_LAYER_TPH2, //
		GROW_7_LAYER_DQ2, //
		GROW_8_SPECIES_LH, //
		GROW_9_SPECIES_PCT, //
		GROW_10_COMPATIBILITY_VARS, //
		GROW_11_SPECIES_UC, //
		GROW_12_SPECIES_UC_SMALL, //
		GROW_13_STORE_SPECIES_DETAILS, //
		GROW, //

		// Must be last
		ALL; //

		public ExecutionStep predecessor() {
			if (this == NONE) {
				throw new IllegalStateException("ExecutionStep.None has no predecessor");
			}

			return ExecutionStep.values()[ordinal() - 1];
		}

		public ExecutionStep successor() {
			if (this == ALL) {
				throw new IllegalStateException("ExecutionStep.All has no successor");
			}

			return ExecutionStep.values()[ordinal() + 1];
		}

		public boolean lt(ExecutionStep that) {
			return this.ordinal() < that.ordinal();
		}

		public boolean le(ExecutionStep that) {
			return this.ordinal() <= that.ordinal();
		}

		public boolean eq(ExecutionStep that) {
			return this.ordinal() == that.ordinal();
		}

		public boolean ge(ExecutionStep that) {
			return this.ordinal() >= that.ordinal();
		}

		public boolean gt(ExecutionStep that) {
			return this.ordinal() > that.ordinal();
		}
	}

	/**
	 * Run all steps of the engine on the given polygon.
	 *
	 * @param polygon the polygon on which to operate
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public void processPolygon(VdypPolygon polygon) throws ProcessingException {

		processPolygon(polygon, ExecutionStep.ALL);
	}

}
