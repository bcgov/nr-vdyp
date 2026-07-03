package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
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
		when(jobExecution.getExecutionContext()).thenReturn(new ExecutionContext());

		StepExecution failedStep = mock(StepExecution.class);
		when(failedStep.getStepName()).thenReturn(BatchConstants.Job.FETCH_AND_PARTITION_FILES_STEP_NAME);
		when(failedStep.getStatus()).thenReturn(BatchStatus.FAILED);
		when(jobExecution.getStepExecutions()).thenReturn(List.of(failedStep));
		when(jobExecution.getAllFailureExceptions()).thenReturn(List.of(new IllegalStateException("Input failed")));

		listener.afterJob(jobExecution);

		verify(jobExecution, atLeastOnce()).getId();
		verify(jobExecution, atLeastOnce()).getJobParameters();
		ArgumentCaptor<VDYPProjectionProgressUpdate> progressCaptor = ArgumentCaptor
				.forClass(VDYPProjectionProgressUpdate.class);
		verify(vdypClient, atLeastOnce()).markComplete(eq("test-proj-123"), eq(false), progressCaptor.capture());
		assertEquals(BatchConstants.FailureType.INPUT, progressCaptor.getValue().batchFailureTypeCode());
		assertEquals("Input failed", progressCaptor.getValue().failureMessage());
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

		verify(vdypClient, never()).markComplete(any(), eq(false), any());
	}
}
