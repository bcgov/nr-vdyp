package ca.bc.gov.nrs.vdyp.batch.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Integration test for Scenario 1: End-to-end batch processing with single polygon test data. - This test verifies the
 * complete batch flow - Starting batch job via /api/batch/start endpoint - Verifying output against expected
 * YieldTable.csv
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
		properties = { "batch.root-directory=${java.io.tmpdir}/vdyp-batch-test",
				"batch.partition.default-partition-size=2" }
)
class Scenario1Test {

	private static final Logger logger = LoggerFactory.getLogger(Scenario1Test.class);

	private static final String TEST_DATA_PATH = "test-data/hcsv/single-polygon";
	private static final String POLYGON_FILE = TEST_DATA_PATH + "/VDYP7_INPUT_POLY.csv";
	private static final String LAYER_FILE = TEST_DATA_PATH + "/VDYP7_INPUT_LAYER.csv";
	private static final String PARAMETERS_FILE = "test-data/hcsv/parameters.json";
	private static final String EXPECTED_OUTPUT_FILE = TEST_DATA_PATH + "/YieldTable.csv";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${batch.root-directory}")
	private String batchRootDirectory;

	@Disabled("Scenario1Test")
	@Test
	@Timeout(value = 300) // 5 minutes max
	void testScenario1_SinglePolygonBatchProcessing() throws Exception {
		logger.info("========== Starting Scenario 1: Single Polygon Batch Processing ==========");

		// Step 1: Load test data files
		logger.info("Step 1: Loading test data files from resources");
		MockMultipartFile polygonFile = loadTestFile(POLYGON_FILE, "polygonFile", "VDYP7_INPUT_POLY.csv");
		MockMultipartFile layerFile = loadTestFile(LAYER_FILE, "layerFile", "VDYP7_INPUT_LAYER.csv");
		String parametersJson = loadTestFileAsString(PARAMETERS_FILE);

		logger.info("Loaded polygon file: {} bytes", polygonFile.getSize());
		logger.info("Loaded layer file: {} bytes", layerFile.getSize());
		logger.info("Loaded parameters: {} characters", parametersJson.length());

		// Step 2: Start batch job
		logger.info("Step 2: Starting batch job via /api/batch/start");
		UUID jobGuid = startBatchJob(polygonFile, layerFile, parametersJson);
		logger.info("Batch job started with GUID: {}", jobGuid);

		// Step 3: Wait for job completion
		logger.info("Step 3: Waiting for batch job to complete");
		Map<String, Object> finalStatus = waitForJobCompletion(jobGuid, Duration.ofMinutes(5));
		logger.info("Batch job completed with status: {}", finalStatus.get(BatchConstants.Job.STATUS));

		// Step 4: Verify job completed successfully
		logger.info("Step 4: Verifying job completion status");
		assertEquals("COMPLETED", finalStatus.get(BatchConstants.Job.STATUS), "Job should complete successfully");
		assertEquals(false, finalStatus.get("isRunning"), "Job should not be running");

		// Step 5: Find and verify the output ZIP file
		logger.info("Step 5: Locating output ZIP file");
		Path outputZipPath = findOutputZipFile(jobGuid);
		assertNotNull(outputZipPath, "Output ZIP file should exist");
		assertTrue(Files.exists(outputZipPath), "Output ZIP file should exist at: " + outputZipPath);
		logger.info("Found output ZIP file: {}", outputZipPath);

		// Step 6: Verify YieldTable.csv content
		logger.info("Step 6: Verifying YieldTable.csv content");
		try {
			verifyYieldTableContent(outputZipPath);
			logger.info("========== Scenario 1 Completed Successfully ==========");
		} catch (AssertionError e) {
			// Check if there were projection errors
			String errorLog = extractFileFromZip(outputZipPath, "ErrorLog.txt");
			if (errorLog != null && !errorLog.trim().isEmpty()) {
				logger.error("========== Projection Errors Detected ==========");
				logger.error(errorLog);
			}
			throw e;
		}
	}

	/**
	 * Loads a test file from the classpath resources.
	 */
	private MockMultipartFile loadTestFile(String resourcePath, String paramName, String originalFilename)
			throws IOException {
		try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (inputStream == null) {
				throw new IOException("Test file not found: " + resourcePath);
			}
			byte[] content = inputStream.readAllBytes();
			return new MockMultipartFile(paramName, originalFilename, "text/csv", content);
		}
	}

	/**
	 * Loads a test file content as String.
	 */
	private String loadTestFileAsString(String resourcePath) throws IOException {
		try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
			if (inputStream == null) {
				throw new IOException("Test file not found: " + resourcePath);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Starts a batch job by calling the /api/batch/start endpoint.
	 *
	 * @return The job GUID
	 */
	private UUID startBatchJob(MockMultipartFile polygonFile, MockMultipartFile layerFile, String parametersJson)
			throws Exception {
		MvcResult result = mockMvc.perform(
				MockMvcRequestBuilders.multipart("/api/batch/start").file(polygonFile).file(layerFile)
						.param("parameters", parametersJson).contentType(MediaType.MULTIPART_FORM_DATA)
		).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

		String responseBody = result.getResponse().getContentAsString();
		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

		assertNotNull(responseMap.get(BatchConstants.Job.GUID), "Response should contain job GUID");
		assertNotNull(responseMap.get(BatchConstants.Job.EXECUTION_ID), "Response should contain execution ID");

		String guidString = (String) responseMap.get(BatchConstants.Job.GUID);
		return UUID.fromString(guidString);
	}

	/**
	 * Waits for the batch job to complete by polling the status endpoint.
	 *
	 * @param jobGuid The job GUID to monitor
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

			// Log partition progress if available
			if (statusMap.containsKey("totalPartitions") && statusMap.containsKey("completedPartitions")) {
				Integer total = (Integer) statusMap.get("totalPartitions");
				Integer completed = (Integer) statusMap.get("completedPartitions");
				logger.info("Progress: {}/{} partitions completed", completed, total);
			}

			// Check if job has completed
			if (Boolean.FALSE.equals(isRunning)) {
				logger.info("Job completed with final status: {}", status);
				return statusMap;
			}

			// Check for failure status
			if ("FAILED".equals(status) || "STOPPED".equals(status)) {
				logger.error("Job ended with status: {}", status);
				return statusMap;
			}

			// Wait before next poll
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
	 * Finds the output ZIP file for the given job GUID.
	 */
	private Path findOutputZipFile(UUID jobGuid) throws Exception {
		// Find job execution to get the timestamp
		JobExecution jobExecution = findJobExecution(jobGuid);
		assertNotNull(jobExecution, "Job execution should be found");

		String jobTimestamp = jobExecution.getJobParameters().getString(BatchConstants.Job.TIMESTAMP);
		assertNotNull(jobTimestamp, "Job should have timestamp parameter");

		// Construct expected job directory path with prefix
		String jobDirName = BatchConstants.Job.BASE_FOLDER_PREFIX + "-" + jobGuid.toString();
		Path jobDir = Paths.get(batchRootDirectory, jobDirName);

		logger.info("Looking for job directory: {}", jobDir);

		if (!Files.exists(jobDir)) {
			// List all directories in batch root for debugging
			logger.error("Job directory not found. Directories in batch root:");
			Path batchRoot = Paths.get(batchRootDirectory);
			if (Files.exists(batchRoot)) {
				Files.list(batchRoot).filter(Files::isDirectory)
						.forEach(path -> logger.error(" - {}", path.getFileName()));
			} else {
				logger.error("Batch root directory does not exist: {}", batchRoot);
			}
			return null;
		}

		// Find the ZIP file in the job directory
		String zipFileName = String.format("vdyp-output-%s.zip", jobTimestamp);
		Path zipFilePath = jobDir.resolve(zipFileName);

		logger.info("Looking for ZIP file at: {}", zipFilePath);

		if (!Files.exists(zipFilePath)) {
			// List all files in job directory for debugging
			logger.error("ZIP file not found. Files in job directory:");
			Files.list(jobDir).forEach(path -> logger.error(" - {}", path.getFileName()));
			return null;
		}

		return zipFilePath;
	}

	/**
	 * Finds the JobExecution for a given job GUID.
	 */
	private JobExecution findJobExecution(UUID jobGuid) throws Exception {
		for (String jobName : jobExplorer.getJobNames()) {
			long instanceCount = jobExplorer.getJobInstanceCount(jobName);
			int start = 0;
			int chunkSize = 1000;

			while (start < instanceCount) {
				List<JobInstance> instances = jobExplorer.getJobInstances(jobName, start, chunkSize);

				for (JobInstance instance : instances) {
					for (JobExecution execution : jobExplorer.getJobExecutions(instance)) {
						String executionGuid = execution.getJobParameters().getString(BatchConstants.Job.GUID);
						if (jobGuid.toString().equals(executionGuid)) {
							return execution;
						}
					}
				}

				start += chunkSize;
			}
		}
		return null;
	}

	/**
	 * Verifies that the YieldTable.csv in the output ZIP matches the expected content.
	 */
	private void verifyYieldTableContent(Path zipFilePath) throws IOException {
		logger.info("Extracting YieldTable.csv from ZIP file");

		// Load expected content
		String expectedContent = loadExpectedYieldTableContent();
		List<String> expectedLines = parseCSVLines(expectedContent);

		logger.info("Expected YieldTable has {} lines", expectedLines.size());

		// Extract actual content from ZIP
		String actualContent = extractYieldTableFromZip(zipFilePath);
		List<String> actualLines = parseCSVLines(actualContent);

		logger.info("Actual YieldTable has {} lines", actualLines.size());

		// Compare line counts
		assertEquals(
				expectedLines.size(), actualLines.size(), "YieldTable should have same number of lines as expected"
		);

		// Compare header
		if (!expectedLines.isEmpty() && !actualLines.isEmpty()) {
			assertEquals(expectedLines.get(0), actualLines.get(0), "YieldTable header should match expected");
		}

		// Compare data rows (allowing for minor floating point differences)
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

	/**
	 * Loads the expected YieldTable.csv content from resources.
	 */
	private String loadExpectedYieldTableContent() throws IOException {
		return loadTestFileAsString(EXPECTED_OUTPUT_FILE);
	}

	/**
	 * Extracts YieldTable.csv content from the output ZIP file.
	 */
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

			// Try to compare as numbers with tolerance
			try {
				double expValue = Double.parseDouble(expField);
				double actValue = Double.parseDouble(actField);

				// Allow 0.1% relative difference or 0.00001 absolute difference
				double tolerance = Math.max(Math.abs(expValue) * 0.001, 0.00001);
				if (Math.abs(expValue - actValue) > tolerance) {
					return false;
				}
			} catch (NumberFormatException e) {
				// Not a number, compare as string
				if (!expField.equals(actField)) {
					return false;
				}
			}
		}

		return true;
	}
}
