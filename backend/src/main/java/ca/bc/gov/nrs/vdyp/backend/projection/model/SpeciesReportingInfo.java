package ca.bc.gov.nrs.vdyp.backend.projection.model;

public class SpeciesReportingInfo implements Comparable<SpeciesReportingInfo> {

	private String sp64Name;
	private double sp64Percent;

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
			return this;
		}

		public VolumeApplier closeUtilizationVolume(double closeUtilizationVolume) {
			return this;
		}

		public VolumeApplier cuVolumeLessDecay(double cuVolumeLessDecay) {
			return this;
		}

		public VolumeApplier cuVolumeLessDecayWaste(double cuVolumeLessDecayWaste) {
			return this;
		}

		public VolumeApplier cuVolumeLessDecayWastageBreakage(double cuVolumeLessDecayWastageBreakage) {
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
