package ca.bc.gov.nrs.vdyp.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompositeFileResolverTest {

	@TempDir
	Path inputTestDir;

	@TempDir
	Path outputTestDir;

	@Test
	void testWorkingDirGetPath() throws Exception {
		var inputResolver = new FileSystemFileResolver();
		var outputResolver = new FileSystemFileResolver(outputTestDir);

		var compositeResolver = new CompositeFileResolver(inputResolver, outputResolver);

		assertThat(compositeResolver.getInputFileResolver(), equalTo(inputResolver));
		assertThat(compositeResolver.getOutputFileResolver(), equalTo(outputResolver));

		var inputResult = compositeResolver.toInputPath("inputTest");
		assertThat(inputResult.toAbsolutePath().toString(), equalTo(System.getProperty("user.dir") + "/inputTest"));

		var outputResult = compositeResolver.toOutputPath("outputTest");
		assertThat(outputResult.toAbsolutePath().toString(), equalTo(outputTestDir.resolve("outputTest").toString()));
	}

	@Test
	void testInput() throws Exception {
		var inputResolver = new FileSystemFileResolver(inputTestDir);
		var outputResolver = new FileSystemFileResolver(outputTestDir);

		var compositeResolver = new CompositeFileResolver(inputResolver, outputResolver);

		try (var writer = Files.newBufferedWriter(inputTestDir.resolve("test"))) {
			writer.write("blah");
		}

		try (var is = compositeResolver.resolveForInput("test")) {
			var result = is.readAllBytes();
			assertThat(result, equalTo("blah".getBytes()));
		}
	}

	@Test
	void testOutput() throws Exception {
		var inputResolver = new FileSystemFileResolver(inputTestDir);
		var outputResolver = new FileSystemFileResolver(outputTestDir);

		var compositeResolver = new CompositeFileResolver(inputResolver, outputResolver);

		try (var os = compositeResolver.resolveForOutput("test")) {
			os.write("blah".getBytes());
		}

		try (var reader = Files.newBufferedReader(outputTestDir.resolve("test"))) {
			assertThat(reader.readLine(), equalTo("blah"));
		}
	}
}
