package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.ArrayList;
import java.util.List;

/** Holds the parameters for the most recent (possibly last) stand projection. */
public class ProjectionParameters {
	
	/** The measurement year for the stand */
	private int measurementYear;
	
	/** The age of the stand at the measurement year */
	private int standAgeAtMeasurementYear;
	
	/** The calendar year for the start of the projection */
	private int yearStart;
	
	/** The calendar year to which the projection is to proceed */
	private int yearEnd;
	
	/** The Species Groups */
	private List<String> reportedSpeciesGroups;
	
	/** true if any only if Forward processing is enabled */
	private boolean enableForward;
	
	/** true if any only if Back processing is enabled */
	private boolean enableBack;

	public int getMeasurementYear() {
		return measurementYear;
	}

	public int getStandAgeAtMeasurementYear() {
		return standAgeAtMeasurementYear;
	}

	public int getYearStart() {
		return yearStart;
	}

	public int getYearEnd() {
		return yearEnd;
	}

	public List<String> getReportedSpeciesGroups() {
		return reportedSpeciesGroups;
	}

	public boolean isEnableForward() {
		return enableForward;
	}

	public boolean isEnableBack() {
		return enableBack;
	}

	void setMeasurementYear(int measurementYear) {
		this.measurementYear = measurementYear;
	}

	void setStandAgeAtMeasurementYear(int standAgeAtMeasurementYear) {
		this.standAgeAtMeasurementYear = standAgeAtMeasurementYear;
	}

	void setYearStart(int yearStart) {
		this.yearStart = yearStart;
	}

	void setYearEnd(int yearEnd) {
		this.yearEnd = yearEnd;
	}

	void setReportedSpeciesGroups(List<String> reportedSpeciesGroups) {
		this.reportedSpeciesGroups = reportedSpeciesGroups;
	}

	void setEnableForward(boolean enableForward) {
		this.enableForward = enableForward;
	}

	void setEnableBack(boolean enableBack) {
		this.enableBack = enableBack;
	}
	
	private ProjectionParameters() {
		enableForward = true;
		enableBack = true;		

		measurementYear = 0;
		standAgeAtMeasurementYear = 0;
		yearStart = 0;
		yearEnd = 0;

		reportedSpeciesGroups = new ArrayList<>();
	}
	
	public static class Builder {
		private ProjectionParameters projectionParameters = new ProjectionParameters();
		
		public Builder measurementYear(int measurementYear) {
			projectionParameters.setMeasurementYear(measurementYear);
			return this;
		}

		public Builder standAgeAtMeasurementYear(int standAgeAtMeasurementYear) {
			projectionParameters.setStandAgeAtMeasurementYear(standAgeAtMeasurementYear);
			return this;
		}

		public Builder yearStart(int yearStart) {
			projectionParameters.setYearStart(yearStart);
			return this;
		}

		public Builder yearEnd(int yearEnd) {
			projectionParameters.setYearEnd(yearEnd);
			return this;
		}

		public Builder reportedSpeciesGroups(List<String> reportedSpeciesGroups) {
			projectionParameters.setReportedSpeciesGroups(reportedSpeciesGroups);
			return this;
		}

		public Builder enableForward(boolean enableForward) {
			projectionParameters.setEnableForward(enableForward);
			return this;
		}

		public Builder enableBack(boolean enableBack) {
			projectionParameters.setEnableBack(enableBack);
			return this;
		}

		public ProjectionParameters build() {
			return projectionParameters;
		}
	}
}
