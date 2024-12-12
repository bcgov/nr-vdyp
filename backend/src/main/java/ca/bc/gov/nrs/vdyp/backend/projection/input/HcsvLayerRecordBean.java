package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvNumber;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;
import com.opencsv.bean.processor.PreAssignmentProcessor;

public class HcsvLayerRecordBean {

	public static CsvToBean<HcsvLayerRecordBean> createHcsvLayerStream(InputStream layersCsvStream) {
		return new CsvToBeanBuilder<HcsvLayerRecordBean>(new BufferedReader(new InputStreamReader(layersCsvStream))) //
				.withSeparator(',') //
				.withType(HcsvLayerRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.build();
	}

	// { "LAYER_FEATURE_ID",               csvFldType_CHAR,   38, 0, "", TRUE },  /*  A. HCSV_IFld_LAYER__FEATURE_ID                  */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "FEATURE_ID")
	private Long layerFeatureId;

	// { "TREE_COVER_ID",                  csvFldType_CHAR,   38, 0, "", TRUE },  /*  B. HCSV_IFld_LAYER__TREE_COVER_ID               */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TREE_COVER_LAYER_ESTIMATED_ID")
	private String treeCoverId;

	// { "LAYER_MAP_ID",                   csvFldType_CHAR,    9, 0, "", TRUE },  /*  C. HCSV_IFld_LAYER__MAP_ID                      */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "MAP_ID")
	private String layerMapId;

	// { "LAYER_POLYGON_NO",               csvFldType_LONG,   10, 0, "", TRUE },  /*  D. HCSV_IFld_LAYER__POLYGON_NO                  */
	@CsvBindByName(column = "POLYGON_NUMBER")
	private Long polygonNumber;
	
	// { "LAYER_LEVEL_CD",                 csvFldType_CHAR,    1, 0, "", TRUE },  /*  E. HCSV_IFld_LAYER__LAYER_LVL_CD                */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAYER_LEVEL_CODE")
	private String layerLevelCode;

	// { "VDYP7_LAYER_LEVEL_CD",           csvFldType_CHAR,    1, 0, "", TRUE },  /*  F. HCSV_IFld_LAYER__VDYP7_LAYER_LVL_CD          */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "VDYP7_LAYER_CD")
	private String vdyp7LayerLevelCode;

	// { "LAYER_STOCKABILITY",             csvFldType_SINGLE,  5, 1, "", TRUE },  /*  G. HCSV_IFld_LAYER__LAYER_STOCKABILITY          */
	@CsvBindByName(column = "LAYER_STOCKABILITY")
	private Double layerStockability;

	// { "LAYER_RANK_CD",                  csvFldType_CHAR,   38, 0, "", TRUE },  /*  H. HCSV_IFld_LAYER__RANK_CODE                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "FOREST_COVER_RANK_CODE")
	private String forestCoverRankCode;

	// { "NON_FOREST_DESCRIPTOR",          csvFldType_CHAR,   10, 0, "", TRUE },  /*  I. HCSV_IFld_LAYER__NON_FOREST_DESC             */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_FOREST_DESCRIPTOR_CODE")
	private String nonForestDescriptorCode;

	// { "EST_SITE_INDEX_SPECIES_CD",      csvFldType_CHAR,   10, 0, "", TRUE },  /*  J. HCSV_IFld_LAYER__EST_SI_SPCS                 */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "EST_SITE_INDEX_SPECIES_CD")
	private String estimatedSiteIndexSpeciesCode;

	// { "EST_SITE_INDEX",                 csvFldType_SINGLE,  5, 1, "", TRUE },  /*  K. HCSV_IFld_LAYER__EST_SI                      */
	@CsvBindByName(column = "ESTIMATED_SITE_INDEX")
	@CsvNumber("#.#")
	private Double estimatedSiteIndex;

	// { "CROWN_CLOSURE",                  csvFldType_SHORT,   3, 0, "", TRUE },  /*  L. HCSV_IFld_LAYER__CC                          */
	@CsvBindByName(column = "CROWN_CLOSURE")
	private Short crownClosure;

	// { "BASAL_AREA",                     csvFldType_SINGLE, 10, 6, "", TRUE },  /*  M. HCSV_IFld_LAYER__BA                          */
	@CsvBindByName(column = "BASAL_AREA_75")
	@CsvNumber("#.######")
	private Double basalArea75;

	// { "STEMS_PER_HA",                   csvFldType_SINGLE,  8, 0, "", TRUE },  /*  N. HCSV_IFld_LAYER__TPH                         */
	@CsvBindByName(column = "STEMS_PER_HA_75")
	private Double stemsPerHectare;

	// { "SPECIES_CD_1",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_1                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_1")
	private String speciesCode1;

	// { "SPECIES_PCT_1",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_1                  */
	@CsvBindByName(column = "SPECIES_PCT_1")
	@CsvNumber("#.##")
	private Double speciesPercent1;

	// { "SPECIES_CD_2",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_2                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_2")
	private String speciesCode2;

	// { "SPECIES_PCT_2",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_2                  */
	@CsvBindByName(column = "SPECIES_PCT_2")
	@CsvNumber("#.##")
	private Double speciesPercent2;

	// { "SPECIES_CD_3",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_3                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_3")
	private String speciesCode3;

	// { "SPECIES_PCT_3",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_3                  */
	@CsvBindByName(column = "SPECIES_PCT_3")
	@CsvNumber("#.##")
	private Double speciesPercent3;

	// { "SPECIES_CD_4",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_4                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_4")
	private String speciesCode4;

	// { "SPECIES_PCT_4",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_4                 */
	@CsvBindByName(column = "SPECIES_PCT_4")
	@CsvNumber("#.##")
	private Double speciesPercent4;

	// { "SPECIES_CD_5",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_5                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_5")
	private String speciesCode5;

	// { "SPECIES_PCT_5",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_5                  */
	@CsvBindByName(column = "SPECIES_PCT_5")
	@CsvNumber("#.##")
	private Double speciesPercent5;

	// { "SPECIES_CD_6",                   csvFldType_CHAR,   10, 0, "", TRUE },  /*  O. HCSV_IFld_LAYER__SPCS_CD_6                   */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SPECIES_CD_6")
	private String speciesCode6;

	// { "SPECIES_PCT_6",                  csvFldType_SINGLE,  5, 2, "", TRUE },  /*  P. HCSV_IFld_LAYER__SPCS_PCT_6                  */
	@CsvBindByName(column = "SPECIES_PCT_6")
	@CsvNumber("#.##")
	private Double speciesPercent6;

	// { "EST_AGE_SPP1",                   csvFldType_SHORT,   4, 0, "", TRUE },  /* AA. HCSV_IFld_LAYER__EST_AGE_SP_1                */
	@CsvBindByName(column = "EST_AGE_SPP1")
	private Short estimatedAgeSpp1;

	// { "EST_HEIGHT_SPP1",                csvFldType_SINGLE,  5, 1, "", TRUE },  /* AB. HCSV_IFld_LAYER__EST_HT_SP_1                 */
	@CsvBindByName(column = "EST_HEIGHT_SPP_1")
	@CsvNumber("#.#")
	private Double estimatedHeightSpp1;

	// { "EST_AGE_SPP2",                   csvFldType_SHORT,   4, 0, "", TRUE },  /* AC. HCSV_IFld_LAYER__EST_AGE_SP_2                */
	@CsvBindByName(column = "EST_AGE_SPP2")
	private Short estimatedAgeSpp2;

	// { "EST_HEIGHT_SPP2",                csvFldType_SINGLE,  5, 1, "", TRUE },  /* AD. HCSV_IFld_LAYER__EST_HT_SP_2                 */
	@CsvBindByName(column = "EST_HEIGHT_SPP_2")
	@CsvNumber("#.#")
	private Double estimatedHeightSpp2;

	// { "ADJUSTMENT_IND",                 csvFldType_CHAR,    1, 0, "", TRUE },  /* AE. HCSV_IFld_LAYER__ADJUSTMENT_IND              */
    @PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "ADJUSTMENT_IND")
	private String adjustmentIndicatorInd;

	// { "ADJ_LOREY_HEIGHT",               csvFldType_SINGLE,  9, 5, "", TRUE },  /* AF. HCSV_IFld_LAYER__ADJ_LOREY_HT                */
	@CsvBindByName(column = "LOREY_HEIGHT_75")
	@CsvNumber("#.#####")
	private Double loreyHeight75;

	// { "ADJ_BASAL_AREA_125",             csvFldType_SINGLE, 10, 6, "", TRUE },  /* AG. HCSV_IFld_LAYER__ADJ_BASAL_AREA              */
	@CsvBindByName(column = "BASAL_AREA_125")
	@CsvNumber("#.######")
	private Double basalArea125;
	
	// { "ADJ_VOL_PER_HA_75",              csvFldType_SINGLE,  9, 5, "", TRUE },  /* AH. HCSV_IFld_LAYER__ADJ_WSV_075                 */
	@CsvBindByName(column = "WS_VOL_PER_HA_75")
	@CsvNumber("#.#####")
	private Double wholeStemVolumePerHectare75;
	
	// { "ADJ_VOL_PER_HA_125",             csvFldType_SINGLE,  9, 5, "", TRUE },  /* AI. HCSV_IFld_LAYER__ADJ_WSV_125                 */
	@CsvBindByName(column = "WS_VOL_PER_HA_125")
	@CsvNumber("#.#####")
	private Double wholeStemVolumePerHectare125;
	
	// { "ADJ_CLOSE_UTIL_VOL_125",         csvFldType_SINGLE,  9, 5, "", TRUE },  /* AJ. HCSV_IFld_LAYER__ADJ_VCU_125                 */
	@CsvBindByName(column = "CU_VOL_PER_HA_125")
	@CsvNumber("#.#####")
	private Double closeUtilizationVolumePerHectare125;
	
	// { "ADJ_CLOSE_UTIL_DECAY_VOL_125",   csvFldType_SINGLE,  9, 5, "", TRUE },  /* AK. HCSV_IFld_LAYER__ADJ_VD_125                  */
	@CsvBindByName(column = "D_VOL_PER_HA_125")
	@CsvNumber("#.#####")
	private Double closeUtilizationVolumeLessDecayPerHectare125;
	
	// { "ADJ_CLOSE_UTIL_WASTE_VOL_125",   csvFldType_SINGLE,  9, 5, "", TRUE }   /* AL. HCSV_IFld_LAYER__ADJ_VDW_125                 */
	@CsvBindByName(column = "DW_VOL_PER_HA_125")
	@CsvNumber("#.#####")
	private Double closeUtilizationVolumeLessDecayAndWastagePerHectare125;

	public Long getLayerFeatureId() {
		return layerFeatureId;
	}

	public String getTreeCoverId() {
		return treeCoverId;
	}

	public String getLayerMapId() {
		return layerMapId;
	}

	public Long getPolygonNumber() {
		return polygonNumber;
	}

	public String getLayerLevelCode() {
		return layerLevelCode;
	}

	public String getVdyp7LayerLevelCode() {
		return vdyp7LayerLevelCode;
	}

	public Double getLayerStockability() {
		return layerStockability;
	}

	public String getForestCoverRankCode() {
		return forestCoverRankCode;
	}

	public String getNonForestDescriptorCode() {
		return nonForestDescriptorCode;
	}

	public String getEstimatedSiteIndexSpeciesCode() {
		return estimatedSiteIndexSpeciesCode;
	}

	public Double getEstimatedSiteIndex() {
		return estimatedSiteIndex;
	}

	public Short getCrownClosure() {
		return crownClosure;
	}

	public Double getBasalArea75() {
		return basalArea75;
	}

	public Double getStemsPerHectare() {
		return stemsPerHectare;
	}

	public String getSpeciesCode1() {
		return speciesCode1;
	}

	public Double getSpeciesPercent1() {
		return speciesPercent1;
	}

	public String getSpeciesCode2() {
		return speciesCode2;
	}

	public Double getSpeciesPercent2() {
		return speciesPercent2;
	}

	public String getSpeciesCode3() {
		return speciesCode3;
	}

	public Double getSpeciesPercent3() {
		return speciesPercent3;
	}

	public String getSpeciesCode4() {
		return speciesCode4;
	}

	public Double getSpeciesPercent4() {
		return speciesPercent4;
	}

	public String getSpeciesCode5() {
		return speciesCode5;
	}

	public Double getSpeciesPercent5() {
		return speciesPercent5;
	}

	public String getSpeciesCode6() {
		return speciesCode6;
	}

	public Double getSpeciesPercent6() {
		return speciesPercent6;
	}

	public Short getEstimatedAgeSpp1() {
		return estimatedAgeSpp1;
	}

	public Double getEstimatedHeightSpp1() {
		return estimatedHeightSpp1;
	}

	public Short getEstimatedAgeSpp2() {
		return estimatedAgeSpp2;
	}

	public Double getEstimatedHeightSpp2() {
		return estimatedHeightSpp2;
	}

	public String getAdjustmentIndicatorInd() {
		return adjustmentIndicatorInd;
	}

	public Double getLoreyHeight75() {
		return loreyHeight75;
	}

	public Double getBasalArea125() {
		return basalArea125;
	}

	public Double getWholeStemVolumePerHectare75() {
		return wholeStemVolumePerHectare75;
	}

	public Double getWholeStemVolumePerHectare125() {
		return wholeStemVolumePerHectare125;
	}

	public Double getCloseUtilizationVolumePerHectare125() {
		return closeUtilizationVolumePerHectare125;
	}

	public Double getCloseUtilizationVolumeLessDecayPerHectare125() {
		return closeUtilizationVolumeLessDecayPerHectare125;
	}

	public Double getCloseUtilizationVolumeLessDecayAndWastagePerHectare125() {
		return closeUtilizationVolumeLessDecayAndWastagePerHectare125;
	}

	public HcsvLayerRecordBean doPostBuildAdjustments() {
		return this;
	}
}
