package ca.bc.gov.nrs.vdyp.application;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;

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
public abstract class Processor {

	private static final Logger logger = LoggerFactory.getLogger(Processor.class);

	/**
	 * @return a parser for the control file for this application
	 */
	protected abstract BaseControlParser<ProcessingDebugSettings> getControlFileParser();

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
			Set<Pass> vdypPassSet, ProcessingResolvedControlMap controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException;
}
