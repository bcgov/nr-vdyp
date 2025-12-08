package ca.bc.gov.nrs.vdyp.integration_tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;

import io.github.classgraph.ClassGraph;

public abstract class BaseDataBasedIntegrationTest {

	/**
	 * Directory containing the integration test data. Do not alter this during the tests.
	 */
	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected static Path testDataDir;

	/**
	 * Copy a classpath resource to a filesystem location
	 *
	 * @param klazz
	 * @param path
	 * @param destination
	 * @return
	 * @throws IOException
	 */
	protected static Path copyResource(Class<?> klazz, String path, Path destination) throws IOException {
		Path result = destination.resolve(path);

		try (var is = klazz.getResourceAsStream(path)) {
			Files.copy(is, result);
		}

		return result;

	}

	/**
	 * Set up {@code testDataDir} with the integration test data
	 *
	 * @throws IOException
	 */
	@BeforeAll
	protected static void initTestData() throws IOException {

		try (
				var scan = new ClassGraph().verbose()
						.addClassLoader(BaseDataBasedIntegrationTest.class.getClassLoader())
						.acceptPaths("ca/bc/gov/nrs/vdyp/integration_tests").scan()
		) {
			var externalTestPath = Optional.ofNullable(System.getProperty("integration_tests.external.path"))
					.map(Path::of);

			externalTestPath.ifPresentOrElse(path -> {
				// An external source of integration tests has been specified, use that
				try (Stream<Path> stream = Files.walk(path)) {
					stream.forEach(subpath -> {
						try {
							if (!path.equals(subpath)) {
								final Path dest = testDataDir.resolve(path.relativize(subpath));
								Files.copy(subpath, dest);
							}
						} catch (IOException ex) {
							throw new IllegalStateException(
									"Failure copying integration test data from file system", ex
							);
						}
					});
				} catch (IOException ex) {
					throw new IllegalStateException("Failure copying integration test data from file system", ex);
				}
			}, () -> {
				// No external source of integration tests so use the standard ones on the classpath
				for (var resource : scan.getResourcesMatchingWildcard("ca/bc/gov/nrs/vdyp/integration_tests/**")) {

					var localPath = resource.getPath().substring("ca/bc/gov/nrs/vdyp/integration_tests/".length());

					if (resource.getPath().endsWith("class"))
						continue;

					final Path dest = testDataDir.resolve(localPath);

					try {
						Files.createDirectories(dest.getParent());

						System.err.printf("Copying %s to %s", resource.getPath(), dest).println();
						try (var is = resource.open()) {
							Files.copy(is, dest);
						}
					} catch (IOException ex) {
						throw new IllegalStateException("Failure copying integration test data from classpath", ex);
					}
				}
			});

		}

	}

	protected static Stream<String> testNameProvider() throws IOException {
		return Files.list(testDataDir).map(p -> p.getFileName().toString());
	}

	protected static Collection<Arguments> testNameAndLayerProvider() throws IOException {
		try {
			return Files.list(testDataDir).filter(p -> Files.isDirectory(p)).flatMap(p -> {
				try {
					return Files.list(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).filter(p -> Files.isDirectory(p)).flatMap(p -> {
				try {
					return Files.list(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).filter(p -> Files.isDirectory(p)).map(
					p -> Arguments.of(p.getParent().getParent().getFileName().toString(), p.getFileName().toString())
			)
					// Collapse together test/layer pairs
					.collect(Collectors.toMap(args -> String.format("%s/%s", args.get()), args -> args, (a1, a2) -> a1))
					.values();
		} catch (UncheckedIOException e) {
			throw new IOException(e);
		}
	}

	public void doSkip(Path testDir, String test) throws IOException {
		Path skip = testDir.resolve("skip");

		if (Files.exists(skip)) {
			boolean doSkip = Files.readAllLines(skip).stream().map(line -> line.split(" ")[0])
					.anyMatch(toSkip -> toSkip.equals(test));
			Assumptions.assumeFalse(doSkip, "Skipping for " + test);
		}
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

	// TODO change to a record once a release of Eclipse JDT that fixes
	// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3745 is available
	static class LineMatchError {
		final long lineNumber;
		final String expected;
		final String actual;

		public LineMatchError(long lineNumber, String expected, String actual) {
			super();
			this.lineNumber = lineNumber;
			this.expected = expected;
			this.actual = actual;
		}
	}

	public void assertFileMatches(
			String testPath, String expectedPath, BufferedReader testStream, BufferedReader expectedStream,
			BiPredicate<String, String> compare
	) throws IOException {

		long actualCount = 0;
		long expectedCount = 0;
		Optional<LineMatchError> firstMismatch = Optional.empty();
		Optional<LineMatchError> firstMissingUnexpected = Optional.empty();
		for (int lineNumber = 1; true; lineNumber++) {
			String testLine = testStream.readLine();
			String expectedLine = expectedStream.readLine();
			if (testLine != null) {
				actualCount = lineNumber;
			}
			if (expectedLine != null) {
				expectedCount = lineNumber;
			}
			final long constActualCount = actualCount;
			final long constExpectedCount = expectedCount;
			final long constLineNumber = lineNumber;
			if (testLine == null && expectedLine == null) {
				// End of the file
				if (!firstMissingUnexpected.isPresent() && !firstMismatch.isPresent()) {
					// Everything is fine
					return;
				}
				// Report whether the files have the same number of lines, if not, what's the first missing/unexpected
				// line,
				// and if any lines present in both files don't match what's the first one.
				fail(Stream.<String>concat(firstMissingUnexpected.stream().map(error -> {
					if (error.expected == null) {
						return "File " + testPath + " had " + constActualCount + " lines  but " + expectedPath + " had "
								+ constExpectedCount + ".  The first unexpected line (" + error.lineNumber + ") was:\n"
								+ error.actual;
					}
					return "File " + testPath + " had " + constActualCount + " lines  but " + expectedPath + " had "
							+ constExpectedCount + ".  The first missing line (" + error.lineNumber + ") was:\n"
							+ error.expected;
				}),
						firstMismatch.stream().map(
								error -> "File " + testPath + " did not match " + expectedPath + ". The first line ("
										+ error.lineNumber + ") to not match was: \n [Expected]: " + error.expected
										+ "\n   [Actual]: " + error.actual
						)
				).collect(Collectors.joining("\n and \n")));

				return;
			}

			if (testLine == null) {
				firstMissingUnexpected = Optional.of(
						firstMissingUnexpected
								.orElseGet(() -> new LineMatchError(constLineNumber, expectedLine, testLine))
				);
				continue;
			}
			if (expectedLine == null) {
				firstMissingUnexpected = Optional.of(
						firstMissingUnexpected
								.orElseGet(() -> new LineMatchError(constLineNumber, expectedLine, testLine))
				);
			}
			if (!compare.test(testLine, expectedLine)) {
				firstMismatch = Optional
						.of(firstMismatch.orElseGet(() -> new LineMatchError(constLineNumber, expectedLine, testLine)));
			}

		}
	}

}
