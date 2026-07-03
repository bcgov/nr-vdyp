package ca.bc.gov.nrs.vdyp.batch.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.ecore.utils.CsvRecordBeanHelper;

class BatchUtilsTest {
	@Test
	void testCreateJobFolderName_WithEmptyPrefix() {
		String prefix = "";
		String guid = "123e4567-e89b-12d3-a456-426614174000";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("-123e4567-e89b-12d3-a456-426614174000", result);
	}

	@Test
	void testCreateJobFolderName_WithEmptyGuid() {
		String prefix = "vdyp-batch";
		String guid = "";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("vdyp-batch-", result);
	}

	@Test
	void testCreateJobFolderName_Complete() {
		String prefix = "job";
		String guid = "123e4567-e89b-12d3-a456-426614174000";

		String result = BatchUtils.createJobFolderName(prefix, guid);

		assertEquals("job-123e4567-e89b-12d3-a456-426614174000", result);
		// Verify format includes both components
		assertTrue(result.contains(prefix));
		assertTrue(result.contains(guid));
	}

	@Test
	void testCreateJobTimestamp() {
		String timestamp = BatchUtils.createJobTimestamp();

		assertNotNull(timestamp);
		// Verify format: yyyy_MM_dd_HH_mm_ss_SSSS
		assertTrue(timestamp.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));

		// Verify it can be parsed back using the expected format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
		assertDoesNotThrow(() -> LocalDateTime.parse(timestamp, formatter));
	}

	@Test
	void testCreateJobTimestamp_UniqueValues() {
		String timestamp1 = BatchUtils.createJobTimestamp();
		String timestamp2 = BatchUtils.createJobTimestamp();

		assertNotNull(timestamp1);
		assertNotNull(timestamp2);

		assertTrue(timestamp1.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
		assertTrue(timestamp2.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{4}"));
	}

	@Test
	void testDateTimeFormatterForFilenames() {
		// Test that createJobTimestamp produces the expected format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");

		assertNotNull(formatter);

		// Test formatting a known date
		LocalDateTime testDate = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123_400_000);
		String formatted = formatter.format(testDate);

		assertEquals("2024_01_15_10_30_45_1234", formatted);

		// Verify createJobTimestamp produces parseable output
		String timestamp = BatchUtils.createJobTimestamp();
		assertDoesNotThrow(() -> LocalDateTime.parse(timestamp, formatter));
	}

	@Test
	void testDateTimeFormatterForFilenames_ParseAndFormat() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
		String timestamp = "2024_12_31_23_59_59_9999";

		LocalDateTime parsed = LocalDateTime.parse(timestamp, formatter);
		String reformatted = formatter.format(parsed);

		assertEquals(timestamp, reformatted);
	}

	@Test
	void buildFinalProgress_emptySteps_returnsZeroCounts() {
		JobExecution jobExecution = mock(JobExecution.class);
		when(jobExecution.getExecutionContext()).thenReturn(new ExecutionContext());
		when(jobExecution.getStepExecutions()).thenReturn(Collections.emptyList());

		VDYPProjectionProgressUpdate result = BatchUtils.buildFinalProgress("job-guid", jobExecution);

		assertEquals("job-guid", result.batchJobGUID());
		assertEquals(0, result.totalPolygons());
		assertEquals(0, result.polygonsProcessed());
		assertEquals(0, result.projectionErrors());
		assertEquals(0, result.polygonsSkipped());
		assertNull(result.batchFailureTypeCode());
		assertNull(result.failureMessage());
	}

	@Test
	void buildFinalProgress_workerSteps_accumulatesCounts() {
		JobExecution jobExecution = mock(JobExecution.class);
		ExecutionContext jobContext = new ExecutionContext();
		jobContext.putInt(BatchConstants.Job.TOTAL_POLYGONS, 10);
		when(jobExecution.getExecutionContext()).thenReturn(jobContext);

		StepExecution workerStep = mock(StepExecution.class);
		when(workerStep.getStepName()).thenReturn(BatchConstants.Job.WORKER_STEP_NAME + ":partition0");
		ExecutionContext stepContext = new ExecutionContext();
		stepContext.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 8);
		stepContext.putInt(BatchConstants.Job.PROJECTION_ERRORS, 1);
		stepContext.putInt(BatchConstants.Job.POLYGONS_SKIPPED, 1);
		when(workerStep.getExecutionContext()).thenReturn(stepContext);
		when(jobExecution.getStepExecutions()).thenReturn(List.of(workerStep));

		VDYPProjectionProgressUpdate result = BatchUtils.buildFinalProgress("job-guid", jobExecution);

		assertEquals(10, result.totalPolygons());
		assertEquals(8, result.polygonsProcessed());
		assertEquals(1, result.projectionErrors());
		assertEquals(1, result.polygonsSkipped());
	}

	@Test
	void buildFailureProgress_inputStepFailure_includesInputFailureDetails() {
		JobExecution jobExecution = failureJobExecution(
				BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME, null,
				List.of(new RuntimeException("Could not fetch input files"))
		);

		VDYPProjectionProgressUpdate result = BatchUtils.buildFailureProgress("job-guid", jobExecution);

		assertEquals(BatchConstants.FailureType.INPUT, result.batchFailureTypeCode());
		assertEquals("Could not fetch input files", result.failureMessage());
	}

	@Test
	void buildFailureProgress_workerStepFailure_includesProcessFailureDetails() {
		JobExecution jobExecution = failureJobExecution(
				BatchConstants.Job.WORKER_STEP_NAME + ":partition0",
				new ExitStatus(ExitStatus.FAILED.getExitCode(), "Projection failed in worker step"),
				Collections.emptyList()
		);

		VDYPProjectionProgressUpdate result = BatchUtils.buildFailureProgress("job-guid", jobExecution);

		assertEquals(BatchConstants.FailureType.PROCESS, result.batchFailureTypeCode());
		assertEquals("Projection failed in worker step", result.failureMessage());
	}

	@ParameterizedTest
	@ValueSource(
			strings = { BatchConstants.Job.POST_PROCESSING_STEP_NAME, BatchConstants.Job.PERSIST_RESULT_FILE_STEP_NAME }
	)
	void buildFailureProgress_outputStepFailure_includesOutputFailureDetails(String stepName) {
		JobExecution jobExecution = failureJobExecution(
				stepName, new ExitStatus(ExitStatus.FAILED.getExitCode(), "Could not build output archive"),
				Collections.emptyList()
		);

		VDYPProjectionProgressUpdate result = BatchUtils.buildFailureProgress("job-guid", jobExecution);

		assertEquals(BatchConstants.FailureType.OUTPUT, result.batchFailureTypeCode());
		assertEquals("Could not build output archive", result.failureMessage());
	}

	@Test
	void buildFailureProgress_longFailureMessage_truncatesToColumnLength() {
		JobExecution jobExecution = failureJobExecution(
				BatchConstants.Job.WORKER_STEP_NAME + ":partition0", null,
				List.of(new RuntimeException("x".repeat(250)))
		);

		VDYPProjectionProgressUpdate result = BatchUtils.buildFailureProgress("job-guid", jobExecution);

		assertEquals(200, result.failureMessage().length());
		assertTrue(result.failureMessage().endsWith("..."));
	}

	@Test
	void toLong_standardInteger_parsesCorrectly() {
		assertEquals(23000000L, CsvRecordBeanHelper.toLong("23000000"));
	}

	@Test
	void toLong_eNotationLowerCase_parsesCorrectly() {
		assertEquals(23000000L, CsvRecordBeanHelper.toLong("2.3e+07"));
	}

	@Test
	void toLong_eNotationUpperCase_parsesCorrectly() {
		assertEquals(23000000L, CsvRecordBeanHelper.toLong("2.3E+07"));
	}

	@Test
	void toLong_invalidValue_throwsNumberFormatException() {
		assertThrows(NumberFormatException.class, () -> CsvRecordBeanHelper.toLong("abc"));
	}

	@Test
	void extractFeatureIdLong_eNotation_returnsLong() {
		assertEquals(23000000L, BatchUtils.extractFeatureIdLong("2.3e+07,092O096,42344045"));
	}

	@Test
	void extractFeatureIdLong_standardInteger_returnsLong() {
		assertEquals(23000000L, BatchUtils.extractFeatureIdLong("23000000,092O096,42344045"));
	}

	@Test
	void extractFeatureIdLong_invalidValue_returnsNull() {
		assertNull(BatchUtils.extractFeatureIdLong("abc,092O096,42344045"));
	}

	@Test
	void deleteDirectoryRecursively_nonExistentDir_doesNothing(@TempDir Path tempDir) {
		Path nonExistent = tempDir.resolve("does-not-exist");
		assertDoesNotThrow(() -> BatchUtils.deleteDirectoryRecursively(nonExistent));
		assertFalse(Files.exists(nonExistent));
	}

	@Test
	void deleteDirectoryRecursively_existingDirWithFiles_deletesAll(@TempDir Path tempDir) throws IOException {
		Path subDir = Files.createDirectory(tempDir.resolve("job-dir"));
		Files.createFile(subDir.resolve("output.zip"));

		BatchUtils.deleteDirectoryRecursively(subDir);

		assertFalse(Files.exists(subDir));
	}

	@Test
	void buildFinalProgress_nonWorkerStepIgnored() {
		JobExecution jobExecution = mock(JobExecution.class);
		when(jobExecution.getExecutionContext()).thenReturn(new ExecutionContext());

		StepExecution nonWorkerStep = mock(StepExecution.class);
		when(nonWorkerStep.getStepName()).thenReturn("someOtherStep");
		when(jobExecution.getStepExecutions()).thenReturn(List.of(nonWorkerStep));

		VDYPProjectionProgressUpdate result = BatchUtils.buildFinalProgress("job-guid", jobExecution);

		assertEquals(0, result.polygonsProcessed());
		assertEquals(0, result.projectionErrors());
		assertEquals(0, result.polygonsSkipped());
	}

	private static JobExecution
			failureJobExecution(String stepName, ExitStatus exitStatus, List<Throwable> exceptions) {
		JobExecution jobExecution = mock(JobExecution.class);
		when(jobExecution.getExecutionContext()).thenReturn(new ExecutionContext());
		when(jobExecution.getAllFailureExceptions()).thenReturn(exceptions);

		StepExecution failedStep = mock(StepExecution.class);
		when(failedStep.getStepName()).thenReturn(stepName);
		when(failedStep.getStatus()).thenReturn(BatchStatus.FAILED);
		when(failedStep.getExitStatus()).thenReturn(exitStatus);
		when(failedStep.getExecutionContext()).thenReturn(new ExecutionContext());
		when(jobExecution.getStepExecutions()).thenReturn(List.of(failedStep));

		return jobExecution;
	}
}
