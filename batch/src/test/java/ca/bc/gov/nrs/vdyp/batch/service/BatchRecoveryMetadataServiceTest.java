package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class BatchRecoveryMetadataServiceTest {

	private static final String DEFAULT_EXIT_DESCRIPTION = "Marked FAILED during startup recovery after abrupt shutdown";

	@Mock
	JobExplorer jobExplorer;

	@Mock
	JobRepository jobRepository;

	BatchRecoveryMetadataService service;

	@BeforeEach
	void setUp() {
		service = new BatchRecoveryMetadataService(jobExplorer, jobRepository);
	}

	@Test
	void markStaleExecutionFailed_whenExecutionDoesNotExist_throws() {
		when(jobExplorer.getJobExecution(1L)).thenReturn(null);

		IllegalStateException exception = assertThrows(
				IllegalStateException.class, () -> service.markStaleExecutionFailed(1L)
		);

		assertEquals("Could not find JobExecution 1", exception.getMessage());
		verifyNoInteractions(jobRepository);
	}

	@Test
	void markStaleExecutionFailed_whenExecutionAlreadyTerminal_returnsWithoutUpdatingMetadata() {
		JobExecution jobExecution = jobExecutionWithStatus(BatchStatus.COMPLETED);
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);

		JobExecution result = service.markStaleExecutionFailed(1L, "custom description");

		assertSame(jobExecution, result);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		verifyNoInteractions(jobRepository);
	}

	@Test
	void markStaleExecutionFailed_whenExecutionIsRunning_marksJobAndRunningStepsFailed() {
		JobExecution jobExecution = jobExecutionWithStatus(BatchStatus.STARTED);
		StepExecution startedStep = stepExecution("startedStep", jobExecution, BatchStatus.STARTED);
		StepExecution stoppingStep = stepExecution("stoppingStep", jobExecution, BatchStatus.STOPPING);
		StepExecution completedStep = stepExecution("completedStep", jobExecution, BatchStatus.COMPLETED);
		LocalDateTime completedStepEndTime = LocalDateTime.now().minusMinutes(1);
		completedStep.setExitStatus(ExitStatus.COMPLETED);
		completedStep.setEndTime(completedStepEndTime);
		jobExecution.addStepExecutions(List.of(startedStep, stoppingStep, completedStep));
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);

		LocalDateTime beforeCall = LocalDateTime.now();
		JobExecution result = service.markStaleExecutionFailed(1L, "custom description");
		LocalDateTime afterCall = LocalDateTime.now();

		assertSame(jobExecution, result);
		assertFailed(jobExecution, "custom description", beforeCall, afterCall);
		assertFailed(startedStep, "custom description", beforeCall, afterCall);
		assertFailed(stoppingStep, "custom description", beforeCall, afterCall);
		assertEquals(BatchStatus.COMPLETED, completedStep.getStatus());
		assertEquals(ExitStatus.COMPLETED, completedStep.getExitStatus());
		assertEquals(completedStepEndTime, completedStep.getEndTime());
		verify(jobRepository).update(startedStep);
		verify(jobRepository).update(stoppingStep);
		verify(jobRepository, never()).update(completedStep);
		verify(jobRepository).update(jobExecution);
	}

	@Test
	void markStaleExecutionFailed_withDefaultExitDescription_usesStartupRecoveryDescription() {
		JobExecution jobExecution = jobExecutionWithStatus(BatchStatus.STARTING);
		StepExecution startingStep = stepExecution("startingStep", jobExecution, BatchStatus.STARTING);
		jobExecution.addStepExecutions(List.of(startingStep));
		when(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution);

		service.markStaleExecutionFailed(1L);

		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertTrue(jobExecution.getExitStatus().getExitDescription().contains(DEFAULT_EXIT_DESCRIPTION));
		assertEquals(BatchStatus.FAILED, startingStep.getStatus());
		assertTrue(startingStep.getExitStatus().getExitDescription().contains(DEFAULT_EXIT_DESCRIPTION));
		verify(jobRepository).update(startingStep);
		verify(jobRepository).update(jobExecution);
	}

	private JobExecution jobExecutionWithStatus(BatchStatus status) {
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(status);
		return jobExecution;
	}

	private StepExecution stepExecution(String stepName, JobExecution jobExecution, BatchStatus status) {
		StepExecution stepExecution = new StepExecution(stepName, jobExecution);
		stepExecution.setStatus(status);
		return stepExecution;
	}

	private void assertFailed(
			JobExecution jobExecution, String exitDescription, LocalDateTime beforeCall, LocalDateTime afterCall
	) {
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertTrue(jobExecution.getExitStatus().getExitDescription().contains(exitDescription));
		assertTimestampBetween(jobExecution.getEndTime(), beforeCall, afterCall);
	}

	private void assertFailed(
			StepExecution stepExecution, String exitDescription, LocalDateTime beforeCall, LocalDateTime afterCall
	) {
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains(exitDescription));
		assertTimestampBetween(stepExecution.getEndTime(), beforeCall, afterCall);
	}

	private void assertTimestampBetween(LocalDateTime actual, LocalDateTime beforeCall, LocalDateTime afterCall) {
		assertNotNull(actual);
		assertTrue(!actual.isBefore(beforeCall), "Expected timestamp to be on or after the call started");
		assertTrue(!actual.isAfter(afterCall), "Expected timestamp to be on or before the call returned");
	}
}
