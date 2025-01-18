package ca.bc.gov.nrs.vdyp.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;

public abstract class VdypProcessingApplication<P extends Processor> extends VdypApplication {

	@SuppressWarnings("java:S106")
	protected static void initLogging(Class<?> klazz) {
		try {
			LogManager.getLogManager()
					.readConfiguration(klazz.getClassLoader().getResourceAsStream("logging.properties"));
		} catch (SecurityException | IOException e) {
			System.err.println("Unable to configure logging system");
		}
	}

	static final Logger logger = LoggerFactory.getLogger(VdypProcessingApplication.class);

	public abstract String getDefaultControlFileName();

	protected abstract P getProcessor();

	public static final int CONFIG_LOAD_ERROR_EXIT = 1;
	public static final int PROCESSING_ERROR_EXIT = 2;
	public static final int NO_ERROR_EXIT = 0;

	protected Set<Pass> getAllPasses() {
		return EnumSet.allOf(Pass.class);
	}

	protected VdypProcessingApplication() {
		super();
	}

	@SuppressWarnings("java:S106") // Using System.out for direct, console based user interaction.
	public int run(final String... args) {
		return run(System.out, System.in, args);
	}

	public int run(final PrintStream os, final InputStream is, final String... args) {
		logVersionInformation();

		final List<String> controlFileNames;

		try {
			if (args.length == 0) {
				controlFileNames = getControlFileNamesFromUser(os, is);
			} else {
				controlFileNames = Arrays.asList(args);
			}
		} catch (Exception ex) {
			logger.error("Error during initialization", ex);
			return CONFIG_LOAD_ERROR_EXIT;
		}

		try {
			Processor processor = getProcessor();

			processor.run(new FileSystemFileResolver(), new FileSystemFileResolver(), controlFileNames, getAllPasses());

		} catch (Exception ex) {
			logger.error("Error during processing", ex);
			return PROCESSING_ERROR_EXIT;
		}

		return NO_ERROR_EXIT;

	}

	public List<String> getControlFileNamesFromUser(final PrintStream os, final InputStream is) throws IOException {
		final String defaultFilename = getDefaultControlFileName();
		List<String> controlFileNames;
		os.print(
				MessageFormat
						.format("Enter name of control file (or RETURN for {0}) or *name for both): ", defaultFilename)
		);

		controlFileNames = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String userResponse = br.readLine();
			if (userResponse.length() == 0) {
				controlFileNames.add(defaultFilename);
			} else {
				if (userResponse.startsWith("*")) {
					controlFileNames.add(defaultFilename);
					userResponse = userResponse.substring(1);
				}
				controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
			}

		}
		return controlFileNames;
	}

	private void logVersionInformation() {
		logger.info("{} {}", RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("{} Ver:{} {}", RESOURCE_BINARY_NAME, RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("VDYP7 Support Ver: {}", AVERSION);
	}

}