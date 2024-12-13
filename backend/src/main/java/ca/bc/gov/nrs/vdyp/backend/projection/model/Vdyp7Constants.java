package ca.bc.gov.nrs.vdyp.backend.projection.model;

public class Vdyp7Constants {
	
	public static final int MAX_LEN_MAPSHEET = 7;
	public static final int MAX_LEN_BEC_ZONE = 4;
	public static final int MAX_LEN_NON_PROD_DESC = 5;

	public static final short MIN_PROJECTION_YEAR = 1500;
	public static final short MAX_PROJECTION_YEAR = 2500;

	public static final short MIN_CALENDAR_YEAR = 1400;
	public static final short MAX_CALENDAR_YEAR = 2500;

	public static final double MIN_SPECIES_AGE = 0.0;
	public static final double MAX_SPECIES_AGE = 2000.0;

	public static final int MAX_NUM_LAYERS_PER_POLYGON = 9;
	public static final int MAX_NUM_SPECIES_PER_LAYER = 6;

	public static final int ACT_NUM_LAYERS_PER_POLYGON = MAX_NUM_LAYERS_PER_POLYGON + 2;
	public static final int NDX_COMPOSITE_LAYER = MAX_NUM_LAYERS_PER_POLYGON;
	public static final int NDX_SPANNING_LAYER = MAX_NUM_LAYERS_PER_POLYGON + 1;
	public static final int MAX_NUM_SP0_PER_LAYER = MAX_NUM_SPECIES_PER_LAYER;
	public static final int MAX_NUM_SP64_PER_SP0 = MAX_NUM_SPECIES_PER_LAYER;

	public static final double MIN_SITEINDEX_AGE = 30.0;

	public static final String VDYP7_LAYER_ID_PRI = "P";
	public static final String VDYP7_LAYER_ID_VET = "V";
}
