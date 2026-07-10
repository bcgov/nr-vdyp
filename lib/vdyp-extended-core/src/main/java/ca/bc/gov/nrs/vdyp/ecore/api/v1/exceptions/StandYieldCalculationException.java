package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import java.text.MessageFormat;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.StandYieldMessageKind;

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

	public StandYieldCalculationException(long featureId, Exception cause) {
		super(withContext(buildContextPrefix(featureId), buildCauseMessage(cause)), cause);
	}

	public StandYieldCalculationException(long featureId, StandYieldMessageKind template, Object... args) {
		super(withContext(buildContextPrefix(featureId), MessageFormat.format(template.template, args)));
	}

	public StandYieldCalculationException(long featureId, String layerId, Exception cause) {
		super(withContext(buildContextPrefix(featureId, layerId), buildCauseMessage(cause)), cause);
	}

	public StandYieldCalculationException(
			long featureId, String layerId, String speciesCode, StandYieldMessageKind template, Object... args
	) {
		super(
				withContext(
						buildContextPrefix(featureId, layerId, speciesCode),
						MessageFormat.format(template.template, args)
				)
		);
	}

	public StandYieldCalculationException(
			long featureId, String layerId, StandYieldMessageKind template, Object... args
	) {
		super(withContext(buildContextPrefix(featureId, layerId), MessageFormat.format(template.template, args)));
	}

	private static String buildCauseMessage(Exception cause) {
		if (cause == null) {
			return "null";
		}
		String causeDetail = cause.getMessage() != null ? ": " + cause.getMessage() : "";
		return cause.getClass().getSimpleName() + causeDetail;
	}
}
