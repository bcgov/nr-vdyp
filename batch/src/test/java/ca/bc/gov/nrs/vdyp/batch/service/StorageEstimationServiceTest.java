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
import org.springframework.batch.core.BatchStatus;
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
		batchProperties.getReader().setDefaultChunkSize(150);
		batchProperties.getStorage().setThresholdPercent(115);
		batchProperties.getStorage().setUnknownPolygonCountPlaceholder(600000);
		batchProperties.getStorage().setBytesPerCompleteLine(200);
		batchProperties.getStorage().setBytesPerInputLine(100);
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
	void testComputeStorageStatus_NoRunningJobs_ExpectedBytesIsZero() {
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of());

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(0L, status.expectedBytes());
		assertEquals(115, status.thresholdPercent());
	}

	@Test
	void testComputeStorageStatus_JobStillDownloadingInput_UsesPlaceholderPolygonCount() {
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());
		job.setExecutionContext(new ExecutionContext());

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(600000L * 4100L, status.expectedBytes());
	}

	static Stream<Arguments> computeStorageStatusExpectedBytesCases() {
		return Stream.of(
				Arguments.of(
						"UsesJobsOwnAgeParameters", 10, "{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\"}",
						33000L
				),
				Arguments.of(
						"ReflectsThatJobsFootprint", 2,
						"{\"ageStart\":\"10\",\"ageEnd\":\"50\",\"ageIncrement\":\"10\"}", 1800L
				),
				Arguments.of(
						"UsesYearStartYearEndWhenAgeFieldsAreNull", 10,
						"{\"ageStart\":null,\"ageEnd\":null,\"yearStart\":\"2000\",\"yearEnd\":\"2080\","
								+ "\"ageIncrement\":\"5\"}",
						33000L
				),
				Arguments.of(
						"MissingAgeIncrementFallsBackToConfiguredDefaults", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":null}", 41000L
				), Arguments.of("MissingParametersJsonFallsBackToConfiguredDefaults", 10, null, 41000L),
				Arguments.of("UnparseableParametersJsonFallsBackToConfiguredDefaults", 10, "not valid json", 41000L),
				Arguments.of(
						"ErrorLoggingRequested_AddsReasonableErrorBytes", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableErrorLogging\"]}",
						53480L
				),
				Arguments.of(
						"DebugLoggingRequested_AddsOptionalDebugLogBytes", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableDebugLogging\"]}",
						73960L
				),
				Arguments.of(
						"BothLoggingOptionsRequested_AddsBothBuffers", 10,
						"{\"ageStart\":\"0\",\"ageEnd\":\"80\",\"ageIncrement\":\"5\","
								+ "\"selectedExecutionOptions\":[\"doEnableErrorLogging\",\"doEnableDebugLogging\"]}",
						94440L
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

		assertEquals(16400L, status.expectedBytes());
	}

	@Test
	void testComputeStorageStatus_WorkerProgressAndTotalBothPresent_PrefersActualProgress() {
		// Per the VDYP-1274 algorithm, a running job's expected footprint should track polygons actually
		// completed so far, not its final declared total, so early progress on a large job keeps the estimate
		// - and thus the sensitivity of the out-of-spec check - proportionate to what's actually happened.
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());
		ExecutionContext jobCtx = new ExecutionContext();
		jobCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, 53042);
		job.setExecutionContext(jobCtx);

		StepExecution workerStep = new StepExecution("workerStep0", job);
		ExecutionContext stepCtx = new ExecutionContext();
		stepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 4);
		workerStep.setExecutionContext(stepCtx);
		job.addStepExecutions(java.util.List.of(workerStep));

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(16400L, status.expectedBytes());
	}

	@Test
	void testComputeStorageStatus_ActiveWorkersInFlight_AddsChunkSizePerActiveWorker() {
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());
		job.setExecutionContext(new ExecutionContext());

		StepExecution workerStep0 = new StepExecution("workerStep0", job);
		ExecutionContext step0Ctx = new ExecutionContext();
		step0Ctx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 4);
		workerStep0.setExecutionContext(step0Ctx);
		workerStep0.setStatus(BatchStatus.STARTED);

		StepExecution workerStep1 = new StepExecution("workerStep1", job);
		workerStep1.setExecutionContext(new ExecutionContext());
		workerStep1.setStatus(BatchStatus.STARTED);

		job.addStepExecutions(java.util.List.of(workerStep0, workerStep1));

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(304L * 4100L, status.expectedBytes());
	}

	@Test
	void testComputeStorageStatus_NoProgressButWorkersActive_UsesInFlightEstimateOnly() {
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());
		job.setExecutionContext(new ExecutionContext());

		StepExecution workerStep = new StepExecution("workerStep0", job);
		workerStep.setExecutionContext(new ExecutionContext());
		workerStep.setStatus(BatchStatus.STARTED);
		job.addStepExecutions(java.util.List.of(workerStep));

		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		StorageEstimationService.StorageStatus status = service.computeStorageStatus();

		assertEquals(150L * 4100L, status.expectedBytes());
	}

	@ParameterizedTest(name = "actual={0} threshold={1} outOfSpec={2}")
	@CsvSource(
		{ "1150, 115, false", // at the ratio boundary -> not out of spec
				"1151, 115, true", // just over the ratio boundary -> out of spec
				"900, 115, false" } // under the ratio boundary -> not out of spec
	)
	void testIsOutOfSpec(long actualBytes, int thresholdPercent, boolean expectedOutOfSpec) {
		assertEquals(expectedOutOfSpec, service.isOutOfSpec(1000L, actualBytes, thresholdPercent));
	}

	@Test
	void testIsOutOfSpec_NoRunningJobsButActualUsageNonZero_OutOfSpec() {
		assertTrue(service.isOutOfSpec(0L, 1L, 115));
	}

	@Test
	void testIsOutOfSpec_NoRunningJobsAndNoUsage_NotOutOfSpec() {
		assertFalse(service.isOutOfSpec(0L, 0L, 115));
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
