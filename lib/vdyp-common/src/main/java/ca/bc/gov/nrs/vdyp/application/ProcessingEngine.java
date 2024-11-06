package ca.bc.gov.nrs.vdyp.application;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public abstract class ProcessingEngine<E extends ProcessingEngine.ExecutionStep<E>> {

	/**
	 * Run all steps of the engine on the given polygon up to and including the given <code>lastStep</code>.
	 *
	 * @param polygon           the polygon on which to operate
	 * @param lastStepInclusive execute up to and including this step
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public abstract void processPolygon(VdypPolygon polygon, E lastStepInclusive) throws ProcessingException;

	public static interface ExecutionStep<E extends ExecutionStep<E>> extends Comparable<E> {

		/**
		 * @return The previous execution step
		 * @throws IllegalStateException if this is the first step
		 */
		public E predecessor() throws IllegalStateException;

		/**
		 * @return The next execution step
		 * @throws IllegalStateException if this is the last step
		 */
		public E successor() throws IllegalStateException;

		public default boolean lt(E that) {
			return this.compareTo(that) < 0;
		}

		public default boolean le(E that) {
			return this.compareTo(that) <= 0;
		}

		public default boolean eq(E that) {
			return this.compareTo(that) == 0;
		}

		public default boolean ge(E that) {
			return this.compareTo(that) >= 0;
		}

		public default boolean gt(E that) {
			return this.compareTo(that) > 0;

		}
	}

	protected abstract E getFirstStep();

	protected abstract E getLastStep();

	/**
	 * Run all steps of the engine on the given polygon.
	 *
	 * @param polygon the polygon on which to operate
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public void processPolygon(VdypPolygon polygon) throws ProcessingException {

		processPolygon(polygon, getLastStep());
	}

}
