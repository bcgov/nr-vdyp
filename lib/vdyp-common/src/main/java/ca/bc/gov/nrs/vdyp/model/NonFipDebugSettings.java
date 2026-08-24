package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

public interface NonFipDebugSettings extends DebugSettings {

	/**
	 * Get the maximum breast height age if there is a limit.
	 */
	Optional<Float> getMaxBreastHeightAge();

	/**
	 * Factor to expand DQ to recover TPH
	 */
	Optional<Float> getExpandDiameterForTPHRecovery();

	/**
	 * Are BA and TPH errors in Mode 1 fatal
	 */
	boolean getMode1ErrorsFatal();
}
