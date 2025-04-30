package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import java.nio.file.Path;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class Scenario {
	protected final TestHelper testHelper;
	protected final Path scenariosResourcePath;

	protected Scenario(TestHelper testHelper) {
		this.testHelper = testHelper;
		this.scenariosResourcePath = Path.of(FileHelper.TEST_DATA_FILES, "hcsv", "scenarios");
	}
}
