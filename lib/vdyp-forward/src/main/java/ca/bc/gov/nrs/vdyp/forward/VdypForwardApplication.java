package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;

public class VdypForwardApplication extends VdypApplication {

	static {
		try {
			LogManager.getLogManager().readConfiguration(
					ForwardProcessor.class.getClassLoader().getResourceAsStream("logging.properties")
			);
		} catch (SecurityException | IOException e) {
			System.err.println("Unable to configure logging system");
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(VdypForwardApplication.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran VDYP Forward would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran VDYP Forward would exit with.

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdyp.ctr";

	public static final Set<ForwardPass> DEFAULT_PASS_SET = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5))
	);
	private static Set<ForwardPass> vdypPassSet = new HashSet<>(DEFAULT_PASS_SET);

	public static void main(final String... args) {
		main(Optional.empty(), Optional.empty(), args);
	}

	/**
	 * @param inputDir  The directory to use as the current directory for input
	 * @param outputDir The directory to use as the current directory for output
	 * @param args      The command line arguments
	 */
	public static void main(final Optional<Path> inputDir, final Optional<Path> outputDir, final String... args) {

		var app = new VdypForwardApplication();

		app.logVersionInformation();

		List<String> controlFileNames = null;

		try {
			controlFileNames = VdypApplication.getControlMapFileNames(
					args, DEFAULT_VDYP_CONTROL_FILE_NAME, VdypApplicationIdentifier.VDYP_FORWARD, System.out, System.in
			);
		} catch (Exception ex) {
			logger.error("Error during initialization", ex);
			System.exit(CONFIG_LOAD_ERROR);
		}

		var inputFileResolver = inputDir.map(FileSystemFileResolver::new).orElseGet(
				FileSystemFileResolver::new
		);
		var outputFileResolver = outputDir.map(FileSystemFileResolver::new).orElseGet(
				FileSystemFileResolver::new
		);

		try {
			ForwardProcessor processor = new ForwardProcessor();

			processor.run(inputFileResolver, outputFileResolver, controlFileNames, vdypPassSet);

		} catch (Exception ex) {
			logger.error("Error during processing", ex);
			System.exit(PROCESSING_ERROR);
		}
	}

	private void logVersionInformation() {
		logger.info("{} {}", RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("{} Ver:{} {}", RESOURCE_BINARY_NAME, RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("VDYP7 Support Ver: {}", AVERSION);
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}
}
