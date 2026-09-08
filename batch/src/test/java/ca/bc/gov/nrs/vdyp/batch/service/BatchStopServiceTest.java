package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

import ca.bc.gov.nrs.vdyp.batch.service.BatchStopService.StopOutcome;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchStopServiceTest {

	@Mock
	private JobOperator jobOperator;

	@Mock
	private JobExecution jobExecution;

	private BatchStopService service;

	@BeforeEach
	void setUp() {
		service = new BatchStopService(jobOperator);
		when(jobExecution.getId()).thenReturn(123L);
	}

	@Test
	void testStopLocally_JobOperatorStopsSuccessfully_ReturnsStopRequested() throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		when(jobOperator.stop(123L)).thenReturn(true);

		StopOutcome outcome = service.stopLocally("job-guid", jobExecution);

		assertEquals("STOP_REQUESTED", outcome.status());
		assertEquals(123L, outcome.executionId());
	}

	@Test
	void testStopLocally_JobOperatorReturnsFalse_ReturnsStopFailed() throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		when(jobOperator.stop(123L)).thenReturn(false);

		StopOutcome outcome = service.stopLocally("job-guid", jobExecution);

		assertEquals("STOP_FAILED", outcome.status());
		assertEquals(123L, outcome.executionId());
	}

	@Test
	void testStopLocally_JobAlreadyStopping_ReturnsAlreadyStopping() throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		when(jobOperator.stop(123L)).thenThrow(new JobExecutionNotRunningException("Already stopping"));

		StopOutcome outcome = service.stopLocally("job-guid", jobExecution);

		assertEquals("ALREADY_STOPPING", outcome.status());
		assertEquals(123L, outcome.executionId());
	}
}
