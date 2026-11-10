package ca.bc.gov.nrs.vdyp.forward;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;

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
 */
public class ForwardProcessor extends Processor<ForwardProcessingEngine> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessor.class);

	@Override
	public ForwardProcessingEngine
			createProcessingEngine(ProcessingResolvedControlMap controlMap, Optional<VdypOutputWriter> outputWriter) {
		return new ForwardProcessingEngine(controlMap.getControlMap(), outputWriter);
	}

	@Override
	protected BaseControlParser<ProcessingDebugSettings> getControlFileParser() {
		return new ProcessingControlParser();
	}
}
