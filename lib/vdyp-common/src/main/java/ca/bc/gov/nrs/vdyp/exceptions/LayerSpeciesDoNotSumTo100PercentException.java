package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * A layer has species with percentages that do not sum to 100%
 *
 * Equivalent to for VRI, equivalent to IPASS=-8, for FIP equivalent to IPASS=-8 for PRIMARY layer, or -9 for VETERAN
 * layer
 */
public class LayerSpeciesDoNotSumTo100PercentException extends LayerValidationException {

	private static final long serialVersionUID = -6922206245038811407L;

	static final String TEMPLATE = "Species in layer do not summ to 100%";

	LayerSpeciesDoNotSumTo100PercentException(LayerType layer, Throwable cause) {
		super(layer, MessageFormat.format(TEMPLATE, layer), cause);
	}

	public LayerSpeciesDoNotSumTo100PercentException(LayerType layer) {
		super(layer, MessageFormat.format(TEMPLATE, layer));
	}

	public LayerSpeciesDoNotSumTo100PercentException(
			RuntimeStandProcessingException cause
	) {
		super(cause, LayerSpeciesDoNotSumTo100PercentException.class);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START:
			switch (getLayer()) {
			case PRIMARY:
				return Optional.of(-8);
			case VETERAN:
				return Optional.of(-9);
			default:
				return Optional.empty();

			}
		case VRI_START:
			return Optional.of(-8);
		default:
			return Optional.empty();

		}
	}

}
