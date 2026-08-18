package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class StorageEstimationServiceTest {

	@Mock
	private JobExplorer jobExplorer;

	private BatchProperties batchProperties;
	private StorageEstimationService service;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.getStorage().setThresholdPercent(115);
		batchProperties.getStorage().setBytesPerCompleteLine(200);
		batchProperties.getStorage().setFallbackYearRange(200);
		batchProperties.getStorage().setFallbackAgeIncrement(10);
		batchProperties.getStorage().setReasonableErrorBytesPerPolygon(2048);
		batchProperties.getStorage().setOptionalDebugLogBytesPerPolygon(4096);
		batchProperties.setRootDirectory(System.getProperty("java.io.tmpdir"));

		service = new StorageEstimationService(jobExplorer, batchProperties, new ObjectMapper());
	}

	private JobExecution runningJobWithTotalPolygons(int totalPolygons, String parametersJson) {
		var params = new HashMap<String, JobParameter<?>>();
		if (parametersJson != null) {
			params.put(
					BatchConstants.Projection.PARAMETERS_JSON, new JobParameter<>(parametersJson, String.class, true)
			);
		}
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters(params));
		ExecutionContext jobCtx = new ExecutionContext();
		jobCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, totalPolygons);
		job.setExecutionContext(jobCtx);
		return job;
	}

	@Test
	void testComputeStorageStatus_NoRunningJobs_ExpectedBytesIsZeroAndNotOutOfSpec() {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(0L, status.expectedBytes());
		assertFalse(status.outOfSpec());
		assertEquals(115, status.thresholdPercent());
	}

	static Stream<Arguments> computeStorageStatusExpectedBytesCases() {
		return Stream.of(
				Arguments.of(
						"UsesJobsOwnAgeParameters", 10, "{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\"}",
						32000L
				),
				Arguments.of(
						"ReflectsThatJobsFootprint", 2,
						"{\"ageStart\":\"10\",\"ageEnd\":\"50\",\"ageIncrement\":\"10\"}", 1600L
				),
				Arguments.of(
						"UsesYearStartYearEndWhenAgeFieldsAreNull", 10,
						"{\"ageStart\":null,\"ageEnd\":null,\"yearStart\":\"2000\",\"yearEnd\":\"2080\","
								+ "\"ageIncrement\":\"5\"}",
						32000L
				),
				Arguments.of(
						"MissingAgeIncrementFallsBackToConfiguredDefaults", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":null}", 40000L
				), Arguments.of("MissingParametersJsonFallsBackToConfiguredDefaults", 10, null, 40000L),
				Arguments.of("UnparseableParametersJsonFallsBackToConfiguredDefaults", 10, "not valid json", 40000L),
				Arguments.of(
						"ErrorLoggingRequested_AddsReasonableErrorBytes", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableErrorLogging\"]}",
						52480L
				),
				Arguments.of(
						"DebugLoggingRequested_AddsOptionalDebugLogBytes", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableDebugLogging\"]}",
						72960L
				),
				Arguments.of(
						"BothLoggingOptionsRequested_AddsBothBuffers", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableErrorLogging\",\"doEnableDebugLogging\"]}",
						93440L
				)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("computeStorageStatusExpectedBytesCases")
	void testComputeStorageStatus_ExpectedBytes(
			String caseName, int totalPolygons, String parametersJson, long expectedBytes
	) {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob"))
				.thenReturn(Set.of(runningJobWithTotalPolygons(totalPolygons, parametersJson)));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(expectedBytes, status.expectedBytes());
	}

	@Test
	void testComputeStorageStatus_NoTotalPolygonsInContext_SumsFromWorkerStepProgress() {
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());
		job.setExecutionContext(new ExecutionContext());

		StepExecution workerStep = new StepExecution("workerStep0", job);
		ExecutionContext stepCtx = new ExecutionContext();
		stepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 4);
		workerStep.setExecutionContext(stepCtx);
		job.addStepExecutions(java.util.List.of(workerStep));

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(16000L, status.expectedBytes());
	}

	@ParameterizedTest(name = "actual={0} threshold={1} outOfSpec={2}")
	@CsvSource({ "1150, 115, false", "1151, 115, true", "900, 115, false" })
	void testIsOutOfSpec(long actualBytes, int thresholdPercent, boolean expectedOutOfSpec) {
		assertEquals(expectedOutOfSpec, service.isOutOfSpec(1000L, actualBytes, thresholdPercent));
	}

	@Test
	void testComputeStorageStatus_RootDirectoryMissing_UsageIsZero() {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());
		batchProperties.setRootDirectory(
				System.getProperty("java.io.tmpdir") + java.io.File.separator + "vdyp-storage-test-does-not-exist"
		);

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(0L, status.usedBytes());
		assertEquals(0L, status.totalBytes());
		assertEquals(0.0, status.percentFull());
	}

	@Test
	void testComputeStorageStatus_RootDirectoryExists_ReportsNonZeroTotalCapacity() throws IOException {
		Path tempDir = Files.createTempDirectory("vdyp-storage-test");
		try {
			batchProperties.setRootDirectory(tempDir.toString());
			when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

			StorageEstimationService.StorageStatus status = service.computeStorageStatus();

			assertTrue(status.totalBytes() > 0);
			assertTrue(status.percentFull() >= 0.0 && status.percentFull() <= 100.0);
		} finally {
			Files.deleteIfExists(tempDir);
		}
	}
}
