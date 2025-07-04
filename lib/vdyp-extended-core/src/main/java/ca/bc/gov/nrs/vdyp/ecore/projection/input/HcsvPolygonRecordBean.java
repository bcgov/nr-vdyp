package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToDefault;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;
import com.opencsv.bean.processor.PreAssignmentProcessor;
import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.CfsEcoZoneCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.OtherVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.CsvRecordBeanHelper;

public class HcsvPolygonRecordBean {

	private static final Logger logger = LoggerFactory.getLogger(HcsvPolygonRecordBean.class);

	private static final String DEFAULT_MAP_ID = "UNKNOWN";

	public static CsvToBean<HcsvPolygonRecordBean>
			createHcsvPolygonStream(CsvExceptionHandler exceptionHandler, InputStream polygonStream) {
		return new CsvToBeanBuilder<HcsvPolygonRecordBean>(new BufferedReader(new InputStreamReader(polygonStream))) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.withVerifier(new HcsvPolygonRecordBeanValidator()) //
				.withExceptionHandler(exceptionHandler) //
				.build();
	}

	public static CsvToBean<HcsvPolygonRecordBean> createHcsvPolygonStream(InputStream polygonStream) {
		return new CsvToBeanBuilder<HcsvPolygonRecordBean>(new BufferedReader(new InputStreamReader(polygonStream))) //
				.withSeparator(',') //
				.withType(HcsvPolygonRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.withVerifier(new HcsvPolygonRecordBeanValidator()) //
				.build();
	}

	// { "POLY_FEATURE_ID", csvFldType_CHAR, 38, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "FEATURE_ID")
	@CsvBindByPosition(position = 0)
	private String polyFeatureId;

	// { "MAP_ID", csvFldType_CHAR, 9, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToDefault.class, paramString = DEFAULT_MAP_ID)
	@CsvBindByName(column = "MAP_ID")
	@CsvBindByPosition(position = 1)
	private String mapId;

	// { "POLYGON_NO", csvFldType_LONG, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "POLYGON_NUMBER")
	@CsvBindByPosition(position = 2)
	private String polygonNumber;

	// { "ORG_UNIT", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "ORG_UNIT")
	@CsvBindByPosition(position = 3)
	private String orgUnit;

	// { "TSA", csvFldType_CHAR, 5, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TSA_NAME")
	@CsvBindByPosition(position = 4)
	private String tsaName;

	// { "TFL", csvFldType_CHAR, 5, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TFL_NAME")
	@CsvBindByPosition(position = 5)
	private String tflName;

	// { "INVENTORY_STANDARD_CD", csvFldType_CHAR, 10, 0, "", TRUE }
	// maps to InventoryStandard
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "INVENTORY_STANDARD_CODE")
	@CsvBindByPosition(position = 6)
	private String inventoryStandardCode;

	// { "TSA_NUM", csvFldType_CHAR, 5, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "TSA_NUMBER")
	@CsvBindByPosition(position = 7)
	private String tsaNumber;

	// { "SHRUB_HEIGHT", csvFldType_SINGLE, 4, 1, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SHRUB_HEIGHT")
	@CsvBindByPosition(position = 8)
	private String shrubHeight;

	// { "SHRUB_CROWN_CLOSURE", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SHRUB_CROWN_CLOSURE")
	@CsvBindByPosition(position = 9)
	private String shrubCrownClosure;

	// { "SHRUB_COVER_PATTERN", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "SHRUB_COVER_PATTERN")
	@CsvBindByPosition(position = 10)
	private String shrubCoverPattern;

	// { "HERB_COVER_TYPE", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "HERB_COVER_TYPE_CODE")
	@CsvBindByPosition(position = 11)
	private String herbCoverTypeCode;

	// { "HERB_COVER_PCT", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "HERB_COVER_PCT")
	@CsvBindByPosition(position = 12)
	private String herbCoverPercent;

	// { "HERB_COVER_PATTERN", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "HERB_COVER_PATTERN_CODE")
	@CsvBindByPosition(position = 13)
	private String herbCoverPatternCode;

	// { "BRYOID_COVER_PCT", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BRYOID_COVER_PCT")
	@CsvBindByPosition(position = 14)
	private String bryoidCoverPercent;

	// { "BEC_ZONE_CD", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BEC_ZONE_CODE")
	@CsvBindByPosition(position = 15)
	private String becZoneCode;

	// { "CFS_ECO_ZONE", csvFldType_SHORT, 3, 0, "", TRUE }
	// Maps to CfsEcoZone
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "CFS_ECOZONE")
	@CsvBindByPosition(position = 16)
	private String cfsEcoZoneCode;

	// { "STOCKABILITY", csvFldType_SINGLE, 4, 1, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "PRE_DISTURBANCE_STOCKABILITY")
	@CsvBindByPosition(position = 17)
	private String percentStockable;

	// { "YIELD_FACTOR", csvFldType_SINGLE, 5, 3, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "YIELD_FACTOR")
	@CsvBindByPosition(position = 18)
	private String yieldFactor;

	// { "NON_PRODUCTIVE_DESCRIPTOR_CD", csvFldType_CHAR, 5, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_PRODUCTIVE_DESCRIPTOR_CD")
	@CsvBindByPosition(position = 19)
	private String nonProductiveDescriptorCode;

	// { "BCLCS_LEVEL_1", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL1_CODE")
	@CsvBindByPosition(position = 20)
	private String bclcsLevel1Code;

	// { "BCLCS_LEVEL_2", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL2_CODE")
	@CsvBindByPosition(position = 21)
	private String bclcsLevel2Code;

	// { "BCLCS_LEVEL_3", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL3_CODE")
	@CsvBindByPosition(position = 22)
	private String bclcsLevel3Code;

	// { "BCLCS_LEVEL_4", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL4_CODE")
	@CsvBindByPosition(position = 23)
	private String bclcsLevel4Code;

	// { "BCLCS_LEVEL_5", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "BCLCS_LEVEL5_CODE")
	@CsvBindByPosition(position = 24)
	private String bclcsLevel5Code;

	// NOTE inconsistency in VDYP7 - called "Reference Year" although col. is called PHOTO_ESTIMATION_BASE_YEAR in CSV
	// { "REFERENCE_YEAR", csvFldType_SHORT, 4, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "PHOTO_ESTIMATION_BASE_YEAR")
	@CsvBindByPosition(position = 25)
	private String referenceYear;

	// NOTE inconsistency in VDYP7 - called "Year of Death" although col. is called REFERENCE_YEAR in CSV
	// { "YEAR_OF_DEATH", csvFldType_SHORT, 4, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "REFERENCE_YEAR")
	@CsvBindByPosition(position = 26)
	private String yearOfDeath;

	// { "STOCKABILITY_DEAD", csvFldType_SINGLE, 4, 1, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "PCT_DEAD")
	@CsvBindByPosition(position = 27)
	private String percentDead;

	// { "NON_VEG_COVER_TYPE_1", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_1")
	@CsvBindByPosition(position = 28)
	private String nonVegCoverType1;

	// { "NON_VEG_COVER_PCT_1", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PCT_1")
	@CsvBindByPosition(position = 29)
	private String nonVegCoverPercent1;

	// { "NON_VEG_COVER_PATTERN_1", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_1")
	@CsvBindByPosition(position = 30)
	private String nonVegCoverPattern1;

	// { "NON_VEG_COVER_TYPE_2", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_2")
	@CsvBindByPosition(position = 31)
	private String nonVegCoverType2;

	// { "NON_VEG_COVER_PCT_2", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PCT_2")
	@CsvBindByPosition(position = 32)
	private String nonVegCoverPercent2;

	// { "NON_VEG_COVER_PATTERN_2", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_2")
	@CsvBindByPosition(position = 33)
	private String nonVegCoverPattern2;

	// { "NON_VEG_COVER_TYPE_3", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_TYPE_3")
	@CsvBindByPosition(position = 34)
	private String nonVegCoverType3;

	// { "NON_VEG_COVER_PCT_3", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PCT_3")
	@CsvBindByPosition(position = 35)
	private String nonVegCoverPercent3;

	// { "NON_VEG_COVER_PATTERN_3", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "NON_VEG_COVER_PATTERN_3")
	@CsvBindByPosition(position = 36)
	private String nonVegCoverPattern3;

	// { "LAND_COVER_CLASS_CD_1", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_1")
	@CsvBindByPosition(position = 37)
	private String landCoverClassCode1;

	// { "LAND_COVER_CLASS_PCT_1", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_PCT_1")
	@CsvBindByPosition(position = 38)
	private String landCoverPercent1;

	// { "LAND_COVER_CLASS_CD_2", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_2")
	@CsvBindByPosition(position = 39)
	private String landCoverClassCode2;

	// { "LAND_COVER_CLASS_PCT_2", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_PCT_2")
	@CsvBindByPosition(position = 40)
	private String landCoverPercent2;

	// { "LAND_COVER_CLASS_CD_3", csvFldType_CHAR, 10, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_CLASS_CD_3")
	@CsvBindByPosition(position = 41)
	private String landCoverClassCode3;

	// { "LAND_COVER_CLASS_PCT_3", csvFldType_SHORT, 3, 0, "", TRUE }
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByName(column = "LAND_COVER_PCT_3")
	@CsvBindByPosition(position = 42)
	private String landCoverPercent3;

	// ACCESSORS

	public Long getFeatureId() {
		return Long.valueOf(polyFeatureId);
	}

	public String getMapId() {
		if (mapId == null) {
			mapId = DEFAULT_MAP_ID;
		}
		return mapId;
	}

	public Long getPolygonNumber() {
		return CsvRecordBeanHelper.parseLongAcceptNull(polygonNumber);
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
		return inventoryStandardCode == null ? null : InventoryStandardCode.valueOf(inventoryStandardCode);
	}

	public String getTsaNumber() {
		return tsaNumber;
	}

	public record OtherVegCoverDetails(Integer otherVegCoverPercent, OtherVegetationTypeCode getNonVegCoverType) {
	}

	public Map<OtherVegetationTypeCode, OtherVegCoverDetails> getOtherVegCoverDetails() {
		var details = new HashMap<OtherVegetationTypeCode, OtherVegCoverDetails>();

		if (getShrubCrownClosure() != null) {
			OtherVegetationTypeCode type = OtherVegetationTypeCode.Shrub;
			details.put(type, new OtherVegCoverDetails(getShrubCrownClosure(), type));
		}
		if (getBryoidCoverPercent() != null) {
			OtherVegetationTypeCode type = OtherVegetationTypeCode.Bryoid;
			details.put(type, new OtherVegCoverDetails(getBryoidCoverPercent(), type));
		}
		if (getHerbCoverPercent() != null) {
			OtherVegetationTypeCode type = OtherVegetationTypeCode.Herb;
			details.put(type, new OtherVegCoverDetails(getHerbCoverPercent(), type));
		}

		return details;
	}

	public Double getShrubHeight() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(shrubHeight);
	}

	public Integer getShrubCrownClosure() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(shrubCrownClosure);
	}

	public String getShrubCoverPattern() {
		return shrubCoverPattern;
	}

	public String getHerbCoverTypeCode() {
		return herbCoverTypeCode;
	}

	public Integer getHerbCoverPercent() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(herbCoverPercent);
	}

	public String getHerbCoverPatternCode() {
		return herbCoverPatternCode;
	}

	public Integer getBryoidCoverPercent() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(bryoidCoverPercent);
	}

	public String getBecZoneCode() {
		return becZoneCode;
	}

	public CfsEcoZoneCode getCfsEcoZoneCode() {
		return cfsEcoZoneCode == null ? null : CfsEcoZoneCode.fromCode(Short.valueOf(cfsEcoZoneCode));
	}

	public Double getPercentStockable() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(percentStockable);
	}

	public Double getYieldFactor() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(yieldFactor);
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
		return CsvRecordBeanHelper.parseIntegerAcceptNull(referenceYear);
	}

	public Integer getYearOfDeath() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(yearOfDeath);
	}

	public Double getPercentDead() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(percentDead);
	}

	public record NonVegCoverDetails(
			String getNonVegCoverPattern, Integer nonVegCoverPercent, NonVegetationTypeCode getNonVegCoverType
	) {
	}

	public Map<NonVegetationTypeCode, NonVegCoverDetails> getNonVegCoverDetails() {
		var details = new HashMap<NonVegetationTypeCode, NonVegCoverDetails>();

		if (getNonVegCoverType1() != null) {
			NonVegetationTypeCode type = NonVegetationTypeCode.fromCode(nonVegCoverType1);
			details.put(type, new NonVegCoverDetails(nonVegCoverPattern1, getNonVegCoverPercent1(), type));
		}
		if (getNonVegCoverType2() != null) {
			NonVegetationTypeCode type = NonVegetationTypeCode.fromCode(nonVegCoverType2);
			details.put(type, new NonVegCoverDetails(nonVegCoverPattern2, getNonVegCoverPercent2(), type));
		}
		if (getNonVegCoverType3() != null) {
			NonVegetationTypeCode type = NonVegetationTypeCode.fromCode(nonVegCoverType3);
			details.put(type, new NonVegCoverDetails(nonVegCoverPattern3, getNonVegCoverPercent3(), type));
		}

		return details;
	}

	public String getNonVegCoverType1() {
		return nonVegCoverType1;
	}

	public Integer getNonVegCoverPercent1() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(nonVegCoverPercent1);
	}

	public String getNonVegCoverPattern1() {
		return nonVegCoverPattern1;
	}

	public String getNonVegCoverType2() {
		return nonVegCoverType2;
	}

	public Integer getNonVegCoverPercent2() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(nonVegCoverPercent1);
	}

	public String getNonVegCoverPattern2() {
		return nonVegCoverPattern2;
	}

	public String getNonVegCoverType3() {
		return nonVegCoverType3;
	}

	public Integer getNonVegCoverPercent3() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(nonVegCoverPercent3);
	}

	public String getNonVegCoverPattern3() {
		return nonVegCoverPattern3;
	}

	public String getLandCoverClassCode1() {
		return landCoverClassCode1;
	}

	public Integer getLandCoverPercent1() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(landCoverPercent1);
	}

	public String getLandCoverClassCode2() {
		return landCoverClassCode2;
	}

	public Integer getLandCoverPercent2() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(landCoverPercent2);
	}

	public String getLandCoverClassCode3() {
		return landCoverClassCode3;
	}

	public Integer getLandCoverPercent3() {
		return CsvRecordBeanHelper.parseIntegerAcceptNull(landCoverPercent3);
	}

	/**
	 * Perform adjustments and checks once the record is initialized from the data in the CSV. This method combines the
	 * logic of lcl_CopyPolygonDataIntoSnapshot and DefPoly_DefinePolygonToVDYP7.
	 *
	 * @return completed record
	 * @throws PolygonValidationException
	 */
	private static class HcsvPolygonRecordBeanValidator implements BeanVerifier<HcsvPolygonRecordBean> {

		@Override
		public boolean verifyBean(HcsvPolygonRecordBean bean) throws CsvConstraintViolationException {

			// Skip lines with no POLY_FEATURE_ID
			if (bean.polyFeatureId == null) {
				return false;
			}

			logger.trace("Performing validation of polygon \"{}\" in isolation", bean.polyFeatureId);

			BeanValidatorHelper bvh = new BeanValidatorHelper(bean.polyFeatureId);

			bvh.validateNumber(bean.polyFeatureId, n -> Long.parseLong(n), "Feature ID");

			// lcl_CopyPolygonDataIntoSnapshot: 3156
			bean.mapId = bvh.truncateString(bean.mapId, Vdyp7Constants.MAX_LEN_MAPSHEET);

			if (bean.referenceYear == null && bean.yearOfDeath != null) {
				// lcl_CopyPolygonDataIntoSnapshot: 3177
				bean.referenceYear = bean.yearOfDeath;
			}

			boolean isValidEnumerationMember = bvh.validateEnumeration(
					bean.inventoryStandardCode, c -> InventoryStandardCode.valueOf(c), "Inventory Standard Code"
			);
			if (isValidEnumerationMember && InventoryStandardCode.I.equals(bean.getInventoryStandardCode())) {
				bean.inventoryStandardCode = InventoryStandardCode.F.toString();
			}

			// SHRUB_HEIGHT is not transferred from the CSV to the layer snapshot in VDYP7. Still, we
			// validate that it's a number.
			bvh.validateNumber(bean.shrubHeight, n -> Double.valueOf(n), "Shrub Height");
			// SHRUB_PATTERN is not transferred from the CSV to the layer snapshot in VDYP7. No validation
			// required.
			bvh.validateNumber(bean.shrubCrownClosure, n -> Integer.valueOf(n), "Shrub Crown Closure");

			// HERB_COVER_TYPE is not transferred from the CSV to the layer snapshot in VDYP7
			// HERB_PATTERN is not transferred from the CSV to the layer snapshot in VDYP7
			bvh.validateNumber(bean.herbCoverPercent, n -> Integer.valueOf(n), "Herb Cover Percent");

			bvh.validateNumber(bean.bryoidCoverPercent, n -> Integer.valueOf(n), "Bryoid Cover Percent");

			if (sum(bean.shrubCrownClosure, bean.herbCoverPercent, bean.bryoidCoverPercent) > 100.0) {
				bvh.addValidationMessage(ValidationMessageKind.NON_TREE_COVERAGE_PERCENTAGES_EXCEED_100);
			}

			if (bean.becZoneCode == null) {
				// V7Ext_StartNewPolygon: 1453
				bvh.addValidationMessage(ValidationMessageKind.MISSING_BEC_ZONE);
			} else if (bean.becZoneCode != null) {

				// lcl_CopyPolygonDataIntoSnapshot: 3200
				bean.becZoneCode = bvh.truncateString(bean.becZoneCode, Vdyp7Constants.MAX_LEN_BEC_ZONE);

				// V7Ext_StartNewPolygon: 1479. Certain BEC zones are not supported; substitute
				// "AT" for each.
				if (bean.becZoneCode.equals("BAFA") || bean.becZoneCode.equals("CMA")
						|| bean.becZoneCode.equals("IMA")) {
					logger.info("Translating unsupported BEC zone {} to {}", bean.becZoneCode, "AT");
					bean.becZoneCode = "AT";
				}
			}

			if (bean.cfsEcoZoneCode == null) {
				// lcl_CopyPolygonDataIntoSnapshot: 3224 makes V7Ext_StartNewPolygon: 1409 redundant
				bean.cfsEcoZoneCode = String.valueOf(CfsEcoZoneCode.Unknown.getCode());
			} else {
				bvh.validateEnumeration(
						bean.cfsEcoZoneCode, c -> CfsEcoZoneCode.fromCode(Short.valueOf(c)), "CFS Eco Zone"
				);
			}

			bvh.validateRange(
					bean.percentStockable, n -> Double.valueOf(n), n -> Double.valueOf(n), 0.0, 100.0,
					"Percent Stockable"
			);

			bvh.validateRange(
					bean.yieldFactor, d -> Double.valueOf(d), d -> Double.valueOf(d), 0.0, 10.0, "Yield Factor"
			);

			// lcl_CopyPolygonDataIntoSnapshot: 3206
			bean.nonProductiveDescriptorCode = bvh
					.truncateString(bean.nonProductiveDescriptorCode, Vdyp7Constants.MAX_LEN_NON_PROD_DESC);

			bvh.validateRange(
					bean.referenceYear, s -> Short.valueOf(s), s -> Short.valueOf(s), Vdyp7Constants.MIN_CALENDAR_YEAR,
					Vdyp7Constants.MAX_CALENDAR_YEAR, "Reference Year"
			);

			bvh.validateRange(
					bean.yearOfDeath, s -> Short.valueOf(s), s -> Short.valueOf(s), Vdyp7Constants.MIN_CALENDAR_YEAR,
					Vdyp7Constants.MAX_CALENDAR_YEAR, "Year of Death"
			);

			bvh.validateRange(
					bean.percentDead, n -> Double.valueOf(n), n -> Double.valueOf(n), 0.0, 100.0, "Percent Dead"
			);

			bvh.validateEnumeration(
					bean.nonVegCoverType1, e -> NonVegetationTypeCode.fromCode(e), "Non-Vegetation Cover Type 1"
			);
			bvh.validateEnumeration(
					bean.nonVegCoverType2, e -> NonVegetationTypeCode.fromCode(e), "Non-Vegetation Cover Type 2"
			);
			bvh.validateEnumeration(
					bean.nonVegCoverType3, e -> NonVegetationTypeCode.fromCode(e), "Non-Vegetation Cover Type 3"
			);

			bvh.validateRange(
					bean.nonVegCoverPercent1, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Non Vegetation Cover Percentage #1"
			);
			bvh.validateRange(
					bean.nonVegCoverPercent2, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Non Vegetation Cover Percentage #2"
			);
			bvh.validateRange(
					bean.nonVegCoverPercent3, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Non Vegetation Cover Percentage #3"
			);

			if (sum(bean.nonVegCoverPercent1, bean.nonVegCoverPercent2, bean.nonVegCoverPercent3) > 100.0) {
				bvh.addValidationMessage(ValidationMessageKind.NON_VEGETATION_COVERAGE_PERCENTAGES_EXCEED_100);
			}

			bvh.validateRange(
					bean.landCoverPercent1, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Land Cover Percentage #1"
			);
			bvh.validateRange(
					bean.landCoverPercent2, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Land Cover Percentage #2"
			);
			bvh.validateRange(
					bean.landCoverPercent3, i -> Integer.valueOf(i), i -> Integer.valueOf(i), 0, 100,
					"Land Cover Percentage #3"
			);

			// Now, throw if there's been any validation errors.

			if (bvh.getValidationMessages().size() > 0) {
				throw new CsvConstraintViolationException(new PolygonValidationException(bvh.getValidationMessages()));
			}

			return true;
		}

		static double sum(String... eTexts) {
			double sum = 0.0;

			try {
				for (String eText : eTexts) {
					double e = 0;
					if (eText != null) {
						e = Double.parseDouble(eText);
					}
					sum += e;
				}

				return sum;
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}
	}
}
