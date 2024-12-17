package ca.bc.gov.nrs.vdyp.test_oracle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.easymock.EasyMock;
import org.hamcrest.Matchers;
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

	void setupInput2() throws IOException {
		for (String testName : INPUT2_TESTS) {
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
				return mockFuture;
			}

		};

		app.run(new String[] { "-i" + inputDir, "-o" + outputDir, "-d" + installDir, "-t" + tempDir });

		em.verify();

	}
}
