package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;
import com.opencsv.bean.processor.PreAssignmentProcessor;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZone;

public class HcsvPolygonRecordBean {

	private static final Logger logger = LoggerFactory.getLogger(HcsvPolygonRecordBean.class);

	public static CsvToBean<HcsvPolygonRecordBean> createHcsvPolygonStream(InputStream polygonStream) {
		return new CsvToBeanBuilder<HcsvPolygonRecordBean>(new BufferedReader(new InputStreamReader(polygonStream))) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.build();
	}

	// { "POLY_FEATURE_ID", csvFldType_CHAR, 38, 0, "", TRUE }, /* A. HCSV_IFld_POLYGON__FEATURE_ID */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "FEATURE_ID")
	@CsvBindByPosition(position = 0)
	private Long polyFeatureId;

	// { "MAP_ID", csvFldType_CHAR, 9, 0, "", TRUE }, /* B. HCSV_IFld_POLYGON__MAP_ID */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "MAP_ID")
	@CsvBindByPosition(position = 1)
	private String mapId;

	// { "POLYGON_NO", csvFldType_LONG, 10, 0, "", TRUE }, /* C. HCSV_IFld_POLYGON__POLYGON_NO */
	@CsvBindByName(column = "POLYGON_NUMBER")
	@CsvBindByPosition(position = 2)
	private Long polygonNumber;

	// { "ORG_UNIT", csvFldType_CHAR, 10, 0, "", TRUE }, /* D. HCVS_IFld_POLYGON__ORG_UNIT */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "ORG_UNIT")
	@CsvBindByPosition(position = 3)
	private String orgUnit;

	// { "TSA", csvFldType_CHAR, 5, 0, "", TRUE }, /* E. HCSV_IFld_POLYGON__TSA */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TSA_NAME")
	@CsvBindByPosition(position = 4)
	private String tsaName;

	// { "TFL", csvFldType_CHAR, 5, 0, "", TRUE }, /* F. HCSV_IFld_POLYGON__TFL */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TFL_NAME")
	@CsvBindByPosition(position = 5)
	private String tflName;

	// { "INVENTORY_STANDARD_CD", csvFldType_CHAR, 10, 0, "", TRUE }, /* G. HCSV_IFld_POLYGON__INVENTORY_STANDARD */
	// maps to InventoryStandard
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "INVENTORY_STANDARD_CODE")
	@CsvBindByPosition(position = 6)
	private InventoryStandardCode inventoryStandardCode;

	// { "TSA_NUM", csvFldType_CHAR, 5, 0, "", TRUE }, /* H. HCSV_IFld_POLYGON__TSA_NUM */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TSA_NUMBER")
	@CsvBindByPosition(position = 7)
	private String tsaNumber;

	// { "SHRUB_HEIGHT", csvFldType_SINGLE, 4, 1, "", TRUE }, /* I. HCSV_IFld_POLYGON__SHRUB_HEIGHT */
	@CsvBindByName(column = "SHRUB_HEIGHT")
	@CsvBindByPosition(position = 8)
	private Double shrubHeight;

	// { "SHRUB_CROWN_CLOSURE", csvFldType_SHORT, 3, 0, "", TRUE }, /* J. HCSV_IFld_POLYGON__SHRUB_CC */
	@CsvBindByName(column = "SHRUB_CROWN_CLOSURE")
	@CsvBindByPosition(position = 9)
	private Integer shrubCrownClosure;

	// { "SHRUB_COVER_PATTERN", csvFldType_CHAR, 10, 0, "", TRUE }, /* K. HCSV_IFld_POLYGON__SHRUB_PATTERN */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SHRUB_COVER_PATTERN")
	@CsvBindByPosition(position = 10)
	private String shrubCoverPattern;

	// { "HERB_COVER_TYPE", csvFldType_CHAR, 10, 0, "", TRUE }, /* L. HCSV_IFld_POLYGON__HERB_COVER_TYPE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "HERB_COVER_TYPE_CODE")
	@CsvBindByPosition(position = 11)
	private String herbCoverTypeCode;

	// { "HERB_COVER_PCT", csvFldType_SHORT, 3, 0, "", TRUE }, /* M. HCSV_IFld_POLYGON__HERB_CC */
	@CsvBindByName(column = "HERB_COVER_PCT")
	@CsvBindByPosition(position = 12)
	private Integer herbCoverPercent;

	// { "HERB_COVER_PATTERN", csvFldType_CHAR, 10, 0, "", TRUE }, /* N. HCSV_IFld_POLYGON__HERB_PATTERN */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "HERB_COVER_PATTERN_CODE")
	@CsvBindByPosition(position = 13)
	private String herbCoverPatternCode;

	// { "BRYOID_COVER_PCT", csvFldType_SHORT, 3, 0, "", TRUE }, /* O. HCSV_IFld_POLYGON__BRYOID_CC */
	@CsvBindByName(column = "BRYOID_COVER_PCT")
	@CsvBindByPosition(position = 14)
	private Integer bryoidCoverPercent;

	// { "BEC_ZONE_CD", csvFldType_CHAR, 10, 0, "", TRUE }, /* P. HCSV_IFld_POLYGON__BEC_ZONE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BEC_ZONE_CODE")
	@CsvBindByPosition(position = 15)
	private String becZoneCode;

	// { "CFS_ECO_ZONE", csvFldType_SHORT, 3, 0, "", TRUE }, /* Q. HCSV_IFld_POLYGON__CFS_ECO_ZONE */
	// Maps to CfsEcoZone
	@CsvBindByName(column = "CFS_ECOZONE")
	@CsvBindByPosition(position = 16)
	private Short cfsEcoZoneCode;

	// { "STOCKABILITY", csvFldType_SINGLE, 4, 1, "", TRUE }, /* R. HCSV_IFld_POLYGON__PCT_STOCKABLE */
	@CsvBindByName(column = "PRE_DISTURBANCE_STOCKABILITY")
	@CsvBindByPosition(position = 17)
	private Double percentStockable;

	// { "YIELD_FACTOR", csvFldType_SINGLE, 5, 3, "", TRUE }, /* S. HCSV_IFld_POLYGON__YIELD_FACTOR */
	@CsvBindByName(column = "YIELD_FACTOR")
	@CsvBindByPosition(position = 18)
	private Double yieldFactor;

	// { "NON_PRODUCTIVE_DESCRIPTOR_CD", csvFldType_CHAR, 5, 0, "", TRUE }, /* T. HCSV_IFld_POLYGON__NON_PROD_DESC */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_PRODUCTIVE_DESCRIPTOR_CD")
	@CsvBindByPosition(position = 19)
	private String nonProductiveDescriptorCode;

	// { "BCLCS_LEVEL_1", csvFldType_CHAR, 10, 0, "", TRUE }, /* U. HCSV_IFld_POLYGON__BCLCS_LVL_1_CODE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL1_CODE")
	@CsvBindByPosition(position = 20)
	private String bclcsLevel1Code;

	// { "BCLCS_LEVEL_2", csvFldType_CHAR, 10, 0, "", TRUE }, /* V. HCSV_IFld_POLYGON__BCLCS_LVL_2_CODE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL2_CODE")
	@CsvBindByPosition(position = 21)
	private String bclcsLevel2Code;

	// { "BCLCS_LEVEL_3", csvFldType_CHAR, 10, 0, "", TRUE }, /* W. HCSV_IFld_POLYGON__BCLCS_LVL_3_CODE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL3_CODE")
	@CsvBindByPosition(position = 22)
	private String bclcsLevel3Code;

	// { "BCLCS_LEVEL_4", csvFldType_CHAR, 10, 0, "", TRUE }, /* X. HCSV_IFld_POLYGON__BCLCS_LVL_4_CODE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL4_CODE")
	@CsvBindByPosition(position = 23)
	private String bclcsLevel4Code;

	// { "BCLCS_LEVEL_5", csvFldType_CHAR, 10, 0, "", TRUE }, /* Y. HCSV_IFld_POLYGON__BCLCS_LVL_5_CODE */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL5_CODE")
	@CsvBindByPosition(position = 24)
	private String bclcsLevel5Code;

	// { "REFERENCE_YEAR", csvFldType_SHORT, 4, 0, "", TRUE }, /* Z. HCSV_IFld_POLYGON__REFERENCE_YEAR */
	@CsvBindByName(column = "PHOTO_ESTIMATION_BASE_YEAR")
	@CsvBindByPosition(position = 25)
	private Integer referenceYear;

	// { "YEAR_OF_DEATH", csvFldType_SHORT, 4, 0, "", TRUE }, /* AA. HCSV_IFld_POLYGON__YEAR_OF_DEATH */
	@CsvBindByName(column = "REFERENCE_YEAR")
	@CsvBindByPosition(position = 26)
	private Integer yearOfDeath;

	// { "STOCKABILITY_DEAD", csvFldType_SINGLE, 4, 1, "", TRUE }, /* AB. HCSV_IFld_POLYGON__PCT_STOCKABLE_DEAD */
	@CsvBindByName(column = "PCT_DEAD")
	@CsvBindByPosition(position = 27)
	private Double percentDead;

	// { "NON_VEG_COVER_TYPE_1", csvFldType_CHAR, 10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_1 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_1")
	@CsvBindByPosition(position = 28)
	private String nonVegCoverType1;

	// { "NON_VEG_COVER_PCT_1", csvFldType_SHORT, 3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_1 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_1")
	@CsvBindByPosition(position = 29)
	private Integer nonVegCoverPercent1;

	// { "NON_VEG_COVER_PATTERN_1", csvFldType_CHAR, 10, 0, "", TRUE }, /* AE.
	// HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_1 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_1")
	@CsvBindByPosition(position = 30)
	private String nonVegCoverPattern1;

	// { "NON_VEG_COVER_TYPE_2", csvFldType_CHAR, 10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_2 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_2")
	@CsvBindByPosition(position = 31)
	private String nonVegCoverType2;

	// { "NON_VEG_COVER_PCT_2", csvFldType_SHORT, 3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_2 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_2")
	@CsvBindByPosition(position = 32)
	private Integer nonVegCoverPercent2;

	// { "NON_VEG_COVER_PATTERN_2", csvFldType_CHAR, 10, 0, "", TRUE }, /* AE.
	// HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_2 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_2")
	@CsvBindByPosition(position = 33)
	private String nonVegCoverPattern2;

	// { "NON_VEG_COVER_TYPE_3", csvFldType_CHAR, 10, 0, "", TRUE }, /* AC. HCSV_IFld_POLYGON__NON_VEG_COVER_TYPE_3 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_3")
	@CsvBindByPosition(position = 34)
	private String nonVegCoverType3;

	// { "NON_VEG_COVER_PCT_3", csvFldType_SHORT, 3, 0, "", TRUE }, /* AD. HCSV_IFld_POLYGON__NON_VEG_COVER_PCT_3 */
	@CsvBindByName(column = "NON_VEG_COVER_PCT_3")
	@CsvBindByPosition(position = 35)
	private Integer nonVegCoverPercent3;

	// { "NON_VEG_COVER_PATTERN_3", csvFldType_CHAR, 10, 0, "", TRUE }, /* AE.
	// HCSV_IFld_POLYGON__NON_VEG_COVER_PATTERN_3 */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_3")
	@CsvBindByPosition(position = 36)
	private String nonVegCoverPattern3;

	// { "LAND_COVER_CLASS_CD_1", csvFldType_CHAR, 10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_1
	// */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_1")
	@CsvBindByPosition(position = 37)
	private String landCoverClassCode1;

	// { "LAND_COVER_CLASS_PCT_1", csvFldType_SHORT, 3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_1
	// */
	@CsvBindByName(column = "LAND_COVER_PCT_1")
	@CsvBindByPosition(position = 38)
	private Integer landCoverPercent1;

	// { "LAND_COVER_CLASS_CD_2", csvFldType_CHAR, 10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_2
	// */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_2")
	@CsvBindByPosition(position = 39)
	private String landCoverClassCode2;

	// { "LAND_COVER_CLASS_PCT_2", csvFldType_SHORT, 3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_2
	// */
	@CsvBindByName(column = "LAND_COVER_PCT_2")
	@CsvBindByPosition(position = 40)
	private Integer landCoverPercent2;

	// { "LAND_COVER_CLASS_CD_3", csvFldType_CHAR, 10, 0, "", TRUE }, /* AL. HCSV_IFld_POLYGON__LAND_COVER_CLASS_CODE_3
	// */
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_3")
	@CsvBindByPosition(position = 41)
	private String landCoverClassCode3;

	// { "LAND_COVER_CLASS_PCT_3", csvFldType_SHORT, 3, 0, "", TRUE }, /* AM. HCSV_IFld_POLYGON__LAND_COVER_CLASS_PCT_3
	// */
	@CsvBindByName(column = "LAND_COVER_PCT_3")
	@CsvBindByPosition(position = 42)
	private Integer landCoverPercent3;

	public Long getPolyFeatureId() {
		return polyFeatureId;
	}

	public String getMapId() {
		return mapId;
	}

	public Long getPolygonNumber() {
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

	public InventoryStandardCode getInventoryStandardCode() {
		return inventoryStandardCode;
	}

	public String getTsaNumber() {
		return tsaNumber;
	}

	public Double getShrubHeight() {
		return shrubHeight;
	}

	public Integer getShrubCrownClosure() {
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

	public Short getCfsEcoZoneCode() {
		return cfsEcoZoneCode;
	}

	public Double getPercentStockable() {
		return percentStockable;
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

	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Integer getYearOfDeath() {
		return yearOfDeath;
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

	/**
	 * Perform adjustments and checks once the record is initialized from the data in the CSV. This method combines the
	 * logic of lcl_CopyPolygonDataIntoSnapshot and DefPoly_DefinePolygonToVDYP7.
	 * 
	 * @return completed record
	 * @throws PolygonValidationException
	 */
	HcsvPolygonRecordBean adjustAndValidate() throws PolygonValidationException {

		var validationMessages = new ArrayList<ValidationMessage>();

		// lcl_CopyPolygonDataIntoSnapshot: 3156
		if (mapId == null) {
			mapId = "UNKNOWN";
		} else if (mapId.length() > Vdyp7Constants.MAX_LEN_MAPSHEET) {
			mapId = mapId.substring(0, Vdyp7Constants.MAX_LEN_MAPSHEET);
		}

		if (referenceYear == null && yearOfDeath != null) {
			// lcl_CopyPolygonDataIntoSnapshot: 3177
			referenceYear = yearOfDeath;
		}

		if (nonProductiveDescriptorCode != null
				&& nonProductiveDescriptorCode.length() > Vdyp7Constants.MAX_LEN_NON_PROD_DESC) {
			// lcl_CopyPolygonDataIntoSnapshot: 3206
			nonProductiveDescriptorCode = nonProductiveDescriptorCode
					.substring(0, Vdyp7Constants.MAX_LEN_NON_PROD_DESC);
		}

		if (cfsEcoZoneCode == null) {
			// lcl_CopyPolygonDataIntoSnapshot: 3224 makes V7Ext_StartNewPolygon: 1409 redundant
			cfsEcoZoneCode = CfsEcoZone.Unknown.getCode();
		} else {
			try {
				CfsEcoZone.fromCode(cfsEcoZoneCode);
			} catch (IllegalArgumentException e) {
				validationMessages.add(
						new ValidationMessage(
								ValidationMessageKind.INVALID_CFS_ECO_ZONE, polyFeatureId.toString(), cfsEcoZoneCode
						)
				);
			}
		}

		if (inventoryStandardCode == InventoryStandardCode.I) {
			inventoryStandardCode = InventoryStandardCode.F;
		}

		if (percentStockable != null && (percentStockable < 0.0 || percentStockable > 100.0)) {
			// V7Ext_StartNewPolygon: 1422
			validationMessages.add(
					new ValidationMessage(
							ValidationMessageKind.INVALID_PERCENT_STOCKABLE_DEAD, polyFeatureId.toString(),
							percentStockable
					)
			);
		}

		if (percentDead != null && (percentDead < 0.0 || percentDead > 100.0)) {
			// V7Ext_StartNewPolygon: 1437
			validationMessages.add(
					new ValidationMessage(
							ValidationMessageKind.INVALID_PERCENT_STOCKABLE_DEAD, polyFeatureId.toString(),
							percentStockable
					)
			);
		}

		if (becZoneCode == null) {
			// V7Ext_StartNewPolygon: 1453
			validationMessages
					.add(new ValidationMessage(ValidationMessageKind.MISSING_BEC_ZONE, polyFeatureId.toString()));
		} else if (becZoneCode != null) {

			if (becZoneCode.length() > Vdyp7Constants.MAX_LEN_BEC_ZONE) {
				// lcl_CopyPolygonDataIntoSnapshot: 3200
				becZoneCode = becZoneCode.substring(0, Vdyp7Constants.MAX_LEN_BEC_ZONE);
			}

			// V7Ext_StartNewPolygon: 1479. Certain BEC zones are not supported; substitute
			// "AT" for each.
			if (becZoneCode.equals("BAFA") || becZoneCode.equals("CMA") || becZoneCode.equals("IMA")) {
				logger.info("Translating unsupported BEC zone {} to {}", becZoneCode, "AT");
				becZoneCode = "AT";
			}
		}

		if (yieldFactor != null && (yieldFactor < 0.0 || yieldFactor > 10.0)) {
			// V7Ext_StartNewPolygon: 1527
			validationMessages.add(new ValidationMessage(ValidationMessageKind.YIELD_FACTOR_OUT_OF_RANGE, yieldFactor));
		}

		if (referenceYear == null || (referenceYear < Vdyp7Constants.MIN_CALENDAR_YEAR
				|| referenceYear > Vdyp7Constants.MAX_CALENDAR_YEAR)) {
			// V7Ext_StartNewPolygon: 1538
			validationMessages.add(
					new ValidationMessage(
							ValidationMessageKind.INVALID_REFERENCE_YEAR, polyFeatureId.toString(),
							Vdyp7Constants.MIN_CALENDAR_YEAR, Vdyp7Constants.MAX_CALENDAR_YEAR, referenceYear
					)
			);
		}

		if (yearOfDeath == null
				|| (yearOfDeath < Vdyp7Constants.MIN_CALENDAR_YEAR || yearOfDeath > Vdyp7Constants.MAX_CALENDAR_YEAR)) {
			// V7Ext_StartNewPolygon: 1561
			validationMessages.add(
					new ValidationMessage(
							ValidationMessageKind.INVALID_REFERENCE_YEAR, polyFeatureId.toString(),
							Vdyp7Constants.MIN_CALENDAR_YEAR, Vdyp7Constants.MAX_CALENDAR_YEAR, referenceYear
					)
			);
		}

		if (validationMessages.size() > 0) {
			throw new PolygonValidationException(validationMessages);
		}

		return this;
	}
}
