package ca.bc.gov.nrs.vdyp.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

/**
 * Base class of all VDYP applications.
 *
 * <p>
 * Expects <tt>application.properties</tt> to be on the class path.
 *
 * @author Michael Junkin, Vivid Solutions
 * @author Kevin Smith, Vivid Solutions
 */
public abstract class VdypApplication<D extends DebugSettings> extends VdypComponent implements AutoCloseable {

	public static final int SUCCESS = 0;
	public static final int CONFIG_LOAD_ERROR = 1;
	public static final int PROCESSING_ERROR = 2;
	public static final int OTHER_ERROR = -1;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VdypApplication.class);

	public abstract VdypApplicationIdentifier getId();

	/** The computation instance used by this engine */
	protected ComputationMethods computers;

	protected Map<String, Object> controlMap = new HashMap<>();

	public EstimationMethods estimationMethods;

	private Optional<D> debugModes = Optional.empty();

	protected ResolvedControlMap resolvedControlMap;

	public D getDebugModes() {
		return debugModes.orElseThrow(() -> new IllegalStateException("Can not get debug modes before initialization"));
	}

	public void setDebugModes(D newDebugModes) {
		debugModes = Optional.of(newDebugModes);
	}

	/**
	 * @returns the ordinal of the application's identifier. It will agree with the JPROGRAM values from the FORTRAN
	 *          implementation.
	 */
	public int getJProgramNumber() {
		return getId().getJProgramNumber();
	}

	public static List<String> getControlMapFileNames(
			final String[] args, final String defaultName, final VdypApplicationIdentifier appId,
			PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs
	) throws IOException {
		List<String> controlFileNames;
		if (args.length == 0) {
			writeToIfNoArgs.printf(
					"Enter name of %s control file (or RETURN for %s) or *name for both): ", appId.toString(),
					defaultName
			);

			controlFileNames = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(readFromIfNoArgs))) {
				String userResponse = br.readLine().strip();
				if (userResponse.length() == 0 || userResponse.equals("*")) {
					controlFileNames.add(defaultName);
				} else if (userResponse.startsWith("*")) {
					controlFileNames.add(defaultName);

					userResponse = userResponse.substring(1);
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				} else {
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				}
			}
		} else {
			controlFileNames = Arrays.stream(args)
					.flatMap(arg -> arg.startsWith("*") ? Stream.of(defaultName, arg.substring(1)) : Stream.of(arg))
					.filter(s -> !s.isEmpty()).toList();
		}
		return controlFileNames;
	}

	protected abstract String getDefaultControlFileName();

	/**
	 * Initialize application
	 *
	 * @param resolver
	 * @param controlFilePath
	 * @throws IOException
	 * @throws ResourceParseException
	 */
	protected void init(
			FileSystemFileResolver resolver, PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs,
			String... controlFilePaths
	) throws IOException, ResourceParseException {

		var controlFileNames = VdypApplication.getControlMapFileNames(
				controlFilePaths, getDefaultControlFileName(), getId(), writeToIfNoArgs, readFromIfNoArgs
		);

		Map<String, Object> controlMap = new HashMap<>();

		init(resolver, getControlFileParser().parseByName(controlFileNames, resolver, controlMap));
	}

	protected abstract BaseControlParser<D> getControlFileParser();

	/**
	 * Initialize application
	 *
	 * @param controlMap
	 * @throws IOException
	 */
	void init(FileSystemFileResolver resolver, Map<String, Object> controlMap) throws IOException {
		setControlMap(controlMap);
	}

	public void doMain(final Path... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {
		doMain(Arrays.stream(args).map(Path::toAbsolutePath).map(Object::toString).toArray(String[]::new));
	}

	public void doMain(final String... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {
		var resolver = new FileSystemFileResolver();

		doMain(resolver, args);
	}

	public void doMain(FileSystemFileResolver resolver, final String... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {

		try {
			init(resolver, System.out, System.in, args);
		} catch (Exception ex) {
			logger.error("Error during initialization", ex);
			throw new VdypApplicationInitializationException(ex);
		}

		try {
			process();
		} catch (Exception ex) {
			logger.error("Error during processing", ex);
			throw new VdypApplicationProcessingException(ex);
		}
	}

	protected abstract void process() throws ProcessingException;

	protected abstract ResolvedControlMap resolveControlMap(Map<String, Object> rawControlMap);

	@SuppressWarnings("unchecked")
	protected void setControlMap(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
		this.resolvedControlMap = resolveControlMap(controlMap);
		this.estimationMethods = new EstimationMethods(this.resolvedControlMap);
		this.debugModes = Optional.of((D) this.resolvedControlMap.getDebugSettings());
		this.computers = new ComputationMethods(estimationMethods, getId());
	}

	protected void logVersionInformation() {
		logger.info("{} {}", RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("{} Ver:{} {}", RESOURCE_BINARY_NAME, RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("VDYP7 Support Ver: {}", AVERSION);
	}

	static protected void runApp(Supplier<? extends VdypApplication<?>> getApp, String... args) {
		System.exit(doRunApp(getApp, args));
	}

	static protected int doRunApp(Supplier<? extends VdypApplication<?>> getApp, String... args) {
		try (var app = getApp.get();) {
			app.doMain(args);
		} catch (VdypApplicationInitializationException e) {
			return CONFIG_LOAD_ERROR;
		} catch (VdypApplicationProcessingException e) {
			return PROCESSING_ERROR;
		} catch (Throwable e) {
			// Error or a RuntimeException, or an exception while closing the app
			logger.atError().setCause(e).setMessage("Error");
			return OTHER_ERROR;
		}
		return SUCCESS;
	}
}
