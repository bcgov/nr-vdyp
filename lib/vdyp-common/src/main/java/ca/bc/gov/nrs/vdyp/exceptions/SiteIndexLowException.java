package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Site index is low or has not been set.
 * 
 * If layer is PRIMARY it's equivalent to IPASS=-11 for FIP. It's equivalent to IPASS=-4 for VRI
 */
public class SiteIndexLowException extends LayerValueLowException {

	private static final long serialVersionUID = 5267990153323800885L;

	static final String DEFAULT_NAME = "Site index";

	public SiteIndexLowException(RuntimeStandProcessingException cause) {
		super(cause, SiteIndexLowException.class);
	}

	public SiteIndexLowException(LayerType layer, Optional<Float> value) {
		this(layer, DEFAULT_NAME, value);
	}

	public SiteIndexLowException(LayerType layer, String name, Optional<Float> value) {
		super(layer, name, value);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		switch (app) {
		case FIP_START:
			if (getLayer() == LayerType.PRIMARY) {
				return Optional.of(-11);
			}
			return Optional.empty();

		case VRI_START:
			return Optional.of(-4);
		default:
			return Optional.empty();
		}
	}

}
