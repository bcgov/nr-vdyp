package ca.bc.gov.nrs.vdyp.application;

public enum Pass {
	INITIALIZE("Perform Initiation activities"), //
	OPEN_FILES("Open the stand data files"), //
	PROCESS_STANDS("Process stands"), //
	MULTIPLE_STANDS("Allow multiple polygons"), //
	CLOSE_FILES("Close data files"), //
	ADDITIONAL_BASE_AREA_CRITERIA("Impose additional base area criteria");

	final String description;

	private Pass(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public static final Pass PASS_1 = INITIALIZE;
	public static final Pass PASS_2 = OPEN_FILES;
	public static final Pass PASS_3 = PROCESS_STANDS;
	public static final Pass PASS_4 = MULTIPLE_STANDS;
	public static final Pass PASS_5 = CLOSE_FILES;
	public static final Pass PASS_6 = ADDITIONAL_BASE_AREA_CRITERIA;
}
