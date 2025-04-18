package ca.bc.gov.nrs.vdyp.fip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.EndOfRecord;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseValidException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.AbstractStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.GroupingStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.value.ControlledValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;

public class FipSpeciesParser
		implements ControlMapValueReplacer<StreamingParserFactory<Collection<FipSpecies>>, String> {

	static final String GENUS = "GENUS"; // SP0
	static final String PERCENT_GENUS = "PERCENT_GENUS"; // PTVSP0

	static final String SPECIES_1 = "SPECIES_1"; // SP641
	static final String PERCENT_SPECIES_1 = "PERCENT_SPECIES_1"; // PCT1
	static final String SPECIES_2 = "SPECIES_2"; // SP642
	static final String PERCENT_SPECIES_2 = "PERCENT_SPECIES_2"; // PCT2
	static final String SPECIES_3 = "SPECIES_3"; // SP643
	static final String PERCENT_SPECIES_3 = "PERCENT_SPECIES_3"; // PCT3
	static final String SPECIES_4 = "SPECIES_4"; // SP644
	static final String PERCENT_SPECIES_4 = "PERCENT_SPECIES_4"; // PCT4

	@Override
	public ControlKey getControlKey() {
		return ControlKey.FIP_INPUT_YIELD_LX_SP0;
	}

	@Override
	public StreamingParserFactory<Collection<FipSpecies>>
			map(String fileName, FileResolver fileResolver, Map<String, Object> controlMap) {
		return () -> {
			var lineParser = new LineParser().strippedString(25, FipPolygonParser.POLYGON_IDENTIFIER).space(1).value(
					1, FipLayerParser.LAYER,
					ValueParser.valueOrMarker(
							ValueParser.LAYER, ValueParser.optionalSingleton("Z"::equals, EndOfRecord.END_OF_RECORD)
					)
			).space(1).value(2, GENUS, ControlledValueParser.optional(ValueParser.GENUS))
					.value(6, PERCENT_GENUS, ValueParser.PERCENTAGE)
					.value(3, SPECIES_1, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_1, ValueParser.PERCENTAGE)
					.value(3, SPECIES_2, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_2, ValueParser.PERCENTAGE)
					.value(3, SPECIES_3, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_3, ValueParser.PERCENTAGE)
					.value(3, SPECIES_4, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_4, ValueParser.PERCENTAGE);

			var is = fileResolver.resolveForInput(fileName);

			var delegateStream = new AbstractStreamingParser<ValueOrMarker<Optional<FipSpecies>, EndOfRecord>>(
					is, lineParser, controlMap
			) {

				@SuppressWarnings("unchecked")
				@Override
				protected ValueOrMarker<Optional<FipSpecies>, EndOfRecord> convert(Map<String, Object> entry)
						throws ResourceParseException {
					var polygonId = (String) entry.get(FipPolygonParser.POLYGON_IDENTIFIER);
					var layer = (ValueOrMarker<Optional<LayerType>, EndOfRecord>) entry.get(FipLayerParser.LAYER);
					String genus;
					if (layer.isValue()) {
						genus = ((Optional<String>) entry.get(GENUS)).orElseThrow(
								() -> new ResourceParseValidException(
										"Genus identifier can not be empty except in end of record entries"
								)
						);
					} else {
						genus = null;
					}
					var percentGenus = (Float) entry.get(PERCENT_GENUS);
					var species1 = (Optional<String>) entry.get(SPECIES_1);
					var percentSpecies1 = (Float) entry.get(PERCENT_SPECIES_1);
					var species2 = (Optional<String>) entry.get(SPECIES_2);
					var percentSpecies2 = (Float) entry.get(PERCENT_SPECIES_2);
					var species3 = (Optional<String>) entry.get(SPECIES_3);
					var percentSpecies3 = (Float) entry.get(PERCENT_SPECIES_3);
					var species4 = (Optional<String>) entry.get(SPECIES_4);
					var percentSpecies4 = (Float) entry.get(PERCENT_SPECIES_4);

					var layerBuilder = new ValueOrMarker.Builder<Optional<FipSpecies>, EndOfRecord>();
					return layer.handle(l -> {
						return layerBuilder.value(l.map(layerType -> {
							List<Sp64Distribution> sp64SpeciesDistributions = new ArrayList<>();
							species1.ifPresent((sp) -> {
								sp64SpeciesDistributions.add(new Sp64Distribution(1, sp, percentSpecies1));
							});
							species2.ifPresent((sp) -> {
								sp64SpeciesDistributions.add(new Sp64Distribution(2, sp, percentSpecies2));
							});
							species3.ifPresent((sp) -> {
								sp64SpeciesDistributions.add(new Sp64Distribution(3, sp, percentSpecies3));
							});
							species4.ifPresent((sp) -> {
								sp64SpeciesDistributions.add(new Sp64Distribution(4, sp, percentSpecies4));
							});
							return FipSpecies.build(specBuilder -> {
								specBuilder.polygonIdentifier(polygonId);
								specBuilder.layerType(layerType);
								specBuilder.genus(genus, controlMap);
								specBuilder.percentGenus(percentGenus);
								specBuilder.sp64DistributionList(sp64SpeciesDistributions);
							});
						}));
					}, layerBuilder::marker);
				}

			};

			return new GroupingStreamingParser<Collection<FipSpecies>, ValueOrMarker<Optional<FipSpecies>, EndOfRecord>>(
					delegateStream
			) {

				@Override
				protected boolean skip(ValueOrMarker<Optional<FipSpecies>, EndOfRecord> nextChild) {
					return nextChild.getValue().map(Optional::isEmpty).orElse(false);
				}

				@Override
				protected boolean stop(ValueOrMarker<Optional<FipSpecies>, EndOfRecord> nextChild) {
					return nextChild.isMarker();
				}

				@Override
				protected Collection<FipSpecies>
						convert(List<ValueOrMarker<Optional<FipSpecies>, EndOfRecord>> children) {
					return children.stream().map(ValueOrMarker::getValue).map(Optional::get) // Should never be empty as
							// we've filtered out
							// markers
							.flatMap(Optional::stream) // Skip if empty (and unknown layer type)
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
