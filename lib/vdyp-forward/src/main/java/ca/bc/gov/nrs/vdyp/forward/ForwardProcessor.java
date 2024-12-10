package ca.bc.gov.nrs.vdyp.forward;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.Pass;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

/**
 *
 * The algorithmic part of VDYP7 GROWTH Program. In October, 2000 this was split off from the main PROGRAM, which now
 * just defines units and fills /C_CNTR/
 *
 * VDYPPASS IN/OUT I*4(10) Major Control Functions
 * <ul>
 * <li>(1) IN Perform Initiation activities? (0=No, 1=Yes)
 * <li>(2) IN Open the stand data files (0=No, 1=Yes)
 * <li>(3) IN Process stands (0=No, 1=Yes)
 * <li>(4) IN Allow multiple polygons (0=No, 1=Yes) (Subset of stand processing. May limit to 1 stand)
 * <li>(5) IN CLOSE data files.
 * <li>(10) OUT Indicator variable that in the case of single stand processing with VDYPPASS(4) set, behaves as follows:
 * <ul>
 * <li>-100 due to EOF, nothing to read
 * <li>other -ve value, incl -99. Could not process the stand.
 * <li>0 Stand was processed and written
 * <li>+ve value. Serious error. Set to IER.
 * <li>IER OUTPUT I*4 Error code
 * <ul>
 * <li>0: No error
 * <li>>0: Error 99: Error generated in routine called by this subr.
 * <li><0: Warning
 * </ul>
 * </ul>
 * </ol>
 *
 * @author Michael Junkin, Vivid Solutions
 * @author Kevin Smith, Vivid Solutions
 */
public class ForwardProcessor extends Processor {

	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessor.class);

	/**
	 * Implements VDYP_SUB, excluding all polygons that don't pass the given <code>polygonFilter</code>.
	 *
	 * @param vdypPassSet        the set of stages (passes) to be executed
	 * @param controlMap         parsed control map
	 * @param outputFileResolver optional file resolver that, if present, locates output files.
	 * @param polygonFilter      a given polygon is processed only if this predicate returns <code>true</code> for it.
	 *
	 * @throws ProcessingException
	 */
	@Override

	public void process(
			Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException {

		logger.info("Beginning processing with given configuration");

		int maxPoly = 0;
		if (vdypPassSet.contains(Pass.INITIALIZE)) {
			Object maxPolyValue = controlMap.get(ControlKey.MAX_NUM_POLY.name());
			if (maxPolyValue != null) {
				maxPoly = (Integer) maxPolyValue;
			}
		}

		logger.debug("MaxPoly: {}", maxPoly);

		if (vdypPassSet.contains(Pass.OPEN_FILES)) {
			// input files are already opened
			// TODO: open output files
		}

		if (vdypPassSet.contains(Pass.PROCESS_STANDS)) {

			Optional<VdypOutputWriter> outputWriter = Optional.empty();

			if (outputFileResolver.isPresent()) {
				try {
					outputWriter = Optional.of(new VdypOutputWriter(controlMap, outputFileResolver.get()));
				} catch (IOException e) {
					throw new ProcessingException(e);
				}
			}

			var fpe = new ForwardProcessingEngine(controlMap, outputWriter);

			var forwardDataStreamReader = new ForwardDataStreamReader(fpe.fps.getControlMap());

			// Fetch the next polygon to process.
			int nPolygonsProcessed = 0;
			while (true) {

				if (nPolygonsProcessed == maxPoly) {
					logger.info(
							"Prematurely terminating polygon processing since MAX_POLY ({}) polygons have been processed",
							maxPoly
					);
				}

				var polygonHolder = forwardDataStreamReader.readNextPolygon();
				if (polygonHolder.isEmpty()) {
					break;
				}

				var polygon = polygonHolder.get();

				if (polygonFilter.test(polygon)) {
					fpe.processPolygon(polygon);
					nPolygonsProcessed += 1;
				}
			}

			outputWriter.ifPresent(ow -> {
				try {
					ow.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	@Override
	protected BaseControlParser getControlFileParser() {
		return new ForwardControlParser();
	}
}
