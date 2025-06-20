package ca.bc.gov.nrs.vdyp.ecore.io.read;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

class TestParamsReader {

	static List<Arguments> executionOptions() {
		var list = new ArrayList<Arguments>();

		list.add(Arguments.of("back", Parameters.ExecutionOption.BACK_GROW_ENABLED));
		list.add(Arguments.of("forward", Parameters.ExecutionOption.FORWARD_GROW_ENABLED));
		list.add(Arguments.of("v7save", Parameters.ExecutionOption.DO_SAVE_INTERMEDIATE_FILES));
		list.add(Arguments.of("includeprojmode", Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE));
		list.add(
				Arguments.of(
						"forceRefYear", Parameters.ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES
				)
		);
		list.add(
				Arguments
						.of("forceCrntYear", Parameters.ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)
		);
		list.add(Arguments.of("includefileheader", Parameters.ExecutionOption.DO_INCLUDE_FILE_HEADER));
		list.add(Arguments.of("includeagerows", Parameters.ExecutionOption.DO_INCLUDE_AGE_ROWS_IN_YIELD_TABLE));
		list.add(Arguments.of("includeyearrows", Parameters.ExecutionOption.DO_INCLUDE_YEAR_ROWS_IN_YIELD_TABLE));
		list.add(
				Arguments.of(
						"yieldtableincpolyid", Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE
				)
		);
		list.add(Arguments.of("projectedbypolygon", Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON));
		list.add(Arguments.of("projectedbylayer", Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER));
		list.add(Arguments.of("projectedbyspecies", Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION));
		list.add(Arguments.of("projectedvolumes", Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES));
		list.add(Arguments.of("projectedmofbiomass", Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_BIOMASS));
		list.add(Arguments.of("projectedcfsbiomass", Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS));
		list.add(Arguments.of("yldtblcolumnhdrs", Parameters.ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE));
		list.add(Arguments.of("allowbatphsub", Parameters.ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION));
		list.add(
				Arguments.of(
						"secondarySpcsHt",
						Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
				)
		);

		return list;
	}

	static List<Arguments> debugOptions() {
		var list = new ArrayList<Arguments>();

		list.add(Arguments.of("incDbgTimestamps", Parameters.DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS));
		list.add(Arguments.of("incDbgRoutineNames", Parameters.DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES));
		list.add(Arguments.of("incDbgEntryExit", Parameters.DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT));
		list.add(Arguments.of("incDbgIndentBlocks", Parameters.DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS));

		return list;
	}

	static List<Arguments> nonBooleanParameters() {
		var list = new ArrayList<Arguments>();

		list.add(Arguments.of("ofmt", "outputFormat", "CSVYieldTable", "YieldTable"));

		list.add(Arguments.of("agestart", "ageStart", "150", "200"));
		list.add(Arguments.of("ageend", "ageEnd", "200", "250"));

		list.add(Arguments.of("yearstart", "yearStart", "1950", "1960"));
		list.add(Arguments.of("yearend", "yearEnd", "2030", "2040"));

		list.add(Arguments.of("forceyear", "yearForcedIntoYieldTable", "1970", "1980"));

		list.add(Arguments.of("inc", "ageIncrement", "10", "5"));

		list.add(Arguments.of("combinerange", "combineAgeYearRange", "union", "intersect"));

		list.add(Arguments.of("progressFrequency", "progressFrequency", "10", "5"));

		list.add(Arguments.of("metadata", "metadataToOutput", "MAIN", "VERSION"));

		return list;
	}

	/*
	 *
	 *
	 * case "filterformaintainer": params.filters.setMaintainer(value); break; case "filterformapsheet":
	 * params.filters.setMapsheet(value); break; case "filterforpolygon": params.filters.setPolygon(value); break; case
	 * "filterforpolygonid": params.filters.setPolygonId(value); break;
	 */
	@ParameterizedTest
	@MethodSource("executionOptions")
	void testExecutionOptionYes(String flag, Parameters.ExecutionOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s Yes", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(params.getSelectedExecutionOptions(), contains(option.getValue()));
		assertThat(params.getExcludedExecutionOptions(), not(contains(option.getValue())));
	}

	@ParameterizedTest
	@MethodSource("executionOptions")
	void testExecutionOptionNo(String flag, Parameters.ExecutionOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s No", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(params.getSelectedExecutionOptions(), not(contains(option.getValue())));
		assertThat(
				params.getExcludedExecutionOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("executionOptions")
	void testExecutionOptionYesAndNo(String flag, Parameters.ExecutionOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s Yes", flag), String.format("-%s No", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(
				params.getSelectedExecutionOptions(), contains(option.getValue())

		);
		assertThat(
				params.getExcludedExecutionOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("executionOptions")
	void testExecutionOptionNoAndYes(String flag, Parameters.ExecutionOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s No", flag), String.format("-%s Yes", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(
				params.getSelectedExecutionOptions(), contains(option.getValue())

		);
		assertThat(
				params.getExcludedExecutionOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("debugOptions")
	void testDebugOptionYes(String flag, Parameters.DebugOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s Yes", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(params.getSelectedDebugOptions(), contains(option.getValue()));
		assertThat(params.getExcludedDebugOptions(), not(contains(option.getValue())));
	}

	@ParameterizedTest
	@MethodSource("debugOptions")
	void testDebugOptionNo(String flag, Parameters.DebugOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s No", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(params.getSelectedDebugOptions(), not(contains(option.getValue())));
		assertThat(
				params.getExcludedDebugOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("debugOptions")
	void testDebugOptionYesAndNo(String flag, Parameters.DebugOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s Yes", flag), String.format("-%s No", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(
				params.getSelectedDebugOptions(), contains(option.getValue())

		);
		assertThat(
				params.getExcludedDebugOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("debugOptions")
	void testDebugOptionNoAndYes(String flag, Parameters.DebugOption option) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s No", flag), String.format("-%s Yes", flag));

		ParamsReader.parseParameters(params, lines);

		assertThat(
				params.getSelectedDebugOptions(), contains(option.getValue())

		);
		assertThat(
				params.getExcludedDebugOptions(), contains(option.getValue())

		);
	}

	@ParameterizedTest
	@MethodSource("nonBooleanParameters")
	void testNonBooleanSimple(String flag, String property, String value) throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s %s", flag, value));

		ParamsReader.parseParameters(params, lines);

		assertThat(params, hasProperty(property, equalTo(value)));
	}

	@ParameterizedTest
	@MethodSource("nonBooleanParameters")
	void testNonBooleanSimpleChange(String flag, String property, String value1, String value2)
			throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(String.format("-%s %s", flag, value1), String.format("-%s %s", flag, value2));

		ParamsReader.parseParameters(params, lines);

		assertThat(params, hasProperty(property, equalTo(value2)));
	}

	@Test
	void testUtils() throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(
				String.format("-%s %s", "util", "A=7.5"), String.format("-%s %s", "util", "B=12.5"),
				String.format("-%s %s", "util", "A=22.5")
		);

		ParamsReader.parseParameters(params, lines);

		assertThat(
				params,
				hasProperty(
						"utils",
						Matchers.hasItem(
								both(hasProperty("speciesName", equalTo("A")))
										.and(hasProperty("utilizationClass", equalTo("7.5")))
						)
				)
		);
		assertThat(
				params,
				hasProperty(
						"utils",
						Matchers.hasItem(
								both(hasProperty("speciesName", equalTo("A")))
										.and(hasProperty("utilizationClass", equalTo("22.5")))
						)
				)
		);
		assertThat(
				params,
				hasProperty(
						"utils",
						Matchers.hasItem(
								both(hasProperty("speciesName", equalTo("B")))
										.and(hasProperty("utilizationClass", equalTo("12.5")))
						)
				)
		);

	}

	/*
	 * case "filterformaintainer": params.filters.setMaintainer(value); break; case "filterformapsheet":
	 * params.filters.setMapsheet(value); break; case "filterforpolygon": params.filters.setPolygon(value); break; case
	 * "filterforpolygonid": params.filters.setPolygonId(value); break;
	 */
	@Test
	void testFilters() throws ResourceParseException {
		var params = new Parameters();

		var lines = Stream.of(
				String.format("-%s %s", "filterformaintainer", "test"),
				String.format("-%s %s", "filterformapsheet", "42"), String.format("-%s %s", "filterforpolygon", "642"),
				String.format("-%s %s", "filterforpolygonid", "TEST 2003")
		);

		ParamsReader.parseParameters(params, lines);

		assertThat(params, hasProperty("filters", hasProperty("maintainer", equalTo("test"))));
		assertThat(params, hasProperty("filters", hasProperty("mapsheet", equalTo("42"))));
		assertThat(params, hasProperty("filters", hasProperty("polygon", equalTo("642"))));
		assertThat(params, hasProperty("filters", hasProperty("polygonId", equalTo("TEST 2003"))));

	}
}
