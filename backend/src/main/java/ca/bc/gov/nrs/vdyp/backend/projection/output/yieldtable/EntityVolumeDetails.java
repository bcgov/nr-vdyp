package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

public record EntityVolumeDetails(
		Double wholeStemVolume, //
		Double closeUtilizationVolume, //
		Double cuVolumeLessDecay, //
		Double cuVolumeLessDecayWastage, //
		Double cuVolumeLessDecayWastageBreakage
) {
}
