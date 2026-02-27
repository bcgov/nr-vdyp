package ca.bc.gov.nrs.vdyp.batch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class ProjectionProgressPushSchedulerTest {
	@Mock
	JobExplorer jobExplorer;

	@Mock
	VdypClient vdypClient;

	@Mock
	ThreadPoolTaskExecutor taskExecutor;
	// for the chained call taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()
	@Mock
	ThreadPoolExecutor threadPoolExecutor;
	@Mock
	BlockingQueue<Runnable> queue;
	@Mock
	JobParameters jobParameters;

	ProjectionProgressPushScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new ProjectionProgressPushScheduler(jobExplorer, vdypClient, taskExecutor);
		when(taskExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
		when(threadPoolExecutor.getQueue()).thenReturn(queue);
	}

	@Test
	void pushProgress_noTaskCapacity_returnsWithoutCalling() {
		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(0);
		scheduler.pushProgress();
	}

	@Test
	void pushProgress_happyPath() {
		var params = new HashMap<String, JobParameter<?>>();

		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>("test-guid", String.class, true));
		;
		JobParameters jobParameters = new JobParameters(params);

		JobExecution job = new JobExecution(1L, jobParameters);
		ExecutionContext jobCtx = new ExecutionContext();
		jobCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, 10);
		job.setExecutionContext(jobCtx);
		ExecutionContext stepNWCtx = new ExecutionContext();
		StepExecution stepNW = new StepExecution("initStep", job);
		stepNWCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 5);
		stepNW.setExecutionContext(stepNWCtx);
		ExecutionContext stepCtx1 = new ExecutionContext();
		StepExecution step1 = new StepExecution("workerStep", job);
		stepCtx1.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 5);
		stepCtx1.putInt(BatchConstants.Job.PROJECTION_ERRORS, 1);
		step1.setExecutionContext(stepCtx1);
		ExecutionContext stepCtx2 = new ExecutionContext();
		StepExecution step2 = new StepExecution("workerStep", job);
		stepCtx2.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 6);
		stepCtx2.putInt(BatchConstants.Job.PROJECTION_ERRORS, 2);
		step2.setExecutionContext(stepCtx2);
		job.addStepExecutions(List.of(stepNW, step1, step2));

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());

		// 👇 THIS executes your lambda
		runnableCaptor.getValue().run();

		verify(vdypClient).pushProgress(anyString(), any());
	}

}
