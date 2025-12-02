package ca.bc.gov.nrs.vdyp.vri.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.integration_tests.IntermediateDataBasedIntegrationTest;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

class ITDataBased extends IntermediateDataBasedIntegrationTest {

	@ParameterizedTest
	@MethodSource("testNameAndLayerProvider")
	void testVriStart(String test, String layer) throws IOException, ResourceParseException, ProcessingException {
		State inputState = State.VriInput;
		State outputState = State.ForwardInput;

		Path testDir = testDataDir.resolve(test);
		Path dataDir = testDir.resolve(inputState.dir).resolve(layer);
		Path expectedDir = testDir.resolve(outputState.dir).resolve(layer);

		Path baseControlFile = copyResource(TestUtils.class, "VRISTART.CTR", testConfigDir);

		Assumptions.assumeTrue(Files.exists(dataDir), "No input data");
		Assumptions.assumeTrue(Files.exists(expectedDir), "No expected output data");

		doSkip(testDir, "testVriStart");

		Path ioControlFile = createIoControlFile(
				inputState, dataDir, false, Data.Polygon, Data.Layer, Data.Site, Data.Species
		);

		Path testControlFile = dataDir.resolve("control.ctl");
		final var controlFiles = Stream.of(baseControlFile, testControlFile, ioControlFile).filter(Files::exists)
				.map(Object::toString).toArray(String[]::new);

		try (var app = new VriStart();) {

			var resolver = new FileSystemFileResolver(configDir);

			app.init(
					resolver, new PrintStream(new ByteArrayOutputStream()), TestUtils.makeInputStream("", ""),
					controlFiles
			);

			app.process();
		}

		assertOutputs(outputState, expectedDir);

	}

}
