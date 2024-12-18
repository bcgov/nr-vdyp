package ca.bc.gov.nrs.vdyp.test_oracle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class OracleRunnerTest {

	@TempDir
	Path installDir;

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
				copyResource(
						OracleRunnerTest.class, "input1/", testName + "/" + filename, inputDir
				);
			}
		}
	}

	@Test
	void testSingleTest() throws Exception {

		setupInput1();

		var em = EasyMock.createControl();

		CompletableFuture<Void> mockFuture = em.createMock(CompletableFuture.class);

		EasyMock.expect(mockFuture.get()).andReturn(null).once();

		em.replay();

		var app = new OracleRunner() {

			@Override
			protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
				assertThat(builder.command(), contains(equalTo("RunVDYP7.cmd")));
				assertThat(builder.directory(), equalTo(tempDir.resolve("test1/input").toAbsolutePath().toFile()));
				assertThat(
						builder.environment(), hasEntry(
								equalTo(OracleRunner.INPUT_DIR_ENV), equalTo(
										tempDir.resolve("test1/input").toAbsolutePath().toString()
								)
						)
				);
				assertThat(
						builder.environment(), hasEntry(
								equalTo(OracleRunner.OUTPUT_DIR_ENV), equalTo(
										tempDir.resolve("test1/output").toAbsolutePath().toString()
								)
						)
				);
				assertThat(
						builder.environment(), hasEntry(
								equalTo(OracleRunner.PARAM_DIR_ENV), equalTo(
										tempDir.resolve("test1/input").toAbsolutePath().toString() //Same as input dir
								)
						)
				);
				assertThat(
						builder.environment(), hasEntry(
								equalTo(OracleRunner.INSTALL_DIR_ENV), equalTo(
										installDir.toAbsolutePath().toString()
								)
						)
				);

				assertThat(tempDir, fileExists("test1/input/RunVDYP7.cmd"));
				assertThat(tempDir, fileExists("test1/input/parms.txt"));
				assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
				assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

				for (var tag : new String[] {
						"7INPP", "7INPS", "7INPU",
						"7OUTP", "7OUTS", "7OUTU", "7OUTC",
						"AJSTA", "AJSTP", "AJSTS", "AJSTU",
						"BINPP", "BINPS", "BINPU",
						"BOUTP", "BOUTS", "BOUTU", "BOUTC",
						"GROW",
						"VRII", "VRIL", "VRIP", "VRIS"
				}) {
					FileUtils.touch(tempDir.resolve("test1/output/P-SAVE_VDYP7_" + tag + ".dat").toFile());
				}

				return mockFuture;
			}

		};

		app.run(new String[] { "-i" + inputDir, "-o" + outputDir, "-d" + installDir, "-t" + tempDir });

		em.verify();

		assertThat(outputDir, fileExists("test1/input/RunVDYP7.cmd"));
		assertThat(outputDir, fileExists("test1/input/parms.txt"));
		assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
		assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

		for (var tag : new String[] { "7INPP", "7INPS", "7INPU" }) {
			assertThat(outputDir, fileExists("test1/forwardInput/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "7OUTP", "7OUTS", "7OUTU", "7OUTC" }) {
			assertThat(outputDir, fileExists("test1/forwardOutput/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "AJSTA", "AJSTP", "AJSTS", "AJSTU" }) {
			assertThat(outputDir, fileExists("test1/adjustInput/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "BINPP", "BINPS", "BINPU" }) {
			assertThat(outputDir, fileExists("test1/backInput/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "BOUTP", "BOUTS", "BOUTU", "BOUTC" }) {
			assertThat(outputDir, fileExists("test1/backOutput/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "GROW" }) {
			assertThat(outputDir, fileExists("test1/other/P-SAVE_VDYP7_" + tag + ".dat"));
		}
		for (var tag : new String[] { "VRII", "VRIL", "VRIP", "VRIS" }) {
			assertThat(outputDir, fileExists("test1/vriInput/P-SAVE_VDYP7_" + tag + ".dat"));
		}


		assertThat(outputDir, fileExists("test1/input/RunVDYP7.cmd"));
		assertThat(outputDir, fileExists("test1/input/parms.txt"));
		assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
		assertThat(outputDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

	}

	Matcher<Path> fileExists(String path) {
		return new TypeSafeDiagnosingMatcher<Path>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("contains a file at ").appendValue(path);
			}

			@Override
			protected boolean matchesSafely(Path item, Description mismatchDescription) {
				if (Files.exists(item.resolve(path)))
					return true;

				mismatchDescription.appendValue("does not exist");
				return false;
			}

		};
	}
}
