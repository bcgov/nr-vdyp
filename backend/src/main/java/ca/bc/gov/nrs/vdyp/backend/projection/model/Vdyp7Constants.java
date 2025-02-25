package ca.bc.gov.nrs.vdyp.backend.projection.model;

public class Vdyp7Constants {
	
	public static final double EMPTY_DECIMAL = -9.0f;
	public static final String EMPTY_DECIMAL_TEXT = Double.toString(EMPTY_DECIMAL);
	public static final int EMPTY_INT = -9;
	public static final String EMPTY_INT_TEXT = Integer.toString(EMPTY_INT);

	// Polygon related constants
	
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

	public static final String VDYP7_LAYER_ID_PRIMARY = "P";
	public static final String VDYP7_LAYER_ID_SPANNING = "X";
	public static final String VDYP7_LAYER_ID_VETERAN = "V";
	
	// Layer related constants
	
	public static final int MAX_LEN_LAYER_ID = 1;
	public static final int MAX_FOREST_COVER_RANK_CODE_LEN = 1;
	public static final int MAX_NON_FOREST_DESCRIPTOR_CODE_LEN = 5;
	public static final int MAX_LEN_ESTIMATED_SITE_INDEX_SPECIES_CODE = 3;
	
	public static final double MIN_VETERAN_LAYER_HEIGHT = 10.0;
	public static final double MIN_VETERAN_LAYER_AGE = 140.0;
	public static final double MIN_VETERAN_LAYER_CROWN_CLOSURE = 1.0;
	public static final double MAX_VETERAN_LAYER_CROWN_CLOSURE = 10.0;
	public static final double MIN_VETERAN_LAYER_TPH = 1.0;
	public static final double MAX_VETERAN_LAYER_TPH_EXCLUSIVE = 25.0;
	public static final double PI_40K = 0.78539816E-04;
	public static final double MIN_VETERAN_LAYER_DBH = 23.0;
	
	public static final double LOW_SITE_INDEX_THRESHOLD = 2.0;
}
