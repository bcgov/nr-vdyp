package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.controlmap.SharedControlMapValues;
import ca.bc.gov.nrs.vdyp.io.FailoverFileResolver;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DebugSettingsParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public abstract class BaseControlParser<S extends DebugSettings> {

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

		outputFiles().forEach(
				outputFileLocationResolver -> controlParser
						.record(outputFileLocationResolver.getControlKey(), outputFileLocationResolver.getValueParser())
		);

		configurationFileParsers().forEach(
				subResourceParser -> controlParser
						.record(subResourceParser.getControlKey(), subResourceParser.getValueParser())
		);

		controlParser.record(ControlKey.MINIMA, minimaParser());

		controlParser.record(ControlKey.DEBUG_SWITCHES, getDebugSettingsParser()); // IPSJF155
	}

	protected abstract DebugSettingsParser<S> getDebugSettingsParser();

	protected abstract ValueParser<Map<String, Float>> minimaParser();

	protected abstract List<ControlMapValueReplacer<Object, String>> inputFileParsers();

	protected abstract List<OutputFileLocationResolver> outputFiles();

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

		Map<String, FileResolver> resolverContext = readControlFiles(resourceNames, fileResolver, map);

		applyAllModifiers(map, resolverContext);

		return map;
	}

	/**
	 * Parse the given control files and apply only the shared modifiers: those that read definition and coefficient
	 * files. The result does not depend on the input or output files of a particular run, so it can be built once and
	 * reused by many runs through {@link #parseForRun}. Entries for the run specific modifiers (input and output files)
	 * are left as the raw values read from the control files.
	 *
	 * @param resourceNames control files to read, in order; later files override earlier ones
	 * @param fileResolver  resolver used to locate the control files and, relative to them, the files they name
	 * @return an unmodifiable control map whose values are read only (see {@link SharedControlMapValues})
	 */
	public Map<String, Object> parseSharedByName(List<String> resourceNames, FileResolver fileResolver)
			throws IOException, ResourceParseException {

		Map<String, Object> map = new HashMap<>();
		Map<String, FileResolver> resolverContext = readControlFiles(resourceNames, fileResolver, map);

		applySharedModifiers(map, resolverContext);

		// The map and its parsed definitions and coefficients will be shared by every run, so make them read only
		SharedControlMapValues.makeReadOnly(map);

		return Collections.unmodifiableMap(map);
	}

	/**
	 * Create the control map for a single run from a map produced by {@link #parseSharedByName}. The shared map is
	 * copied, not modified, and the parsed definitions and coefficients it holds are shared by reference, so they must
	 * not be modified by the run. The run control files are applied over the copy, then the run specific modifiers
	 * (input and output files) are applied, resolving file names with {@code runFileResolver}.
	 *
	 * @param sharedMap           a map produced by {@link #parseSharedByName} on a parser of the same class
	 * @param runControlFileNames additional control files for this run only, resolved with {@code runFileResolver}
	 * @param runFileResolver     resolver for the input and output files of this run
	 * @return a new, modifiable control map for the run
	 * @throws IllegalArgumentException if a run control file sets an entry that is handled by a shared modifier
	 */
	public Map<String, Object>
			parseForRun(Map<String, Object> sharedMap, List<String> runControlFileNames, FileResolver runFileResolver)
					throws IOException, ResourceParseException {

		Map<String, Object> map = new HashMap<>(sharedMap);
		Map<String, FileResolver> resolverContext = new HashMap<>();
		Set<String> sharedKeys = sharedModifiers().stream().map(ControlMapModifier::getControlKeyName)
				.collect(Collectors.toSet());

		for (var resourceName : runControlFileNames) {
			logger.debug("Resolving and parsing run control file {}", resourceName);
			try (var is = runFileResolver.resolveForInput(resourceName)) {
				var newEntries = controlParser.parseExplicitEntries(is, map);
				for (var key : newEntries.keySet()) {
					if (sharedKeys.contains(key)) {
						throw new IllegalArgumentException(
								"Run control file " + resourceName + " sets " + key
										+ ", which is read once and shared between runs; it must be set in the shared control files"
						);
					}
					resolverContext.put(
							key,
							new FailoverFileResolver(runFileResolver.relativeToParent(resourceName), runFileResolver)
					);
				}
				map.putAll(newEntries);
			}
		}

		resolverContext.put(null, runFileResolver); // Default file resolver

		applyRunModifiers(map, resolverContext);

		return map;
	}

	private Map<String, FileResolver>
			readControlFiles(List<String> resourceNames, FileResolver fileResolver, Map<String, Object> map)
					throws IOException, ResourceParseException {
		Map<String, FileResolver> resolverContext = new HashMap<>();
		for (var resourceName : resourceNames) {
			logger.info("Resolving and parsing {}", resourceName);
			try (var is = fileResolver.resolveForInput(resourceName)) {

				var newEntries = controlParser.parse(is, map);
				map.putAll(newEntries);
				for (var changedKey : newEntries.keySet()) {
					resolverContext.put(
							changedKey,
							new FailoverFileResolver(fileResolver.relativeToParent(resourceName), fileResolver)
					);
				}
			}
		}
		return resolverContext;
	}

	/**
	 * Apply all modifiers: the shared modifiers, then the run specific modifiers.
	 */
	protected void applyAllModifiers(Map<String, Object> map, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException {
		applySharedModifiers(map, fileResolverContext);
		applyRunModifiers(map, fileResolverContext);
	}

	/**
	 * The modifiers that read definition and coefficient files, in the order they must be applied. Their results do not
	 * depend on the files of a particular run.
	 */
	protected abstract List<? extends ControlMapModifier> sharedModifiers();

	protected void applySharedModifiers(Map<String, Object> map, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException {
		applyModifiers(map, sharedModifiers(), fileResolverContext);
	}

	/**
	 * Apply the modifiers that depend on the files of a particular run: the input file parsers and the output file
	 * locations. These run after the shared modifiers, so they may read the parsed definitions.
	 */
	protected void applyRunModifiers(Map<String, Object> map, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException {
		applyModifiers(map, inputFileParsers(), fileResolverContext);
		applyModifiers(map, outputFiles(), fileResolverContext);
	}

	protected abstract VdypApplicationIdentifier getProgramId();
}
