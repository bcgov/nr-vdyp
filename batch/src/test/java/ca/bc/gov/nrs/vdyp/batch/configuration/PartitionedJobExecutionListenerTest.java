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
class PartitionedJobExecutionListenerTest {

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	private PartitionedJobExecutionListener listener;

	@BeforeEach
	void setUp() {
		listener = new PartitionedJobExecutionListener();
	}

	@Test
	void testConstructor() {
		assertNotNull(listener);
	}

	@Test
	void testBeforeJob_WithJobParameters() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(BatchConstants.Partition.SIZE, 4L)
				.addString(BatchConstants.Job.GUID, "test-guid-123").toJobParameters();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getJobParameters();
	}

	@Test
	void testBeforeJob_WithoutPartitionSize() {
		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "test-guid-456")
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(2L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);

		// Should not throw even without partition size
		assertDoesNotThrow(() -> listener.beforeJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getId();
	}

	@Test
	void testAfterJob_FirstTime() {
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
	void testAfterJob_WithDuration() {
		Long jobId = 3L;
		String jobGuid = "guid-003";

		LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
		LocalDateTime endTime = LocalDateTime.now();

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(startTime);
		when(jobExecution.getEndTime()).thenReturn(endTime);

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getStartTime();
		verify(jobExecution, atLeastOnce()).getEndTime();
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
	void testBeforeJob_MultipleJobs() {
		JobExecution job1 = mock(JobExecution.class);
		JobExecution job2 = mock(JobExecution.class);

		JobParameters params1 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "guid-multi-1")
				.toJobParameters();
		JobParameters params2 = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "guid-multi-2")
				.toJobParameters();

		when(job1.getId()).thenReturn(10L);
		when(job1.getJobParameters()).thenReturn(params1);
		when(job2.getId()).thenReturn(20L);
		when(job2.getJobParameters()).thenReturn(params2);

		assertDoesNotThrow(() -> listener.beforeJob(job1));
		assertDoesNotThrow(() -> listener.beforeJob(job2));

		verify(job1, atLeastOnce()).getId();
		verify(job2, atLeastOnce()).getId();
	}

	@Test
	void testAfterJob_WithCompletedStatus() {
		Long jobId = 6L;
		String jobGuid = "guid-006";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getStatus();
	}

	@Test
	void testAfterJob_WithFailedStatus() {
		Long jobId = 7L;
		String jobGuid = "guid-007";

		JobParameters jobParameters = new JobParametersBuilder().addString(BatchConstants.Job.GUID, jobGuid)
				.toJobParameters();

		when(jobExecution.getId()).thenReturn(jobId);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
		when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusMinutes(5));
		when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

		listener.beforeJob(jobExecution);

		assertDoesNotThrow(() -> listener.afterJob(jobExecution));

		verify(jobExecution, atLeastOnce()).getStatus();
	}
}
