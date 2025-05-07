package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.csvRowContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class Scenario3 extends Scenario {

	@Inject
	Scenario3(TestHelper testHelper) {
		super(testHelper, "scenario3");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionHscvVri_testNoProgressLogging() throws IOException {

		Parameters parameters = new Parameters();
		parameters.ageStart(10).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.FORWARD_GROW_ENABLED)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)
				.addExcludedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES);

		var zipInputStream = super.runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		Assert.assertEquals("YieldTable.csv", entry1.getName());

		String[] csvLines = new String(testHelper.readZipEntry(zipFile, entry1)).split("\n");
		assertThat(csvLines, arrayWithSize(2));
		assertThat(csvLines[0], startsWith("\"TABLE_NUM\",\"FEATURE_ID\""));

		assertThat(
				csvLines[1], csvRowContaining(
						"1", is(13919428), "", "093C090", //
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

		assertTrue(zipFile.getNextEntry() == null);
	}
}
