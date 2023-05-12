package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for a Volume Equation Group data file
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class VolumeEquationGroupParser implements ResourceParser<Map<String, Map<String, Integer>>>{
	
	LineParser lineParser = new LineParser() 
		.strippedString(2, "sp0Alias")
		.space(1)
		.strippedString(4, "becAlias")
		.space(1)
		.integer(3,"vgrpId");
	
	@Override
	public Map<String, Map<String, Integer>> parse(InputStream is) throws IOException, ResourceParseException {
		Map<String, Map<String, Integer>> result = new HashMap<>();
		result = lineParser.parse(is, result, (v, r)->{
			String sp0Alias = (String) v.get("sp0Alias");
			String becAlias = (String) v.get("becAlias");
			int vgrpId = (Integer) v.get("vgrpId");
			
			r.computeIfAbsent(sp0Alias, k->new HashMap<>()).put(becAlias, vgrpId);
			return r;
		});
		
		for (var e : result.entrySet()) {
			result.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
		}
		return Collections.unmodifiableMap(result);
	}
	
}
