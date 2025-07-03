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
import jakarta.inject.Inject;

@QuarkusTest
class Scenario2 extends Scenario {

	@Inject
	Scenario2(TestHelper testHelper) {
		super(testHelper, "scenario2");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionHscvFip_shouldReturnStatusOK() throws IOException {

		logger.info("Starting testProjectionHscvFip_shouldReturnStatusOK");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING,
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING,
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER
		);
		parameters.ageStart(10).ageEnd(100);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_FIP.csv", "VDYP7_INPUT_LAYER_FIP.csv", parameters
		);
		ZipInputStream zipFile = new ZipInputStream(zipInputStream);

		var resultYieldTable = assertYieldTableNext(zipFile, s -> s.length() > 0);

		// Year at layer age 0 is 1874.
		assertHasAgeRange(resultYieldTable, "6993168", "1", allOfRange(1884, 1974));

		assertProgressLogNext(zipFile, s -> s.contains("starting projection (type HCSV)"));

		assertErrorLogNext(zipFile, s -> s.length() == 0);

		assertDebugLogNext(zipFile, s -> s.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		ZipEntry entry = zipFile.getNextEntry();
		while (entry != null) {
			var contents = TestHelper.readZipEntry(zipFile, entry);
			logger.info("Saw projection file " + entry + " containing " + contents.length + " bytes");

			entry = zipFile.getNextEntry();
		}
	}
}
