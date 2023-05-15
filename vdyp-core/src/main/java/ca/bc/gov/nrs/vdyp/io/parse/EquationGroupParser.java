package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.SP0Definition;

/**
 * Parser for a Volume Equation Group data file
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class EquationGroupParser implements ResourceParser<Map<String, Map<String, Integer>>>{
	
	public static final String VOLUME_CONTROL_KEY = "VOLUME_EQN_GROUPS";

	public static final String DECAY_CONTROL_KEY = "DECAY_GROUPS";

	public static final String BREAKAGE_CONTROL_KEY = "BREAKAGE_GROUPS";
	
	LineParser lineParser = new LineParser() 
		.strippedString(2, "sp0Alias")
		.space(1)
		.strippedString(4, "becAlias")
		.space(1)
		.integer(3,"vgrpId");
	
	@Override
	public Map<String, Map<String, Integer>> parse(InputStream is, Map<String, Object> control) throws IOException, ResourceParseException {
		
		@SuppressWarnings("unchecked")
		List<SP0Definition> sp0List = ResourceParser.expectParsedControl(control, SP0DefinitionParser.CONTROL_KEY, List.class);
		@SuppressWarnings("unchecked")
		Map<String, BecDefinition> becMap = ResourceParser.expectParsedControl(control, BecDefinitionParser.CONTROL_KEY, Map.class);
		
		Map<String, Map<String, Integer>> result = new HashMap<>();
		result = lineParser.parse(is, result, (v, r)->{
			final String sp0Alias = (String) v.get("sp0Alias");
			final String becAlias = (String) v.get("becAlias");
			
			if(!sp0List.stream().anyMatch(def->def.getAlias().equalsIgnoreCase(sp0Alias))) {
				throw new ValueParseException(sp0Alias, sp0Alias+" is not an SP0 identifier");
			}
			if(!becMap.containsKey(becAlias)) {
				throw new ValueParseException(becAlias, becAlias+" is not a BEC identifier");
			}
						
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
