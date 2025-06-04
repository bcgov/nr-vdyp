package ca.bc.gov.nrs.vdyp.test_oracle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.test_oracle.OracleRunner.Layer;

class OracleRunnerTest {

	@TempDir
	Path installDir;

	Path configDir;

	@BeforeEach
	void setupInstallDir() throws IOException {
		configDir = Files.createDirectory(installDir.resolve("VDYP_CFG"));
	}

	@Nested
	class FullTest {

		@TempDir
		Path inputDir;

		@TempDir(cleanup = CleanupMode.ON_SUCCESS)
		Path tempDir;

		@TempDir(cleanup = CleanupMode.ON_SUCCESS)
		Path outputDir;

		static Path copyResource(Class<?> klazz, String context, String path, Path destination) throws IOException {
			Path result = destination.resolve(path);

			try (var is = klazz.getResourceAsStream(context + path)) {
				Files.copy(is, result);
			}

			return result;

		}

		String[] FILES = new String[] { "parms.txt", "RunVDYP7.cmd", "VDYP7_INPUT_LAYER.csv", "VDYP7_INPUT_POLY.csv" };
		String[] INPUT1_TESTS = new String[] { "test1" };
		String[] INPUT2_TESTS = new String[] { "test1", "test2" };

		void setupInput1() throws IOException {
			for (String testName : INPUT1_TESTS) {
				Files.createDirectories(inputDir.resolve(testName));
				for (String filename : FILES) {
					copyResource(OracleRunnerTest.class, "input1/", testName + "/" + filename, inputDir);
				}
			}
		}

		@Test
		void testSimple() throws Exception {

			setupInput1();

			var em = EasyMock.createControl();

			CompletableFuture<Void> mockFuture = em.createMock(CompletableFuture.class);

			EasyMock.expect(mockFuture.get()).andReturn(null).once();

			em.replay();

			var app = new OracleRunner() {

				@Override
				protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
					assertThat(
							builder.command(),
							contains(
									endsWith("VDYP7Console.exe"), equalTo("-p"),
									matchesRegex(".*?test1[/\\\\]input[/\\\\]parms.txt"), equalTo("-env"),
									matchesRegex("InputFileDir=.*?[/\\\\]test1[/\\\\]input"), equalTo("-env"),
									matchesRegex("OutputFileDir=.*?[/\\\\]test1[/\\\\]output"), equalTo("-env"),
									equalTo("InstallDir=" + installDir.toString()), equalTo("-env"),
									matchesRegex("ParmsFileDir=.*?[/\\\\]test1[/\\\\]input")
							)
					);
					assertThat(builder.directory(), equalTo(tempDir.resolve("test1/input").toAbsolutePath().toFile()));
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1/input").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.OUTPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1/output").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.PARAM_DIR_ENV),
									equalTo(
											tempDir.resolve("test1/input").toAbsolutePath().toString() // Same as input
																										// dir
									)
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INSTALL_DIR_ENV),
									equalTo(installDir.toAbsolutePath().toString())
							)
					);

					assertThat(tempDir, fileExists("test1/input/RunVDYP7.cmd"));
					assertThat(tempDir, fileExists("test1/input/parms.txt"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

					addExecution("Polygon 1", Layer.PRIMARY);

					FileUtils.touch(tempDir.resolve("test1/output/Output_YldTbl.csv").toFile());

					return mockFuture;
				}

			};

			app.run(new String[] { "-i" + inputDir, "-o" + outputDir, "-d" + installDir, "-t" + tempDir });

			em.verify();

			assertThat(outputDir, fileExists("test1/input/RunVDYP7.cmd"));
			assertThat(outputDir, fileExists("test1/input/parms.txt"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

			assertTestFiles("test1", "Polygon 1-primary");

			assertThat(outputDir, fileExists("test1/output/Output_YldTbl.csv"));

		}

		@Test
		void testMultipleLayers() throws Exception {

			setupInput1();

			var em = EasyMock.createControl();

			CompletableFuture<Void> mockFuture = em.createMock(CompletableFuture.class);

			EasyMock.expect(mockFuture.get()).andReturn(null).once();

			em.replay();

			var app = new OracleRunner() {

				@Override
				protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
					assertThat(
							builder.command(),
							contains(
									endsWith("VDYP7Console.exe"), equalTo("-p"),
									matchesRegex(".*?test1[/\\\\]input[/\\\\]parms.txt"), equalTo("-env"),
									matchesRegex("InputFileDir=.*?[/\\\\]test1[/\\\\]input"), equalTo("-env"),
									matchesRegex("OutputFileDir=.*?[/\\\\]test1[/\\\\]output"), equalTo("-env"),
									equalTo("InstallDir=" + installDir.toString()), equalTo("-env"),
									matchesRegex("ParmsFileDir=.*?[/\\\\]test1[/\\\\]input")
							)
					);
					assertThat(
							builder.directory(),
							equalTo(tempDir.resolve("test1").resolve("input").toAbsolutePath().toFile())
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1").resolve("input").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.OUTPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1").resolve("output").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.PARAM_DIR_ENV),
									equalTo(
											tempDir.resolve("test1").resolve("input").toAbsolutePath().toString() // Same
																													// as
																													// input
																													// dir
									)
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INSTALL_DIR_ENV),
									equalTo(installDir.toAbsolutePath().toString())
							)
					);

					assertThat(tempDir, fileExists("test1/input/RunVDYP7.cmd"));
					assertThat(tempDir, fileExists("test1/input/parms.txt"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

					addExecution("Polygon 1", Layer.PRIMARY);
					addExecution("Polygon 1", Layer.VETERAN);

					FileUtils.touch(tempDir.resolve("test1/output/Output_YldTbl.csv").toFile());

					return mockFuture;
				}

			};

			app.run(new String[] { "-i" + inputDir, "-o" + outputDir, "-d" + installDir, "-t" + tempDir });

			em.verify();

			assertThat(outputDir, fileExists("test1/input/RunVDYP7.cmd"));
			assertThat(outputDir, fileExists("test1/input/parms.txt"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

			assertTestFiles("test1", "Polygon 1-primary", "Polygon 1-veteran");

			assertThat(outputDir, fileExists("test1/output/Output_YldTbl.csv"));

		}

		@Test
		void testMultiplePolygons() throws Exception {

			setupInput1();

			var em = EasyMock.createControl();

			CompletableFuture<Void> mockFuture = em.createMock(CompletableFuture.class);

			EasyMock.expect(mockFuture.get()).andReturn(null).once();

			em.replay();

			var app = new OracleRunner() {

				@Override
				protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
					assertThat(
							builder.command(),
							contains(
									endsWith("VDYP7Console.exe"), equalTo("-p"),
									matchesRegex(".*?test1[/\\\\]input[/\\\\]parms.txt"), equalTo("-env"),
									matchesRegex("InputFileDir=.*?[/\\\\]test1[/\\\\]input"), equalTo("-env"),
									matchesRegex("OutputFileDir=.*?[/\\\\]test1[/\\\\]output"), equalTo("-env"),
									equalTo("InstallDir=" + installDir.toString()), equalTo("-env"),
									matchesRegex("ParmsFileDir=.*?[/\\\\]test1[/\\\\]input")
							)
					);
					assertThat(builder.directory(), equalTo(tempDir.resolve("test1/input").toAbsolutePath().toFile()));
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1/input").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.OUTPUT_DIR_ENV),
									equalTo(tempDir.resolve("test1/output").toAbsolutePath().toString())
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.PARAM_DIR_ENV),
									equalTo(
											tempDir.resolve("test1/input").toAbsolutePath().toString() // Same as input
																										// dir
									)
							)
					);
					assertThat(
							builder.environment(),
							hasEntry(
									equalTo(OracleRunner.INSTALL_DIR_ENV),
									equalTo(installDir.toAbsolutePath().toString())
							)
					);

					assertThat(tempDir, fileExists("test1/input/RunVDYP7.cmd"));
					assertThat(tempDir, fileExists("test1/input/parms.txt"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
					assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

					addExecution("Polygon 1", Layer.PRIMARY);
					addExecution("Polygon 2", Layer.PRIMARY);

					FileUtils.touch(tempDir.resolve("test1/output/Output_YldTbl.csv").toFile());

					return mockFuture;
				}

			};

			app.run(new String[] { "-i" + inputDir, "-o" + outputDir, "-d" + installDir, "-t" + tempDir });

			em.verify();

			assertThat(outputDir, fileExists("test1/input/RunVDYP7.cmd"));
			assertThat(outputDir, fileExists("test1/input/parms.txt"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
			assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

			assertTestFiles("test1", "Polygon 1-primary", "Polygon 2-primary");

			assertThat(outputDir, fileExists("test1/output/Output_YldTbl.csv"));

		}

		private void assertTestFiles(String testName, String... executions) {
			for (var execution : executions) {
				for (var tag : new String[] { "polygon", "species", "util", "grow" }) {
					final Path path = Path.of(testName, "forwardInput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path, fileEachLine(containsString(execution))));
				}
				final Path forwardControlPath = Path.of("test1", "forwardInput", execution, "control.ctl");
				assertThat(outputDir, fileExists(forwardControlPath));
				for (var tag : new String[] { "polygon", "species", "util", "compat" }) {
					final Path path = Path.of(testName, "forwardOutput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path));
				}
				for (var tag : new String[] { "adjust", "polygon", "species", "util" }) {
					final Path path = Path.of(testName, "adjustInput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path));
				}
				final Path backControlPath = Path.of("test1", "backInput", execution, "control.ctl");
				assertThat(outputDir, fileExists(backControlPath));
				for (var tag : new String[] { "polygon", "species", "util" }) {
					final Path path = Path.of(testName, "backInput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path));
				}
				for (var tag : new String[] { "polygon", "species", "util", "compat" }) {
					final Path path = Path.of(testName, "backOutput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path));
				}
				for (var tag : new String[] { "site", "layer", "polygon", "species" }) {
					final Path path = Path.of(testName, "vriInput", execution, tag + ".dat");
					assertThat(outputDir, fileExists(path));
				}
			}
		}
	}

	@FunctionalInterface
	interface FileManipulator {
		void manipulate(Path path) throws IOException;
	}

	String addExecution(final String polygonId, final Layer layer) throws IOException {
		final String executionId = UUID.randomUUID().toString();
		final String niceExecutionId = String.format("%s-%s", polygonId, layer.filename);
		Path executionDir = Files.createDirectories(configDir.resolve("execution-" + executionId));

		for (var tag : new String[] { "7INPP", "7INPS", "7INPU", //
				"7OUTP", "7OUTS", "7OUTU", "7OUTC", //
				"AJSTA", "AJSTP", "AJSTS", "AJSTU", //
				"BINPP", "BINPS", "BINPU", //
				"BOUTP", "BOUTS", "BOUTU", "BOUTC", //
				"GROW", //
				"VRII", "VRIL", "VRIP", "VRIS" //
		}) {
			FileUtils.write(
					configDir.resolve(layer.code + "-SAVE_VDYP7_" + tag + ".dat").toFile(),
					String.format(
							"%-21s%d Execution %s %s %s %s line %d" + System.lineSeparator(), polygonId, 2025,
							executionId, layer, tag, niceExecutionId, 1
					), StandardCharsets.UTF_8, true // Append to the file
			);

		}
		System.lineSeparator();
		FileUtils.write(
				configDir.resolve(layer.code + "-VDYP7_VDYP.ctl").toFile(),
				"Execution " + polygonId + " " + layer + " " + executionId + " forward control " + niceExecutionId
						+ System.lineSeparator(),
				StandardCharsets.UTF_8, false // Overwrite the file
		);

		FileUtils.write(
				configDir.resolve(layer.code + "-VDYP7_BACK.ctl").toFile(),
				"Execution " + polygonId + " " + layer + " " + executionId + " back control " + niceExecutionId
						+ System.lineSeparator(),
				StandardCharsets.UTF_8, false // Overwrite the file
		);

		try (var stream = Files.newDirectoryStream(configDir, (Path f) -> Files.isRegularFile(f))) {
			for (var toCopy : stream) {
				FileUtils.copyToDirectory(toCopy.toFile(), executionDir.toFile());
			}
		}
		return executionId;
	}

	@Nested
	class Separator {

		void assertSeparated(String executionId, String polygonId, Layer layer, String tag) throws IOException {
			String content = FileUtils.readFileToString(
					configDir.resolve("execution-" + polygonId + "-" + layer.filename)
							.resolve(layer.code + "-SAVE_VDYP7_" + tag + ".dat").toFile(),
					StandardCharsets.UTF_8
			);

			assertThat(
					content,
					equalTo(
							String.format(
									"%-21s%d Execution %s %s %s %s-%s line %d" + System.lineSeparator(), polygonId,
									2025, executionId, layer, tag, polygonId, layer.filename, 1
							)
					)
			);

			for (Layer shouldBeRemoved : Layer.values()) {
				if (shouldBeRemoved == layer)
					continue;
				assertThat(
						configDir,
						not(
								fileExists(
										Path.of(
												"execution-" + polygonId + "-" + layer.filename,
												shouldBeRemoved.code + "-SAVE_VDYP7_" + tag + ".dat"
										)
								)
						)
				);
			}
		}

		void assertSeparated(String executionId, String polygonId, Layer layer) throws IOException {
			for (String tag : new String[] { "7INPP", "7INPS", "7INPU", //
					"7OUTP", "7OUTS", "7OUTU", "7OUTC", //
					"AJSTA", "AJSTP", "AJSTS", "AJSTU", //
					"BINPP", "BINPS", "BINPU", //
					"BOUTP", "BOUTS", "BOUTU", "BOUTC", //
					"GROW", //
					"VRII", "VRIL", "VRIP", "VRIS" //
			}) {
				assertSeparated(executionId, polygonId, layer, tag);
			}
		}

		@Test
		void testOneLayerOnePolygon() throws IOException {

			var execution1 = addExecution("Polygon 1", Layer.PRIMARY);

			var app = new OracleRunner();

			app.separateExecutions(configDir);

			assertSeparated(execution1, "Polygon 1", Layer.PRIMARY);
		}

		@Test
		void testTwoLayersOnePolygon() throws IOException {

			var execution1 = addExecution("Polygon 1", Layer.PRIMARY);
			var execution2 = addExecution("Polygon 1", Layer.VETERAN);

			var app = new OracleRunner();

			app.separateExecutions(configDir);

			assertSeparated(execution1, "Polygon 1", Layer.PRIMARY);
			assertSeparated(execution2, "Polygon 1", Layer.VETERAN);
		}

		@Test
		void testOneLayerTwoPolygons() throws IOException {

			var execution1 = addExecution("Polygon 1", Layer.PRIMARY);
			var execution2 = addExecution("Polygon 2", Layer.PRIMARY);

			var app = new OracleRunner();

			app.separateExecutions(configDir);

			assertSeparated(execution1, "Polygon 1", Layer.PRIMARY);
			assertSeparated(execution2, "Polygon 2", Layer.PRIMARY);
		}

		@Test
		void testTwoLayersTwoPolygons() throws IOException {

			var execution1 = addExecution("Polygon 1", Layer.PRIMARY);
			var execution2 = addExecution("Polygon 1", Layer.VETERAN);
			var execution3 = addExecution("Polygon 2", Layer.PRIMARY);
			var execution4 = addExecution("Polygon 2", Layer.VETERAN);

			var app = new OracleRunner();

			app.separateExecutions(configDir);

			assertSeparated(execution1, "Polygon 1", Layer.PRIMARY);
			assertSeparated(execution2, "Polygon 1", Layer.VETERAN);
			assertSeparated(execution3, "Polygon 2", Layer.PRIMARY);
			assertSeparated(execution4, "Polygon 2", Layer.VETERAN);
		}
	}

	Matcher<Path> fileExists(String path) {
		return fileExists(Path.of(path));
	}

	Matcher<Path> fileExists(Path path) {
		return new TypeSafeDiagnosingMatcher<Path>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains a file at ").appendValue(path);
			}

			@Override
			protected boolean matchesSafely(Path item, Description mismatchDescription) {
				if (Files.exists(item.resolve(path)))
					return true;

				mismatchDescription.appendValue(item.resolve(path)).appendText(" does not exist");
				return false;
			}

		};
	}

	Matcher<Path> fileLines(Matcher<List<String>> linesMatcher) {
		return new TypeSafeDiagnosingMatcher<Path>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("has lines ").appendDescriptionOf(linesMatcher);
			}

			@Override
			protected boolean matchesSafely(Path item, Description mismatchDescription) {
				if (Files.exists(item)) {
					List<String> content;
					try {
						content = Files.readAllLines(item);
					} catch (IOException e) {
						throw new IllegalStateException("Error while reading file to compare", e);
					}

					if (linesMatcher.matches(content)) {
						return true;
					} else {
						linesMatcher.describeMismatch(content, mismatchDescription);
						return false;
					}
				}

				mismatchDescription.appendValue(item).appendText(" does not exist");
				return false;
			}
		};
	}

	Matcher<Path> fileEachLine(Matcher<String> lineMatcher) {
		return new TypeSafeDiagnosingMatcher<Path>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("has lines that all ").appendDescriptionOf(lineMatcher);
			}

			@Override
			protected boolean matchesSafely(Path item, Description mismatchDescription) {
				if (Files.exists(item)) {
					List<String> content;
					try {
						content = Files.readAllLines(item);
					} catch (IOException e) {
						throw new IllegalStateException("Error while reading file to compare", e);
					}

					for (var line : content) {
						if (!lineMatcher.matches(line)) {
							lineMatcher.describeMismatch(line, mismatchDescription);
						}
					}
					return true;
				}

				mismatchDescription.appendValue(item).appendText(" does not exist");
				return false;
			}
		};
	}

	Matcher<Path> fileExists(Path path, Matcher<Path> fileMatcher) {
		return new TypeSafeDiagnosingMatcher<Path>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains a file at ").appendValue(path);
			}

			@Override
			protected boolean matchesSafely(Path item, Description mismatchDescription) {
				if (Files.exists(item.resolve(path))) {
					if (fileMatcher.matches(item.resolve(path))) {
						return true;
					} else {
						mismatchDescription.appendValue(item.resolve(path)).appendText(" has contents that ");
						fileMatcher.describeMismatch(item.resolve(path), mismatchDescription);
						return false;
					}

				}

				mismatchDescription.appendValue(item.resolve(path)).appendText(" does not exist");
				return false;
			}

		};
	}
}
