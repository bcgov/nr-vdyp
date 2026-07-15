package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;

public abstract class VdypProcessingApplication extends VdypApplication<ProcessingDebugSettings> {

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

	protected abstract Processor getProcessor();

	public static final int CONFIG_LOAD_ERROR_EXIT = 1;
	public static final int PROCESSING_ERROR_EXIT = 2;
	public static final int NO_ERROR_EXIT = 0;

	protected Set<Pass> getAllPasses() {
		return EnumSet.allOf(Pass.class);
	}

	public static final Set<Pass> DEFAULT_PASS_SET = Collections.unmodifiableSet(
			EnumSet.of(
					Pass.INITIALIZE, //
					Pass.OPEN_FILES, //
					Pass.PROCESS_STANDS, //
					Pass.MULTIPLE_STANDS, //
					Pass.CLOSE_FILES //
			)
	);

	protected Set<Pass> getDefaultPasses() {
		return DEFAULT_PASS_SET;
	}

	protected VdypProcessingApplication() {
		super();
	}

	@Override
	protected void process() throws ProcessingException {
		Processor processor = getProcessor();

		processor.process(
				this.getDefaultPasses(), (ProcessingResolvedControlMap) this.resolvedControlMap,
				Optional.of(new FileSystemFileResolver()), p -> true
		);
	}

	@Override
	protected ProcessingControlParser getControlFileParser() {
		return new ProcessingControlParser();
	}

	@Override
	protected ResolvedControlMap resolveControlMap(Map<String, Object> rawControlMap) {
		return new ProcessingResolvedControlMapImpl(rawControlMap);
	}

	@Override
	public void close() {
		// nothing to do
	}
}