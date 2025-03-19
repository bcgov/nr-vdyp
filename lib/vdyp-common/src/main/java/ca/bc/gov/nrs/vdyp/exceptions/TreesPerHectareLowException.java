package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Trees per hectare is low or has not been set.
 * 
 * Equivalent to IPASS= -7 for VRI
 */
public class TreesPerHectareLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Trees per hectare";

	public TreesPerHectareLowException(RuntimeStandProcessingException cause) {
		super(cause, TreesPerHectareLowException.class);
	}

	public TreesPerHectareLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public TreesPerHectareLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VDYP_BACK)
			return Optional.of(-7);
		return Optional.empty();
	}

}
