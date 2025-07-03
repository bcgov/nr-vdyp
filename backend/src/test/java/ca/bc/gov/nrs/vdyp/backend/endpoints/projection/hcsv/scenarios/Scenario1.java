package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class Scenario1 extends Scenario {

	@Inject
	Scenario1(TestHelper testHelper) {
		super(testHelper, "scenario1");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionHscvVri_shouldReturnStatusOK() throws IOException {

		logger.info("Starting testProjectionHscvVri_shouldReturnStatusOK");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER
		);
		parameters.yearStart(2000).yearEnd(2050);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);
		ZipInputStream zipFile = new ZipInputStream(zipInputStream);

		var resultYieldTable = assertYieldTableNext(zipFile, s -> s.length() > 0);

		assertHasAgeRange(resultYieldTable, "13919428", "1", allOfRange(2000, 2050));
		var yieldTableRow = resultYieldTable.get("13919428").get("1").get("2000");
		/* !DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE - default off */
		Assert.assertFalse(yieldTableRow.keySet().contains("POLYGON_ID"));
		/* DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE - default on */
		Assert.assertTrue(yieldTableRow.keySet().contains("PRJ_MODE"));

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
