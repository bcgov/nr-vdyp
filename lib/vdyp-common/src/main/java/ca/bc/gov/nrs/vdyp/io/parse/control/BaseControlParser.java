package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.FailoverFileResolver;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DebugSettingsParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

public abstract class BaseControlParser {

	private static final Logger logger = LoggerFactory.getLogger(BaseControlParser.class);

	protected static final ValueParser<String> FILENAME = String::strip;

	public static final String MINIMUM_HEIGHT = "MINIMUM_HEIGHT";
	public static final String MINIMUM_BASE_AREA = "MINIMUM_BASE_AREA";
	public static final String MINIMUM_PREDICTED_BASE_AREA = "MINIMUM_PREDICTED_BASE_AREA";
	public static final String MINIMUM_FULLY_STOCKED_AREA = "MINIMUM_FULLY_STOCKED_BASE_AREA";
	public static final String MINIMUM_VETERAN_HEIGHT = "MINIMUM_VETERAN_HEIGHT";

	public static final float DEFAULT_MINIMUM_VETERAN_HEIGHT = 10.0f;

	@SuppressWarnings("unchecked")
	protected static List<ControlMapValueReplacer<Object, String>>
			inputParserList(@SuppressWarnings("rawtypes") ControlMapValueReplacer... inputParsers) {

		return Arrays.asList(inputParsers);
	}

	protected ControlFileParser controlParser = new ControlFileParser();

	/**
	 * This method is to be called after the concrete Control Parsers are initialized. This can be from the constructors
	 * of those classes, after initialization is complete.
	 */
	protected void initialize() {

		controlParser.record(ControlKey.MAX_NUM_POLY, ValueParser.INTEGER);

		inputFileParsers().forEach(
				subResourceParser -> controlParser
						.record(subResourceParser.getControlKey(), subResourceParser.getValueParser())
		);

		outputFileParsers().forEach(key -> controlParser.record(key, ValueParser.FILENAME));

		configurationFileParsers().forEach(
				subResourceParser -> controlParser
						.record(subResourceParser.getControlKey(), subResourceParser.getValueParser())
		);

		controlParser.record(ControlKey.MINIMA, minimaParser());

		controlParser.record(ControlKey.DEBUG_SWITCHES, new DebugSettingsParser()); // IPSJF155
	}

	protected abstract ValueParser<Map<String, Float>> minimaParser();

	protected abstract List<ControlMapValueReplacer<Object, String>> inputFileParsers();

	protected abstract List<ControlKey> outputFileParsers();

	protected abstract List<ResourceControlMapModifier> configurationFileParsers();

	protected void applyModifiers(
			Map<String, Object> control, List<? extends ControlMapModifier> modifiers,
			Map<String, FileResolver> fileResolverContext
	) throws ResourceParseException, IOException {
		for (var modifier : modifiers) {
			modifier.modify(control, fileResolverContext);
		}
	}

	public Map<String, Object> parse(InputStream is, FileResolver fileResolver, Map<String, Object> map)
			throws IOException, ResourceParseException {
		return parse(List.of(is), fileResolver, map);
	}

	public Map<String, Object> parse(List<InputStream> resources, FileResolver fileResolver, Map<String, Object> map)
			throws IOException, ResourceParseException {

		Map<String, FileResolver> resolverContext = new HashMap<>();
		for (var is : resources) {
			var newEntries = controlParser.parse(is, map);
			map.putAll(newEntries);
			for (var changedKey : newEntries.keySet()) {
				resolverContext.put(changedKey, fileResolver);
			}
		}

		resolverContext.put(null, fileResolver); // Default file resolver

		applyAllModifiers(map, resolverContext);

		return map;
	}

	public Map<String, Object>
			parseByName(List<String> resourceNames, FileResolver fileResolver, Map<String, Object> map)
					throws IOException, ResourceParseException {

		Map<String, FileResolver> resolverContext = new HashMap<>();
		for (var resourceName : resourceNames) {
			logger.info("Resolving and parsing {}", resourceName);
			try (var is = fileResolver.resolveForInput(resourceName)) {

				var newEntries = controlParser.parse(is, map);
				map.putAll(controlParser.parse(is, map));
				for (var changedKey : newEntries.keySet()) {
					resolverContext.put(
							changedKey,
							new FailoverFileResolver(fileResolver.relativeToParent(resourceName), fileResolver)
					);
				}
			}
		}

		applyAllModifiers(map, resolverContext);

		return map;
	}

	protected abstract void applyAllModifiers(Map<String, Object> map, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException;

	protected abstract VdypApplicationIdentifier getProgramId();
}
