package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

/**
 * Equivalent to IPASS=-2 for FIPStart, possibly also -2 for VRIStart
 */
public class IncorrectLayerCodesException extends StandProcessingException {
	private static final long serialVersionUID = -8897339909560075564L;
	static final String TEMPLATE = "Invalid layer code {}";

	public IncorrectLayerCodesException(String code) {
		super(MessageFormat.format(TEMPLATE, code));
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.FIP_START)
			return Optional.of(-2);
		return Optional.empty();
	}

}
