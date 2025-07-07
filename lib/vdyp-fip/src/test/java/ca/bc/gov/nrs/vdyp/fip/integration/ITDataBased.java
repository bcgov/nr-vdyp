package ca.bc.gov.nrs.vdyp.fip.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSite;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.integration_tests.IntermediateDataBasedIntegrationTest;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ITDataBased extends IntermediateDataBasedIntegrationTest {

	@ParameterizedTest
	@MethodSource("testNameAndLayerProvider")
	void testFipStart(String test, String layer) throws IOException, ResourceParseException, ProcessingException {
		State inputState = State.FipInput;
		State outputState = State.ForwardInput;

		Path testDir = testDataDir.resolve(test);
		Path dataDir = testDir.resolve(inputState.dir).resolve(layer);
		Path expectedDir = testDir.resolve(outputState.dir).resolve(layer);

		Assumptions.assumeTrue(Files.exists(dataDir), "No input data: " + dataDir);
		Assumptions.assumeTrue(Files.exists(expectedDir), "No expected output data" + expectedDir);

		doSkip(testDir, "testFipStart");

		Path baseControlFile = copyResource(TestUtils.class, "FIPSTART.CTR", testConfigDir);

		Path testControlFile = dataDir.resolve("control.ctl");

		Path ioControlFile = createIoControlFile(inputState, dataDir, false, Data.Polygon, Data.Layer, Data.Species);

		final var controlFiles = Stream.of(baseControlFile, testControlFile, ioControlFile).filter(Files::exists)
				.map(Object::toString).toArray(String[]::new);

		try (VdypStartApplication<FipPolygon, FipLayer, FipSpecies, FipSite> app = new FipStart();) {

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
