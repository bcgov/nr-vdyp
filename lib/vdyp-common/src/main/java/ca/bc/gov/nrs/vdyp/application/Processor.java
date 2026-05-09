package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

/**
 *
 * The common code for the algorithmic part of a forward or backward growth predictor
 *
 * VDYPPASS IN/OUT I*4(10) Major Control Functions + *
 * <ol>
 *
 * <li>IN Perform Initiation activities? (0=No, 1=Yes) + *
 * <li>IN Open the stand data files (0=No, 1=Yes) + *
 * <li>IN Process stands (0=No, 1=Yes) + *
 * <li>IN Allow multiple polygons (0=No, 1=Yes) (Subset of stand processing. May limit to 1 stand) + *
 * </ol>
 *
 * @author Michael Junkin, Vivid Solutions
 * @author Kevin Smith, Vivid Solutions
 *
 */
public abstract class Processor<DS extends DebugSettings> {

	private static final Logger logger = LoggerFactory.getLogger(Processor.class);

	/**
	 * Initialize Processor
	 *
	 * @param inputFileResolver  File resolver for the input, including the control files
	 * @param outputFileResolver File resolver for the output
	 * @param controlFileNames   File names of control maps to load, entries specified in later maps will override
	 *                           earlier ones
	 * @param vdypPassSet        Phases of the process to implement
	 * @param polygonFilter      A polygon will be processed if and only if this returns true for that polygon
	 *
	 * @throws IOException
	 * @throws ResourceParseException
	 * @throws ProcessingException
	 */
	public void run(
			FileResolver inputFileResolver, FileResolver outputFileResolver, List<String> controlFileNames,
			Set<Pass> vdypPassSet, Predicate<VdypPolygon> polygonFilter
	) throws IOException, ResourceParseException, ProcessingException {

		logPass(vdypPassSet);

		// Load the control map
		Map<String, Object> controlMap = new HashMap<>();

		var parser = getControlFileParser();

		parser.parseByName(controlFileNames, inputFileResolver, controlMap);

		process(vdypPassSet, controlMap, Optional.of(outputFileResolver), polygonFilter);
	}

	/**
	 * Initialize Processor
	 *
	 * @param inputFileResolver  File resolver for the input, including the control files
	 * @param outputFileResolver File resolver for the output
	 * @param controlFileNames   File names of control maps to load, entries specified in later maps will override
	 *                           earlier ones
	 * @param vdypPassSet        Phases of the process to implement
	 *
	 * @throws IOException
	 * @throws ResourceParseException
	 * @throws ProcessingException
	 */
	public void run(
			FileResolver inputFileResolver, FileResolver outputFileResolver, List<String> controlFileNames,
			Set<Pass> vdypPassSet
	) throws IOException, ResourceParseException, ProcessingException {

		run(inputFileResolver, outputFileResolver, controlFileNames, vdypPassSet, p -> true);
	}

	/**
	 * @return a parser for the control file for this application
	 */
	protected abstract BaseControlParser<DS> getControlFileParser();

	/**
	 * @return all values of the pass enum applicable for this processor
	 */
	protected Set<Pass> getAllPasses() {
		return EnumSet.allOf(Pass.class);
	}

	/**
	 * Log the settings of the pass set
	 *
	 * @param vdypPassSet Phases of the process to implement
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
	 * @param polygonFilter      A polygon will be processed if and only if this returns true for that polygon
	 * @throws ProcessingException
	 */
	public abstract void process(
			Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException;

	/**
	 * Implements the process
	 *
	 * @param vdypPassSet        Phases of the process to implement
	 * @param controlMap         Control map to configure the process
	 * @param outputFileResolver File resolver for the output
	 * @throws ProcessingException
	 */
	public void
			process(Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver)
					throws ProcessingException {
		process(vdypPassSet, controlMap, outputFileResolver, p -> true);
	}
}
