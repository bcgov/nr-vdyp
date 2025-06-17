package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class Scenario6 extends Scenario {

	@Inject
	Scenario6(TestHelper testHelper) {
		super(testHelper, "scenario6");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionHscvVri_TextYieldTable() throws IOException {

		logger.info("Starting testProjectionHscvVri_TextYieldTable");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER, //
				Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON
		).outputFormat(OutputFormat.YIELD_TABLE) //
				.yearStart(2000) //
				.yearEnd(2050);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);

		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.txt", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));
		assertTrue(entry1Content.length() > 0);

		String expectedContent = new String(
				Files.readAllBytes(testHelper.getResourceFile(scenarioResourcePath, "expected-yield-table.txt"))
		);

		expectedContent = removeTimestamp(expectedContent);
		entry1Content = removeTimestamp(entry1Content);
		for (int i = 0; i < expectedContent.length(); i++) {
			if (entry1Content.charAt(i) != expectedContent.charAt(i)) {
				Assert.fail(MessageFormat.format("{0} != {1}", entry1Content.charAt(i), expectedContent.charAt(i)));
			}
		}

		assertEquals(expectedContent.length(), entry1Content.length());

		assertProgressLogNext(zipFile, s -> s.contains("starting projection (type HCSV)"));

		assertErrorLogNext(zipFile, s -> s.length() == 0);

		assertDebugLogNext(zipFile, s -> s.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		ZipEntry entry = zipFile.getNextEntry();
		while (entry != null) {
			var contents = testHelper.readZipEntry(zipFile, entry);
			logger.info("Saw projection file " + entry + " containing " + contents.length + " bytes");

			entry = zipFile.getNextEntry();
		}
	}

	private String removeTimestamp(String textYieldTable) {
		return textYieldTable.substring(0, textYieldTable.indexOf("Run completed: "));
	}
}
