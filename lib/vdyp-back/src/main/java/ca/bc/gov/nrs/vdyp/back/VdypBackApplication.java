package ca.bc.gov.nrs.vdyp.back;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypProcessingApplication;
import ca.bc.gov.nrs.vdyp.back.io.parse.BackControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;

public class VdypBackApplication extends VdypProcessingApplication {

	static {
		initLogging(VdypBackApplication.class);
	}

	static final Logger logger = LoggerFactory.getLogger(VdypBackApplication.class);

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdypback.ctr";

	public static void main(final String... args) {

		VdypApplication.doRunApp(VdypBackApplication::new, args);
	}

	@Override
	protected ProcessingControlParser getControlFileParser() {
		// Back flips the sign of relative target years
		// VDYP7 did so at the point where the offset was applied
		// VDYP8 does it in the control file parser to ensure consistency
		return new BackControlParser();
	}

	@Override
	protected BackProcessor getProcessor() {
		return new BackProcessor();
	}

	@Override
	public String getDefaultControlFileName() {
		return DEFAULT_VDYP_CONTROL_FILE_NAME;
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VDYP_BACK;
	}

}
