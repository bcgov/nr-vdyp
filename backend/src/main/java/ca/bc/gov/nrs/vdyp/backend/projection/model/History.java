package ca.bc.gov.nrs.vdyp.backend.projection.model;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.SilviculturalBase;

/** Records information about disturbances to the polygon. */
public class History {
	
	/** Identifies the Silvicultural Base code that occurred. */
	private SilviculturalBase silvicultureBase;
	
	/** The year the disturbance started. */
	private int disturbanceStartYear;
	
	/** The year the disturbance ended. */
	private int disturbanceEndYear;
	
	/** The percent of the layer that was disturned. */
	private double percent;
	
	private History() {
		silvicultureBase = SilviculturalBase.getDefault();
		disturbanceEndYear = 0;
		disturbanceStartYear = 0;
		percent = 0.0;
	}
	
	public SilviculturalBase getSilvicultureBase() {
		return silvicultureBase;
	}

	public int getDisturbanceStartYear() {
		return disturbanceStartYear;
	}

	public int getDisturbanceEndYear() {
		return disturbanceEndYear;
	}

	public double getPercent() {
		return percent;
	}

	public static class Builder {
		private History history = new History();
		
		public Builder silvicultureBase(SilviculturalBase silvicultureBase) {
			history.silvicultureBase = silvicultureBase;
			return this;
		}
		
		public Builder disturbanceStartYear(int disturbanceStartYear) {
			history.disturbanceStartYear = disturbanceStartYear;
			return this;
		}
		
		public Builder disturbanceEndYear(int disturbanceEndYear) {
			history.disturbanceEndYear = disturbanceEndYear;
			return this;
		}
		
		public Builder percent(double percent) {
			history.percent = percent;
			return this;
		}
		
		public History build() { 
			return history;
		}
	}
}
