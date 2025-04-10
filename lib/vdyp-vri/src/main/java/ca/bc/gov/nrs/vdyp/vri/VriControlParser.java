package ca.bc.gov.nrs.vdyp.vri;

import java.util.List;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.control.NonFipControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.OutputFileLocationResolver;

/**
 * Parser for VRI control files
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VriControlParser extends NonFipControlParser {

	public VriControlParser() {

		initialize();
	}

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.VRI_START;
	}

	@Override
	protected List<ControlMapValueReplacer<Object, String>> inputFileParsers() {
		return inputParserList(

				new VriPolygonParser(),

				new VriLayerParser(),

				new VriSpeciesParser(),

				new VriSiteParser()
		);
	}

	@Override
	protected List<OutputFileLocationResolver> outputFiles() {
		return List.of(
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_POLYGON), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL)
		);
	}
}
