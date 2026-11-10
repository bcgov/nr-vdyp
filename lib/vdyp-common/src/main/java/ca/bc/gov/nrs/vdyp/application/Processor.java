package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.projection.ProcessingDataStreamReader;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
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
public abstract class Processor<E extends ProcessingEngine<?, ?>> {

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
	 * Implements VDYP_SUB/VDYPBSUB, excluding all polygons that don't pass the given <code>polygonFilter</code>.
	 *
	 * @param vdypPassSet        the set of stages (passes) to be executed
	 * @param controlMap         parsed control map
	 * @param outputFileResolver optional file resolver that, if present, locates output files.
	 * @param polygonFilter      a given polygon is processed only if this predicate returns <code>true</code> for it.
	 *
	 * @throws ProcessingException
	 */
	public void process(
			Set<Pass> vdypPassSet, ProcessingResolvedControlMap controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException {

		// It seems like this is is only ever called with a polygonFilter that always returns true.

		logger.info("Beginning processing with given configuration");

		int maxPoly = 0;
		if (vdypPassSet.contains(Pass.INITIALIZE)) {
			maxPoly = initialize(controlMap, maxPoly);
		}

		logger.debug("MaxPoly: {}", maxPoly);

		// Pass 2, OPEN_FILES not used in VDYP8 That happens in processStands now.

		if (vdypPassSet.contains(Pass.PROCESS_STANDS)) {

			processStands(controlMap, outputFileResolver, polygonFilter, maxPoly);

		}
	}

	protected int initialize(ProcessingResolvedControlMap controlMap, int defaultMaxPoly) {
		return controlMap.getMaximumPolygonCount().orElse(defaultMaxPoly);
	}

	protected void processStands(
			ProcessingResolvedControlMap controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter, int maxPoly
	) throws ProcessingException {
		try {
			var outputWriter = createWriter(controlMap, outputFileResolver);
			E engine = createProcessingEngine(controlMap, outputWriter);

			try (var reader = createDataStream(engine);) {
				// Fetch the next polygon to process.
				int nPolygonsProcessed = 0;
				while (true) {

					if (nPolygonsProcessed == maxPoly) {
						logger.info(
								"Prematurely terminating polygon processing since MAX_POLY ({}) polygons have been processed",
								maxPoly
						);
						break;
					}

					var polygonHolder = reader.readNextPolygon();
					if (polygonHolder.isEmpty()) {
						break;
					}

					var polygon = polygonHolder.get();

					if (polygonFilter.test(polygon)) {
						engine.processPolygon(polygon);
						nPolygonsProcessed += 1;
					}
				}
			} finally {
				Utils.ifPresent(outputWriter, VdypOutputWriter::close);
			}
		} catch (IOException e) {
			throw new ProcessingException(e);
		}
	}

	protected Optional<VdypOutputWriter>
			createWriter(ProcessingResolvedControlMap controlMap, Optional<FileResolver> outputFileResolver)
					throws IOException {
		return Utils.map(outputFileResolver, ofr -> new VdypOutputWriter(controlMap.getControlMap(), ofr));
	}

	protected ProcessingDataStreamReader createDataStream(E engine) throws ProcessingException {
		return new ProcessingDataStreamReader(engine.getState().getControlMap());
	}

	/**
	 * Get the processing engine to use
	 *
	 * @param controlMap
	 * @param outputWriter
	 * @return
	 */
	public abstract E
			createProcessingEngine(ProcessingResolvedControlMap controlMap, Optional<VdypOutputWriter> outputWriter);
}
