package ca.bc.gov.nrs.vdyp.backend.projection.model;

public class SpeciesReportingInfo {

	private String sp64Name;
	private Double sp64Percent;
	
	public String getSp64Name() {
		return sp64Name;
	}
	public Double getSp64Percent() {
		return sp64Percent;
	}
	
	private SpeciesReportingInfo() {
	}
	
	public static class Builder {
		private SpeciesReportingInfo speciesReportingInfo = new SpeciesReportingInfo();
		
		public Builder sp64Name(String sp64Name) {
			speciesReportingInfo.sp64Name = sp64Name;
			return this;
		}
		
		public Builder sp64Percent(Double sp64Percent) {
			speciesReportingInfo.sp64Percent = sp64Percent;
			return this;
		}
		
		public SpeciesReportingInfo build() {
			return speciesReportingInfo;
		}
	}
}
