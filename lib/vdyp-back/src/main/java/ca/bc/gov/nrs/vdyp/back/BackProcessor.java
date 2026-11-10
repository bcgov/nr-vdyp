package ca.bc.gov.nrs.vdyp.back;

import java.util.Optional;
import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.back.io.parse.BackControlParser;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;

/**
 *
 * The Back algorithm.
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
 *
 * @author Kevin Smith, Vivid Solutions
 */
public class BackProcessor extends Processor<BackProcessingEngine> {

	@Override
	public BackProcessingEngine
			createProcessingEngine(ProcessingResolvedControlMap controlMap, Optional<VdypOutputWriter> outputWriter) {
		return new BackProcessingEngine(controlMap.getControlMap(), outputWriter);
	}

	@Override
	protected BaseControlParser<ProcessingDebugSettings> getControlFileParser() {
		return new BackControlParser();
	}

}
