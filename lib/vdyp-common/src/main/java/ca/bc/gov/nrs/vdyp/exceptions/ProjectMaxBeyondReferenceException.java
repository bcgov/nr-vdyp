package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

public class ProjectMaxBeyondReferenceException extends StandProcessingException {
	private static final long serialVersionUID = -4051659652585072913L;

	public ProjectMaxBeyondReferenceException() {
		super();
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case VDYP_FORWARD:
			return Optional.of(100);
		default:
			return Optional.empty();
		}
	}
}
