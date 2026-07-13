package ca.bc.gov.nrs.vdyp.back;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypProcessingApplication;

public class VdypBackApplication extends VdypProcessingApplication {

	static {
		initLogging(VdypBackApplication.class);
	}

	static final Logger logger = LoggerFactory.getLogger(VdypBackApplication.class);

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdyp.ctr";

	public static void main(final String... args) {

		VdypApplication.doRunApp(VdypBackApplication::new, args);
	}

	@Override
	protected BackProcessor getProcessor() {
		return null; // TODO
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
