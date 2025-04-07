package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;

/**
 * Equivalent to IPASS -2 in VRIStart
 */
public class UnsupportedSpeciesException extends StandProcessingException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String TEMPLATE = "Unexpected species {0}";

	private final String species;

	public UnsupportedSpeciesException(String species, Throwable cause) {
		super(MessageFormat.format(TEMPLATE, species), cause);
		this.species = species;
	}

	public UnsupportedSpeciesException(RuntimeStandProcessingException cause) {
		super(cause);
		this.species = unwrap(cause, UnsupportedSpeciesException.class).species;
	}

	public UnsupportedSpeciesException(String species) {
		super(MessageFormat.format(TEMPLATE, species));
		this.species = species;
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-2);
		return Optional.empty();
	}

}
