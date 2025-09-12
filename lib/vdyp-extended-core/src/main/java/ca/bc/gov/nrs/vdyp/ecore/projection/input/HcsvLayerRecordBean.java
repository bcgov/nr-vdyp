package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;
import com.opencsv.bean.processor.PreAssignmentProcessor;
import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.LayerValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.Vdyp7LayerTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.CsvRecordBeanHelper;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SpeciesTable;

public class HcsvLayerRecordBean {

	private static final Logger logger = LoggerFactory.getLogger(HcsvLayerRecordBean.class);

	public static CsvToBean<HcsvLayerRecordBean>
			createHcsvLayerStream(CsvExceptionHandler exceptionHandler, InputStream layersCsvStream) {
		return new CsvToBeanBuilder<HcsvLayerRecordBean>(new BufferedReader(new InputStreamReader(layersCsvStream))) //
				.withSeparator(',') //
				.withType(HcsvLayerRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.withVerifier(new HcsvLayerRecordBeanValidator()) //
				.withExceptionHandler(exceptionHandler) //
				.build();
	}

	public static CsvToBean<HcsvLayerRecordBean> createHcsvLayerStream(InputStream layersCsvStream) {
		return new CsvToBeanBuilder<HcsvLayerRecordBean>(new BufferedReader(new InputStreamReader(layersCsvStream))) //
				.withSeparator(',') //
				.withType(HcsvLayerRecordBean.class) //
				.withFilter(new HcsvLineFilter(true, true)) //
				.withVerifier(new HcsvLayerRecordBeanValidator()) //
				.build();
	}

	// { "LAYER_FEATURE_ID", csvFldType_CHAR, 38, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 0)
	private String featureId;

	// { "TREE_COVER_ID", csvFldType_CHAR, 38, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 1)
	private String treeCoverId;

	// { "LAYER_MAP_ID", csvFldType_CHAR, 9, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 2)
	private String layerMapId;

	// { "LAYER_POLYGON_NO", csvFldType_LONG, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 3)
	private String polygonNumber;

	// { "LAYER_LEVEL_CD", csvFldType_CHAR, 1, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 4)
	private String layerId;

	// { "VDYP7_LAYER_LEVEL_CD", csvFldType_CHAR, 1, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 5)
	private String targetVdyp7LayerCode;

	// { "LAYER_STOCKABILITY", csvFldType_SINGLE, 5, 1, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 6)
	private String layerStockability;

	// { "LAYER_RANK_CD", csvFldType_CHAR, 38, 0, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 7)
	private String forestCoverRankCode;

	// { "NON_FOREST_DESCRIPTOR", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 8)
	private String nonForestDescriptorCode;

	// { "EST_SITE_INDEX_SPECIES_CD", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 9)
	private String estimatedSiteIndexSpeciesCode;

	// { "EST_SITE_INDEX", csvFldType_SINGLE, 5, 1, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 10)
	private String estimatedSiteIndex;

	// { "CROWN_CLOSURE", csvFldType_SHORT, 3, 0, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 11)
	private String crownClosure;

	// { "BASAL_AREA", csvFldType_SINGLE, 10, 6, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 12)
	private String basalArea;

	// { "STEMS_PER_HA", csvFldType_SINGLE, 8, 0, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 13)
	private String stemsPerHectare;

	// { "SPECIES_CD_1", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 14)
	private String speciesCode1;

	// { "SPECIES_PCT_1", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 15)
	private String speciesPercent1;

	// { "SPECIES_CD_2", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 16)
	private String speciesCode2;

	// { "SPECIES_PCT_2", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 17)
	private String speciesPercent2;

	// { "SPECIES_CD_3", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 18)
	private String speciesCode3;

	// { "SPECIES_PCT_3", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 19)
	private String speciesPercent3;

	// { "SPECIES_CD_4", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 20)
	private String speciesCode4;

	// { "SPECIES_PCT_4", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 21)
	private String speciesPercent4;

	// { "SPECIES_CD_5", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 22)
	private String speciesCode5;

	// { "SPECIES_PCT_5", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 23)
	private String speciesPercent5;

	// { "SPECIES_CD_6", csvFldType_CHAR, 10, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 24)
	private String speciesCode6;

	// { "SPECIES_PCT_6", csvFldType_SINGLE, 5, 2, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 25)
	private String speciesPercent6;

	// { "EST_AGE_SPP1", csvFldType_SHORT, 4, 0, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 26)
	private String estimatedAgeSpp1;

	// { "EST_HEIGHT_SPP1", csvFldType_SINGLE, 5, 1, "", TRUE },
	@PreAssignmentProcessor(processor = ConvertEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 27)
	private String estimatedHeightSpp1;

	// { "EST_AGE_SPP2", csvFldType_SHORT, 4, 0, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 28)
	private String estimatedAgeSpp2;

	// { "EST_HEIGHT_SPP2", csvFldType_SINGLE, 5, 1, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 29)
	private String estimatedHeightSpp2;

	// { "ADJUSTMENT_IND", csvFldType_CHAR, 1, 0, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 30)
	private String adjustmentIndicatorInd;

	// { "ADJ_LOREY_HEIGHT", csvFldType_SINGLE, 9, 5, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 31)
	private String loreyHeight75Adjustment;

	// { "ADJ_BASAL_AREA_125", csvFldType_SINGLE, 10, 6, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 32)
	private String basalArea125Adjustment;

	// { "ADJ_VOL_PER_HA_75", csvFldType_SINGLE, 9, 5, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 33)
	private String wholeStemVolumePerHectare75Adjustment;

	// { "ADJ_VOL_PER_HA_125", csvFldType_SINGLE, 9, 5, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 34)
	private String wholeStemVolumePerHectare125Adjustment;

	// { "ADJ_CLOSE_UTIL_VOL_125", csvFldType_SINGLE, 9, 5, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 35)
	private String closeUtilizationVolumePerHectare125Adjustment;

	// { "ADJ_CLOSE_UTIL_DECAY_VOL_125", csvFldType_SINGLE, 9, 5, "", TRUE },
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 36)
	private String closeUtilizationVolumeLessDecayPerHectare125Adjustment;

	// { "ADJ_CLOSE_UTIL_WASTE_VOL_125", csvFldType_SINGLE, 9, 5, "", TRUE }
	@PreAssignmentProcessor(processor = NAEmptyOrBlankStringsToNull.class)
	@CsvBindByPosition(position = 37)
	private String closeUtilizationVolumeLessDecayAndWastagePerHectare125Adjustment;

	public long getFeatureId() {
		Validate.notNull(featureId, "HcsvLayerRecordBean.getFeatureId: featureId must not be null");
		return CsvRecordBeanHelper.parseLongAcceptNull(featureId);
	}

	public String getTreeCoverId() {
		return treeCoverId;
	}

	public String getLayerMapId() {
		return layerMapId;
	}

	public Long getPolygonNumber() {
		return CsvRecordBeanHelper.parseLongAcceptNull(polygonNumber);
	}

	public String getLayerId() {
		return layerId;
	}

	public ProjectionTypeCode getTargetVdyp7LayerCode() {
		return targetVdyp7LayerCode == null ? null : ProjectionTypeCode.fromVdyp7LayerTypeText(targetVdyp7LayerCode);
	}

	public Double getLayerStockability() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(layerStockability);
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
		return CsvRecordBeanHelper.parseDoubleAcceptNull(estimatedSiteIndex);
	}

	public Short getCrownClosure() {
		return CsvRecordBeanHelper.parseShortAcceptNull(crownClosure);
	}

	public Double getBasalArea() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(basalArea);
	}

	public Double getStemsPerHectare() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(stemsPerHectare);
	}

	public record SpeciesDetails(
			int layerSpeciesIndex, String speciesCode, double percent, Short estimatedAge, Double estimatedHeight
	) {
	}

	/**
	 * Return the species details supplied for each of the up to 6 species whose details are given in the layer record.
	 * The resulting list is ordered species 1, species 2, ... , species 6 (including only those that for which a
	 * percentage value was supplied.)
	 *
	 * @return as described
	 */
	public List<SpeciesDetails> getSpeciesDetails() {
		var details = new ArrayList<SpeciesDetails>();

		if (getSpeciesPercent1() != null) {
			var speciesCode = getSpeciesCode1();
			details.add(
					new SpeciesDetails(
							1, speciesCode, getSpeciesPercent1(), getEstimatedAgeSpp1(), getEstimatedHeightSpp1()
					)
			);
		}
		if (getSpeciesPercent2() != null) {
			var speciesCode = getSpeciesCode2();
			details.add(
					new SpeciesDetails(
							2, speciesCode, getSpeciesPercent2(), getEstimatedAgeSpp2(), getEstimatedHeightSpp2()
					)
			);
		}
		if (getSpeciesPercent3() != null) {
			var speciesCode = getSpeciesCode3();
			details.add(new SpeciesDetails(3, speciesCode, getSpeciesPercent3(), null, null));
		}
		if (getSpeciesPercent4() != null) {
			var speciesCode = getSpeciesCode4();
			details.add(new SpeciesDetails(4, speciesCode, getSpeciesPercent4(), null, null));
		}
		if (getSpeciesPercent5() != null) {
			var speciesCode = getSpeciesCode5();
			details.add(new SpeciesDetails(5, speciesCode, getSpeciesPercent5(), null, null));
		}
		if (getSpeciesPercent6() != null) {
			var speciesCode = getSpeciesCode6();
			details.add(new SpeciesDetails(6, speciesCode, getSpeciesPercent6(), null, null));
		}

		return details;
	}

	public String getSpeciesCode1() {
		return speciesCode1;
	}

	public Double getSpeciesPercent1() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent1);
	}

	public String getSpeciesCode2() {
		return speciesCode2;
	}

	public Double getSpeciesPercent2() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent2);
	}

	public String getSpeciesCode3() {
		return speciesCode3;
	}

	public Double getSpeciesPercent3() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent3);
	}

	public String getSpeciesCode4() {
		return speciesCode4;
	}

	public Double getSpeciesPercent4() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent4);
	}

	public String getSpeciesCode5() {
		return speciesCode5;
	}

	public Double getSpeciesPercent5() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent5);
	}

	public String getSpeciesCode6() {
		return speciesCode6;
	}

	public Double getSpeciesPercent6() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(speciesPercent6);
	}

	public Short getEstimatedAgeSpp1() {
		return CsvRecordBeanHelper.parseShortAcceptNull(estimatedAgeSpp1);
	}

	public Double getEstimatedHeightSpp1() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(estimatedHeightSpp1);
	}

	public Short getEstimatedAgeSpp2() {
		return CsvRecordBeanHelper.parseShortAcceptNull(estimatedAgeSpp2);
	}

	public Double getEstimatedHeightSpp2() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(estimatedHeightSpp2);
	}

	public Boolean getAdjustmentIndicatorInd() {
		return adjustmentIndicatorInd != null && adjustmentIndicatorInd.equalsIgnoreCase("Y");
	}

	public Double getLoreyHeight75Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(loreyHeight75Adjustment);
	}

	public Double getBasalArea125Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(basalArea125Adjustment);
	}

	public Double getWholeStemVolumePerHectare75Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(wholeStemVolumePerHectare75Adjustment);
	}

	public Double getWholeStemVolumePerHectare125Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(wholeStemVolumePerHectare125Adjustment);
	}

	public Double getCloseUtilizationVolumePerHectare125Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(closeUtilizationVolumePerHectare125Adjustment);
	}

	public Double getCloseUtilizationVolumeLessDecayPerHectare125Adjustment() {
		return CsvRecordBeanHelper.parseDoubleAcceptNull(closeUtilizationVolumeLessDecayPerHectare125Adjustment);
	}

	public Double getCloseUtilizationVolumeLessDecayAndWastagePerHectare125Adjustment() {
		return CsvRecordBeanHelper
				.parseDoubleAcceptNull(closeUtilizationVolumeLessDecayAndWastagePerHectare125Adjustment);
	}

	/**
	 * Perform adjustments and checks once the record is initialized from the data in the CSV. The validation checks are
	 * limited to those of lcl_CopyLayerDataIntoSnapshot and DefPoly_DefineLayerToVDYP7 that evaluate the Layer
	 * independently of context.
	 *
	 * @return completed record
	 */
	private static class HcsvLayerRecordBeanValidator implements BeanVerifier<HcsvLayerRecordBean> {

		@Override
		public boolean verifyBean(HcsvLayerRecordBean bean) throws CsvConstraintViolationException {

			// Skip lines with no LAYER_FEATURE_ID
			if (bean.featureId == null) {
				return false;
			}

			logger.trace("Performing validation of layer \"{}:{}\" in isolation", bean.featureId, bean.layerId);

			BeanValidatorHelper bvh = new BeanValidatorHelper(bean.featureId);

			// V7Ext_AddLayer 2606
			if (bean.layerId == null) {
				bvh.addValidationMessage(ValidationMessageKind.MISSING_LAYER_CODE, bean.layerId);
			}

			// V7Ext_AddLayer 2613
			bvh.validateEnumeration(
					bean.targetVdyp7LayerCode, t -> ProjectionTypeCode.fromVdyp7LayerTypeText(t),
					"Target VDYP7 Layer Code"
			);

			bvh.validateNumber(bean.featureId, n -> Long.parseLong(n), "Feature Id");
			bvh.validateNumber(bean.polygonNumber, n -> Long.parseLong(n), "Polygon Number");

			// TREE_COVER_ID is not transferred from the CSV to the layer snapshot in VDYP7
			// LAYER_MAP_ID is not transferred from the CSV to the layer snapshot in VDYP7
			// LAYER_POLYGON_NO is not transferred from the CSV to the layer snapshot in VDYP7

			// V7Ext_AddLayer, 2637
			bvh.validateRange(
					bean.layerStockability, n -> Double.parseDouble(n), n -> Double.parseDouble(n), 0d, 100d,
					"Layer Stockability"
			);

			// lcl_CopyLayerDataIntoSnapshot, 4526
			bean.layerId = bvh.truncateString(bean.layerId, Vdyp7Constants.MAX_LEN_LAYER_ID);

			// lcl_CopyLayerDataIntoSnapshot, 4604
			bean.forestCoverRankCode = bvh
					.truncateString(bean.forestCoverRankCode, Vdyp7Constants.MAX_FOREST_COVER_RANK_CODE_LEN);

			// lcl_CopyLayerDataIntoSnapshot, 4610
			bean.nonForestDescriptorCode = bvh
					.truncateString(bean.nonForestDescriptorCode, Vdyp7Constants.MAX_NON_FOREST_DESCRIPTOR_CODE_LEN);

			// lcl_CopyLayerDataIntoSnapshot, 4616
			bean.estimatedSiteIndexSpeciesCode = bvh.truncateString(
					bean.estimatedSiteIndexSpeciesCode, Vdyp7Constants.MAX_LEN_ESTIMATED_SITE_INDEX_SPECIES_CODE
			);

			// V7Ext_AddLayer, 2658
			bvh.validateRange(
					bean.estimatedSiteIndex, n -> Double.parseDouble(n), n -> Double.parseDouble(n), 0d,
					Double.MAX_VALUE, "Estimated Site Index"
			);

			// V7Ext_AddLayer, 2637
			bvh.validateRange(
					bean.crownClosure, n -> Short.parseShort(n), n -> Short.parseShort(n), (short) 0, (short) 100,
					"Crown Closure"
			);

			// V7Ext_AddLayer, 2644
			bvh.validateRange(
					bean.basalArea, n -> Double.parseDouble(n), n -> Double.parseDouble(n), 0d, Double.MAX_VALUE,
					"Basal Area"
			);
			// V7Ext_AddLayer, 2851
			bean.basalArea = BeanValidatorHelper.round(bean.basalArea, 5);

			// V7Ext_AddLayer, 2651
			bvh.validateRange(
					bean.stemsPerHectare, n -> Double.parseDouble(n), n -> Double.parseDouble(n), 0d, Double.MAX_VALUE,
					"Stems per Hectare"
			);
			// V7Ext_AddLayer, 2852
			bean.stemsPerHectare = BeanValidatorHelper.round(bean.stemsPerHectare, 2);

			// V7Ext_AddLayer, 2672
			if (bean.targetVdyp7LayerCode == null) {
				if (Vdyp7LayerTypeCode.isLayerIdAVdyp7LayerType(bean.layerId)) {
					Vdyp7LayerTypeCode layerType = Vdyp7LayerTypeCode.fromCode(bean.layerId);
					bean.targetVdyp7LayerCode = ProjectionTypeCode
							.fromVdyp7LayerType(layerType).specialLayerTypeCodeText;
				}
			}

			// V7Ext_AddLayer, 2698
			if (bean.estimatedSiteIndexSpeciesCode != null) {
				int speciesIndex = SiteTool.getSpeciesIndex(bean.estimatedSiteIndexSpeciesCode);
				if (speciesIndex == SpeciesTable.UNKNOWN_ENTRY_INDEX) {
					bvh.addValidationMessage(
							ValidationMessageKind.UNRECOGNIZED_SPECIES, bean.layerId, bean.estimatedSiteIndexSpeciesCode
					);
				}
			}

			// V7Ext_AddSpeciesComponent, line 4022

			bvh.validateNumber(bean.speciesPercent1, n -> Double.parseDouble(n), "Species Percent 1");
			bean.speciesPercent1 = BeanValidatorHelper.round(bean.speciesPercent1, 1);
			bvh.validateNumber(bean.speciesPercent2, n -> Double.parseDouble(n), "Species Percent 2");
			bean.speciesPercent2 = BeanValidatorHelper.round(bean.speciesPercent2, 1);
			bvh.validateNumber(bean.speciesPercent3, n -> Double.parseDouble(n), "Species Percent 3");
			bean.speciesPercent3 = BeanValidatorHelper.round(bean.speciesPercent3, 1);
			bvh.validateNumber(bean.speciesPercent4, n -> Double.parseDouble(n), "Species Percent 4");
			bean.speciesPercent4 = BeanValidatorHelper.round(bean.speciesPercent4, 1);
			bvh.validateNumber(bean.speciesPercent5, n -> Double.parseDouble(n), "Species Percent 5");
			bean.speciesPercent5 = BeanValidatorHelper.round(bean.speciesPercent5, 1);
			bvh.validateNumber(bean.speciesPercent6, n -> Double.parseDouble(n), "Species Percent 6");
			bean.speciesPercent6 = BeanValidatorHelper.round(bean.speciesPercent6, 1);

			double highestSpeciesPercentageSeen = Double.POSITIVE_INFINITY;
			for (var sd : bean.getSpeciesDetails()) {
				if (sd.speciesCode() == null) {
					// V7Ext_AddSpecies, line 3677
					bvh.addValidationMessage(
							ValidationMessageKind.MISSING_SPECIES_NAME, bean.layerId, sd.layerSpeciesIndex
					);
				}

				// Ensure that the species are supplied in descending order of percentage - that is,
				// the more common species precede the less common species.
				if (sd.percent > highestSpeciesPercentageSeen) {
					bvh.addValidationMessage(
							ValidationMessageKind.PERCENTAGES_INCREASING, bean.featureId, bean.layerId,
							sd.layerSpeciesIndex, sd.percent
					);
				}

				// V7Ext_AddSpecies, line 3683
				bvh.validateRange(sd.percent, 0.0, 100.0, "Species Code " + sd.layerSpeciesIndex);
				highestSpeciesPercentageSeen = sd.percent;
				// V7Ext_AddSpecies, line 3689
				bvh.validateRange(sd.estimatedAge, (short) 0, (short) 2000, "Estimated Age " + sd.layerSpeciesIndex);
				// V7Ext_AddSpecies, line 3695
				bvh.validateRange(sd.estimatedHeight, 0.0, 150.0, "Estimated Height " + sd.layerSpeciesIndex);

				// All other Species fields are uninitialized in HCSV input.
			}

			bvh.validateNumber(bean.estimatedAgeSpp1, n -> Short.parseShort(n), "Estimated Age Spp 1");
			bvh.validateNumber(bean.estimatedHeightSpp1, n -> Double.parseDouble(n), "Estimated Height Spp 1");
			bvh.validateNumber(bean.estimatedAgeSpp2, n -> Short.parseShort(n), "Estimated Age Spp 2");
			bvh.validateNumber(bean.estimatedHeightSpp2, n -> Double.parseDouble(n), "Estimated Height Spp 2");
			bvh.validateNumber(
					bean.loreyHeight75Adjustment, n -> Double.parseDouble(n), "Lorey Height 7.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.basalArea125Adjustment, n -> Double.parseDouble(n), "Basal Area 12.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.wholeStemVolumePerHectare75Adjustment, n -> Double.parseDouble(n),
					"Whole Stem Volume 7.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.closeUtilizationVolumePerHectare125Adjustment, n -> Double.parseDouble(n),
					"Close Utilization 12.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.closeUtilizationVolumeLessDecayPerHectare125Adjustment, n -> Double.parseDouble(n),
					"Close Utilization Volume Less Decay 12.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.closeUtilizationVolumeLessDecayAndWastagePerHectare125Adjustment, n -> Double.parseDouble(n),
					"Close Utilization Volume Less Decay and Wastage 12.5cm+ Adjustment"
			);
			bvh.validateNumber(
					bean.wholeStemVolumePerHectare125Adjustment, n -> Double.parseDouble(n),
					"Whole Stem Volume 12.5cm+ Adjustment"
			);

			// Now, throw if there's been any validation errors.

			if (!bvh.getValidationMessages().isEmpty()) {
				throw new CsvConstraintViolationException(new LayerValidationException(bvh.getValidationMessages()));
			}

			return true;
		}
	}
}
