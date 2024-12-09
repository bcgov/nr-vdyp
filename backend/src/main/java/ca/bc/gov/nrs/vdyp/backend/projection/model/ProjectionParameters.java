package ca.bc.gov.nrs.vdyp.backend.projection.model;

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
	
	/** The Species Group */
	private List<String> reportedSpeciesGroups;
	
	/** true if any only if Forward processing is enabled */
	private boolean enableForward;
	
	/** true if any only if Back processing is enabled */
	private boolean enableBack;
}
