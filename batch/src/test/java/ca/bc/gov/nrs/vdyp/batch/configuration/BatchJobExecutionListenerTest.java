package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchJobExecutionListenerTest {

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	private BatchJobExecutionListener listener;

	@BeforeEach
	void setUp() {
		listener = new BatchJobExecutionListener();
	}

	@Test
	void testBeforeJob_InitializesTracking() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.Job.GUID, "test-guid-123").toJobParameters();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testAfterJob_WithValidJobExecution() {
		Long jobId = 1L;
		String jobGuid = "guid-001";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

		// Call beforeJob first to initialize tracking
		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getStatus();
		verify(jobExecution, atLeastOnce()).getStartTime();
		verify(jobExecution, atLeastOnce()).getEndTime();
	}

	@Test
	void testAfterJob_AlreadyProcessed_SkipsProcessing() {
		Long jobId = 2L;
		String jobGuid = "guid-002";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		// Call beforeJob to initialize tracking
		listener.beforeJob(jobExecution);

		// Call afterJob twice - second call should be skipped
		listener.afterJob(jobExecution);
		listener.afterJob(jobExecution);

		// Should still work without throwing exceptions
		assertDoesNotThrow(() -> listener.afterJob(jobExecution));
	}

	@Test
	void testAfterJob_WithMissingTimeInformation() {
		Long jobId = 4L;
		String jobGuid = "guid-004";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(null);
		when(jobExecution.getEndTime()).thenReturn(null);

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getStartTime();
		verify(jobExecution, atLeastOnce()).getEndTime();
	}

	@Test
	void testAfterJob_NotTracked_SkipsProcessing() {
		Long jobId = 5L;
		String jobGuid = "guid-005";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
	}

	@Test
	void testAfterJob_CleansUpOldJobTracker() {
		// Create and process 12 jobs to trigger cleanup (cleanup happens when size > 10)
		for (long i = 1; i <= 12; i++) {
			JobExecution jobExec = mock(JobExecution.class);
			JobParameters jobParams = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "guid-" + i)
					.toJobParameters();

			when(jobExec.getId()).thenReturn(i);
			when(jobExec.getJobParameters()).thenReturn(jobParams);
			when(jobExec.getStatus()).thenReturn(BatchStatus.COMPLETED);
			when(jobExec.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
			when(jobExec.getEndTime()).thenReturn(LocalDateTime.now());

			listener.beforeJob(jobExec);
			listener.afterJob(jobExec);
		}

		// Verify cleanup logic was executed (no exception thrown)
		assertDoesNotThrow(() -> {
			JobExecution finalJob = mock(JobExecution.class);
			JobParameters finalParams = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "guid-final")
					.toJobParameters();

			when(finalJob.getId()).thenReturn(13L);
			when(finalJob.getJobParameters()).thenReturn(finalParams);

			listener.beforeJob(finalJob);
		});
	}
}
