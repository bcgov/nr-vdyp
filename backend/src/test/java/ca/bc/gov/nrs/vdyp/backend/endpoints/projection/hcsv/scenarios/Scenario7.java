package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv.scenarios;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ValidationMessageListResource;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ValidationMessageResource;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.AgeYearRangeCombinationKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.OutputFormat;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class Scenario7 extends Scenario {

	@Inject
	Scenario7(TestHelper testHelper) {
		super(testHelper, "scenario7");
	}

	@BeforeEach
	void setup() {
	}

	@Test
	void testAgeRangeForDifferentAgeRanges() throws IOException {

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
		).outputFormat(OutputFormat.CSV_YIELD_TABLE);

		// Age 0 is 1833
		parameters.ageStart(185).ageEnd(235);
		parameters.yearStart(2100).yearEnd(2150);
		parameters.setCombineAgeYearRange(AgeYearRangeCombinationKind.INTERSECT);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);
		ZipInputStream zipFile = new ZipInputStream(zipInputStream);

		var resultYieldTable = assertYieldTableNext(zipFile, s -> s.length() > 0);

		// Nothing, because layer age is 180 in 2013, which carries the age range to 2018 - 2068, and
		// that range doesn't intersect with 2100 - 2150.
		assertHasAgeRange(resultYieldTable, "13919428", "1");
		// Rows for 2138 - 2150 because layer age is 60 (rather than 180, as is the case for layer "1") in 2013,
		// which carries the age range to 2138 - 2188, leaving an intersection of the given range.
		assertHasAgeRange(resultYieldTable, "13919428", "2", allOfRange(2138, 2150));
		// 2013 only because this is a dead layer and year start/end and age start/end are max'd to year of
		// death (2013), which forces 2013 (only) into the table.
		assertHasAgeRange(resultYieldTable, "13919428", "D", 2013);

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

	@Test
	void testEmptyResults() throws IOException {

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
		).outputFormat(OutputFormat.CSV_YIELD_TABLE);

		// Age 0 is 1833
		parameters.ageStart(15).ageEnd(65);
		parameters.yearStart(2100).yearEnd(2150);
		parameters.setCombineAgeYearRange(AgeYearRangeCombinationKind.INTERSECT);

		InputStream zipInputStream = runExpectedSuccessfulRequest(
				"VDYP7_INPUT_POLY_VRI.csv", "VDYP7_INPUT_LAYER_VRI.csv", parameters
		);
		ZipInputStream zipFile = new ZipInputStream(zipInputStream);

		var resultYieldTable = assertYieldTableNext(zipFile, s -> s.length() > 0);

		// Nothing, because layer age is 180 in 2013, which carries the age range to 1838 - 1868, and
		// that range doesn't intersect with 2100 - 2150.
		assertHasAgeRange(resultYieldTable, "13919428", "1");
		// Rows for 2138 - 2150 because layer age is 60 (rather than 180, as is the case for layer "1") in 2013,
		// which carries the age range to 1958 - 2008, leaving an intersection of the given range.
		assertHasAgeRange(resultYieldTable, "13919428", "2");
		// 2013 only because this is a dead layer and year start/end and age start/end are max'd to year of
		// death (2013), which forces 2013 (only) into the table.
		assertHasAgeRange(resultYieldTable, "13919428", "D", 2013);

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

	private static ObjectMapper mapper = new ObjectMapper();

	@Test
	void testInvalidIntersectionType() throws IOException {

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
		).outputFormat(OutputFormat.CSV_YIELD_TABLE);

		// Age 0 is 1833
		parameters.ageStart(185).ageEnd(235);
		parameters.yearStart(2100).yearEnd(2150);
		parameters.setCombineAgeYearRange(AgeYearRangeCombinationKind.UNION);

		Object result = given().basePath(TestHelper.ROOT_PATH).when() //
				.multiPart(ParameterNames.PROJECTION_PARAMETERS, parameters, MediaType.APPLICATION_JSON) //
				.multiPart(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						testHelper.getResourceFile(scenarioResourcePath, "VDYP7_INPUT_POLY_VRI.csv").toFile()
				) //
				.multiPart(
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						testHelper.getResourceFile(scenarioResourcePath, "VDYP7_INPUT_LAYER_VRI.csv").toFile()
				) //
				.post("/projection/hcsv?trialRun=false") //
				.then().statusCode(400) //
				.extract().asByteArray();

		ValidationMessageListResource e = mapper.readValue((byte[]) result, ValidationMessageListResource.class);

		Assert.assertEquals(e.getMessages().size(), 1);
		var expectedMessage = new ValidationMessageResource(
				new ValidationMessage(ValidationMessageKind.UNSUPPORTED_COMBINE_AGE_YEAR_RANGE_OPTION, "union")
		);
		Assert.assertEquals(expectedMessage, e.getMessages().get(0));
	}
}
