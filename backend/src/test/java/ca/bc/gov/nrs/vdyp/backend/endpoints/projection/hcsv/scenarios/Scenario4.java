package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.csvRowContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

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
class Scenario4 extends Scenario {

	@Inject
	Scenario4(TestHelper testHelper) {
		super(testHelper, "scenario4");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void run2MapsheetTest() throws IOException {

		logger.info("Starting run2MapsheetTest");

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters(), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING, //
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES, //
				Parameters.ExecutionOption.FORWARD_GROW_ENABLED, //
				Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE, //
				Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES
		);
		parameters.yearStart(2000).yearEnd(2050);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY.csv", "VDYP7_INPUT_LAYER.csv", parameters
		);

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.csv", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));

		var csvLines = entry1Content.split("\n");
		assertThat(csvLines, arrayWithSize(104));
		assertThat(csvLines[0], startsWith("\"TABLE_NUM\",\"FEATURE_ID\""));

		Integer indexOfLineOfInterest = null;
		int nextIndex = 0;
		for (var line : csvLines) {
			if (line.contains("13919428") && line.contains("\"D\"")) {
				indexOfLineOfInterest = nextIndex;
				break;
			}
			nextIndex += 1;
		}

		assertThat(indexOfLineOfInterest, not(nullValue()));
		assertThat(
				csvLines[indexOfLineOfInterest], csvRowContaining(
						"3", "13919428", "", "093C090", //
						"94833422", "D", "2013", "170", //
						"PLI", closeTo(100.00000), "", "", //
						"", "", "", "", //
						"", "", "", "", //
						closeTo(30.00000), closeTo(10.02000), closeTo(17.99054), "", closeTo(15.23330), //
						closeTo(20.53386), closeTo(452.95999), closeTo(14.60411), closeTo(97.19800), //
						closeTo(86.55379), closeTo(82.18890), closeTo(80.94500), closeTo(79.21390), //
						"Ref"
				)
		);

		assertProgressLogNext(zipFile, s -> s.contains("starting projection (type HCSV)"));

		assertErrorLogNext(zipFile, s -> s.length() == 0);

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
		Assert.assertTrue(outputSeen);
	}
}
