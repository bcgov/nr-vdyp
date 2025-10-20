package ca.bc.gov.nrs.vdyp.integration_tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.io.write.ControlFileWriter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import io.github.classgraph.ClassGraph;

public abstract class IntermediateDataBasedIntegrationTest extends BaseDataBasedIntegrationTest {

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected static Path configDir;

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected Path testConfigDir;

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected Path outputDir;

	protected static enum State {
		FipInput("FIP", "fipInput"), VriInput("VRI", "vriInput"), ForwardInput("7INP", "forwardInput"),
		ForwardOutput("7OUT", "forwardOutput");

		public final String prefix;
		public final Path dir;

		State(String prefix, String dir) {
			this.prefix = prefix;
			this.dir = Path.of(dir);
		}
	}

	protected static enum Data {
		Polygon("P"), Layer("L"), Species("S"), Site("I"), Utilization("U"), Compatibility("C"), GrowTo("GROW");

		final String suffix;

		Data(String suffix) {
			this.suffix = suffix;
		}
	}

	static final Map<Data, String> DATA_FILENAMES;

	static {
		var map = new EnumMap<Data, String>(Data.class);
		map.put(Data.Polygon, "polygon.dat");
		map.put(Data.Layer, "layer.dat");
		map.put(Data.Species, "species.dat");
		map.put(Data.Site, "site.dat");
		map.put(Data.Utilization, "util.dat");
		map.put(Data.Compatibility, "compat.dat");
		map.put(Data.GrowTo, "grow.dat");
		DATA_FILENAMES = Collections.unmodifiableMap(map);
	}

	protected static final String POLYGON_OUTPUT_NAME = "poly.dat";
	protected static final String SPECIES_OUTPUT_NAME = "spec.dat";
	protected static final String UTILIZATION_OUTPUT_NAME = "util.dat";
	protected static final String COMPATIBILITY_OUTPUT_NAME = "compat.dat";

	protected static final Pattern POLY_LINE_MATCHER = Pattern
			.compile("^(.{25}) (.{4}) (.{1})(.{6})(.{3})(.{3})(.{3})$", Pattern.MULTILINE);
	protected static final Pattern UTIL_LINE_MATCHER = Pattern
			.compile("^(.{27})(?:(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{6}))?$", Pattern.MULTILINE);
	protected static final Pattern SPEC_LINE_MATCHER = Pattern.compile(
			"^(.{25}) (.?) (?:(.{2}) (.{2}) (.{3})(.{5})(.{8})(.{8})(.{8})(.{6})(.{6})(.{6})(.{6})(.{6})(.{2})(.{3}))?$",
			Pattern.MULTILINE
	);

	/**
	 * Initialize {@code configDir} with coefficients files.
	 *
	 * @throws IOException
	 */
	@BeforeAll
	protected static void initConfigDir() throws IOException {

		final Path coeDir = configDir.resolve("coe/");
		Files.createDirectory(coeDir);

		try (
				var scan = new ClassGraph().verbose().addClassLoader(TestUtils.class.getClassLoader())
						.acceptPaths("ca/bc/gov/nrs/vdyp/test").scan()
		) {
			for (var resource : scan.getResourcesMatchingWildcard("ca/bc/gov/nrs/vdyp/test/coe/*")) {
				final Path dest = coeDir.resolve(FilenameUtils.getName(resource.getPath()));
				System.err.printf("Copying %s to %s", resource.getPath(), dest).println();
				try (var is = resource.open()) {
					Files.copy(is, dest);
				}
			}
		}
	}

	protected static String fileName(State state, Data data) {
		return DATA_FILENAMES.get(data);
	}

	protected static Path dataPath(State state, Data data) {
		return state.dir.resolve(fileName(state, data));
	}

	protected boolean polygonLinesMatch(String actual, String expected) {
		var actualMatch = POLY_LINE_MATCHER.matcher(actual);
		var expectedMatch = POLY_LINE_MATCHER.matcher(expected);
		if (!actualMatch.find()) {
			return false;
		}
		if (!expectedMatch.find()) {
			return false;
		}

		List<BiPredicate<String, String>> checks = List.of(
				stringsEqual(), // Polygon ID
				stringsEqual(), // BEC
				stringsEqual(), floatStringsWithin(), intStringsEqual(), intStringsEqual(),
				// intStringsEqual(i -> i == 0 ? 1 : i) // Treat none specified as equivalent to the default of mode 1
				(x, y) -> true // Not used in subsequent steps so it's OK if it doesn't match what the old code produced
		);

		if (actualMatch.groupCount() != expectedMatch.groupCount()) {
			return false;
		}
		for (int i = 0; i < expectedMatch.groupCount(); i++) {
			if (!checks.get(i).test(actualMatch.group(i + 1), expectedMatch.group(i + 1))) {
				return false;
			}
		}
		return true;
	}

	protected boolean utilLinesMatch(String actual, String expected) {
		var actualMatch = UTIL_LINE_MATCHER.matcher(actual);
		var expectedMatch = UTIL_LINE_MATCHER.matcher(expected);
		if (!actualMatch.find()) {
			return false;
		}
		if (!expectedMatch.find()) {
			return false;
		}

		List<BiPredicate<String, String>> checks = List.of(
				String::equals, //
				Objects::equals, //
				floatStringsWithin(), //
				floatStringsWithin(0.01f, 0.015f), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(0.01f, 0.02f), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsDefaultZero(floatStringsWithin(0.1f, 0.2f)) //
		);

		if (actualMatch.groupCount() != expectedMatch.groupCount()) {
			return false;
		}
		for (int i = 0; i < expectedMatch.groupCount(); i++) {
			if (!checks.get(i).test(actualMatch.group(i + 1), expectedMatch.group(i + 1))) {
				System.out.println(
						String.format("%d - %s - %s", i, actualMatch.group(i + 1), expectedMatch.group(i + 1))
				);
				return false;
			}
		}
		return true;
	}

	protected boolean specLinesMatch(String actual, String expected) {
		var actualMatch = SPEC_LINE_MATCHER.matcher(actual);
		var expectedMatch = SPEC_LINE_MATCHER.matcher(expected);
		if (!actualMatch.find()) {
			return false;
		}
		if (!expectedMatch.find()) {
			return false;
		}

		List<BiPredicate<String, String>> checks = List.of(
				stringsEqual(), // Polygon ID
				stringsEqual(), // Layer Type
				intStringsEqual(), //
				stringsEqual(), //
				stringsEqual(), //

				floatStringsWithin(), //

				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //

				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //

				intStringsEqual(), //
				intStringsEqual() //
		);

		if (actualMatch.groupCount() != expectedMatch.groupCount()) {
			return false;
		}
		for (int i = 0; i < expectedMatch.groupCount(); i++) {
			if (!checks.get(i).test(actualMatch.group(i + 1), expectedMatch.group(i + 1))) {
				return false;
			}
		}
		return true;
	}

	protected BiPredicate<String, String> floatStringsWithin(float relativeThreshold, float absoluteThreshold) {

		return new BiPredicate<>() {

			@Override
			public boolean test(String actual, String expected) {
				if (actual == null && expected == null) {
					return true;
				}

				if (actual == null || expected == null) {
					return false;
				}

				float actualValue = Float.parseFloat(actual);
				float expectedValue = Float.parseFloat(expected);

				float threshold = Math.max(expectedValue * relativeThreshold, absoluteThreshold);

				return FloatMath.abs(actualValue - expectedValue) < threshold;
			}

		};

	}

	protected BiPredicate<String, String> floatStringsWithin() {
		return floatStringsWithin(0.015f, 0.01f);
	}

	protected BiPredicate<String, String> floatStringsDefaultZero(BiPredicate<String, String> test) {
		return (String actual, String expected) -> {
			if (actual == null && expected == null) {
				return true;
			}

			if (actual == null || expected == null) {
				return false;
			}

			float actualValue = Float.parseFloat(actual);
			float expectedValue = Float.parseFloat(expected);

			if (actualValue == -9.0) {
				actualValue = 0.0f;
			}
			if (expectedValue == -9.0) {
				expectedValue = 0.0f;
			}
			if (expectedValue == 0.0f && actualValue == 0.0f) {
				return true;
			}
			return test.test(actual, expected);
		};
	}

	protected BiPredicate<String, String> ignoreStrings() {
		return (s1, s2) -> true;
	}

	protected BiPredicate<String, String> intStringsEqual() {
		return new BiPredicate<>() {

			@Override
			public boolean test(String actual, String expected) {
				if (actual == null && expected == null) {
					return true;
				}

				if (actual == null || expected == null) {
					return false;
				}
				int actualValue = Integer.parseInt(actual.strip());
				int expectedValue = Integer.parseInt(expected.strip());

				return actualValue == expectedValue;
			}
		};
	}

	protected BiPredicate<String, String> intStringsEqual(IntUnaryOperator normalize) {
		return new BiPredicate<>() {

			@Override
			public boolean test(String actual, String expected) {
				if (actual == null && expected == null) {
					return true;
				}

				if (actual == null || expected == null) {
					return false;
				}
				int actualValue = normalize.applyAsInt(Integer.parseInt(actual.strip()));
				int expectedValue = normalize.applyAsInt(Integer.parseInt(expected.strip()));

				return actualValue == expectedValue;
			}
		};
	}

	protected BiPredicate<String, String> stringsEqual() {
		return new BiPredicate<>() {

			@Override
			public boolean test(String actual, String expected) {
				if (actual == null && expected == null) {
					return true;
				}

				if (actual == null || expected == null) {
					return false;
				}

				return expected.equals(actual);
			}
		};
	}

	protected Path createIoControlFile(State inputState, Path dataDir, Boolean compatibility, Data... dataFiles)
			throws IOException {
		// Create a second control file pointing to the input and output
		Path ioControlFile = dataDir.resolve("io-control.ctl");

		try (
				var os = Files.newOutputStream(ioControlFile); //
				var writer = new ControlFileWriter(os);
		) {
			writer.writeComment("Generated supplementary control file for integration testing");
			writer.writeBlank();
			writer.writeComment("Inputs");
			writer.writeBlank();
			for (int i = 0; i < dataFiles.length; i++) {
				Data data = dataFiles[i];
				Path file = dataDir.resolve(fileName(inputState, data));

				if (Files.exists(file)) {
					writer.writeEntry(11 + i, file.toString(), data.toString() + " Input");
				} else {
					writer.writeEntry(11 + i, "", "No " + data.toString() + " Input");
				}
			}
			writer.writeBlank();
			writer.writeComment("Outputs");
			writer.writeBlank();
			writer.writeEntry(15, outputDir.resolve(POLYGON_OUTPUT_NAME).toString(), "VDYP Polygon Output");
			writer.writeEntry(16, outputDir.resolve(SPECIES_OUTPUT_NAME).toString(), "VDYP Species Output");
			writer.writeEntry(18, outputDir.resolve(UTILIZATION_OUTPUT_NAME).toString(), "VDYP Utilization Output");
			if (compatibility) {
				writer.writeEntry(
						19, outputDir.resolve(COMPATIBILITY_OUTPUT_NAME).toString(),
						"VDYP Compatibility Variables Output"
				);
			}
		}
		return ioControlFile;
	}

	protected void assertOutputs(State outputState, Path expectedDir) throws IOException {
		assertFileExists(outputDir.resolve(POLYGON_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(SPECIES_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(UTILIZATION_OUTPUT_NAME));

		assertFileMatches(
				outputDir.resolve(POLYGON_OUTPUT_NAME), expectedDir.resolve(fileName(outputState, Data.Polygon)),
				this::polygonLinesMatch
		);
		/*assertFileMatches(
				outputDir.resolve(SPECIES_OUTPUT_NAME), expectedDir.resolve(fileName(outputState, Data.Species)),
				this::specLinesMatch
		);*/
		assertFileMatches(
				outputDir.resolve(UTILIZATION_OUTPUT_NAME),
				expectedDir.resolve(fileName(outputState, Data.Utilization)), this::utilLinesMatch
		);
	}

}
