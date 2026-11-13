package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.util.List;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BasalAreaGrowthEmpiricalParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DqGrowthEmpiricalLimitsParser;

public class ForwardProcessingControlParser extends ProcessingControlParser {
	@Override
	protected List<OutputFileLocationResolver> outputFiles() {
		final List<OutputFileLocationResolver> result = List.of(
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_POLYGON), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_COMPATIBILITY_VARIABLES)
		);
		assert result.containsAll(super.outputFiles());
		return result;
	}

	@Override
	protected void initialize() {

		// V7O_VOC - 19
		addOutputWriter(ControlKey.VDYP_OUTPUT_COMPATIBILITY_VARIABLES);

		// RD_E121
		addConfigurationParser(new BasalAreaGrowthEmpiricalParser());
		// RD_E123
		addConfigurationParser(new DqGrowthEmpiricalLimitsParser());

		super.initialize();
	}

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

}
