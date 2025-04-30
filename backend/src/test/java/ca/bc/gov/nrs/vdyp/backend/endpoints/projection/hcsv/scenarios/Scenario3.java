package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class Scenario3 extends Scenario {

	private static final Logger logger = LoggerFactory.getLogger(Scenario3.class);

	@Inject
	Scenario3(TestHelper testHelper) {
		super(testHelper);
	}

	private Path resourceFolderPath;

	@BeforeEach
	void setup() {
		resourceFolderPath = Path.of(super.scenariosResourcePath.toString(), "scenario3");
	}

	@Test
	void testProjectionHscvVri_testNoProgressLogging() throws IOException {

		Parameters parameters = new Parameters();
		parameters.ageStart(10).ageEnd(100)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.FORWARD_GROW_ENABLED)
				.addExcludedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_FILES);

		File polygonFile = testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY_VRI.csv").toFile();
		File layerFile = testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER_VRI.csv").toFile();

		InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonFile) //
				.multiPart(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerFile) //
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.csv", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));
		assertTrue(entry1Content.length() > 0);

		assertTrue(zipFile.getNextEntry() == null);
	}
}
