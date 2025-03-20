package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class StubHcsvProjectionEndpointTest {

	private final TestHelper testHelper;

	@Inject
	StubHcsvProjectionEndpointTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testProjectionHscv_shouldReturnStatusOK() throws IOException {

		Path resourceFolderPath = Path.of("VDYP7Console-sample-files", FileHelper.HCSV, FileHelper.VDYP_240);

		Parameters parameters = testHelper.addSelectedOptions(
				new Parameters().ageStart(10).ageEnd(20), //
				Parameters.ExecutionOption.DO_ENABLE_DEBUG_LOGGING,
				Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING,
				Parameters.ExecutionOption.DO_ENABLE_ERROR_LOGGING
		);

		// Included to generate JSON text of parameters as needed
//		ObjectMapper mapper = new ObjectMapper();
//		String serializedParametersText = mapper.writeValueAsString(parameters);

		InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY.csv"))
				) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER.csv"))
				) //
				.post("/projection/hcsv?trialRun=true") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.csv", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));
		assertTrue(entry1Content.length() == 0);

		ZipEntry entry2 = zipFile.getNextEntry();
		assertEquals("ProgressLog.txt", entry2.getName());
		String entry2Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry2Content.contains("starting projection (type HCSV)"));

		ZipEntry entry3 = zipFile.getNextEntry();
		assertEquals("ErrorLog.txt", entry3.getName());
		String entry3Content = new String(testHelper.readZipEntry(zipFile, entry3));
		assertTrue(entry3Content.isBlank());

		ZipEntry entry4 = zipFile.getNextEntry();
		assertEquals("DebugLog.txt", entry4.getName());
		String entry4Content = new String(testHelper.readZipEntry(zipFile, entry4));
		assertTrue(entry4Content.startsWith(LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
	}

	@Test
	void testProjectionHscv_testNoProgressLogging() throws IOException {

		Path resourceFolderPath = Path.of("VDYP7Console-sample-files", FileHelper.HCSV, FileHelper.VDYP_240);

		Parameters parameters = new Parameters().ageStart(10).ageEnd(20);

		InputStream zipInputStream = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY.csv"))
				) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_LAYER.csv"))
				) //
				.post("/projection/hcsv?trialRun=true") //
				.then().statusCode(201) //
				.and().contentType("application/octet-stream") //
				.and().header("content-disposition", Matchers.startsWith("attachment;filename=\"vdyp-output-")) //
				.extract().body().asInputStream();

		ZipInputStream zipFile = new ZipInputStream(zipInputStream);
		ZipEntry entry1 = zipFile.getNextEntry();
		assertEquals("YieldTable.csv", entry1.getName());
		String entry1Content = new String(testHelper.readZipEntry(zipFile, entry1));
		assertTrue(entry1Content.length() == 0);

		ZipEntry entry2 = zipFile.getNextEntry();
		assertEquals("ProgressLog.txt", entry2.getName());
		String entry2Content = new String(testHelper.readZipEntry(zipFile, entry2));
		assertTrue(entry2Content.isBlank());

		ZipEntry entry3 = zipFile.getNextEntry();
		assertEquals("ErrorLog.txt", entry3.getName());
		String entry3Content = new String(testHelper.readZipEntry(zipFile, entry3));
		assertTrue(entry3Content.isBlank());

		ZipEntry entry4 = zipFile.getNextEntry();
		assertEquals("DebugLog.txt", entry4.getName());
		String entry4Content = new String(testHelper.readZipEntry(zipFile, entry4));
		assertTrue(entry4Content.isBlank());
	}
}
