package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.Optional;

import javax.annotation.Nullable;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;

/**
 * The polygon is using a mode that is not supported
 *
 * Equivalent to IPASS= -4 for FIP
 */
public class UnsupportedModeException extends StandProcessingException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String TEMPLATE = "Mode {0} is not supported.";

	@Nullable
	private final PolygonMode mode;

	public UnsupportedModeException(Optional<PolygonMode> mode) {
		super(MessageFormat.format(TEMPLATE, Utils.optNa(mode)));
		this.mode = mode.orElse(null);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		// odd that this is the same code as HeightLowException but that's what VDYP7 used
		if (app == VdypApplicationIdentifier.FIP_START)
			return Optional.of(-4);
		return Optional.empty();
	}

	public UnsupportedModeException(RuntimeStandProcessingException cause) {
		this(cause, unwrap(cause, UnsupportedModeException.class));
	}

	private UnsupportedModeException(RuntimeStandProcessingException cause, UnsupportedModeException unwrapped) {
		super(unwrapped.getMessage(), cause);
		this.mode = unwrapped.getMode().orElse(null);
	}

	public Optional<PolygonMode> getMode() {
		return Optional.ofNullable(mode);
	}

}
