package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.mockito.ArgumentMatchers.any;
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

	private BatchProperties batchProperties;
	private PrioritizationPauseTracker pauseTracker;
	private VDYPJobMetricListener listener;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.getPartition().setInterimDirsCleanupEnabled(true);
		pauseTracker = new PrioritizationPauseTracker();
		listener = new VDYPJobMetricListener(
				metricsCollector, batchProperties, resultAggregationService, ownershipService, pauseTracker
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
}
