package ca.bc.gov.nrs.vdyp.forward.parsers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.io.EndOfRecord;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.AbstractStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.GroupingStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.value.ControlledValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

public class VdypSpeciesParser implements ControlMapValueReplacer<Object, String> {

	private static final String DESCRIPTION = "DESCRIPTION"; // POLYDESC
	private static final String LAYER_TYPE = "LAYER_TYPE"; // LAYERG
	private static final String SPECIES_GROUP_INDEX = "SPECIES_INDEX"; // ISP
	private static final String SPECIES_GROUP = "SPECIES"; // SP0

	private static final String SPECIES_0 = "SPECIES_0"; // SP640
	private static final String PERCENT_SPECIES_0 = "PERCENT_SPECIES_0"; // PCT0
	private static final String SPECIES_1 = "SPECIES_1"; // SP641
	private static final String PERCENT_SPECIES_1 = "PERCENT_SPECIES_1"; // PCT1
	private static final String SPECIES_2 = "SPECIES_2"; // SP642
	private static final String PERCENT_SPECIES_2 = "PERCENT_SPECIES_2"; // PCT2
	private static final String SPECIES_3 = "SPECIES_3"; // SP643
	private static final String PERCENT_SPECIES_3 = "PERCENT_SPECIES_3"; // PCT3

	private static final String SITE_INDEX = "SITE_INDEX"; // SI
	private static final String DOMINANT_HEIGHT = "DOMINANT_HEIGHT"; // HD
	private static final String TOTAL_AGE = "TOTAL_AGE"; // AGETOT
	private static final String AGE_AT_BREAST_HEIGHT = "AGE_AT_BREAST_HEIGHT"; // AGEBH
	private static final String YEARS_TO_BREAST_HEIGHT = "YEARS_TO_BREAST_HEIGHT"; // YTBH
	private static final String IS_PRIMARY_SPECIES = "IS_PRIMARY_SPECIES"; // INSITESP
	private static final String SITE_CURVE_NUMBER = "SITE_CURVE_NUMBER"; // SCN

	@Override
	public ControlKey getControlKey() {
		return ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES;
	}

	@Override
	public StreamingParserFactory<Collection<VdypSpecies>>
			map(String fileName, FileResolver fileResolver, Map<String, Object> controlMap) {
		return () -> {
			var lineParser = new LineParser().strippedString(25, DESCRIPTION) //
					.space(1) //
					.value(
							1, LAYER_TYPE,
							ValueParser.valueOrMarker(
									ValueParser.LAYER,
									ValueParser.optionalSingleton(
											x -> x == null || x.trim().length() == 0 || x.trim().equals("Z"),
											EndOfRecord.END_OF_RECORD
									)
							)
					) //
					.space(1) //
					.value(2, SPECIES_GROUP_INDEX, ValueParser.INTEGER) //
					.space(1) //
					.value(2, SPECIES_GROUP, ControlledValueParser.optional(ControlledValueParser.GENUS)) //
					.space(1) //
					.value(3, SPECIES_0, ControlledValueParser.optional(ControlledValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_0, ControlledValueParser.optional(ValueParser.PERCENTAGE))
					.value(3, SPECIES_1, ControlledValueParser.optional(ControlledValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_1, ControlledValueParser.optional(ValueParser.PERCENTAGE))
					.value(3, SPECIES_2, ControlledValueParser.optional(ControlledValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_2, ControlledValueParser.optional(ValueParser.PERCENTAGE))
					.value(3, SPECIES_3, ControlledValueParser.optional(ControlledValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_3, ControlledValueParser.optional(ValueParser.PERCENTAGE))
					.value(6, SITE_INDEX, VdypForwardDefaultingParser.FLOAT_WITH_DEFAULT)
					.value(6, DOMINANT_HEIGHT, VdypForwardDefaultingParser.FLOAT_WITH_DEFAULT)
					.value(6, TOTAL_AGE, VdypForwardDefaultingParser.FLOAT_WITH_DEFAULT)
					.value(6, AGE_AT_BREAST_HEIGHT, VdypForwardDefaultingParser.FLOAT_WITH_DEFAULT)
					.value(6, YEARS_TO_BREAST_HEIGHT, VdypForwardDefaultingParser.FLOAT_WITH_DEFAULT)
					.value(2, IS_PRIMARY_SPECIES, ControlledValueParser.optional(ValueParser.LOGICAL_0_1))
					.value(3, SITE_CURVE_NUMBER, VdypForwardDefaultingParser.INTEGER_WITH_DEFAULT);

			var is = fileResolver.resolveForInput(fileName);

			var speciesGroupDefinitionMap = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());

			var delegateStream = new AbstractStreamingParser<ValueOrMarker<Optional<VdypSpecies>, EndOfRecord>>(
					is, lineParser, controlMap
			) {
				@SuppressWarnings("unchecked")
				@Override
				protected ValueOrMarker<Optional<VdypSpecies>, EndOfRecord> convert(Map<String, Object> entry)
						throws ResourceParseException {

					var polygonId = VdypPolygonDescriptionParser.parse((String) entry.get(DESCRIPTION));
					var layerType = (ValueOrMarker<Optional<LayerType>, EndOfRecord>) entry.get(LAYER_TYPE);
					if (layerType == null) {
						var builder = new ValueOrMarker.Builder<Optional<LayerType>, EndOfRecord>();
						layerType = builder.marker(EndOfRecord.END_OF_RECORD);
					}
					var speciesGroupIndex = (Integer) entry.get(SPECIES_GROUP_INDEX);
					var optionalSpeciesGroup = (Optional<String>) entry.get(SPECIES_GROUP);
					var speciesNameText0 = (Optional<String>) entry.get(SPECIES_0);
					var percentSpecies0 = (Optional<Float>) entry.get(PERCENT_SPECIES_0);
					var speciesNameText1 = (Optional<String>) entry.get(SPECIES_1);
					var percentSpecies1 = (Optional<Float>) entry.get(PERCENT_SPECIES_1);
					var speciesNameText2 = (Optional<String>) entry.get(SPECIES_2);
					var percentSpecies2 = (Optional<Float>) entry.get(PERCENT_SPECIES_2);
					var speciesNameText3 = (Optional<String>) entry.get(SPECIES_3);
					var percentSpecies3 = (Optional<Float>) entry.get(PERCENT_SPECIES_3);
					var siteIndex = (Float) (entry.get(SITE_INDEX));
					var dominantHeight = (Float) (entry.get(DOMINANT_HEIGHT));
					var totalAge = (Float) (entry.get(TOTAL_AGE));
					var yearsAtBreastHeight = (Float) (entry.get(AGE_AT_BREAST_HEIGHT));
					var yearsToBreastHeight = (Float) (entry.get(YEARS_TO_BREAST_HEIGHT));
					var isPrimarySpecies = Utils.<Boolean>optSafe(entry.get(IS_PRIMARY_SPECIES));
					var siteCurveNumber = Utils.<Integer>optSafe(entry.get(SITE_CURVE_NUMBER))
							.orElse(VdypEntity.MISSING_INTEGER_VALUE);

					var builder = new ValueOrMarker.Builder<Optional<VdypSpecies>, EndOfRecord>();
					return layerType.handle(l -> builder.value(l.map(lt -> {

						List<Sp64Distribution> gdList = new ArrayList<>();

						Utils.ifBothPresent(
								speciesNameText0, percentSpecies0, (s, p) -> gdList.add(new Sp64Distribution(1, s, p))
						);

						Utils.ifBothPresent(
								speciesNameText1, percentSpecies1, (s, p) -> gdList.add(new Sp64Distribution(2, s, p))
						);

						Utils.ifBothPresent(
								speciesNameText2, percentSpecies2, (s, p) -> gdList.add(new Sp64Distribution(3, s, p))
						);

						Utils.ifBothPresent(
								speciesNameText3, percentSpecies3, (s, p) -> gdList.add(new Sp64Distribution(4, s, p))
						);

						Sp64DistributionSet speciesDistributionSet = new Sp64DistributionSet(4, gdList);

						var speciesGroup = optionalSpeciesGroup
								.orElse(speciesGroupDefinitionMap.getByIndex(speciesGroupIndex).getAlias());

						var iTotalAge = totalAge;
						var iYearsToBreastHeight = yearsToBreastHeight;

						// From VDYPGETS.FOR, lines 235 to 255.
						if (Float.isNaN(totalAge)) {
							if (yearsAtBreastHeight > 0.0 && yearsToBreastHeight > 0.0)
								iTotalAge = yearsAtBreastHeight + yearsToBreastHeight;
						} else if (Float.isNaN(yearsToBreastHeight)) {
							if (yearsAtBreastHeight > 0.0 && totalAge > yearsAtBreastHeight)
								iYearsToBreastHeight = totalAge - yearsAtBreastHeight;
						}

						var inferredTotalAge = iTotalAge;
						var inferredYearsToBreastHeight = iYearsToBreastHeight;
						var inferYearsAtBreastHeight = (yearsAtBreastHeight == null || yearsAtBreastHeight.isNaN()
								|| yearsAtBreastHeight <= 0f);

						Float calculatedDominantHeight = null;
						// VDYP7loaddata.for lines 1337 -1356
						/*
						 * 2003/02/10 Replace the VDYP7 generated Dom Height with a value generated directly from SINDEX
						 * as per Cam's instructions. According to Cam, the height generated by VDYP7 is considered an
						 * "internal" height and not to be reported outside of those applications.
						 */
						try {
							var siteIndexEquation = SiteIndexEquation.getByIndex(siteCurveNumber);
							calculatedDominantHeight = (float) SiteTool.ageAndSiteIndexToHeight(
									siteIndexEquation, inferredTotalAge, SiteIndexAgeType.SI_AT_TOTAL, siteIndex,
									inferredYearsToBreastHeight
							);
						} catch (CommonCalculatorException ex) {
							// TODO how to handle an issue here....
						}

						var extDominantHeight = calculatedDominantHeight == null ? dominantHeight
								: calculatedDominantHeight;

						return VdypSpecies.build(speciesBuilder -> {
							speciesBuilder.sp64DistributionSet(speciesDistributionSet);
							speciesBuilder.polygonIdentifier(polygonId);
							speciesBuilder.layerType(lt);
							speciesBuilder.genus(speciesGroup, controlMap);
							speciesBuilder.isPrimary(isPrimarySpecies.orElse(false));
							speciesBuilder.addSite(siteBuilder -> {
								siteBuilder.ageTotal(inferredTotalAge);
								siteBuilder.height(extDominantHeight);
								siteBuilder.polygonIdentifier(polygonId);
								siteBuilder.siteCurveNumber(siteCurveNumber);
								siteBuilder.layerType(lt);
								siteBuilder.siteGenus(speciesGroup);
								siteBuilder.siteIndex(siteIndex);
								siteBuilder.yearsToBreastHeight(inferredYearsToBreastHeight);
								if (inferYearsAtBreastHeight) {
									siteBuilder.yearsAtBreastHeightAuto();
								} else {
									siteBuilder.yearsAtBreastHeight(yearsAtBreastHeight);
								}
							});
						});
					})), builder::marker);
				}
			};

			return new GroupingStreamingParser<Collection<VdypSpecies>, ValueOrMarker<Optional<VdypSpecies>, EndOfRecord>>(
					delegateStream
			) {
				@Override
				protected boolean skip(ValueOrMarker<Optional<VdypSpecies>, EndOfRecord> nextChild) {
					return nextChild.getValue().map(Optional::isEmpty).orElse(false);
				}

				@Override
				protected boolean stop(ValueOrMarker<Optional<VdypSpecies>, EndOfRecord> nextChild) {
					return nextChild.isMarker();
				}

				@Override
				protected Collection<VdypSpecies>
						convert(List<ValueOrMarker<Optional<VdypSpecies>, EndOfRecord>> children) {
					// Skip if empty (and unknown layer type)
					return children.stream().map(ValueOrMarker::getValue).map(Optional::get).flatMap(Optional::stream)
							.toList();
				}
			};
		};
	}

	@Override
	public ValueParser<Object> getValueParser() {
		return FILENAME;
	}
}
