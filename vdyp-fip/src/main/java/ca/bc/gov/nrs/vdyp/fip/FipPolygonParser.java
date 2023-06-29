package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.io.parse.AbstractStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;

public class FipPolygonParser implements ControlMapSubResourceParser<StreamingParserFactory<FipPolygon>> {

	public static final String CONTROL_KEY = "FIP_POLYGONS"; 
	
	
	static final String POLYGON_IDENTIFIER = "POLYGON_IDENTIFIER"; // POLYDESC
	static final String FOREST_INVENTORY_ZONE = "FOREST_INVENTORY_ZONE"; // FIZ
	static final String BIOGEOGRAPHIC_ZONE = "BIOGEOGRAPHIC_ZONE"; // BEC
	static final String PERCENT_FOREST_LAND = "PERCENT_FOREST_LAND"; // PCTFLAND
	static final String FIP_MODE = "FIP_MODE";  // MODEfip
	static final String NONPRODUCTIVE_DESCRIPTION = "NONPRODUCTIVE_DESCRIPTION"; // NPDESC
	static final String YIELD_FACTOR = "YIELD_FACTOR"; // YLDFACT
	
	
	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

	@Override
	public StreamingParserFactory<FipPolygon> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {
		return ()->{
			var lineParser = new LineParser()
					.strippedString(25, POLYGON_IDENTIFIER)
					.space(1)
					.strippedString(1, FOREST_INVENTORY_ZONE)
					.space(1)
					.strippedString(4, BIOGEOGRAPHIC_ZONE)
					.space(1)
					.floating(4, PERCENT_FOREST_LAND)
					.space(1)
					.integer(2, FIP_MODE)
					.space(1)
					.string(5, NONPRODUCTIVE_DESCRIPTION)
					.space(1)
					.floating(5, YIELD_FACTOR);
			
			// TODO Default yield factor
			
			return new AbstractStreamingParser<FipPolygon>(is, lineParser, control) {

				@Override
				protected FipPolygon convert(Map<String, Object> entry) {
					var polygonId = (String) entry.get(POLYGON_IDENTIFIER);
					var fizId = (String) entry.get(FOREST_INVENTORY_ZONE);
					var becId = (String) entry.get(BIOGEOGRAPHIC_ZONE);
					var percentForestLand = (float) entry.get(PERCENT_FOREST_LAND);
					var fipMode = (int) entry.get(FIP_MODE);
					var nonproductiveDesc = (String) entry.get(NONPRODUCTIVE_DESCRIPTION);
					var yieldFactor = (float) entry.get(YIELD_FACTOR);
					
					return new FipPolygon(polygonId, fizId, becId, percentForestLand, fipMode, nonproductiveDesc, yieldFactor);
				}
				
			};
		};
	}

}
