package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

public class CSVYieldTableRecordBean {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CSVYieldTableRecordBean.class);

	public static StatefulBeanToCsv<CSVYieldTableRecordBean> createCsvOutputStream(FileWriter writer) {
		return new StatefulBeanToCsvBuilder<CSVYieldTableRecordBean>(writer) //
				.build();
	}

	public enum MultiFieldPrefixes {
		SPECIES_, PRJ_SP
	};

	public enum MultiFieldSuffixes {
		_CODE, _PCNT, _VOL_WS, _VOL_CU, _VOL_D, _VOL_DW, _VOL_DWB, //
		_MOF_BIO_WS, _MOF_BIO_CU, _MOF_BIO_D, _MOF_BIO_DW, _MOF_BIO_DWB
	}

	/**
	 * The valid (prefix, suffix) pairs are those where if the prefix ends with "_", the suffix is either _CODE or _PCNT
	 * OR both statements are false.
	 *
	 * @param p prefix
	 * @param s suffix
	 * @return as described
	 */
	public static boolean isValidPrefixSuffixPair(MultiFieldPrefixes p, MultiFieldSuffixes s) {
		return p.name().endsWith("_") && (s == MultiFieldSuffixes._CODE || s == MultiFieldSuffixes._PCNT)
				|| !p.name().endsWith("_") && (s != MultiFieldSuffixes._CODE && s != MultiFieldSuffixes._PCNT);
	}

//  { "(TABLE_NUM)"(,                      csvFldType_LONG,   10, 0, "", TRUE },  /* csvYldTbl_TblNum                       */)
	@CsvBindByName(column = "TABLE_NUM")
	@CsvBindByPosition(position = 0)
	private String tableNumber;

//  { "(FEATURE_ID)"(,                     csvFldType_CHAR,   38, 0, "", TRUE },  /* csvYldTbl_FeatureID                    */)
	@CsvBindByName(column = "FEATURE_ID")
	@CsvBindByPosition(position = 1)
	private String featureId;

//  { "(DISTRICT)"(,                       csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_District                     */)
	@CsvBindByName(column = "DISTRICT")
	@CsvBindByPosition(position = 2)
	private String district;

//  { "(MAP_ID)"(,                         csvFldType_CHAR,    9, 0, "", TRUE },  /* csvYldTbl_MapID                        */)
	@CsvBindByName(column = "MAP_ID")
	@CsvBindByPosition(position = 3)
	private String mapId;

//  { "(POLYGON_ID)"(,                     csvFldType_LONG,   10, 0, "", TRUE },  /* csvYldTbl_PolygonID                    */)
	@CsvBindByName(column = "POLYGON_ID")
	@CsvBindByPosition(position = 4)
	private String polygonId;

//  { "(LAYER_ID)"(,                       csvFldType_CHAR,    1, 0, "", TRUE },  /* csvYldTbl_LayerID                      */)
	@CsvBindByName(column = "LAYER_ID")
	@CsvBindByPosition(position = 5)
	private String layerId;

//  { "(PROJECTION_YEAR)"(,                csvFldType_SHORT,   4, 0, "", TRUE },  /* csvYldTbl_ProjectionYear               */)
	@CsvBindByName(column = "PROJECTION_YEAR")
	@CsvBindByPosition(position = 6)
	private String projectionYear;

//  { "(PRJ_TOTAL_AGE)"(,                  csvFldType_SHORT,   4, 0, "", TRUE },  /* csvYldTbl_ProjectionTotalAge           */)
	@CsvBindByName(column = "PRJ_TOTAL_AGE")
	@CsvBindByPosition(position = 7)
	private String totalAge;

//  { "(SPECIES_1_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species1Code                 */)
	@CsvBindByName(column = "SPECIES_1_CODE")
	@CsvBindByPosition(position = 8)
	private String species1Code;

//  { "(SPECIES_1_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species1Pcnt                 */)
	@CsvBindByName(column = "SPECIES_1_PCNT")
	@CsvBindByPosition(position = 9)
	private String species1Percent;

//  { "(SPECIES_2_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species2Code                 */)
	@CsvBindByName(column = "SPECIES_2_CODE")
	@CsvBindByPosition(position = 10)
	private String species2Code;

//  { "(SPECIES_2_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species2Pcnt                 */)
	@CsvBindByName(column = "SPECIES_2_PCNT")
	@CsvBindByPosition(position = 11)
	private String species2Percent;

//  { "(SPECIES_3_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species3Code                 */)
	@CsvBindByName(column = "SPECIES_3_CODE")
	@CsvBindByPosition(position = 12)
	private String species3Code;

//  { "(SPECIES_3_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species3Pcnt                 */)
	@CsvBindByName(column = "SPECIES_3_PCNT")
	@CsvBindByPosition(position = 13)
	private String species3Percent;

//  { "(SPECIES_4_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species4Code                 */)
	@CsvBindByName(column = "SPECIES_4_CODE")
	@CsvBindByPosition(position = 14)
	private String species4Code;

//  { "(SPECIES_4_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species4Pcnt                 */)
	@CsvBindByName(column = "SPECIES_4_PCNT")
	@CsvBindByPosition(position = 15)
	private String species4Percent;

//  { "(SPECIES_5_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species5Code                 */)
	@CsvBindByName(column = "SPECIES_5_CODE")
	@CsvBindByPosition(position = 16)
	private String species5Code;

//  { "(SPECIES_5_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species5Pcnt                 */)
	@CsvBindByName(column = "SPECIES_5_PCNT")
	@CsvBindByPosition(position = 17)
	private String species5Percent;

//  { "(SPECIES_6_CODE)"(,                 csvFldType_CHAR,    3, 0, "", TRUE },  /* csvYldTbl_Species6Code                 */)
	@CsvBindByName(column = "SPECIES_6_CODE")
	@CsvBindByPosition(position = 18)
	private String species6Code;

//  { "(SPECIES_6_PCNT)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_Species6Pcnt                 */)
	@CsvBindByName(column = "SPECIES_6_PCNT")
	@CsvBindByPosition(position = 19)
	private String species6Percent;

//  { "(PRJ_PCNT_STOCK)"(,                 csvFldType_SINGLE,  5, 2, "", TRUE },  /* csvYldTbl_ProjectionPctStock           */)
	@CsvBindByName(column = "PRJ_PCNT_STOCK")
	@CsvBindByPosition(position = 20)
	private String percentStockable;

//  { "(PRJ_SITE_INDEX)"(,                 csvFldType_SINGLE,  9, 5, "", TRUE },  /* csvYldTbl_ProjectionSI                 */)
	@CsvBindByName(column = "PRJ_SITE_INDEX")
	@CsvBindByPosition(position = 21)
	private String siteIndex;

//  { "(PRJ_DOM_HT)"(,                     csvFldType_SINGLE,  9, 5, "", TRUE },  /* csvYldTbl_ProjectionDomHt              */)
	@CsvBindByName(column = "PRJ_DOM_HT")
	@CsvBindByPosition(position = 22)
	private String dominantHeight;

//  { "(PRJ_SCND_HT)"(,                    csvFldType_SINGLE,  9, 5, "", TRUE },  /* csvYldTbl_ProjectionScndHt             */)
	@CsvBindByName(column = "PRJ_SCND_HT")
	@CsvBindByPosition(position = 23)
	private String secondaryHeight;

//  { "(PRJ_LOREY_HT)"(,                   csvFldType_SINGLE,  9, 5, "", TRUE },  /* csvYldTbl_ProjectionLoreyHt            */)
	@CsvBindByName(column = "PRJ_LOREY_HT")
	@CsvBindByPosition(position = 24)
	private String loreyHeight;

//  { "(PRJ_DIAMETER)"(,                   csvFldType_SINGLE,  7, 2, "", TRUE },  /* csvYldTbl_ProjectionDiam               */)
	@CsvBindByName(column = "PRJ_DIAMETER")
	@CsvBindByPosition(position = 25)
	private String diameter;

//  { "(PRJ_TPH)"(,                        csvFldType_SINGLE,  8, 2, "", TRUE },  /* csvYldTbl_ProjectionTPH                */)
	@CsvBindByName(column = "PRJ_TPH")
	@CsvBindByPosition(position = 26)
	private String treesPerHectare;

//  { "(PRJ_BA)"(,                         csvFldType_SINGLE, 10, 6, "", TRUE },  /* csvYldTbl_ProjectionBA                 */)
	@CsvBindByName(column = "PRJ_BA")
	@CsvBindByPosition(position = 27)
	private String basalArea;

//  { "(PRJ_VOL_WS)"(,                     csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolWS              */)
	@CsvBindByName(column = "PRJ_VOL_WS")
	@CsvBindByPosition(position = 28)
	private String wholeStemVolume;

//  { "(PRJ_VOL_CU)"(,                     csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolCU              */)
	@CsvBindByName(column = "PRJ_VOL_CU")
	@CsvBindByPosition(position = 29)
	private String closeUtilizationVolume;

//  { "(PRJ_VOL_D)"(,                      csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolD               */)
	@CsvBindByName(column = "PRJ_VOL_D")
	@CsvBindByPosition(position = 30)
	private String cuVolumeLessDepth;

//  { "(PRJ_VOL_DW)"(,                     csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolDW              */)
	@CsvBindByName(column = "PRJ_VOL_DW")
	@CsvBindByPosition(position = 31)
	private String cuVolumeLessDepthWastage;

//  { "(PRJ_VOL_DWB)"(,                    csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolDWB             */)
	@CsvBindByName(column = "PRJ_VOL_DWB")
	@CsvBindByPosition(position = 32)
	private String cuVolumeLessDepthWastageBreakage;

//  { "(PRJ_MOF_BIO_WS)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioWS           */)
	@CsvBindByName(column = "PRJ_MOF_BIO_WS")
	@CsvBindByPosition(position = 33)
	private String mofBiomassWholeStemVolume;

//  { "(PRJ_MOF_BIO_CU)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioCU           */)
	@CsvBindByName(column = "PRJ_MOF_BIO_CU")
	@CsvBindByPosition(position = 34)
	private String mofBiomassCloseUtilizationVolume;

//  { "(PRJ_MOF_BIO_D)"(,                  csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioD            */)
	@CsvBindByName(column = "PRJ_MOF_BIO_D")
	@CsvBindByPosition(position = 35)
	private String mofBiomassCuVolumeLessDepth;

//  { "(PRJ_MOF_BIO_DW)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioDW           */)
	@CsvBindByName(column = "PRJ_MOF_BIO_DW")
	@CsvBindByPosition(position = 36)
	private String mofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_MOF_BIO_DWB)"(,                csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioDWB          */)
	@CsvBindByName(column = "PRJ_MOF_BIO_DWB")
	@CsvBindByPosition(position = 37)
	private String mofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP1_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolWS         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_WS")
	@CsvBindByPosition(position = 38)
	private String species1WholeStemVolume;

//  { "(PRJ_SP1_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolCU         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_CU")
	@CsvBindByPosition(position = 39)
	private String species1CloseUtilizationVolume;

//  { "(PRJ_SP1_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolD          */)
	@CsvBindByName(column = "PRJ_SP1_VOL_D")
	@CsvBindByPosition(position = 40)
	private String species1CuVolumeLessDepth;

//  { "(PRJ_SP1_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolDW         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_DW")
	@CsvBindByPosition(position = 41)
	private String species1CuVolumeLessDepthWastage;

//  { "(PRJ_SP1_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolDWB        */)
	@CsvBindByName(column = "PRJ_SP1_VOL_DWB")
	@CsvBindByPosition(position = 42)
	private String species1CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP2_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolWS         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_WS")
	@CsvBindByPosition(position = 43)
	private String species2WholeStemVolume;

//  { "(PRJ_SP2_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolCU         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_CU")
	@CsvBindByPosition(position = 44)
	private String species2CloseUtilizationVolume;

//  { "(PRJ_SP2_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolD          */)
	@CsvBindByName(column = "PRJ_SP2_VOL_D")
	@CsvBindByPosition(position = 45)
	private String species2CuVolumeLessDepth;

//  { "(PRJ_SP2_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolDW         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_DW")
	@CsvBindByPosition(position = 46)
	private String species2CuVolumeLessDepthWastage;

//  { "(PRJ_SP2_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolDWB        */)
	@CsvBindByName(column = "PRJ_SP2_VOL_DWB")
	@CsvBindByPosition(position = 47)
	private String species2CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP3_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolWS         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_WS")
	@CsvBindByPosition(position = 48)
	private String species3WholeStemVolume;

//  { "(PRJ_SP3_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolCU         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_CU")
	@CsvBindByPosition(position = 49)
	private String species3CloseUtilizationVolume;

//  { "(PRJ_SP3_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolD          */)
	@CsvBindByName(column = "PRJ_SP3_VOL_D")
	@CsvBindByPosition(position = 50)
	private String species3CuVolumeLessDepth;

//  { "(PRJ_SP3_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolDW         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_DW")
	@CsvBindByPosition(position = 51)
	private String species3CuVolumeLessDepthWastage;

//  { "(PRJ_SP3_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolDWB        */)
	@CsvBindByName(column = "PRJ_SP3_VOL_DWB")
	@CsvBindByPosition(position = 52)
	private String species3CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP4_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolWS         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_WS")
	@CsvBindByPosition(position = 53)
	private String species4WholeStemVolume;

//  { "(PRJ_SP4_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolCU         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_CU")
	@CsvBindByPosition(position = 54)
	private String species4CloseUtilizationVolume;

//  { "(PRJ_SP4_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolD          */)
	@CsvBindByName(column = "PRJ_SP4_VOL_D")
	@CsvBindByPosition(position = 55)
	private String species4CuVolumeLessDepth;

//  { "(PRJ_SP4_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolDW         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_DW")
	@CsvBindByPosition(position = 56)
	private String species4CuVolumeLessDepthWastage;

//  { "(PRJ_SP4_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolDWB        */)
	@CsvBindByName(column = "PRJ_SP4_VOL_DWB")
	@CsvBindByPosition(position = 57)
	private String species4CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP5_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolWS         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_WS")
	@CsvBindByPosition(position = 58)
	private String species5WholeStemVolume;

//  { "(PRJ_SP5_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolCU         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_CU")
	@CsvBindByPosition(position = 59)
	private String species5CloseUtilizationVolume;

//  { "(PRJ_SP5_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolD          */)
	@CsvBindByName(column = "PRJ_SP5_VOL_D")
	@CsvBindByPosition(position = 60)
	private String species5CuVolumeLessDepth;

//  { "(PRJ_SP5_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolDW         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_DW")
	@CsvBindByPosition(position = 61)
	private String species5CuVolumeLessDepthWastage;

//  { "(PRJ_SP5_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolDWB        */)
	@CsvBindByName(column = "PRJ_SP5_VOL_DWB")
	@CsvBindByPosition(position = 62)
	private String species5CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP6_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolWS         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_WS")
	@CsvBindByPosition(position = 63)
	private String species6WholeStemVolume;

//  { "(PRJ_SP6_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolCU         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_CU")
	@CsvBindByPosition(position = 64)
	private String species6CloseUtilizationVolume;

//  { "(PRJ_SP6_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolD          */)
	@CsvBindByName(column = "PRJ_SP6_VOL_D")
	@CsvBindByPosition(position = 65)
	private String species6CuVolumeLessDepth;

//  { "(PRJ_SP6_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolDW         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_DW")
	@CsvBindByPosition(position = 66)
	private String species6CuVolumeLessDepthWastage;

//  { "(PRJ_SP6_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolDWB        */)
	@CsvBindByName(column = "PRJ_SP6_VOL_DWB")
	@CsvBindByPosition(position = 67)
	private String species6CuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP1_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP1_MOF_BIO_WS")
	@CsvBindByPosition(position = 68)
	private String species1MofBiomassWholeStemVolume;

//  { "(PRJ_SP1_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP1_MOF_BIO_CU")
	@CsvBindByPosition(position = 69)
	private String species1MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP1_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP1_MOF_BIO_D")
	@CsvBindByPosition(position = 70)
	private String species1MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP1_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP1_MOF_BIO_DW")
	@CsvBindByPosition(position = 71)
	private String species1MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP1_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP1_MOF_BIO_DWB")
	@CsvBindByPosition(position = 72)
	private String species1MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP2_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP2_MOF_BIO_WS")
	@CsvBindByPosition(position = 73)
	private String species2MofBiomassWholeStemVolume;

//  { "(PRJ_SP2_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP2_MOF_BIO_CU")
	@CsvBindByPosition(position = 74)
	private String species2MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP2_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP2_MOF_BIO_D")
	@CsvBindByPosition(position = 75)
	private String species2MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP2_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP2_MOF_BIO_DW")
	@CsvBindByPosition(position = 76)
	private String species2MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP2_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP2_MOF_BIO_DWB")
	@CsvBindByPosition(position = 77)
	private String species2MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP3_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP3_MOF_BIO_WS")
	@CsvBindByPosition(position = 78)
	private String species3MofBiomassWholeStemVolume;

//  { "(PRJ_SP3_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP3_MOF_BIO_CU")
	@CsvBindByPosition(position = 79)
	private String species3MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP3_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP3_MOF_BIO_D")
	@CsvBindByPosition(position = 80)
	private String species3MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP3_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP3_MOF_BIO_DW")
	@CsvBindByPosition(position = 81)
	private String species3MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP3_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP3_MOF_BIO_DWB")
	@CsvBindByPosition(position = 82)
	private String species3MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP4_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP4_MOF_BIO_WS")
	@CsvBindByPosition(position = 83)
	private String species4MofBiomassWholeStemVolume;

//  { "(PRJ_SP4_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP4_MOF_BIO_CU")
	@CsvBindByPosition(position = 84)
	private String species4MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP4_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP4_MOF_BIO_D")
	@CsvBindByPosition(position = 85)
	private String species4MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP4_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP4_MOF_BIO_DW")
	@CsvBindByPosition(position = 86)
	private String species4MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP4_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP4_MOF_BIO_DWB")
	@CsvBindByPosition(position = 87)
	private String species4MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP5_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP5_MOF_BIO_WS")
	@CsvBindByPosition(position = 88)
	private String species5MofBiomassWholeStemVolume;

//  { "(PRJ_SP5_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP5_MOF_BIO_CU")
	@CsvBindByPosition(position = 89)
	private String species5MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP5_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP5_MOF_BIO_D")
	@CsvBindByPosition(position = 90)
	private String species5MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP5_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP5_MOF_BIO_DW")
	@CsvBindByPosition(position = 91)
	private String species5MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP5_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP5_MOF_BIO_DWB")
	@CsvBindByPosition(position = 92)
	private String species5MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_SP6_MOF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP6_MOF_BIO_WS")
	@CsvBindByPosition(position = 93)
	private String species6MofBiomassWholeStemVolume;

//  { "(PRJ_SP6_MOF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP6_MOF_BIO_CU")
	@CsvBindByPosition(position = 94)
	private String species6MofBiomassCloseUtilizationVolume;

//  { "(PRJ_SP6_MOF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP6_MOF_BIO_D")
	@CsvBindByPosition(position = 95)
	private String species6MofBiomassCuVolumeLessDepth;

//  { "(PRJ_SP6_MOF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP6_MOF_BIO_DW")
	@CsvBindByPosition(position = 96)
	private String species6MofBiomassCuVolumeLessDepthWastage;

//  { "(PRJ_SP6_MOF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP6_MOF_BIO_DWB")
	@CsvBindByPosition(position = 97)
	private String species6MofBiomassCuVolumeLessDepthWastageBreakage;

//  { "(PRJ_CFS_BIO_STEM)"(,               csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassStem     */)
	@CsvBindByName(column = "PRJ_CFS_BIO_STEM")
	@CsvBindByPosition(position = 98)
	private String cfsBiomassStem;

//  { "(PRJ_CFS_BIO_BARK)"(,               csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassBark     */)
	@CsvBindByName(column = "PRJ_CFS_BIO_BARK")
	@CsvBindByPosition(position = 99)
	private String cfsBiomassBark;

//  { "(PRJ_CFS_BIO_BRANCH)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassBranch   */)
	@CsvBindByName(column = "PRJ_CFS_BIO_BRANCH")
	@CsvBindByPosition(position = 100)
	private String cfsBiomassBranch;

//  { "(PRJ_CFS_BIO_FOLIAGE)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassFoliage  */)
	@CsvBindByName(column = "PRJ_CFS_BIO_FOLIAGE")
	@CsvBindByPosition(position = 101)
	private String cfsBiomassFoliage;

//  { "(PRJ_MODE)"(,                       csvFldType_CHAR,    4, 0, "", TRUE }   /* csvYldTbl_ProjectionMode               */)
	@CsvBindByName(column = "PRJ_MODE")
	@CsvBindByPosition(position = 102)
	private String mode;

	private static Map<String, Field> csvFields = new HashMap<>();

	static {
		for (Field f : CSVYieldTableRecordBean.class.getDeclaredFields()) {
			for (var a : f.getAnnotationsByType(CsvBindByName.class)) {
				assert !csvFields.containsKey(a.column());
				csvFields.put(a.column(), f);
			}
		}
	}

	public Object getSpeciesField(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix) {

		if (speciesNumber < 1 || speciesNumber > 6) {
			throw new IllegalArgumentException("speciesNumber");
		}

		String column = prefix.name() + speciesNumber + suffix.name();
		assert csvFields.containsKey(column);
		try {
			return (String) csvFields.get(column).get(this);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	String getTableNumber() {
		return tableNumber;
	}

	String getFeatureId() {
		return featureId;
	}

	String getDistrict() {
		return district;
	}

	String getMapId() {
		return mapId;
	}

	String getPolygonId() {
		return polygonId;
	}

	String getLayerId() {
		return layerId;
	}

	String getProjectionYear() {
		return projectionYear;
	}

	String getTotalAge() {
		return totalAge;
	}

	String getPercentStockable() {
		return percentStockable;
	}

	String getSiteIndex() {
		return siteIndex;
	}

	String getDominantHeight() {
		return dominantHeight;
	}

	String getSecondaryHeight() {
		return secondaryHeight;
	}

	String getLoreyHeight() {
		return loreyHeight;
	}

	String getDiameter() {
		return diameter;
	}

	String getTreesPerHectare() {
		return treesPerHectare;
	}

	String getBasalArea() {
		return basalArea;
	}

	String getWholeStemVolume() {
		return wholeStemVolume;
	}

	String getCloseUtilizationVolume() {
		return closeUtilizationVolume;
	}

	String getCuVolumeLessDepth() {
		return cuVolumeLessDepth;
	}

	String getCuVolumeLessDepthWastage() {
		return cuVolumeLessDepthWastage;
	}

	String getCuVolumeLessDepthWastageBreakage() {
		return cuVolumeLessDepthWastageBreakage;
	}

	String getMofBiomassWholeStemVolume() {
		return mofBiomassWholeStemVolume;
	}

	String getMofBiomassCloseUtilizationVolume() {
		return mofBiomassCloseUtilizationVolume;
	}

	String getMofBiomassCuVolumeLessDepth() {
		return mofBiomassCuVolumeLessDepth;
	}

	String getMofBiomassCuVolumeLessDepthWastage() {
		return mofBiomassCuVolumeLessDepthWastage;
	}

	String getMofBiomassCuVolumeLessDepthWastageBreakage() {
		return mofBiomassCuVolumeLessDepthWastageBreakage;
	}

	String getCfsBiomassStem() {
		return cfsBiomassStem;
	}

	String getCfsBiomassBark() {
		return cfsBiomassBark;
	}

	String getCfsBiomassBranch() {
		return cfsBiomassBranch;
	}

	String getCfsBiomassFoliage() {
		return cfsBiomassFoliage;
	}

	String getMode() {
		return mode;
	}

	public void setSpeciesField(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, Object value) {

		if (speciesNumber < 1 || speciesNumber > 6) {
			throw new IllegalArgumentException("speciesNumber");
		}

		String column = prefix.name() + speciesNumber + suffix.name();
		assert csvFields.containsKey(column);
		try {
			csvFields.get(column).set(this, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTableNumber(String tableNumber) {
		this.tableNumber = tableNumber;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public void setPolygonId(String polygonId) {
		this.polygonId = polygonId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public void setProjectionYear(String projectionYear) {
		this.projectionYear = projectionYear;
	}

	public void setTotalAge(String totalAge) {
		this.totalAge = totalAge;
	}

	public void setPercentStockable(String percentStockable) {
		this.percentStockable = percentStockable;
	}

	public void setSiteIndex(String siteIndex) {
		this.siteIndex = siteIndex;
	}

	public void setDominantHeight(String dominantHeight) {
		this.dominantHeight = dominantHeight;
	}

	public void setSecondaryHeight(String secondaryHeight) {
		this.secondaryHeight = secondaryHeight;
	}

	public void setLoreyHeight(String loreyHeight) {
		this.loreyHeight = loreyHeight;
	}

	public void setDiameter(String diameter) {
		this.diameter = diameter;
	}

	public void setTreesPerHectare(String treesPerHectare) {
		this.treesPerHectare = treesPerHectare;
	}

	public void setBasalArea(String basalArea) {
		this.basalArea = basalArea;
	}

	public void setWholeStemVolume(String wholeStemVolume) {
		this.wholeStemVolume = wholeStemVolume;
	}

	public void setCloseUtilizationVolume(String closeUtilizationVolume) {
		this.closeUtilizationVolume = closeUtilizationVolume;
	}

	public void setCuVolumeLessDepth(String cuVolumeLessDepth) {
		this.cuVolumeLessDepth = cuVolumeLessDepth;
	}

	public void setCuVolumeLessDepthWastage(String cuVolumeLessDepthWastage) {
		this.cuVolumeLessDepthWastage = cuVolumeLessDepthWastage;
	}

	public void setCuVolumeLessDepthWastageBreakage(String cuVolumeLessDepthWastageBreakage) {
		this.cuVolumeLessDepthWastageBreakage = cuVolumeLessDepthWastageBreakage;
	}

	public void setMofBiomassWholeStemVolume(String mofBiomassWholeStemVolume) {
		this.mofBiomassWholeStemVolume = mofBiomassWholeStemVolume;
	}

	public void setMofBiomassCloseUtilizationVolume(String mofBiomassCloseUtilizationVolume) {
		this.mofBiomassCloseUtilizationVolume = mofBiomassCloseUtilizationVolume;
	}

	public void setMofBiomassCuVolumeLessDepth(String mofBiomassCuVolumeLessDepth) {
		this.mofBiomassCuVolumeLessDepth = mofBiomassCuVolumeLessDepth;
	}

	public void setMofBiomassCuVolumeLessDepthWastage(String mofBiomassCuVolumeLessDepthWastage) {
		this.mofBiomassCuVolumeLessDepthWastage = mofBiomassCuVolumeLessDepthWastage;
	}

	public void setMofBiomassCuVolumeLessDepthWastageBreakage(String mofBiomassCuVolumeLessDepthWastageBreakage) {
		this.mofBiomassCuVolumeLessDepthWastageBreakage = mofBiomassCuVolumeLessDepthWastageBreakage;
	}

	public void setSpeciesWholeStemVolume(int speciesNumber, String species1WholeStemVolume) {
		this.species1WholeStemVolume = species1WholeStemVolume;
	}

	public void setCfsBiomassStem(String cfsBiomassStem) {
		this.cfsBiomassStem = cfsBiomassStem;
	}

	public void setCfsBiomassBark(String cfsBiomassBark) {
		this.cfsBiomassBark = cfsBiomassBark;
	}

	public void setCfsBiomassBranch(String cfsBiomassBranch) {
		this.cfsBiomassBranch = cfsBiomassBranch;
	}

	public void setCfsBiomassFoliage(String cfsBiomassFoliage) {
		this.cfsBiomassFoliage = cfsBiomassFoliage;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
}
