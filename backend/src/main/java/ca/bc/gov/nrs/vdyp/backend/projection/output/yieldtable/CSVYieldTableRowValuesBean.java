package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CSVYieldTableRowValuesBean implements YieldTableRowBean {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CSVYieldTableRowValuesBean.class);

	public static StatefulBeanToCsv<CSVYieldTableRowValuesBean>
			createCsvOutputStream(FileWriter writer, ProjectionContext context) {

		var doGenerateHeader = context.getParams().containsOption(ExecutionOption.DO_INCLUDE_FILE_HEADER);

		if (doGenerateHeader) {

			var unfilteredFields = CSVYieldTableRowValuesBean.class.getDeclaredFields();
			var filteredFields = List.of(unfilteredFields).stream() //
					.filter(m -> m.isAnnotationPresent(CsvBindByPosition.class)) //
					.filter(
							m -> !m.isAnnotationPresent(OptionalField.class)
									|| isActiveCategory(context, m.getAnnotation(OptionalField.class).category())
					) //
					.collect(Collectors.toList());

			var strategy = new CustomMappingStrategy<CSVYieldTableRowValuesBean>(filteredFields);
			strategy.setType(CSVYieldTableRowValuesBean.class);
			return new StatefulBeanToCsvBuilder<CSVYieldTableRowValuesBean>(writer).withMappingStrategy(strategy)
					.build();
		} else {
			return new StatefulBeanToCsvBuilder<CSVYieldTableRowValuesBean>(writer).build();
		}
	}

	private static boolean isActiveCategory(ProjectionContext context, YieldTable.Category category) {

		return context.getYieldTableCategories().contains(category);
	}

	public CSVYieldTableRowValuesBean() {
		// default constructor necessary for reflection
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
	@OptionalField(category = YieldTable.Category.POLYGON_ID)
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
	@OptionalField(category = YieldTable.Category.LAYER_MOFVOLUMES)
	private String wholeStemVolume;

//  { "(PRJ_VOL_CU)"(,                     csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolCU              */)
	@CsvBindByName(column = "PRJ_VOL_CU")
	@CsvBindByPosition(position = 29)
	@OptionalField(category = YieldTable.Category.LAYER_MOFVOLUMES)
	private String closeUtilizationVolume;

//  { "(PRJ_VOL_D)"(,                      csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolD               */)
	@CsvBindByName(column = "PRJ_VOL_D")
	@CsvBindByPosition(position = 30)
	@OptionalField(category = YieldTable.Category.LAYER_MOFVOLUMES)
	private String cuVolumeLessDecay;

//  { "(PRJ_VOL_DW)"(,                     csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolDW              */)
	@CsvBindByName(column = "PRJ_VOL_DW")
	@CsvBindByPosition(position = 31)
	@OptionalField(category = YieldTable.Category.LAYER_MOFVOLUMES)
	private String cuVolumeLessDecayWastage;

//  { "(PRJ_VOL_DWB)"(,                    csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionVolDWB             */)
	@CsvBindByName(column = "PRJ_VOL_DWB")
	@CsvBindByPosition(position = 32)
	@OptionalField(category = YieldTable.Category.LAYER_MOFVOLUMES)
	private String cuVolumeLessDecayWastageBreakage;

//  { "(PRJ_MoF_BIO_WS)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioWS           */)
	@CsvBindByName(column = "PRJ_MoF_BIO_WS")
	@CsvBindByPosition(position = 33)
	@OptionalField(category = YieldTable.Category.LAYER_MOFBIOMASS)
	private String MoFBiomassWholeStemVolume;

//  { "(PRJ_MoF_BIO_CU)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioCU           */)
	@CsvBindByName(column = "PRJ_MoF_BIO_CU")
	@CsvBindByPosition(position = 34)
	@OptionalField(category = YieldTable.Category.LAYER_MOFBIOMASS)
	private String MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_MoF_BIO_D)"(,                  csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioD            */)
	@CsvBindByName(column = "PRJ_MoF_BIO_D")
	@CsvBindByPosition(position = 35)
	@OptionalField(category = YieldTable.Category.LAYER_MOFBIOMASS)
	private String MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_MoF_BIO_DW)"(,                 csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioDW           */)
	@CsvBindByName(column = "PRJ_MoF_BIO_DW")
	@CsvBindByPosition(position = 36)
	@OptionalField(category = YieldTable.Category.LAYER_MOFBIOMASS)
	private String MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_MoF_BIO_DWB)"(,                csvFldType_SINGLE, 10, 5, "", FALSE }, /* csvYldTbl_ProjectionMoFBioDWB          */)
	@CsvBindByName(column = "PRJ_MoF_BIO_DWB")
	@CsvBindByPosition(position = 37)
	@OptionalField(category = YieldTable.Category.LAYER_MOFBIOMASS)
	private String MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP1_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolWS         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_WS")
	@CsvBindByPosition(position = 38)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species1WholeStemVolume;

//  { "(PRJ_SP1_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolCU         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_CU")
	@CsvBindByPosition(position = 39)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species1CloseUtilizationVolume;

//  { "(PRJ_SP1_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolD          */)
	@CsvBindByName(column = "PRJ_SP1_VOL_D")
	@CsvBindByPosition(position = 40)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species1CuVolumeLessDecay;

//  { "(PRJ_SP1_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolDW         */)
	@CsvBindByName(column = "PRJ_SP1_VOL_DW")
	@CsvBindByPosition(position = 41)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species1CuVolumeLessDecayWastage;

//  { "(PRJ_SP1_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1VolDWB        */)
	@CsvBindByName(column = "PRJ_SP1_VOL_DWB")
	@CsvBindByPosition(position = 42)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species1CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP2_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolWS         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_WS")
	@CsvBindByPosition(position = 43)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species2WholeStemVolume;

//  { "(PRJ_SP2_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolCU         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_CU")
	@CsvBindByPosition(position = 44)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species2CloseUtilizationVolume;

//  { "(PRJ_SP2_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolD          */)
	@CsvBindByName(column = "PRJ_SP2_VOL_D")
	@CsvBindByPosition(position = 45)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species2CuVolumeLessDecay;

//  { "(PRJ_SP2_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolDW         */)
	@CsvBindByName(column = "PRJ_SP2_VOL_DW")
	@CsvBindByPosition(position = 46)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species2CuVolumeLessDecayWastage;

//  { "(PRJ_SP2_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2VolDWB        */)
	@CsvBindByName(column = "PRJ_SP2_VOL_DWB")
	@CsvBindByPosition(position = 47)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species2CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP3_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolWS         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_WS")
	@CsvBindByPosition(position = 48)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species3WholeStemVolume;

//  { "(PRJ_SP3_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolCU         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_CU")
	@CsvBindByPosition(position = 49)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species3CloseUtilizationVolume;

//  { "(PRJ_SP3_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolD          */)
	@CsvBindByName(column = "PRJ_SP3_VOL_D")
	@CsvBindByPosition(position = 50)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species3CuVolumeLessDecay;

//  { "(PRJ_SP3_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolDW         */)
	@CsvBindByName(column = "PRJ_SP3_VOL_DW")
	@CsvBindByPosition(position = 51)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species3CuVolumeLessDecayWastage;

//  { "(PRJ_SP3_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3VolDWB        */)
	@CsvBindByName(column = "PRJ_SP3_VOL_DWB")
	@CsvBindByPosition(position = 52)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species3CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP4_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolWS         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_WS")
	@CsvBindByPosition(position = 53)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species4WholeStemVolume;

//  { "(PRJ_SP4_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolCU         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_CU")
	@CsvBindByPosition(position = 54)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species4CloseUtilizationVolume;

//  { "(PRJ_SP4_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolD          */)
	@CsvBindByName(column = "PRJ_SP4_VOL_D")
	@CsvBindByPosition(position = 55)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species4CuVolumeLessDecay;

//  { "(PRJ_SP4_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolDW         */)
	@CsvBindByName(column = "PRJ_SP4_VOL_DW")
	@CsvBindByPosition(position = 56)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species4CuVolumeLessDecayWastage;

//  { "(PRJ_SP4_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4VolDWB        */)
	@CsvBindByName(column = "PRJ_SP4_VOL_DWB")
	@CsvBindByPosition(position = 57)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species4CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP5_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolWS         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_WS")
	@CsvBindByPosition(position = 58)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species5WholeStemVolume;

//  { "(PRJ_SP5_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolCU         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_CU")
	@CsvBindByPosition(position = 59)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species5CloseUtilizationVolume;

//  { "(PRJ_SP5_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolD          */)
	@CsvBindByName(column = "PRJ_SP5_VOL_D")
	@CsvBindByPosition(position = 60)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species5CuVolumeLessDecay;

//  { "(PRJ_SP5_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolDW         */)
	@CsvBindByName(column = "PRJ_SP5_VOL_DW")
	@CsvBindByPosition(position = 61)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species5CuVolumeLessDecayWastage;

//  { "(PRJ_SP5_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5VolDWB        */)
	@CsvBindByName(column = "PRJ_SP5_VOL_DWB")
	@CsvBindByPosition(position = 62)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species5CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP6_VOL_WS)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolWS         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_WS")
	@CsvBindByPosition(position = 63)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species6WholeStemVolume;

//  { "(PRJ_SP6_VOL_CU)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolCU         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_CU")
	@CsvBindByPosition(position = 64)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species6CloseUtilizationVolume;

//  { "(PRJ_SP6_VOL_D)"(,                  csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolD          */)
	@CsvBindByName(column = "PRJ_SP6_VOL_D")
	@CsvBindByPosition(position = 65)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species6CuVolumeLessDecay;

//  { "(PRJ_SP6_VOL_DW)"(,                 csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolDW         */)
	@CsvBindByName(column = "PRJ_SP6_VOL_DW")
	@CsvBindByPosition(position = 66)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species6CuVolumeLessDecayWastage;

//  { "(PRJ_SP6_VOL_DWB)"(,                csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6VolDWB        */)
	@CsvBindByName(column = "PRJ_SP6_VOL_DWB")
	@CsvBindByPosition(position = 67)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFVOLUME)
	private String species6CuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP1_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP1_MoF_BIO_WS")
	@CsvBindByPosition(position = 68)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species1MoFBiomassWholeStemVolume;

//  { "(PRJ_SP1_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP1_MoF_BIO_CU")
	@CsvBindByPosition(position = 69)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species1MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP1_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP1_MoF_BIO_D")
	@CsvBindByPosition(position = 70)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species1MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP1_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP1_MoF_BIO_DW")
	@CsvBindByPosition(position = 71)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species1MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP1_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs1MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP1_MoF_BIO_DWB")
	@CsvBindByPosition(position = 72)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species1MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP2_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP2_MoF_BIO_WS")
	@CsvBindByPosition(position = 73)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species2MoFBiomassWholeStemVolume;

//  { "(PRJ_SP2_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP2_MoF_BIO_CU")
	@CsvBindByPosition(position = 74)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species2MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP2_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP2_MoF_BIO_D")
	@CsvBindByPosition(position = 75)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species2MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP2_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP2_MoF_BIO_DW")
	@CsvBindByPosition(position = 76)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species2MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP2_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs2MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP2_MoF_BIO_DWB")
	@CsvBindByPosition(position = 77)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species2MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP3_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP3_MoF_BIO_WS")
	@CsvBindByPosition(position = 78)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species3MoFBiomassWholeStemVolume;

//  { "(PRJ_SP3_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP3_MoF_BIO_CU")
	@CsvBindByPosition(position = 79)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species3MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP3_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP3_MoF_BIO_D")
	@CsvBindByPosition(position = 80)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species3MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP3_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP3_MoF_BIO_DW")
	@CsvBindByPosition(position = 81)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species3MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP3_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs3MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP3_MoF_BIO_DWB")
	@CsvBindByPosition(position = 82)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species3MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP4_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP4_MoF_BIO_WS")
	@CsvBindByPosition(position = 83)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species4MoFBiomassWholeStemVolume;

//  { "(PRJ_SP4_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP4_MoF_BIO_CU")
	@CsvBindByPosition(position = 84)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species4MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP4_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP4_MoF_BIO_D")
	@CsvBindByPosition(position = 85)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species4MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP4_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP4_MoF_BIO_DW")
	@CsvBindByPosition(position = 86)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species4MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP4_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs4MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP4_MoF_BIO_DWB")
	@CsvBindByPosition(position = 87)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species4MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP5_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP5_MoF_BIO_WS")
	@CsvBindByPosition(position = 88)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species5MoFBiomassWholeStemVolume;

//  { "(PRJ_SP5_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP5_MoF_BIO_CU")
	@CsvBindByPosition(position = 89)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species5MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP5_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP5_MoF_BIO_D")
	@CsvBindByPosition(position = 90)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species5MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP5_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP5_MoF_BIO_DW")
	@CsvBindByPosition(position = 91)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species5MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP5_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs5MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP5_MoF_BIO_DWB")
	@CsvBindByPosition(position = 92)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species5MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_SP6_MoF_BIO_WS)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioWS      */)
	@CsvBindByName(column = "PRJ_SP6_MoF_BIO_WS")
	@CsvBindByPosition(position = 93)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species6MoFBiomassWholeStemVolume;

//  { "(PRJ_SP6_MoF_BIO_CU)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioCU      */)
	@CsvBindByName(column = "PRJ_SP6_MoF_BIO_CU")
	@CsvBindByPosition(position = 94)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species6MoFBiomassCloseUtilizationVolume;

//  { "(PRJ_SP6_MoF_BIO_D)"(,              csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioD       */)
	@CsvBindByName(column = "PRJ_SP6_MoF_BIO_D")
	@CsvBindByPosition(position = 95)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species6MoFBiomassCuVolumeLessDecay;

//  { "(PRJ_SP6_MoF_BIO_DW)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioDW      */)
	@CsvBindByName(column = "PRJ_SP6_MoF_BIO_DW")
	@CsvBindByPosition(position = 96)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species6MoFBiomassCuVolumeLessDecayWastage;

//  { "(PRJ_SP6_MoF_BIO_DWB)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionSpcs6MoFBioDWB     */)
	@CsvBindByName(column = "PRJ_SP6_MoF_BIO_DWB")
	@CsvBindByPosition(position = 97)
	@OptionalField(category = YieldTable.Category.SPECIES_MOFBIOMASS)
	private String species6MoFBiomassCuVolumeLessDecayWastageBreakage;

//  { "(PRJ_CFS_BIO_STEM)"(,               csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassStem     */)
	@CsvBindByName(column = "PRJ_CFS_BIO_STEM")
	@CsvBindByPosition(position = 98)
	@OptionalField(category = YieldTable.Category.LAYER_CFSBIOMASS)
	private String cfsBiomassStem;

//  { "(PRJ_CFS_BIO_BARK)"(,               csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassBark     */)
	@CsvBindByName(column = "PRJ_CFS_BIO_BARK")
	@CsvBindByPosition(position = 99)
	@OptionalField(category = YieldTable.Category.LAYER_CFSBIOMASS)
	private String cfsBiomassBark;

//  { "(PRJ_CFS_BIO_BRANCH)"(,             csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassBranch   */)
	@CsvBindByName(column = "PRJ_CFS_BIO_BRANCH")
	@CsvBindByPosition(position = 100)
	@OptionalField(category = YieldTable.Category.LAYER_CFSBIOMASS)
	private String cfsBiomassBranch;

//  { "(PRJ_CFS_BIO_FOLIAGE)"(,            csvFldType_SINGLE, 10, 5, "", TRUE },  /* csvYldTbl_ProjectionCFSBiomassFoliage  */)
	@CsvBindByName(column = "PRJ_CFS_BIO_FOLIAGE")
	@CsvBindByPosition(position = 101)
	@OptionalField(category = YieldTable.Category.LAYER_CFSBIOMASS)
	private String cfsBiomassFoliage;

//  { "(PRJ_MODE)"(,                       csvFldType_CHAR,    4, 0, "", TRUE }   /* csvYldTbl_ProjectionMode               */)
	@CsvBindByName(column = "PRJ_MODE")
	@CsvBindByPosition(position = 102)
	@OptionalField(category = YieldTable.Category.PROJECTION_MODE)
	private String mode;

	private static Map<String, Field> csvFields = new HashMap<>();

	static {
		for (Field f : CSVYieldTableRowValuesBean.class.getDeclaredFields()) {
			for (var a : f.getAnnotationsByType(CsvBindByName.class)) {
				Validate.isTrue(
						!csvFields.containsKey(a.column()),
						MessageFormat.format("csvFields must not already contain key {0}", a.column())
				);
				csvFields.put(a.column(), f);
			}
		}
	}

	@Override
	public String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix) {

		Validate.isTrue(
				isValidPrefixSuffixPair(prefix, suffix),
				MessageFormat.format(
						"CSVYieldTableRowValuesBean.getSpeciesFieldValue: {0} and {1} must be a valid prefix suffix pair",
						prefix, suffix
				)
		);

		if (speciesNumber < 1 || speciesNumber > 6) {
			throw new IllegalArgumentException("speciesNumber");
		}

		String column = prefix.fieldName + speciesNumber + suffix.fieldName;
		Validate.isTrue(
				csvFields.containsKey(column),
				MessageFormat
						.format("CSVYieldTableRowValuesBean.getSpeciesFieldValue: {0} must exist in csvFields", column)
		);

		try {
			return (String) csvFields.get(column).get(this);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getTableNumber() {
		return tableNumber;
	}

	@Override
	public String getFeatureId() {
		return featureId;
	}

	@Override
	public String getDistrict() {
		return district;
	}

	@Override
	public String getMapId() {
		return mapId;
	}

	@Override
	public String getPolygonId() {
		return polygonId;
	}

	@Override
	public String getLayerId() {
		return layerId;
	}

	@Override
	public String getProjectionYear() {
		return projectionYear;
	}

	@Override
	public String getTotalAge() {
		return totalAge;
	}

	@Override
	public String getPercentStockable() {
		return percentStockable;
	}

	@Override
	public String getSiteIndex() {
		return siteIndex;
	}

	@Override
	public String getDominantHeight() {
		return dominantHeight;
	}

	@Override
	public String getSecondaryHeight() {
		return secondaryHeight;
	}

	@Override
	public String getLoreyHeight() {
		return loreyHeight;
	}

	@Override
	public String getDiameter() {
		return diameter;
	}

	@Override
	public String getTreesPerHectare() {
		return treesPerHectare;
	}

	@Override
	public String getBasalArea() {
		return basalArea;
	}

	@Override
	public String getWholeStemVolume() {
		return wholeStemVolume;
	}

	@Override
	public String getCloseUtilizationVolume() {
		return closeUtilizationVolume;
	}

	@Override
	public String getMoFBiomassWholeStemVolume() {
		return MoFBiomassWholeStemVolume;
	}

	@Override
	public String getMoFBiomassCloseUtilizationVolume() {
		return MoFBiomassCloseUtilizationVolume;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecay() {
		return MoFBiomassCuVolumeLessDecay;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastage() {
		return MoFBiomassCuVolumeLessDecayWastage;
	}

	@Override
	public String getMoFBiomassCuVolumeLessDecayWastageBreakage() {
		return MoFBiomassCuVolumeLessDecayWastageBreakage;
	}

	@Override
	public String getCfsBiomassStem() {
		return cfsBiomassStem;
	}

	@Override
	public String getCfsBiomassBark() {
		return cfsBiomassBark;
	}

	@Override
	public String getCfsBiomassBranch() {
		return cfsBiomassBranch;
	}

	@Override
	public String getCfsBiomassFoliage() {
		return cfsBiomassFoliage;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, String value
	) {

		if (speciesNumber < 1 || speciesNumber > 6) {
			throw new IllegalArgumentException("speciesNumber");
		}

		String column = prefix.fieldName + speciesNumber + suffix.fieldName;

		Validate.isTrue(
				csvFields.containsKey(column),
				MessageFormat.format(
						"CSVYieldTableRowValuesBean.setSpeciesFieldValue: {0} must exist already exist in csvFields",
						column
				)
		);

		try {
			csvFields.get(column).set(this, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setTableNumber(int tableNumber) {
		this.tableNumber = FieldFormatter.format(tableNumber);
	}

	@Override
	public void setFeatureId(Long featureId) {
		if (featureId != null) {
			this.featureId = FieldFormatter.format(featureId);
		}
	}

	@Override
	public void setDistrict(String district) {
		this.district = district;
	}

	@Override
	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	@Override
	public void setPolygonId(Long polygonId) {
		if (polygonId != null) {
			this.polygonId = FieldFormatter.format(polygonId);
		}
	}

	@Override
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	@Override
	public void setProjectionYear(Integer projectionYear) {
		this.projectionYear = FieldFormatter.format(projectionYear);
	}

	@Override
	public void setTotalAge(Integer totalAge) {
		this.totalAge = FieldFormatter.format(totalAge);
	}

	@Override
	public void setPercentStockable(Double percentStockable) {
		this.percentStockable = FieldFormatter.format(percentStockable);
	}

	@Override
	public void setSiteIndex(Double siteIndex) {
		this.siteIndex = FieldFormatter.format(siteIndex);
	}

	@Override
	public void setDominantHeight(Double dominantHeight) {
		this.dominantHeight = FieldFormatter.format(dominantHeight);
	}

	@Override
	public void setSecondaryHeight(Double secondaryHeight) {
		this.secondaryHeight = FieldFormatter.format(secondaryHeight);
	}

	@Override
	public void setLoreyHeight(Double loreyHeight) {
		this.loreyHeight = FieldFormatter.format(loreyHeight);
	}

	@Override
	public void setDiameter(Double diameter) {
		this.diameter = FieldFormatter.format(diameter);
	}

	@Override
	public void setTreesPerHectare(Double treesPerHectare) {
		this.treesPerHectare = FieldFormatter.format(treesPerHectare);
	}

	@Override
	public void setBasalArea(Double basalArea) {
		this.basalArea = FieldFormatter.format(basalArea);
	}

	@Override
	public void setWholeStemVolume(Double wholeStemVolume) {
		this.wholeStemVolume = FieldFormatter.format(wholeStemVolume);
	}

	@Override
	public void setCloseUtilizationVolume(Double closeUtilizationVolume) {
		this.closeUtilizationVolume = FieldFormatter.format(closeUtilizationVolume);
	}

	@Override
	public void setMoFBiomassWholeStemVolume(Double MoFBiomassWholeStemVolume) {
		this.MoFBiomassWholeStemVolume = FieldFormatter.format(MoFBiomassWholeStemVolume);
	}

	@Override
	public void setMoFBiomassCloseUtilizationVolume(Double MoFBiomassCloseUtilizationVolume) {
		this.MoFBiomassCloseUtilizationVolume = FieldFormatter.format(MoFBiomassCloseUtilizationVolume);
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecay(Double MoFBiomassCuVolumeLessDecay) {
		this.MoFBiomassCuVolumeLessDecay = FieldFormatter.format(MoFBiomassCuVolumeLessDecay);
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastage(Double MoFBiomassCuVolumeLessDecayWastage) {
		this.MoFBiomassCuVolumeLessDecayWastage = FieldFormatter.format(MoFBiomassCuVolumeLessDecayWastage);
	}

	@Override
	public void setMoFBiomassCuVolumeLessDecayWastageBreakage(Double MoFBiomassCuVolumeLessDecayWastageBreakage) {
		this.MoFBiomassCuVolumeLessDecayWastageBreakage = FieldFormatter
				.format(MoFBiomassCuVolumeLessDecayWastageBreakage);
	}

	@Override
	public void setCfsBiomassStem(Double cfsBiomassStem) {
		this.cfsBiomassStem = FieldFormatter.format(cfsBiomassStem);
	}

	@Override
	public void setCfsBiomassBark(Double cfsBiomassBark) {
		this.cfsBiomassBark = FieldFormatter.format(cfsBiomassBark);
	}

	@Override
	public void setCfsBiomassBranch(Double cfsBiomassBranch) {
		this.cfsBiomassBranch = FieldFormatter.format(cfsBiomassBranch);
	}

	@Override
	public void setCfsBiomassFoliage(Double cfsBiomassFoliage) {
		this.cfsBiomassFoliage = FieldFormatter.format(cfsBiomassFoliage);
	}

	@Override
	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public void setCuVolumeLessDecayWastageBreakage(Double cuVolumeLessDecayWastageBreakage) {
		this.cuVolumeLessDecayWastageBreakage = FieldFormatter.format(cuVolumeLessDecayWastageBreakage);
	}

	@Override
	public String getCuVolumeLessDecayWastageBreakage() {
		return cuVolumeLessDecayWastageBreakage;
	}

	@Override
	public void setCuVolumeLessDecayWastage(Double cuVolumeLessDecayWastage) {
		this.cuVolumeLessDecayWastage = FieldFormatter.format(cuVolumeLessDecayWastage);
	}

	@Override
	public String getCuVolumeLessDecayWastage() {
		return cuVolumeLessDecayWastage;
	}

	@Override
	public void setCuVolumeLessDecay(Double cuVolumeLessDecay) {
		this.cuVolumeLessDecay = FieldFormatter.format(cuVolumeLessDecay);
	}

	@Override
	public String getCuVolumeLessDecay() {
		return cuVolumeLessDecay;
	}
}
