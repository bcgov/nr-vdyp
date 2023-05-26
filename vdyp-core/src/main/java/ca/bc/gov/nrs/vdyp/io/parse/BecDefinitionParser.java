package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parser for a BEC Definition data file
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class BecDefinitionParser implements ResourceParser<Map<String, BecDefinition>> {

	public static final String CONTROL_KEY = "BEC_DEF";

	LineParser lineParser = new LineParser() {

		@Override
		public boolean isStopSegment(List<String> segments) {
			return "Z".equalsIgnoreCase(segments.get(2));
		}

	}.strippedString(4, "alias").space(1).value(1, "region", ValueParser.REGION).space(1).strippedString("name");

	@Override
	public Map<String, BecDefinition> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {
		Map<String, BecDefinition> result = new HashMap<>();
		result = lineParser.parse(is, result, (v, r) -> {
			String alias = (String) v.get("alias");
			Region region = (Region) v.get("region");
			String name = (String) v.get("name");
			var defn = new BecDefinition(alias, region, name);
			r.put(alias, defn);
			return r;
		});

		return Collections.unmodifiableMap(result);
	}

	/**
	 * Get all the BECs mapped by their aliases
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static Map<String, BecDefinition> getBecs(Map<String, Object> control) {
		return ResourceParser.expectParsedControl(control, CONTROL_KEY, Map.class);
	}

	/**
	 * Get all the BECs in the specified region
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static Collection<BecDefinition> getBecsByRegion(Map<String, Object> control, Region region) {
		return getBecs(control).values().stream().filter(bec -> bec.getRegion() == region).toList();
	}

	/**
	 * Get all the aliases for defined BECs
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static Collection<String> getBecAliases(Map<String, Object> control) {
		return getBecs(control).keySet();
	}

	/**
	 * Find a set of BECs for the given scope. If the scope is blank, that's all
	 * BECs, if it's a Region alias, it's all BECs for that region, otherwise its
	 * treated as a BEC alias and the BEC matching it is returned.
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @param scope   The scope to match
	 * @return A collection of matching BECs, or the empty set if none match.
	 */
	public static Collection<BecDefinition> getBecsByScope(Map<String, Object> control, String scope) {
		if (scope.isBlank()) {
			return getBecs(control).values();
		}
		return Region.fromAlias(scope).map(region -> getBecsByRegion(control, region))
				.orElseGet(() -> Utils.singletonOrEmpty(getBecs(control).get(scope)));
	}
}
