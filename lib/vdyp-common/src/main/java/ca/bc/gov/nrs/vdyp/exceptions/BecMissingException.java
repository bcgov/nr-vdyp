package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

/**
 * BEC has not been set.
 *
 * Equivalent to IPASS= -12 for VRI
 */
public class BecMissingException extends StandProcessingException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String TEMPLATE_MISSING = "Required BEC was missing";

	BecMissingException(Throwable cause) {
		super(TEMPLATE_MISSING, cause);
	}

	BecMissingException() {
		super(TEMPLATE_MISSING);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-12);
		return Optional.empty();
	}

}
