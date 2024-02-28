package ca.bc.gov.nrs.vdyp.forward;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common_calculators.FizCheck;
import ca.bc.gov.nrs.vdyp.forward.model.FipMode;
import ca.bc.gov.nrs.vdyp.forward.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.AbstractStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.value.ControlledValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;

public class VdypPolygonParser implements ControlMapValueReplacer<StreamingParserFactory<VdypPolygon>, String> {

	private static final String DESCRIPTION = "DESCRIPTION"; // POLYDESC
	private static final String BIOGEOCLIMATIC_ZONE = "BIOGEOCLIMATIC_ZONE"; // BEC
	private static final String FOREST_INVENTORY_ZONE = "FOREST_INVENTORY_ZONE"; // FIZ
	private static final String PERCENT_FOREST_LAND = "PERCENT_FOREST_LAND"; // PCTFLAND
	private static final String INVENTORY_TYPE_GROUP = "INVENTORY_TYPE_GROUP"; // ITG
	private static final String BASAL_AREA_GROUP = "BASAL_AREA_GROUP"; // GRPBA1
	private static final String FIP_MODE = "FIP_MODE"; // MODEfip

	@Override
	public ControlKey getControlKey() {
		return ControlKey.VDYP_POLYGON;
	}

	private static Pattern descriptionPattern = Pattern.compile(".*([\\d]{4}$)");

	@Override
	public StreamingParserFactory<VdypPolygon>
			map(String fileName, FileResolver fileResolver, Map<String, Object> control)
					throws IOException, ResourceParseException {
		return () -> {
			var lineParser = new LineParser() {
				@Override
				public boolean isStopLine(String line) {
					return line.substring(0, Math.min(25, line.length())).trim().length() == 0;
				}
			}.strippedString(25, DESCRIPTION).space(1).value(4, BIOGEOCLIMATIC_ZONE, ValueParser.BEC).space(1)
					.value(1, FOREST_INVENTORY_ZONE, ValueParser.CHARACTER) // TODO: add ValueParser.FIZ
					.value(6, PERCENT_FOREST_LAND, ValueParser.FLOAT)
					.value(3, INVENTORY_TYPE_GROUP, ControlledValueParser.optional(ValueParser.INTEGER))
					.value(3, BASAL_AREA_GROUP, ValueParser.optional(ValueParser.INTEGER))
					.value(3, FIP_MODE, ValueParser.optional(ValueParser.INTEGER));

			var is = fileResolver.resolveForInput(fileName);

			return new AbstractStreamingParser<VdypPolygon>(is, lineParser, control) {

				@SuppressWarnings("unchecked")
				@Override
				protected VdypPolygon convert(Map<String, Object> entry) throws ResourceParseException {
					var description = (String) entry.get(DESCRIPTION);
					var becAlias = (String) entry.get(BIOGEOCLIMATIC_ZONE);
					var fizId = (Character) entry.get(FOREST_INVENTORY_ZONE);
					var percentForestLand = (Float) entry.get(PERCENT_FOREST_LAND);
					var inventoryTypeGroup = (Optional<Integer>) entry.get(INVENTORY_TYPE_GROUP);
					if (inventoryTypeGroup == null) {
						inventoryTypeGroup = Optional.empty();
					}
					var basalAreaGroup = (Optional<Integer>) entry.get(BASAL_AREA_GROUP);
					if (basalAreaGroup == null) {
						basalAreaGroup = Optional.empty();
					}
					var fipMode = (Optional<Integer>) entry.get(FIP_MODE);
					if (fipMode == null) {
						fipMode = Optional.empty();
					}

					Integer year;
					Matcher matcher = descriptionPattern.matcher(description);
					if (matcher.matches() && matcher.group(1) != null) {
						year = Integer.parseInt(matcher.group(1));
					} else {
						throw new ResourceParseException(
								"Polygon description " + description + " did not end with a four-digit year value."
										+ " Instead, it ended with "
										+ description.substring(description.length() - 4, description.length())
						);
					}

					BecLookup becLookup = (BecLookup) control.get(ControlKey.BEC_DEF.name());
					BecDefinition bec = becLookup.get(becAlias)
							.orElseThrow(() -> new ResourceParseException(becAlias + " is not a recognized BEC alias"));

					if (FizCheck.fizCheck(fizId) == FizCheck.FIZ_UNKNOWN) {
						throw new ResourceParseException(
								"Forest Inventory Zone " + fizId
										+ " is not a recognized FIZ (only 'A' ... 'L' are supported)"
						);
					}

					return new VdypPolygon(
							description, year, bec, fizId, percentForestLand, inventoryTypeGroup, basalAreaGroup,
							fipMode.flatMap(FipMode::getByCode)
					);
				}

			};
		};
	}

}
