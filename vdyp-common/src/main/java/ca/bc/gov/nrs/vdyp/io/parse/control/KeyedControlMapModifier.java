package ca.bc.gov.nrs.vdyp.io.parse.control;

import ca.bc.gov.nrs.vdyp.common.ControlKey;

public interface KeyedControlMapModifier extends ControlMapModifier {

	/**
	 * The key for this resource's entry in the control map
	 *
	 * @return
	 */
	default String getControlKeyName() {
		return getControlKey().name();
	}

	/**
	 * The key for this resource's entry in the control map
	 *
	 * @return
	 */
	ControlKey getControlKey();

}