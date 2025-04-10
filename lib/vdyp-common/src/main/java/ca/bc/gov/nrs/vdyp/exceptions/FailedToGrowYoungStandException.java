package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

/**
 * While pre-processing a YOUNG stand, failed to grow it to the required size
 *
 * Equivalent to IPASS= -14 for VRI
 */
public class FailedToGrowYoungStandException extends StandProcessingException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String TEMPLATE = "Failed to grow YOUNG stand to required size";

	public FailedToGrowYoungStandException(Throwable cause) {
		super(TEMPLATE, cause);
	}

	public FailedToGrowYoungStandException() {
		super(TEMPLATE);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-14);
		return Optional.empty();
	}

}
