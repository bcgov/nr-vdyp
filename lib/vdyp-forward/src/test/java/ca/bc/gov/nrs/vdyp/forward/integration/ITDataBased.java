package ca.bc.gov.nrs.vdyp.forward.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessor;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.integration_tests.IntermediateDataBasedIntegrationTest;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ITDataBased extends IntermediateDataBasedIntegrationTest {

	@ParameterizedTest
	@MethodSource("testNameAndLayerProvider")
	void testVdypForward(String test, String layer) throws IOException, ResourceParseException, ProcessingException {
		State inputState = State.ForwardInput;
		State outputState = State.ForwardOutput;

		Path testDir = testDataDir.resolve(test);
		Path dataDir = testDir.resolve(inputState.dir).resolve(layer);
		Path expectedDir = testDir.resolve(outputState.dir).resolve(layer);

		Assumptions.assumeTrue(Files.exists(dataDir), "No input data");
		Assumptions.assumeTrue(Files.exists(expectedDir), "No expected output data");

		doSkip(testDir, "testVdypForward");

		Path baseControlFile = copyResource(TestUtils.class, "VDYP.CTR", testConfigDir);

		Path testControlFile = dataDir.resolve("control.ctl");

		Path ioControlFile = createIoControlFile(
				inputState, dataDir, true, Data.Polygon, Data.Species, Data.Utilization, Data.GrowTo
		);

		final var controlFiles = Stream.of(baseControlFile, testControlFile, ioControlFile).filter(Files::exists)
				.map(Object::toString).toList();

		{

			FileResolver inputFileResolver = new FileSystemFileResolver(configDir);
			FileResolver outputFileResolver = new FileSystemFileResolver(configDir);

			ForwardProcessor processor = new ForwardProcessor();
			processor.run(inputFileResolver, outputFileResolver, controlFiles, VdypForwardApplication.DEFAULT_PASS_SET);

		}

		assertOutputs(outputState, expectedDir);

		assertFileExists(outputDir.resolve(COMPATIBILITY_OUTPUT_NAME));
		assertFileMatches(
				outputDir.resolve(COMPATIBILITY_OUTPUT_NAME),
				expectedDir.resolve(fileName(outputState, Data.Compatibility)), String::equals
		);

	}

}
