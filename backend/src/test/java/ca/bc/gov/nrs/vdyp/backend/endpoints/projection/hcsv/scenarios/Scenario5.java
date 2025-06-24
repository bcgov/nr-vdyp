package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;
import jakarta.inject.Inject;

@QuarkusTest
class Scenario5 extends Scenario {

	@Inject
	Scenario5(TestHelper testHelper) {
		super(testHelper, "scenario5");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testMissingTotalAge() throws IOException {

		logger.info("Starting testMissingTotalAge");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES
		);
		parameters.yearStart(2000).yearEnd(2050);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY.csv", "VDYP7_INPUT_LAYER.csv", parameters
		);

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		assertYieldTableNext(zipFile, s -> s.length() == 0);

		assertProgressLogNext(zipFile, s -> s.contains("starting projection (type HCSV)"));

		assertErrorLogNext(
				zipFile,
				s -> s.contains(
						"Species entry references layer of type \"PRIMARY\" of polygon 000A000  88584471    2024 but it is not present"
				)
		);

		assertDebugLogNext(zipFile, s -> s.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		ZipEntry projectionResultsEntry;
		var outputSeen = false;
		while ( (projectionResultsEntry = zipFile.getNextEntry()) != null) {
			logger.info("Name: {}", projectionResultsEntry.getName());
			String entryContent = new String(testHelper.readZipEntry(zipFile, projectionResultsEntry));
			if (entryContent.length() > 0) {
				logger.info("Content: {}", entryContent.substring(0, Math.min(entryContent.length(), 60)));
			} else {
				logger.info("Content: <empty>");
			}

			outputSeen = true;
		}
		Assert.assertFalse(outputSeen);
	}
}
