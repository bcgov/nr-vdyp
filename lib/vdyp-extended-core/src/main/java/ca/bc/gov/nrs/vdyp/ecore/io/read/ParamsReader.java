package ca.bc.gov.nrs.vdyp.ecore.io.read;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.FilterParameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.common.RuntimeResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

/**
 * Tool to read the VDYP7 params.txt file format and produce a Parameters object from it
 */
public class ParamsReader {

	static final Pattern PARAM_LINE_MATCHER = Pattern.compile("^\\-(\\w+)\\s+(\\S.*)$");

	Parameters loadParametersFile(Path paramFile) throws IOException, ResourceParseException {
		Parameters params = new Parameters();

		try (var in = Files.newBufferedReader(paramFile); var lines = in.lines();) {
			parseParameters(params, lines);
		}

		return params;
	}

	private static void setExecutionOption(Parameters params, Parameters.ExecutionOption option, String value)
			throws ResourceParseException {
		try {
			boolean parsedValue = ValueParser.LOGICAL_PERMISSIVE.parse(value);
			if (parsedValue) {
				params.addSelectedExecutionOptionsItem(option);
			} else {
				params.addExcludedExecutionOptionsItem(option);
			}
		} catch (ValueParseException e) {
			throw new ResourceParseException("unexpected value \"" + value + "\" for " + option, e);
		}
	}

	private static void setDebugOption(Parameters params, Parameters.DebugOption option, String value)
			throws ResourceParseException {
		try {
			boolean parsedValue = ValueParser.LOGICAL_PERMISSIVE.parse(value);
			if (parsedValue) {
				params.addSelectedDebugOptionsItem(option);
			} else {
				params.addExcludedDebugOptionsItem(option);
			}
		} catch (ValueParseException e) {
			throw new ResourceParseException("unexpected value \"" + value + "\" for " + option, e);
		}
	}

	/**
	 * Parse parameters file to map. Each keyed by a flag with a list of a ll the values it has been invoked with in
	 * order as the value.
	 *
	 * @param parsed Map to add the parsed values to
	 * @param lines  Stream of parameter file lines
	 * @throws ResourceParseException
	 */
	public static void parseParameters(Map<String, List<String>> parsed, Stream<String> lines)
			throws ResourceParseException {
		try {
			lines.filter(line -> !line.startsWith("#") && !line.isBlank()).forEach(line -> {
				try {
					var matcher = PARAM_LINE_MATCHER.matcher(line);
					if (!matcher.find()) {
						throw new ResourceParseException(
								"Parameter file line does not match expected form: " + System.lineSeparator() + line
						);
					}
					String flag = matcher.group(1);
					String value = matcher.group(2);
					var list = Utils.<List<String>>optSafe(parsed.get(flag)).orElseGet(() -> new ArrayList<>());
					list.add(value.trim());
					parsed.put(flag, list);

				} catch (ResourceParseException e) {
					throw new RuntimeResourceParseException(e);
				}
			});
		} catch (RuntimeResourceParseException e) {
			throw new ResourceParseException(e);
		}
	}

	/**
	 * Takes a map of the flags in a parameters file and their values and fills in a Parameters object
	 *
	 * @param params parameters object to fill in
	 * @param map    Map parsed from a parameters file
	 * @throws ResourceParseException
	 */
	public static void parseParameters(Parameters params, Map<String, List<String>> map) throws ResourceParseException {

		if (params.getFilters() == null) {
			params.setFilters(new FilterParameters());
		}

		for (var entry : map.entrySet()) {
			String flag = entry.getKey();
			for (String value : entry.getValue()) {
				switch (flag.toLowerCase()) {
				case "agestart":
					params.setAgeStart(value);
					break;
				case "ageend":
					params.setAgeEnd(value);
					break;
				case "yearstart":
					params.setYearStart(value);
					break;
				case "yearend":
					params.setYearEnd(value);
					break;
				case "forceyear":
					params.setYearForcedIntoYieldTable(value);
					break;
				case "inc":
					params.setAgeIncrement(value);
					break;
				case "combinerange":
					params.setCombineAgeYearRange(value);
					break;
				case "progressfrequency":
					params.setProgressFrequency(value);
					break;
				case "metadata":
					params.setMetadataToOutput(value);
					break;

				case "filterformaintainer":
					params.filters.setMaintainer(value);
					break;
				case "filterformapsheet":
					params.filters.setMapsheet(value);
					break;
				case "filterforpolygon":
					params.filters.setPolygon(value);
					break;
				case "filterforpolygonid":
					params.filters.setPolygonId(value);
					break;

				case "util":
					var util = new UtilizationParameter();
					var split = value.split("=");
					util.setSpeciesName(split[0]);
					String uClass = split[1];
					// in VDYP8 classes other than Excl have + on the end
					if (!uClass.equalsIgnoreCase("Excl") && !uClass.endsWith("+")) {
						uClass = String.format("%s+", uClass);
					}
					util.setUtilizationClass(uClass);
					params.addUtilsItem(util);
					break;

				case "ifmt":
					// Do Nothing
					break;
				case "ofmt":
					if (value != null && !value.isBlank())
						try {
							// Try to normalize the value
							params.setOutputFormat(OutputFormat.fromValue(value).getValue());
						} catch (IllegalArgumentException ex) {
							params.setOutputFormat(value);
						}
					break;
				case "back":
					setExecutionOption(params, Parameters.ExecutionOption.BACK_GROW_ENABLED, value);
					break;
				case "forward":
					setExecutionOption(params, Parameters.ExecutionOption.FORWARD_GROW_ENABLED, value);
					break;
				case "v7save":
					setExecutionOption(params, Parameters.ExecutionOption.DO_SAVE_INTERMEDIATE_FILES, value);
					break;
				case "includeprojmode":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE, value
					);
					break;
				case "forcerefyear":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES, value
					);
					break;
				case "forcecrntyear":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES, value
					);
					break;
				case "includefileheader":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_FILE_HEADER, value);
					break;
				case "includeagerows":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE, value);
					break;
				case "includeyearrows":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE, value);
					break;
				case "yieldtableincpolyid":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, value
					);
					break;
				case "projectedbypolygon":
					// Not sure of this mapping
					setExecutionOption(params, Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON, value);
					break;
				case "projectedbylayer":
					setExecutionOption(params, Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, value);
					break;
				case "projectedbyspecies":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION, value);
					break;
				case "projectedvolumes":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, value);
					break;
				case "projectedmofbiomass":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS, value);
					break;
				case "projectedcfsbiomass":
					setExecutionOption(params, Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS, value);
					break;
				case "yldtblcolumnhdrs":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE, value
					);
					break;
				case "allowbatphsub":
					setExecutionOption(
							params, Parameters.ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION, value
					);
					break;
				case "secondaryspcsht":
					setExecutionOption(
							params,
							Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE,
							value
					);
					break;
				case "incdbgtimestamps":
					setDebugOption(params, Parameters.DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS, value);
					break;
				case "incdbgroutinenames":
					setDebugOption(params, Parameters.DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES, value);
					break;
				case "incdbgentryexit":
					setDebugOption(params, Parameters.DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT, value);
					break;
				case "incdbgindentblocks":
					setDebugOption(params, Parameters.DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS, value);
					break;
				/*
				 * These don't seem to have a counterpart in the old style params file setExecutionOption( params,
				 * Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES, value ); setExecutionOption( params,
				 * Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, value ); setExecutionOption( params,
				 * Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, value ); setExecutionOption( params,
				 * Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, value ); setExecutionOption( params,
				 * Parameters.ExecutionOption.DO_DELAY_EXECUTION_FOLDER_DELETION, value ); break;
				 */

				default:
					// do nothing
					break;
				}
			}
		}
	}

	/**
	 * Fills in a Parameters object from a parameters file
	 *
	 * @param params parameters object to fill in
	 * @param lines  Stream of the lines in the parameters file
	 * @throws ResourceParseException
	 */
	public static void parseParameters(Parameters params, Stream<String> lines) throws ResourceParseException {
		Map<String, List<String>> map = new HashMap<>();

		parseParameters(map, lines);
		parseParameters(params, map);
	}

}
