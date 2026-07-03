package ca.bc.gov.nrs.vdyp.batch.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.service.ComsFileService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Integration test for Scenario 1: End-to-end batch processing with single polygon test data. - This test verifies the
 * complete batch flow - Starting batch job via /api/batch/startWithGUIDs endpoint - Verifying output against expected
 * YieldTable.csv
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(
		properties = { "batch.root-directory=${java.io.tmpdir}/vdyp-batch-test",
				"batch.partition.default-number-of-partitions=2" }
)
class Scenario1Test {

	private static final Logger logger = LoggerFactory.getLogger(Scenario1Test.class);

	private static final String TEST_DATA_PATH = "test-data/hcsv/single-polygon";
	private static final String POLYGON_FILE = TEST_DATA_PATH + "/VDYP7_INPUT_POLY.csv";
	private static final String LAYER_FILE = TEST_DATA_PATH + "/VDYP7_INPUT_LAYER.csv";
	private static final String PARAMETERS_FILE = "test-data/hcsv/parameters.json";
	private static final String EXPECTED_OUTPUT_FILE = TEST_DATA_PATH + "/YieldTable.csv";

	private static final UUID PROJECTION_GUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID POLYGON_FILESET_GUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID LAYER_FILESET_GUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID RESULT_FILESET_GUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID POLYGON_COMS_GUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID LAYER_COMS_GUID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	private static final UUID RESULT_COMS_GUID = UUID.fromString("77777777-7777-7777-7777-777777777777");

	@MockitoBean
	private VdypClient vdypClient;

	@MockitoBean
	private ComsFileService comsFileService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private volatile Path capturedZipPath;

	@BeforeEach
	void setupMocks() throws Exception {
		capturedZipPath = null;

		VdypProjectionDetails projectionDetails = new VdypProjectionDetails(
				PROJECTION_GUID.toString(),
				new VdypProjectionDetails.VdypProjectionFileSet(POLYGON_FILESET_GUID.toString()),
				new VdypProjectionDetails.VdypProjectionFileSet(LAYER_FILESET_GUID.toString()),
				new VdypProjectionDetails.VdypProjectionFileSet(RESULT_FILESET_GUID.toString()), "TestReport"
		);
		when(vdypClient.getProjectionDetails(PROJECTION_GUID.toString())).thenReturn(projectionDetails);
		when(vdypClient.getFileSetFiles(PROJECTION_GUID.toString(), POLYGON_FILESET_GUID.toString()))
				.thenReturn(List.of(new FileMappingDetails("filemap-poly", POLYGON_COMS_GUID.toString())));
		when(vdypClient.getFileSetFiles(PROJECTION_GUID.toString(), LAYER_FILESET_GUID.toString()))
				.thenReturn(List.of(new FileMappingDetails("filemap-layer", LAYER_COMS_GUID.toString())));
		when(vdypClient.getFileSetFiles(PROJECTION_GUID.toString(), RESULT_FILESET_GUID.toString()))
				.thenReturn(List.of(new FileMappingDetails("filemap-result", RESULT_COMS_GUID.toString())));

		byte[] polygonBytes = loadTestFileBytes(POLYGON_FILE);
		byte[] layerBytes = loadTestFileBytes(LAYER_FILE);

		doAnswer(invocation -> {
			UUID objectId = invocation.getArgument(0);
			Path target = invocation.getArgument(1);
			if (POLYGON_COMS_GUID.equals(objectId)) {
				Files.write(target, polygonBytes);
			} else if (LAYER_COMS_GUID.equals(objectId)) {
				Files.write(target, layerBytes);
			}
			return null;
		}).when(comsFileService).fetchObjectToFile(any(UUID.class), any(Path.class));

		// Capture the result ZIP before ResultPersistenceTasklet deletes the job directory
		doAnswer(invocation -> {
			Path zipPath = invocation.getArgument(1);
			capturedZipPath = Files.createTempFile("scenario1-output", ".zip");
			Files.copy(zipPath, capturedZipPath, StandardCopyOption.REPLACE_EXISTING);
			return null;
		}).when(comsFileService).updateStoredObject(any(UUID.class), any(Path.class), any(String.class));
	}

	@AfterEach
	void cleanup() throws IOException {
		if (capturedZipPath != null) {
			Files.deleteIfExists(capturedZipPath);
		}
	}

	@Test
	@Timeout(value = 300) // 5 minutes max
	void testScenario1_SinglePolygonBatchProcessing() throws Exception {
		logger.info("========== Starting Scenario 1: Single Polygon Batch Processing ==========");

		// Step 1: Load projection parameters
		logger.info("Step 1: Loading projection parameters from resources");
		String parametersJson = loadTestFileAsString(PARAMETERS_FILE);
		logger.info("Loaded parameters: {} characters", parametersJson.length());

		// Step 2: Start batch job via GUID-based endpoint
		logger.info("Step 2: Starting batch job via /api/batch/startWithGUIDs");
		UUID jobGuid = startBatchJob(parametersJson);
		logger.info("Batch job started with GUID: {}", jobGuid);

		// Step 3: Wait for job completion
		logger.info("Step 3: Waiting for batch job to complete");
		Map<String, Object> finalStatus = waitForJobCompletion(jobGuid, Duration.ofMinutes(5));
		logger.info("Batch job completed with status: {}", finalStatus.get(BatchConstants.Job.STATUS));

		// Step 4: Verify job completed successfully
		logger.info("Step 4: Verifying job completion status");
		assertEquals("COMPLETED", finalStatus.get(BatchConstants.Job.STATUS), "Job should complete successfully");
		assertEquals(false, finalStatus.get("isRunning"), "Job should not be running");

		// Step 5: Verify result ZIP was captured from persistence step
		logger.info("Step 5: Verifying result ZIP was captured from persistence step");
		assertNotNull(capturedZipPath, "Result ZIP should have been persisted via COMS mock");
		assertTrue(Files.exists(capturedZipPath), "Captured ZIP file should exist");

		// Step 6: Verify YieldTable.csv content
		logger.info("Step 6: Verifying YieldTable.csv content");
		try {
			verifyYieldTableContent(capturedZipPath);
			logger.info("========== Scenario 1 Completed Successfully ==========");
		} catch (AssertionError e) {
			String errorLog = extractFileFromZip(capturedZipPath, "ErrorLog.txt");
			if (errorLog != null && !errorLog.trim().isEmpty()) {
				logger.error("========== Projection Errors Detected ==========");
				logger.error(errorLog);
			}
			throw e;
		}
	}

	private byte[] loadTestFileBytes(String resourcePath) throws IOException {
		try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (inputStream == null) {
				throw new IOException("Test file not found: " + resourcePath);
			}
			return inputStream.readAllBytes();
		}
	}

	private String loadTestFileAsString(String resourcePath) throws IOException {
		try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (inputStream == null) {
				throw new IOException("Test file not found: " + resourcePath);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Starts a batch job by calling the /api/batch/startWithGUIDs endpoint.
	 *
	 * @return The internal job GUID
	 */
	private UUID startBatchJob(String parametersJson) throws Exception {
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.multipart("/api/batch/startWithGUIDs")
						.param("projectionGUID", PROJECTION_GUID.toString())
						.param("projectionParametersJson", parametersJson).contentType(MediaType.MULTIPART_FORM_DATA)
		).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

		String responseBody = result.getResponse().getContentAsString();
		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

		assertNotNull(responseMap.get(BatchConstants.Job.GUID), "Response should contain job GUID");
		assertNotNull(responseMap.get(BatchConstants.Job.EXECUTION_ID), "Response should contain execution ID");

		return UUID.fromString((String) responseMap.get(BatchConstants.Job.GUID));
	}

	/**
	 * Waits for the batch job to complete by polling the status endpoint.
	 *
	 * @param jobGuid The internal job GUID to monitor
	 * @param timeout Maximum time to wait
	 * @return The final status map
	 */
	@SuppressWarnings("java:S2925") // Sleep is acceptable for polling in integration tests
	private Map<String, Object> waitForJobCompletion(UUID jobGuid, Duration timeout) throws Exception {
		long startTime = System.currentTimeMillis();
		long timeoutMillis = timeout.toMillis();
		int pollIntervalSeconds = 2;

		while (System.currentTimeMillis() - startTime < timeoutMillis) {
			MvcResult result = mockMvc.perform(
					MockMvcRequestBuilders.get("/api/batch/status/{jobGuid}", jobGuid)
							.accept(MediaType.APPLICATION_JSON)
			).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

			String responseBody = result.getResponse().getContentAsString();
			@SuppressWarnings("unchecked")
			Map<String, Object> statusMap = objectMapper.readValue(responseBody, Map.class);

			String status = (String) statusMap.get(BatchConstants.Job.STATUS);
			Boolean isRunning = (Boolean) statusMap.get("isRunning");

			logger.debug("Job status: {}, isRunning: {}", status, isRunning);

			if (Boolean.FALSE.equals(isRunning)) {
				logger.info("Job completed with final status: {}", status);
				return statusMap;
			}

			if ("FAILED".equals(status) || "STOPPED".equals(status)) {
				logger.error("Job ended with status: {}", status);
				return statusMap;
			}

			try {
				TimeUnit.SECONDS.sleep(pollIntervalSeconds);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Job monitoring was interrupted", e);
			}
		}

		throw new AssertionError("Job did not complete within timeout: " + timeout);
	}

	/**
	 * Verifies that the YieldTable.csv in the output ZIP matches the expected content.
	 */
	private void verifyYieldTableContent(Path zipFilePath) throws IOException {
		logger.info("Extracting YieldTable.csv from ZIP file");

		String expectedContent = loadTestFileAsString(EXPECTED_OUTPUT_FILE);
		List<String> expectedLines = parseCSVLines(expectedContent);

		logger.info("Expected YieldTable has {} lines", expectedLines.size());

		String actualContent = extractYieldTableFromZip(zipFilePath);
		List<String> actualLines = parseCSVLines(actualContent);

		logger.info("Actual YieldTable has {} lines", actualLines.size());

		assertEquals(
				expectedLines.size(), actualLines.size(), "YieldTable should have same number of lines as expected"
		);

		if (!expectedLines.isEmpty() && !actualLines.isEmpty()) {
			assertEquals(expectedLines.get(0), actualLines.get(0), "YieldTable header should match expected");
		}

		for (int i = 1; i < expectedLines.size(); i++) {
			String expectedLine = expectedLines.get(i);
			String actualLine = actualLines.get(i);

			if (!compareCsvLines(expectedLine, actualLine)) {
				logger.error("Mismatch at line {}", i + 1);
				logger.error("Expected: {}", expectedLine);
				logger.error("Actual: {}", actualLine);
				fail(String.format("Line %d does not match expected content", i + 1));
			}
		}

		logger.info("YieldTable.csv verification passed - all {} lines match", expectedLines.size());
	}

	private String extractYieldTableFromZip(Path zipFilePath) throws IOException {
		return extractFileFromZip(zipFilePath, "YieldTable.csv");
	}

	/**
	 * Extracts a specific file content from the output ZIP file.
	 */
	private String extractFileFromZip(Path zipFilePath, String fileName) throws IOException {
		try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
			ZipEntry entry = zipFile.stream().filter(e -> e.getName().endsWith(fileName)).findFirst().orElse(null);

			if (entry == null) {
				logger.warn("{} not found in output ZIP", fileName);
				return null;
			}

			logger.debug("Found {} in ZIP: {}", fileName, entry.getName());

			try (
					var inputStream = zipFile.getInputStream(entry);
					var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
			) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		}
	}

	/**
	 * Parses CSV content into list of lines, filtering out empty lines.
	 */
	private List<String> parseCSVLines(String content) {
		List<String> lines = new ArrayList<>();
		for (String line : content.split("\n")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				lines.add(trimmed);
			}
		}
		return lines;
	}

	/**
	 * Compares two CSV lines, allowing for minor floating point differences.
	 *
	 * @param expected Expected CSV line
	 * @param actual   Actual CSV line
	 * @return true if lines match (within tolerance for numeric values)
	 */
	private boolean compareCsvLines(String expected, String actual) {
		String[] expectedFields = expected.split(",", -1);
		String[] actualFields = actual.split(",", -1);

		if (expectedFields.length != actualFields.length) {
			return false;
		}

		for (int i = 0; i < expectedFields.length; i++) {
			String expField = expectedFields[i].replace("\"", "").trim();
			String actField = actualFields[i].replace("\"", "").trim();

			try {
				double expValue = Double.parseDouble(expField);
				double actValue = Double.parseDouble(actField);

				double tolerance = Math.max(Math.abs(expValue) * 0.001, 0.00001);
				if (Math.abs(expValue - actValue) > tolerance) {
					return false;
				}
			} catch (NumberFormatException e) {
				if (!expField.equals(actField)) {
					return false;
				}
			}
		}

		return true;
	}
}
