package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

public interface NonFipDebugSettings extends DebugSettings {

	/**
	 * Get the maximum breast height age if there is a limit.
	 */
	Optional<Float> getMaxBreastHeightAge();

}
