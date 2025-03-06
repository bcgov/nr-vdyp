package ca.bc.gov.nrs.vdyp.test_oracle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.test_oracle.OracleRunner.Layer;

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

	@BeforeEach
	void setupInstallDir() throws IOException {
		Files.createDirectory(installDir.resolve("VDYP_CFG"));
	}

	void setupInput1() throws IOException {
		for (String testName : INPUT1_TESTS) {
			Files.createDirectories(inputDir.resolve(testName));
			for (String filename : FILES) {
				copyResource(OracleRunnerTest.class, "input1/", testName + "/" + filename, inputDir);
			}
		}
	}

	
	@Test
	void testSingleTestPrimary() throws Exception {

		setupInput1();
		
		final var layers = new Layer[] {Layer.PRIMARY};

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
								endsWith("VDYP7Console.exe"), equalTo("-p"), endsWith("test1/input/parms.txt"),
								equalTo("-env"), matchesRegex("InputFileDir=.*?/test1/input"), equalTo("-env"),
								matchesRegex("OutputFileDir=.*?/test1/output"), equalTo("-env"),
								equalTo("InstallDir=" + installDir.toString()), equalTo("-env"),
								matchesRegex("ParmsFileDir=.*?/test1/input")
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
										tempDir.resolve("test1/input").toAbsolutePath().toString() // Same as input dir
								)
						)
				);
				assertThat(
						builder.environment(),
						hasEntry(equalTo(OracleRunner.INSTALL_DIR_ENV), equalTo(installDir.toAbsolutePath().toString()))
				);

				assertThat(tempDir, fileExists("test1/input/RunVDYP7.cmd"));
				assertThat(tempDir, fileExists("test1/input/parms.txt"));
				assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_POLY.csv"));
				assertThat(tempDir, fileExists("test1/input/VDYP7_INPUT_LAYER.csv"));

				for (var layer: layers) {
					for (var tag : new String[] { "7INPP", "7INPS", "7INPU", //
							"7OUTP", "7OUTS", "7OUTU", "7OUTC", //
							"AJSTA", "AJSTP", "AJSTS", "AJSTU", //
							"BINPP", "BINPS", "BINPU", //
							"BOUTP", "BOUTS", "BOUTU", "BOUTC", //
							"GROW", //
							"VRII", "VRIL", "VRIP", "VRIS" //
					}) {
						FileUtils.touch(installDir.resolve("VDYP_CFG/"+layer.code+"-SAVE_VDYP7_" + tag + ".dat").toFile());
					}
					FileUtils.touch(installDir.resolve("VDYP_CFG/"+layer.code+"-VDYP7_VDYP.ctl").toFile());
					FileUtils.touch(installDir.resolve("VDYP_CFG/"+layer.code+"-VDYP7_BACK.ctl").toFile());
				}

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

		for (var layer: layers) {
			for (var tag : new String[] { "polygon", "species", "util", "grow" }) {
				assertThat(outputDir, fileExists("test1/forwardInput/primary/" + tag + ".dat"));
			}
			assertThat(outputDir, fileExists("test1/forwardInput/primary/control.ctl"));
			for (var tag : new String[] { "polygon", "species", "util", "compat" }) {
				assertThat(outputDir, fileExists("test1/forwardOutput/primary/" + tag + ".dat"));
			}
			for (var tag : new String[] { "adjust", "polygon", "species", "util" }) {
				assertThat(outputDir, fileExists("test1/adjustInput/primary/" + tag + ".dat"));
			}
			for (var tag : new String[] { "polygon", "species", "util" }) {
				assertThat(outputDir, fileExists("test1/backInput/primary/" + tag + ".dat"));
			}
			assertThat(outputDir, fileExists("test1/backInput/primary/control.ctl"));
			for (var tag : new String[] { "polygon", "species", "util", "compat" }) {
				assertThat(outputDir, fileExists("test1/backOutput/primary/" + tag + ".dat"));
			}
			for (var tag : new String[] { "site", "layer", "polygon", "species" }) {
				assertThat(outputDir, fileExists("test1/vriInput/primary/" + tag + ".dat"));
			}
		}

		assertThat(outputDir, fileExists("test1/output/Output_YldTbl.csv"));

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

				mismatchDescription.appendValue(item.resolve(path)).appendText(" does not exist");
				return false;
			}

		};
	}
}
