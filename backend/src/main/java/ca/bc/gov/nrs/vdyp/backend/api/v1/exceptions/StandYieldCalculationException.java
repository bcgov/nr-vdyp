package ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.backend.model.v1.StandYieldMessageKind;

/**
 * Exceptions thrown during the calculation of stand yields during the creation of yield tables. These exceptions are
 * meant to indicate that while the yield table information for a given polygon may be incomplete, generation should
 * continue.
 */
public class StandYieldCalculationException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = -4342933248690426666L;

	public StandYieldCalculationException(Exception cause) {
		super(
				cause != null ? cause.getClass().getSimpleName()
						+ (cause.getMessage() != null ? ": " + cause.getMessage() : "") : "null",
				cause
		);
	}

	public StandYieldCalculationException(StandYieldMessageKind template, Object... args) {
		super(MessageFormat.format(template.template, args).toString());
	}
}