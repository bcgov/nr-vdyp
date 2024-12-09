package ca.bc.gov.nrs.vdyp.backend.projection.input;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;

public class HcsvPolygonRecordBean {

	// { "POLY_FEATURE_ID", csvFldType_CHAR, 38, 0, "", TRUE }, /* A. HCSV_IFld_POLYGON__FEATURE_ID */
	@CsvBindByName(column = "FEATURE_ID", required = true)
	private String polyFeatureId;

	// { "MAP_ID", csvFldType_CHAR, 9, 0, "", TRUE }, /* B. HCSV_IFld_POLYGON__MAP_ID */
	@CsvBindByName(column = "MAP_ID", required = true)
	private String mapId;

	//	{ "POLYGON_NO",                     csvFldType_LONG,   10, 0, "", TRUE }, /* C. HCSV_IFld_POLYGON__POLYGON_NO */
	@CsvBindByName(column = "POLYGON_NUMBER", required = true)
	private long polygonNumber;

	//	{ "ORG_UNIT",                       csvFldType_CHAR,   10, 0, "", TRUE }, /* D. HCVS_IFld_POLYGON__ORG_UNIT */
	@CsvBindByName(column = "ORG_UNIT", required = true)
	private String orgUnit;

	//	{ "TSA",                            csvFldType_CHAR,    5, 0, "", TRUE }, /* E. HCSV_IFld_POLYGON__TSA */
	@CsvBindByName(column = "TSA_NAME", required = true)
	private String tsaName;

	//	{ "TFL",                            csvFldType_CHAR,    5, 0, "", TRUE }, /* F. HCSV_IFld_POLYGON__TFL */
	@CsvBindByName(column = "TFL_NAME", required = true)
	private String tflName;

	//	{ "INVENTORY_STANDARD_CD",          csvFldType_CHAR,   10, 0, "", TRUE }, /* G. HCSV_IFld_POLYGON__INVENTORY_STANDARD */
	@CsvBindByName(column = "INVENTORY_STANDARD_CODE", required = true)
	private String inventoryStandardCode;

	//	{ "TSA_NUM",                        csvFldType_CHAR,    5, 0, "", TRUE }, /* H. HCSV_IFld_POLYGON__TSA_NUM */
	@CsvBindByName(column = "TSA_NUMBER", required = true)
	private String tsaNumber;

    //	{ "SHRUB_HEIGHT",                   csvFldType_SINGLE,  4, 1, "", TRUE }, /* I. HCSV_IFld_POLYGON__SHRUB_HEIGHT */
	@CsvBindByName(column = "SHRUB_HEIGHT")
	@CsvNumber("#.#")
	private Double shrubHeight;

    //	{ "SHRUB_CROWN_CLOSURE",            csvFldType_SHORT,   3, 0, "", TRUE }, /* J. HCSV_IFld_POLYGON__SHRUB_CC */
	@CsvBindByName(column = "SHRUB_CROWN_CLOSURE")
	private Double shrubCrownClosure;

    //	{ "SHRUB_COVER_PATTERN",            csvFldType_CHAR,   10, 0, "", TRUE }, /* K. HCSV_IFld_POLYGON__SHRUB_PATTERN */
	@CsvBindByName(column = "SHRUB_COVER_PATTERN")
	private String shrubCoverPattern;

    //	{ "HERB_COVER_TYPE",                csvFldType_CHAR,   10, 0, "", TRUE }, /* L. HCSV_IFld_POLYGON__HERB_COVER_TYPE */
	@CsvBindByName(column = "HERB_COVER_TYPE_CODE")
	private String herbCoverTypeCode;

    //	{ "HERB_COVER_PCT",                 csvFldType_SHORT,   3, 0, "", TRUE }, /* M. HCSV_IFld_POLYGON__HERB_CC */
	@CsvBindByName(column = "HERB_COVER_PCT")
	private Integer herbCoverPercent;

    //	{ "HERB_COVER_PATTERN",             csvFldType_CHAR,   10, 0, "", TRUE }, /* N. HCSV_IFld_POLYGON__HERB_PATTERN */
	@CsvBindByName(column = "HERB_COVER_PATTERN_CODE")
	private String herbCoverPatternCode;

    //	{ "BRYOID_COVER_PCT",               csvFldType_SHORT,   3, 0, "", TRUE }, /* O. HCSV_IFld_POLYGON__BRYOID_CC */
	@CsvBindByName(column = "BRYOID_COVER_PCT")
	private Integer bryoidCoverPercent;

    //	{ "BEC_ZONE_CD",                    csvFldType_CHAR,   10, 0, "", TRUE }, /* P. HCSV_IFld_POLYGON__BEC_ZONE */
	@CsvBindByName(column = "BEC_ZONE_CODE")
	private String becZoneCode;

    //	{ "CFS_ECO_ZONE",                   csvFldType_SHORT,   3, 0, "", TRUE }, /* Q. HCSV_IFld_POLYGON__CFS_ECO_ZONE */
	@CsvBindByName(column = "CFS_ECOZONE")
	private Integer cfsEcoZone;

    //	{ "STOCKABILITY",                   csvFldType_SINGLE,  4, 1, "", TRUE }, /* R. HCSV_IFld_POLYGON__PCT_STOCKABLE */
	@CsvBindByName(column = "PRE_DISTURBANCE_STOCKABILITY")
	@CsvNumber("#.#")
	private Double preDisturbanceStockability;

    //	{ "YIELD_FACTOR",                   csvFldType_SINGLE,  5, 3, "", TRUE }, /* S. HCSV_IFld_POLYGON__YIELD_FACTOR */
	@CsvBindByName(column = "YIELD_FACTOR")
	@CsvNumber("#.###")
	private Double yieldFactor;

    //	{ "NON_PRODUCTIVE_DESCRIPTOR_CD",   csvFldType_CHAR,    5, 0, "", TRUE }, /* T. HCSV_IFld_POLYGON__NON_PROD_DESC */
	@CsvBindByName(column = "NON_PRODUCTIVE_DESCRIPTOR_CD")
	private String nonProductiveDescriptorCode;

    //	{ "BCLCS_LEVEL_1",                  csvFldType_CHAR,   10, 0, "", TRUE }, /* U. HCSV_IFld_POLYGON__BCLCS_LVL_1_CODE */
	@CsvBindByName(column = "BCLCS_LEVEL1_CODE")
	private String bclcsLevel1Code;

    //	{ "BCLCS_LEVEL_2",                  csvFldType_CHAR,   10, 0, "", TRUE }, /* V. HCSV_IFld_POLYGON__BCLCS_LVL_2_CODE */
	@CsvBindByName(column = "BCLCS_LEVEL2_CODE")
	private String bclcsLevel2Code;
    //	{ "BCLCS_LEVEL_3",                  csvFldType_CHAR,   10, 0, "", TRUE }, /* W. HCSV_IFld_POLYGON__BCLCS_LVL_3_CODE */
	@CsvBindByName(column = "BCLCS_LEVEL3_CODE")
	private String bclcsLevel3Code;
    //	{ "BCLCS_LEVEL_4",                  csvFldType_CHAR,   10, 0, "", TRUE }, /* X. HCSV_IFld_POLYGON__BCLCS_LVL_4_CODE */
	@CsvBindByName(column = "BCLCS_LEVEL4_CODE")
	private String bclcsLevel4Code;
    //	{ "BCLCS_LEVEL_5",                  csvFldType_CHAR,   10, 0, "", TRUE }, /* Y. HCSV_IFld_POLYGON__BCLCS_LVL_5_CODE */
	@CsvBindByName(column = "BCLCS_LEVEL5_CODE")
	private String bclcsLevel5Code;

    //	{ "REFERENCE_YEAR",                 csvFldType_SHORT,   4, 0, "", TRUE }, /* Z. HCSV_IFld_POLYGON__REFERENCE_YEAR */
	@CsvBindByName(column = "PHOTO_ESTIMATION_BASE_YEAR")
	private Integer photoEstimationBaseYear;
    //	{ "YEAR_OF_DEATH",                  csvFldType_SHORT,   4, 0, "", TRUE }, /* AA. HCSV_IFld_POLYGON__YEAR_OF_DEATH */
	@CsvBindByName(column = "REFERENCE_YEAR")
	private Integer referenceYear;

    //	{ "STOCKABILITY_DEAD",              csvFldType_SINGLE,  4, 1, "", TRUE }, /* AB. HCSV_IFld_POLYGON__PCT_STOCKABLE_DEAD */
	@CsvBindByName(column = "PCT_DEAD")
	@CsvNumber("#.#")
	private Double percentDead;

    //	{ "NON_VEG_COVER_TYPE_1",           csvFldType_CHAR,   10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_1 */
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_1")
	private String nonVegCoverType1;

    //	{ "NON_VEG_COVER_PCT_1",            csvFldType_SHORT,   3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_1 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_1")
	private Integer nonVegCoverPercent1;

    //	{ "NON_VEG_COVER_PATTERN_1",        csvFldType_CHAR,   10, 0, "", TRUE }, /* AE. HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_1 */
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_1")
	private String nonVegCoverPattern1;

    //	{ "NON_VEG_COVER_TYPE_2",           csvFldType_CHAR,   10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_2 */
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_2")
	private String nonVegCoverType2;

    //	{ "NON_VEG_COVER_PCT_2",            csvFldType_SHORT,   3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_2 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_2")
	private Integer nonVegCoverPercent2;

    //	{ "NON_VEG_COVER_PATTERN_2",        csvFldType_CHAR,   10, 0, "", TRUE }, /* AE. HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_2 */
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_2")
	private String nonVegCoverPattern2;

    //	{ "NON_VEG_COVER_TYPE_3",           csvFldType_CHAR,   10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_3 */
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_3")
	private String nonVegCoverType3;

    //	{ "NON_VEG_COVER_PCT_3",            csvFldType_SHORT,   3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_3 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_3")
	private Integer nonVegCoverPercent3;

    //	{ "NON_VEG_COVER_PATTERN_3",        csvFldType_CHAR,   10, 0, "", TRUE }, /* AE. HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_3 */
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_3")
	private String nonVegCoverPattern3;

    //	{ "LAND_COVER_CLASS_CD_1",          csvFldType_CHAR,   10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_1 */
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_1")
	private String landCoverClassCode1;

	//	{ "LAND_COVER_CLASS_PCT_1",         csvFldType_SHORT,   3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_1 */
	@CsvBindByName(column = "LAND_COVER_PCT_1")
	private Integer landCoverPercent1;

    //	{ "LAND_COVER_CLASS_CD_2",          csvFldType_CHAR,   10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_2 */
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_2")
	private String landCoverClassCode2;

	//	{ "LAND_COVER_CLASS_PCT_2",         csvFldType_SHORT,   3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_2 */
	@CsvBindByName(column = "LAND_COVER_PCT_2")
	private Integer landCoverPercent2;

    //	{ "LAND_COVER_CLASS_CD_3",          csvFldType_CHAR,   10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_3 */
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_3")
	private String landCoverClassCode3;

	//	{ "LAND_COVER_CLASS_PCT_3",         csvFldType_SHORT,   3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_3 */
	@CsvBindByName(column = "LAND_COVER_PCT_3")
	private Integer landCoverPercent3;

	public String getPolyFeatureId() {
		return polyFeatureId;
	}

	public String getMapId() {
		return mapId;
	}

	public long getPolygonNumber() {
		return polygonNumber;
	}

	public String getOrgUnit() {
		return orgUnit;
	}

	public String getTsaName() {
		return tsaName;
	}

	public String getTflName() {
		return tflName;
	}

	public String getInventoryStandardCode() {
		return inventoryStandardCode;
	}

	public String getTsaNumber() {
		return tsaNumber;
	}

	public Double getShrubHeight() {
		return shrubHeight;
	}

	public Double getShrubCrownClosure() {
		return shrubCrownClosure;
	}

	public String getShrubCoverPattern() {
		return shrubCoverPattern;
	}

	public String getHerbCoverTypeCode() {
		return herbCoverTypeCode;
	}

	public Integer getHerbCoverPercent() {
		return herbCoverPercent;
	}

	public String getHerbCoverPatternCode() {
		return herbCoverPatternCode;
	}

	public Integer getBryoidCoverPercent() {
		return bryoidCoverPercent;
	}

	public String getBecZoneCode() {
		return becZoneCode;
	}

	public Integer getCfsEcoZone() {
		return cfsEcoZone;
	}

	public Double getPreDisturbanceStockability() {
		return preDisturbanceStockability;
	}

	public Double getYieldFactor() {
		return yieldFactor;
	}

	public String getNonProductiveDescriptorCode() {
		return nonProductiveDescriptorCode;
	}

	public String getBclcsLevel1Code() {
		return bclcsLevel1Code;
	}

	public String getBclcsLevel2Code() {
		return bclcsLevel2Code;
	}

	public String getBclcsLevel3Code() {
		return bclcsLevel3Code;
	}

	public String getBclcsLevel4Code() {
		return bclcsLevel4Code;
	}

	public String getBclcsLevel5Code() {
		return bclcsLevel5Code;
	}

	public Integer getPhotoEstimationBaseYear() {
		return photoEstimationBaseYear;
	}

	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Double getPercentDead() {
		return percentDead;
	}

	public String getNonVegCoverType1() {
		return nonVegCoverType1;
	}

	public Integer getNonVegCoverPercent1() {
		return nonVegCoverPercent1;
	}

	public String getNonVegCoverPattern1() {
		return nonVegCoverPattern1;
	}

	public String getNonVegCoverType2() {
		return nonVegCoverType2;
	}

	public Integer getNonVegCoverPercent2() {
		return nonVegCoverPercent2;
	}

	public String getNonVegCoverPattern2() {
		return nonVegCoverPattern2;
	}

	public String getNonVegCoverType3() {
		return nonVegCoverType3;
	}

	public Integer getNonVegCoverPercent3() {
		return nonVegCoverPercent3;
	}

	public String getNonVegCoverPattern3() {
		return nonVegCoverPattern3;
	}

	public String getLandCoverClassCode1() {
		return landCoverClassCode1;
	}

	public Integer getLandCoverPercent1() {
		return landCoverPercent1;
	}

	public String getLandCoverClassCode2() {
		return landCoverClassCode2;
	}

	public Integer getLandCoverPercent2() {
		return landCoverPercent2;
	}

	public String getLandCoverClassCode3() {
		return landCoverClassCode3;
	}

	public Integer getLandCoverPercent3() {
		return landCoverPercent3;
	}
}
