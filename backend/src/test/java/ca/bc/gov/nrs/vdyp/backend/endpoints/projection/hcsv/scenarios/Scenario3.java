package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.csvRowContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
				.addExcludedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES)
				.addSelectedExecutionOptionsItem(
						Parameters.ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
				);

		var zipInputStream = super.runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		Assert.assertEquals("YieldTable.csv", entry1.getName());

		String[] csvLines = new String(TestHelper.readZipEntry(zipFile, entry1)).split("\n");
		assertThat(csvLines, arrayWithSize(184));
		assertThat(
				csvLines[0],
				startsWith(
						"\"TABLE_NUM\",\"FEATURE_ID\",\"DISTRICT\",\"MAP_ID\",\"POLYGON_ID\",\"LAYER_ID\",\"PROJECTION_YEAR\""
				)
		);

		assertThat(
				csvLines[1], csvRowContaining(
						"1", is(13919428), "", "093C090", "94833422", "1", "1843", "10", "PLI", closeTo(60.00), "SX",
						closeTo(40.00), "", "", "", "", "", "", "", "", closeTo(1.00), closeTo(9.79000),
						closeTo(1.32755), closeTo(0.25340), "", closeTo(20.60), closeTo(300.00), closeTo(10.000010), "",
						"", "", "", "", "Back"
				)
		);

		assertTrue(zipFile.getNextEntry() == null);
	}
}
