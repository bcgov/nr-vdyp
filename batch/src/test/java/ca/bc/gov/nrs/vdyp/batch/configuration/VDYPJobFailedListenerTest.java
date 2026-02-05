package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VDYPJobFailedListenerTest {

	@Mock
	private VdypClient vdypClient;

	@Mock
	private JobExecution jobExecution;

	private VDYPJobFailedListener listener;

	@BeforeEach
	void setUp() {
		listener = new VDYPJobFailedListener(vdypClient);
	}

	@Test
	void testAfterJob_callsVdypWhenFailed() {
		JobParameters jobParameters = new JobParametersBuilder().addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.Job.GUID, "test-guid-123")
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, "test-proj-123").toJobParameters();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

		listener.afterJob(jobExecution);

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getJobParameters();
		verify(vdypClient, atLeastOnce()).markComplete(any(), eq(false));
	}

	@ParameterizedTest
	@EnumSource(value = BatchStatus.class, names = { "COMPLETED", "STOPPED", "ABANDONED" })
	void testAfterJob_doesNotCallVdyp_whenNotFailed(BatchStatus batchStatus) {
		JobParameters jobParameters = new JobParametersBuilder().addLong(BatchConstants.Partition.NUMBER, 4L)
				.addString(BatchConstants.Job.GUID, "test-guid-123")
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, "test-proj-123").toJobParameters();

		when(jobExecution.getId()).thenReturn(1L);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobExecution.getStatus()).thenReturn(batchStatus);

		listener.afterJob(jobExecution);

		verify(vdypClient, never()).markComplete(any(), eq(false));
	}
}
