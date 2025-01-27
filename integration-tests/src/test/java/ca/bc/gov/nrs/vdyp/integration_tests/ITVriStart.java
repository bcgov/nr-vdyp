package ca.bc.gov.nrs.vdyp.integration_tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.write.ControlFileWriter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.vri.VriStart;
import ca.bc.gov.nrs.vdyp.vri.model.VriLayer;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;
import ca.bc.gov.nrs.vdyp.vri.model.VriSpecies;
import io.github.classgraph.ClassGraph;

class ITVriStart {

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	static Path configDir;

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	static Path testDataDir;

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	Path outputDir;

	static private Path baseControlFile;
	private Path ioControlFile;

	static Path copyResource(Class<?> klazz, String path, Path destination) throws IOException {
		Path result = destination.resolve(path);

		try (var is = klazz.getResourceAsStream(path)) {
			Files.copy(is, result);
		}

		return result;

	}

	private static final String POLYGON_OUTPUT_NAME = "vri_poly.dat";
	private static final String SPECIES_OUTPUT_NAME = "vri_spec.dat";
	private static final String UTILIZATION_OUTPUT_NAME = "vri_util.dat";

	private static final String POLYGON_EXPECTED_NAME = "P-SAVE_VDYP7_7INPP.dat";
	private static final String SPECIES_EXPECTED_NAME = "P-SAVE_VDYP7_7INPS.dat";
	private static final String UTILIZATION_EXPECTED_NAME = "P-SAVE_VDYP7_7INPU.dat";

	private static final String POLYGON_INPUT_NAME = "P-SAVE_VDYP7_VRIP.dat";
	private static final String LAYER_INPUT_NAME = "P-SAVE_VDYP7_VRIL.dat";
	private static final String SPECIES_INPUT_NAME = "P-SAVE_VDYP7_VRIS.dat";
	private static final String SITE_INPUT_NAME = "P-SAVE_VDYP7_VRII.dat";

	static final Pattern UTIL_LINE_MATCHER = Pattern
			.compile("^(.{27})(?:(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{9})(.{6}))?$", Pattern.MULTILINE);
	static final Pattern SPEC_LINE_MATCHER = Pattern
			.compile(
					"^(.{25}) (.?) (?:(.{2}) (.{2}) (.{3})(.{5})(.{8})(.{8})(.{8})(.{6})(.{6})(.{6})(.{6})(.{6})(.{2})(.{3}))?$",
					Pattern.MULTILINE
			);

	@BeforeAll
	static void init() throws IOException {
		baseControlFile = copyResource(TestUtils.class, "VRISTART.CTR", configDir);

		final Path coeDir = configDir.resolve("coe/");
		Files.createDirectory(coeDir);

		try (
				var scan = new ClassGraph().verbose()
						.addClassLoader(TestUtils.class.getClassLoader())
						.addClassLoader(ITVriStart.class.getClassLoader())
						.acceptPaths("ca/bc/gov/nrs/vdyp/test")
						.acceptPaths("ca/bc/gov/nrs/vdyp/integration_tests")
						.scan()
		) {
			for (var resource : scan.getResourcesMatchingWildcard("ca/bc/gov/nrs/vdyp/test/coe/*")) {
				final Path dest = coeDir.resolve(FilenameUtils.getName(resource.getPath()));
				System.err.printf("Copying %s to %s", resource.getPath(), dest).println();
				try (var is = resource.open()) {
					Files.copy(is, dest);
				}
			}
			for (var resource : scan.getResourcesMatchingWildcard("ca/bc/gov/nrs/vdyp/integration_tests/**")) {

				var localPath = resource.getPath().substring("ca/bc/gov/nrs/vdyp/integration_tests/".length());

				if (resource.getPath().endsWith("class"))
					continue;

				final Path dest = testDataDir.resolve(localPath);

				Files.createDirectories(dest.getParent());

				System.err.printf("Copying %s to %s", resource.getPath(), dest).println();
				try (var is = resource.open()) {
					Files.copy(is, dest);
				}
			}
		}

	}

	static Stream<String> testNameProvider() throws IOException {
		return Files.list(testDataDir).map(p -> p.getFileName().toString());
	}

	@ParameterizedTest
	@MethodSource("testNameProvider")
	void test(String test) throws IOException, ResourceParseException, ProcessingException {
		Path dataDir = testDataDir.resolve(test).resolve("vriInput");
		Path expectedDir = testDataDir.resolve(test).resolve("forwardInput");

		// Create a second control file pointing to the input and output
		ioControlFile = dataDir.resolve("vri.ctr");

		try (
				var os = Files.newOutputStream(ioControlFile); //
				var writer = new ControlFileWriter(os);
		) {
			writer.writeComment("Generated supplementarty control file for integration testing");
			writer.writeBlank();
			writer.writeComment("Inputs");
			writer.writeBlank();
			writer.writeEntry(11, dataDir.resolve(POLYGON_INPUT_NAME).toString(), "VRI Polygon Input");
			writer.writeEntry(12, dataDir.resolve(LAYER_INPUT_NAME).toString(), "VRI Layer Input");
			writer.writeEntry(13, dataDir.resolve(SITE_INPUT_NAME).toString(), "VRI Site Input");
			writer.writeEntry(14, dataDir.resolve(SPECIES_INPUT_NAME).toString(), "VRI Species Input");
			writer.writeBlank();
			writer.writeComment("Outputs");
			writer.writeBlank();
			writer.writeEntry(15, outputDir.resolve(POLYGON_OUTPUT_NAME).toString(), "VRI Polygon Output");
			writer.writeEntry(16, outputDir.resolve(SPECIES_OUTPUT_NAME).toString(), "VRI Species Output");
			writer.writeEntry(18, outputDir.resolve(UTILIZATION_OUTPUT_NAME).toString(), "VRI Utilization Output");
		}

		try (VdypStartApplication<VriPolygon, VriLayer, VriSpecies, VriSite> app = new VriStart();) {

			var resolver = new FileSystemFileResolver(configDir);

			app.init(resolver, baseControlFile.toString(), ioControlFile.toString());

			app.process();
		}

		assertFileExists(outputDir.resolve(POLYGON_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(SPECIES_OUTPUT_NAME));
		assertFileExists(outputDir.resolve(UTILIZATION_OUTPUT_NAME));

		assertFileMatches(
				outputDir.resolve(POLYGON_OUTPUT_NAME), expectedDir.resolve(POLYGON_EXPECTED_NAME), String::equals
		);
		assertFileMatches(
				outputDir.resolve(SPECIES_OUTPUT_NAME), expectedDir.resolve(SPECIES_EXPECTED_NAME),
				this::specLinesMatch
		);
		assertFileMatches(
				outputDir.resolve(UTILIZATION_OUTPUT_NAME), expectedDir.resolve(UTILIZATION_EXPECTED_NAME),
				this::utilLinesMatch
		);

	}

	public void assertFileExists(Path path) {
		assertTrue(Files.exists(path), path + " does not exist");
	}

	public void assertFileMatches(Path testPath, Path expectedPath, BiPredicate<String, String> compare)
			throws IOException {

		try (
				var testStream = Files.newBufferedReader(testPath); //
				var expectedStream = Files.newBufferedReader(expectedPath);
		) {
			assertFileMatches(testPath.toString(), expectedPath.toString(), testStream, expectedStream, compare);
		}
	}

	public void assertFileMatches(
			Path testPath, Class<?> expectedClass, String expectedPath, BiPredicate<String, String> compare
	) throws IOException {

		try (
				var testStream = Files.newBufferedReader(testPath); //
				var expectedStream = new BufferedReader(
						new InputStreamReader(expectedClass.getResourceAsStream(expectedPath))
				);
		) {
			assertFileMatches(testPath.toString(), expectedPath, testStream, expectedStream, compare);
		}
	}

	public void assertFileMatches(
			String testPath, String expectedPath, BufferedReader testStream, BufferedReader expectedStream,
			BiPredicate<String, String> compare
	) throws IOException {

		for (int i = 1; true; i++) {
			String testLine = testStream.readLine();
			String expectedLine = expectedStream.readLine();

			if (testLine == null && expectedLine == null) {
				return;
			}
			if (testLine == null) {
				fail(
						"File " + testPath + " did not match " + expectedPath
								+ ". Missing expected lines. The first missing line (" + i + ") was:\n" + expectedLine
				);
			}
			if (expectedLine == null) {
				fail(
						"File " + testPath + " did not match " + expectedPath
								+ ". Unexpected lines at the end. The first unexpected line (" + i + ") was:\n"
								+ testLine
				);
			}

			if (!compare.test(testLine, expectedLine)) {
				fail(
						"File " + testPath + " did not match " + expectedPath + ". The first line (" + i
								+ ") to not match was: \n [Expected]: " + expectedLine + "\n   [Actual]: " + testLine
				);
			}

		}
	}

	boolean utilLinesMatch(String actual, String expected) {
		var actualMatch = UTIL_LINE_MATCHER.matcher(actual);
		var expectedMatch = UTIL_LINE_MATCHER.matcher(expected);
		if (!actualMatch.find()) {
			return false;
		}
		if (!expectedMatch.find()) {
			return false;
		}

		List<BiPredicate<String, String>> checks = List.of(
				String::equals, Objects::equals, //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin(), //
				floatStringsWithin() //
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

	boolean specLinesMatch(String actual, String expected) {
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

	BiPredicate<String, String> floatStringsWithin(float relativeThreshold, float absoluteThreshold) {

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

	BiPredicate<String, String> floatStringsWithin() {
		return floatStringsWithin(0.01f, 0.0001f);
	}

	BiPredicate<String, String> intStringsEqual() {
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

	BiPredicate<String, String> stringsEqual() {
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

}
