package ca.bc.gov.nrs.vdyp.forward;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.Pass;
import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.application.VdypApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypProcessingApplication;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;

public class VdypForwardApplication extends VdypProcessingApplication {

	private static final Logger logger = LoggerFactory.getLogger(VdypForwardApplication.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran VDYP Forward would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran VDYP Forward would exit with.

	public static final String DEFAULT_VDYP_CONTROL_FILE_NAME = "vdyp.ctr";

	public static final Set<Pass> DEFAULT_PASS_SET = Collections
			.unmodifiableSet(EnumSet.of(Pass.PASS_1, Pass.PASS_2, Pass.PASS_3, Pass.PASS_4, Pass.PASS_5));
	private static Set<Pass> vdypPassSet = EnumSet.copyOf(DEFAULT_PASS_SET);

	public static void main(final String... args) {
		main(Optional.empty(), Optional.empty(), args);
	}

	/**
	 * @param inputDir  The directory to use as the current directory for input
	 * @param outputDir The directory to use as the current directory for output
	 * @param args      The command line arguments
	 */
	public static void main(final Optional<Path> inputDir, final Optional<Path> outputDir, final String... args) {

		try (var app = new VdypForwardApplication()) {
			app.doMain(inputDir, outputDir, args);
		} catch (VdypApplicationInitializationException e) {
			logger.error("Error during initialization", e);
			System.exit(CONFIG_LOAD_ERROR);
		} catch (VdypApplicationProcessingException e) {
			logger.error("Error during processing", e);
			System.exit(PROCESSING_ERROR);
		}
	}

	/**
	 * @param inputDir  The directory to use as the current directory for input
	 * @param outputDir The directory to use as the current directory for output
	 * @param args      The command line arguments
	 * @throws VdypApplicationInitializationException
	 * @throws VdypApplicationProcessingException
	 */
	public void doMain(final Optional<Path> inputDir, final Optional<Path> outputDir, final String... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {

		logVersionInformation();

		List<String> controlFileNames = null;

		try {
			controlFileNames = VdypApplication.getControlMapFileNames(
					args, DEFAULT_VDYP_CONTROL_FILE_NAME, VdypApplicationIdentifier.VDYP_FORWARD, System.out, System.in
			);
		} catch (Exception ex) {
			throw new VdypApplicationInitializationException(ex);
		}

		var inputFileResolver = inputDir.map(FileSystemFileResolver::new).orElseGet(FileSystemFileResolver::new);
		var outputFileResolver = outputDir.map(FileSystemFileResolver::new).orElseGet(FileSystemFileResolver::new);

		try {
			ForwardProcessor processor = new ForwardProcessor();

			processor.run(inputFileResolver, outputFileResolver, controlFileNames, vdypPassSet);

		} catch (Exception ex) {
			throw new VdypApplicationProcessingException(ex);
		}
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

	@Override
	public String getDefaultControlFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Processor getProcessor() {
		// TODO Auto-generated method stub
		return null;
	}
}
