package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parser for a BEC Definition data file
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class BecDefinitionParser implements ControlMapSubResourceParser<BecLookup> {

	public static final String CONTROL_KEY = "BEC_DEF";

	/**
	 * Alias of the default BEC
	 */
	public static final String DEFAULT_BEC = "ESSF";

	LineParser lineParser = new LineParser() {

		@Override
		public boolean isStopSegment(List<String> segments) {
			return "Z".equalsIgnoreCase(segments.get(2));
		}

	}.strippedString(4, "alias").space(1).value(1, "region", ValueParser.REGION).space(1).strippedString("name");

	public static final Set<String> NON_GROWTH_BECS = Set.of("AT", "BG");
	public static final Set<String> NON_VOLUME_BECS = Set.of("BG");
	public static final Set<String> NON_DECAY_BECS = Set.of();

	@Override
	public BecLookup parse(InputStream is, Map<String, Object> control) throws IOException, ResourceParseException {
		List<BecDefinition> result = new ArrayList<>(16);
		result = lineParser.parse(is, result, (v, r) -> {
			String alias = (String) v.get("alias");
			Region region = (Region) v.get("region");
			String name = (String) v.get("name");
			var defn = new BecDefinition(alias, region, name);
			r.add(defn);

			return r;
		}, control);
		var defaultBec = result.stream().filter(bec -> bec.getAlias().equals(DEFAULT_BEC)).findAny()
				.orElseThrow(() -> new IllegalStateException("Could not find default BEC " + DEFAULT_BEC));
		if (NON_GROWTH_BECS.contains(defaultBec.getAlias())) {
			throw new IllegalStateException("Default BEC " + defaultBec + " is not a growth BEC.");
		}
		if (NON_DECAY_BECS.contains(defaultBec.getAlias())) {
			throw new IllegalStateException("Default BEC " + defaultBec + " is not a decay BEC.");
		}
		if (NON_VOLUME_BECS.contains(defaultBec.getAlias())) {
			throw new IllegalStateException("Default BEC " + defaultBec + " is not a volume BEC.");
		}

		// Map the BECs so they know what their contextual alternate BECs are
		result = result.stream().map(baseBec -> {
			var isGrowth = !NON_GROWTH_BECS.contains(baseBec.getAlias());
			var isDecay = !NON_VOLUME_BECS.contains(baseBec.getAlias());
			var isVolume = !NON_DECAY_BECS.contains(baseBec.getAlias());
			if (isGrowth && isDecay && isVolume) {
				return baseBec;
			}
			return new BecDefinition(
					baseBec, defaultBec, !NON_GROWTH_BECS.contains(baseBec.getAlias()),
					!NON_VOLUME_BECS.contains(baseBec.getAlias()), !NON_DECAY_BECS.contains(baseBec.getAlias())
			);
		}).toList();
		return new BecLookup(result);
	}

	/**
	 * Get all the BECs mapped by their aliases
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static BecLookup getBecs(Map<String, Object> control) {
		return Utils.expectParsedControl(control, CONTROL_KEY, BecLookup.class);
	}

	/**
	 * Get all the BECs in the specified region
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static Collection<BecDefinition> getBecsByRegion(Map<String, Object> control, Region region) {
		return getBecs(control).getBecsForRegion(region);
	}

	/**
	 * Get all the aliases for defined BECs
	 *
	 * @param control Control map containing the parsed BEC definitions.
	 * @return
	 */
	public static Collection<String> getBecAliases(Map<String, Object> control) {
		return getBecs(control).getBecs().stream().map(BecDefinition::getAlias).toList();
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
		return getBecs(control).getBecsForScope(scope);
	}

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}
}
