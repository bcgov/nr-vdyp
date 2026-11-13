package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.util.List;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BasalAreaGrowthEmpiricalParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BasalAreaGrowthFiatParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DqGrowthEmpiricalLimitsParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DqGrowthEmpiricalParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DqGrowthFiatParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.NonPrimarySpeciesBasalAreaGrowthParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.NonPrimarySpeciesDqGrowthParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.PrimarySpeciesBasalAreaGrowthParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.PrimarySpeciesDqGrowthParser;

public class ForwardControlParser extends ProcessingControlParser {
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

		// RD_E111
		addConfigurationParser(new BasalAreaGrowthFiatParser());
		// RD_E117
		addConfigurationParser(new DqGrowthFiatParser());
		// RD_E121
		addConfigurationParser(new BasalAreaGrowthEmpiricalParser());
		// RD_E122
		addConfigurationParser(new DqGrowthEmpiricalParser());
		// RD_E123
		addConfigurationParser(new DqGrowthEmpiricalLimitsParser());
		// RD_E148
		addConfigurationParser(new PrimarySpeciesBasalAreaGrowthParser());
		// RD_E149
		addConfigurationParser(new NonPrimarySpeciesBasalAreaGrowthParser());
		// RD_E150
		addConfigurationParser(new PrimarySpeciesDqGrowthParser());
		// RD_E151
		addConfigurationParser(new NonPrimarySpeciesDqGrowthParser());

		super.initialize();

	}

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

}
