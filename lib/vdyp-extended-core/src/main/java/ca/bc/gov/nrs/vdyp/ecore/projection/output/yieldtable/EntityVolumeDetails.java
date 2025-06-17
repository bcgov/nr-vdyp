package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

public record EntityVolumeDetails(
		Double wholeStemVolume, //
		Double closeUtilizationVolume, //
		Double cuVolumeLessDecay, //
		Double cuVolumeLessDecayWastage, //
		Double cuVolumeLessDecayWastageBreakage
) {
}
