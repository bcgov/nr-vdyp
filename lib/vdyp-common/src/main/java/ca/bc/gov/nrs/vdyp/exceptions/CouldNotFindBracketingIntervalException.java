package ca.bc.gov.nrs.vdyp.exceptions;

import org.apache.commons.math3.exception.NoBracketingException;

/**
 * Exception when findInterval can't find an interval
 */
public class CouldNotFindBracketingIntervalException extends NoBracketingException {

	private static final long serialVersionUID = -6704673821120552923L;

	final boolean exitEarly;
	final int iteration;

	/**
	 *
	 * @param lo        Lower end of the interval.
	 * @param hi        Higher end of the interval.
	 * @param fLo       Value at lower end of the interval.
	 * @param fHi       Value at higher end of the interval.
	 * @param iteration index of the iteration when this occurred, starting from 0.
	 * @param exitEarly did the execution exit early due to the test provided by the caller.
	 */
	public CouldNotFindBracketingIntervalException(
			double lo, double hi, double fLo, double fHi, int iteration, boolean exitEarly
	) {
		super(lo, hi, fLo, fHi);
		this.iteration = iteration;
		this.exitEarly = exitEarly;
	}

	/**
	 * Did the execution exit early due to the test provided by the caller.
	 */
	public boolean isExitEarly() {
		return exitEarly;
	}

	/**
	 * Index of the iteration when this occurred, starting from 0.
	 */
	public int getIteration() {
		return iteration;
	}
}
