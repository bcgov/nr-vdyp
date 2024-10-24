package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

/**
 *
 * The common code for the algorithmic part of a forward or backward growth predictor
 *
 * VDYPPASS IN/OUT I*4(10) Major Control Functions
 * <ol>
 * <li>IN Perform Initiation activities? (0=No, 1=Yes)
 * <li>IN Open the stand data files (0=No, 1=Yes)
 * <li>IN Process stands (0=No, 1=Yes)
 * <li>IN Allow multiple polygons (0=No, 1=Yes) (Subset of stand processing. May limit to 1 stand)
 * </ol>
 *
 * @author Michael Junkin, Vivid Solutions
 * @author Kevin Smith, Vivid Solutions
 */

public abstract class Processor {

	private static final Logger logger = LoggerFactory.getLogger(Processor.class);

	/**
	 * Initialize Processor
	 *
	 * @param inputFileResolver
	 * @param outputFileResolver
	 * @param controlFileNames
	 *
	 * @throws IOException
	 * @throws ResourceParseException
	 * @throws ProcessingException
	 */
	public void run(
			FileResolver inputFileResolver, FileResolver outputFileResolver, List<String> controlFileNames,
			Set<Pass> vdypPassSet
	) throws IOException, ResourceParseException, ProcessingException {

		logPass(vdypPassSet);

		// Load the control map
		Map<String, Object> controlMap = new HashMap<>();

		var parser = getControlFileParser();

		for (var controlFileName : controlFileNames) {
			logger.info("Resolving and parsing {}", controlFileName);

			try (var is = inputFileResolver.resolveForInput(controlFileName)) {
				Path controlFilePath = inputFileResolver.toPath(controlFileName).getParent();
				FileSystemFileResolver relativeResolver = new FileSystemFileResolver(controlFilePath);

				controlMap = parser.parse(is, relativeResolver, controlMap);
			}
		}

		process(vdypPassSet, controlMap, Optional.of(outputFileResolver));
	}

	/**
	 * Get a parser for the control file for this application
	 *
	 * @return
	 */
	protected abstract BaseControlParser getControlFileParser();

	/**
	 * @return all values of the pass enum applicable for this processor
	 */
	protected Set<Pass> getAllPasses() {
		return EnumSet.allOf(Pass.class);
	}

	/**
	 * Log the settings of the pass set
	 *
	 * @param vdypPassSet
	 */
	protected void logPass(Set<Pass> active) {
		logger.atInfo().addArgument(active).setMessage("VDYPPASS: {}").log();
		for (var pass : getAllPasses()) {
			String activeIndicator = active.contains(pass) ? "☑" : "☐";
			logger.atDebug().addArgument(activeIndicator).addArgument(pass.toString())
					.addArgument(pass.getDescription());
		}
	}

	/**
	 * Implements the process
	 *
	 * @param vdypPassSet        Phases of the process to implement
	 * @param controlMap         Control map to configure the process
	 * @param outputFileResolver File resolver for the output
	 */
	public void
			process(Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver)
					throws ProcessingException {
		process(vdypPassSet, controlMap, outputFileResolver, p -> true);
	}

	/**
	 * Implements the process
	 *
	 * @param vdypPassSet        Phases of the process to implement
	 * @param controlMap         Control map to configure the process
	 * @param outputFileResolver File resolver for the output
	 * @param polygonFilter      A polygon will be processed if and only if this returns true for that polygon
	 * @throws ProcessingException
	 */
	public abstract void process(
			Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException;
}
