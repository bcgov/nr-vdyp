package ca.bc.gov.nrs.vdyp.backend.projection.model;

public class SpeciesReportingInfo implements Comparable<SpeciesReportingInfo> {

	private String sp64Name;
	private double sp64Percent;

	private Double wholeStemVolume;
	private Double closeUtilizationVolume;
	private Double cuVolumeLessDecay;
	private Double cuVolumeLessDecayWaste;
	private Double cuVolumeLessDecayWastageBreakage;

	private int asSuppliedIndex;

	public String getSp64Name() {
		return sp64Name;
	}

	public double getSp64Percent() {
		return sp64Percent;
	}

	public int getAsSuppliedIndex() {
		return asSuppliedIndex;
	}

	public static class Builder {
		private SpeciesReportingInfo speciesReportingInfo = new SpeciesReportingInfo();

		public Builder sp64Name(String sp64Name) {
			speciesReportingInfo.sp64Name = sp64Name;
			return this;
		}

		public Builder sp64Percent(double sp64Percent) {
			speciesReportingInfo.sp64Percent = sp64Percent;
			return this;
		}

		public Builder wholeStemVolume(double wholeStemVolume) {
			speciesReportingInfo.wholeStemVolume = wholeStemVolume;
			return this;
		}

		public Builder closeUtilizationVolume(double closeUtilizationVolume) {
			speciesReportingInfo.closeUtilizationVolume = closeUtilizationVolume;
			return this;
		}

		public Builder cuVolumeLessDecay(double cuVolumeLessDecay) {
			speciesReportingInfo.cuVolumeLessDecay = cuVolumeLessDecay;
			return this;
		}

		public Builder cuVolumeLessDecayWaste(double cuVolumeLessDecayWaste) {
			speciesReportingInfo.cuVolumeLessDecayWaste = cuVolumeLessDecayWaste;
			return this;
		}

		public Builder cuVolumeLessDecayWastageBreakage(double cuVolumeLessDecayWastageBreakage) {
			speciesReportingInfo.cuVolumeLessDecayWastageBreakage = cuVolumeLessDecayWastageBreakage;
			return this;
		}

		public Builder asSuppliedIndex(int asSuppliedIndex) {
			speciesReportingInfo.asSuppliedIndex = asSuppliedIndex;
			return this;
		}

		public SpeciesReportingInfo build() {
			return speciesReportingInfo;
		}
	}

	public static class VolumeApplier {
		private SpeciesReportingInfo speciesReportingInfo;

		public VolumeApplier(SpeciesReportingInfo speciesReportingInfo) {
			this.speciesReportingInfo = speciesReportingInfo;
		}

		public VolumeApplier wholeStemVolume(double wholeStemVolume) {
			speciesReportingInfo.wholeStemVolume = wholeStemVolume;
			return this;
		}

		public VolumeApplier closeUtilizationVolume(double closeUtilizationVolume) {
			speciesReportingInfo.closeUtilizationVolume = closeUtilizationVolume;
			return this;
		}

		public VolumeApplier cuVolumeLessDecay(double cuVolumeLessDecay) {
			speciesReportingInfo.cuVolumeLessDecay = cuVolumeLessDecay;
			return this;
		}

		public VolumeApplier cuVolumeLessDecayWaste(double cuVolumeLessDecayWaste) {
			speciesReportingInfo.cuVolumeLessDecayWaste = cuVolumeLessDecayWaste;
			return this;
		}

		public VolumeApplier cuVolumeLessDecayWastageBreakage(double cuVolumeLessDecayWastageBreakage) {
			speciesReportingInfo.cuVolumeLessDecayWastageBreakage = cuVolumeLessDecayWastageBreakage;
			return this;
		}

		public SpeciesReportingInfo apply() {
			return speciesReportingInfo;
		}
	}

	@Override
	/**
	 * Implementation of the default sort order - by percentage, decreasing. In the event of ties, in the order
	 * supplied.
	 */
	public int compareTo(SpeciesReportingInfo that) {
		long thisPercentage = Math.round(this.sp64Percent);
		long thatPercentage = Math.round(that.sp64Percent);
		if (thatPercentage == thisPercentage) {
			return this.asSuppliedIndex - that.asSuppliedIndex;
		} else {
			return thisPercentage < thatPercentage ? 1 : -1;
		}
	}
}
