package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariables;

public class StartResolvedControlMapImpl extends ResolvedControlMapImpl {

	// TODO alter the start applications to use resolved control map instead of direct map access.
	// Need to implement appropriate subclasses of RCM, DebugSettings, and ControlVariables

	public StartResolvedControlMapImpl(Map<String, Object> controlMap) {
		super(controlMap);
	}

	@Override
	public DebugSettings getDebugSettings() {
		return get(ControlKey.DEBUG_SWITCHES, DebugSettings.class);
	}

	@Override
	public ControlVariables getControlVariables() {
		return get(ControlKey.VTROL, ControlVariables.class);
	}

}
