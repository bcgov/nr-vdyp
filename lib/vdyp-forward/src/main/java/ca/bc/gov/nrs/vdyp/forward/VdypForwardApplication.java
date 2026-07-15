package ca.bc.gov.nrs.vdyp.forward;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypProcessingApplication;

public class VdypForwardApplication extends VdypProcessingApplication {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VdypForwardApplication.class);

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdyp.ctr";

	public static void main(final String... args) {

		VdypApplication.runApp(VdypForwardApplication::new, args);

	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

	@Override
	public String getDefaultControlFileName() {
		return DEFAULT_VDYP_CONTROL_FILE_NAME;
	}

	@Override
	protected Processor getProcessor() {
		return new ForwardProcessor();
	}

}
