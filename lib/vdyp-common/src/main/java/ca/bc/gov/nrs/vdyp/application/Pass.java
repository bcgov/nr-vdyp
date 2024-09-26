package ca.bc.gov.nrs.vdyp.application;

public enum Pass {
	INITIALIZE("Perform Initiation activities"),
	OPEN_FILES("Open the stand data files"),
	PROCESS_STANDS("Process stands"),
	MULTIPLE_STANDS("Allow multiple polygons"),
	CLOSE_FILES("Close data files"),
	ADDITIONAL_BASE_AREA_CRITERIA("Impose additional base area criteria");

	final String description;

	private Pass(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
