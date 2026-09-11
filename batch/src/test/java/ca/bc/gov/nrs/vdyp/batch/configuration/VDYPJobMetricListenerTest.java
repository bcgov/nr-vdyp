package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;

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

import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.BatchResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.service.PrioritizationPauseTracker;
import ca.bc.gov.nrs.vdyp.batch.service.ThreadReservationService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VDYPJobMetricListenerTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private BatchResultAggregationService resultAggregationService;

	@Mock
	private JobOwnershipService ownershipService;

	@Mock
	private ThreadReservationService threadReservationService;

	private BatchProperties batchProperties;
	private PrioritizationPauseTracker pauseTracker;
	private VDYPJobMetricListener listener;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.getPartition().setInterimDirsCleanupEnabled(true);
		pauseTracker = new PrioritizationPauseTracker();
		listener = new VDYPJobMetricListener(
				metricsCollector, batchProperties, resultAggregationService, ownershipService, pauseTracker,
				threadReservationService
		);
	}

	private JobExecution stoppedExecution(long executionId, String jobBaseDir) {
		JobInstance instance = new JobInstance(executionId, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "test-guid")
				.addString(BatchConstants.Job.BASE_DIR, jobBaseDir).toJobParameters();
		JobExecution execution = new JobExecution(instance, executionId, params);
		execution.setStatus(BatchStatus.STOPPED);
		return execution;
	}

	@Test
	void afterJob_stoppedAndPausedForResume_doesNotCleanUpInterimDirectories() {
		JobExecution execution = stoppedExecution(200L, "/tmp/job-200");
		pauseTracker.markPausedForResume(200L);

		listener.afterJob(execution);

		verify(resultAggregationService, never()).cleanupInputPartitionDirectories(any(Path.class));
		verify(resultAggregationService, never()).cleanupOutputPartitionDirectories(any(Path.class));
	}

	@Test
	void afterJob_stoppedAndNotPaused_cleansUpInterimDirectories() {
		JobExecution execution = stoppedExecution(201L, "/tmp/job-201");

		listener.afterJob(execution);

		verify(resultAggregationService, times(1)).cleanupInputPartitionDirectories(any(Path.class));
		verify(resultAggregationService, times(1)).cleanupOutputPartitionDirectories(any(Path.class));
	}

	@Test
	void afterJob_releasesReservedThreadsRecordedInExecutionContext() {
		JobExecution execution = stoppedExecution(202L, "/tmp/job-202");
		execution.getExecutionContext().putInt(BatchConstants.Job.RESERVED_THREADS, 4);

		listener.afterJob(execution);

		verify(threadReservationService).release(4);
	}

	@Test
	void afterJob_doesNotReleaseWhenNoThreadsWereReserved() {
		JobExecution execution = stoppedExecution(203L, "/tmp/job-203");

		listener.afterJob(execution);

		verify(threadReservationService, never()).release(anyInt());
	}

	@Test
	void afterJob_stoppedAndPausedForResume_doesNotReleaseReservedThreads() {
		JobExecution execution = stoppedExecution(204L, "/tmp/job-204");
		execution.getExecutionContext().putInt(BatchConstants.Job.RESERVED_THREADS, 4);
		pauseTracker.markPausedForResume(204L);

		listener.afterJob(execution);

		verify(threadReservationService, never()).release(anyInt());
	}

	@Test
	void afterJob_resumedExecutionEventuallyReleasesReservationExactlyOnce() {
		JobExecution stopped = stoppedExecution(200L, "/tmp/job-200");
		stopped.getExecutionContext().putInt(BatchConstants.Job.RESERVED_THREADS, 4);
		pauseTracker.markPausedForResume(200L);
		listener.afterJob(stopped);

		JobExecution resumed = stoppedExecution(205L, "/tmp/job-200");
		resumed.setStatus(BatchStatus.COMPLETED);
		resumed.getExecutionContext().putInt(BatchConstants.Job.RESERVED_THREADS, 4);
		listener.afterJob(resumed);

		verify(threadReservationService, times(1)).release(4);
	}
}
