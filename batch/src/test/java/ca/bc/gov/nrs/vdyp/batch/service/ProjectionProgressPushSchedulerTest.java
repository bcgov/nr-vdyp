package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.HttpClientErrorException;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

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
	BatchRecoveryMetadataService batchRecoveryMetadataService;

	ProjectionProgressPushScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new ProjectionProgressPushScheduler(
				jobExplorer, vdypClient, taskExecutor, batchRecoveryMetadataService
		);
		when(taskExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
		when(threadPoolExecutor.getQueue()).thenReturn(queue);
	}

	private JobExecution runningJobWithProgress(String projectionGuid, String batchJobGuid) {
		var params = new HashMap<String, JobParameter<?>>();
		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));
		JobParameters jobParameters = new JobParameters(params);

		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, jobParameters);
		ExecutionContext jobCtx = new ExecutionContext();
		jobCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, 10);
		job.setExecutionContext(jobCtx);
		ExecutionContext stepCtx = new ExecutionContext();
		StepExecution step = new StepExecution("workerStep", job);
		stepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 5);
		step.setExecutionContext(stepCtx);
		job.addStepExecutions(List.of(step));

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(job));
		return job;
	}

	@Test
	void pushProgress_noTaskCapacity_returnsWithoutCalling() {
		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(0);
		scheduler.pushProgress();
		verify(jobExplorer, never()).findJobInstancesByJobName(any(), anyInt(), anyInt());
	}

	@Test
	void pushProgress_missingProjectionGuid_skipsJob() {
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters());

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));

		scheduler.pushProgress();

		verify(jobExplorer, never()).getJobExecutions(any());
		verify(taskExecutor, never()).execute(any(Runnable.class));
	}

	@Test
	void pushProgress_emptySnapshot_skipsUntilProgressExists() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		var params = new HashMap<String, JobParameter<?>>();
		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));

		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters(params));
		job.setExecutionContext(new ExecutionContext());

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(job));

		try (MockedStatic<BatchUtils> batchUtils = mockStatic(BatchUtils.class)) {
			batchUtils.when(() -> BatchUtils.calculateThreadsInUse(job, true)).thenReturn(0);

			scheduler.pushProgress();
			verify(taskExecutor, never()).execute(any(Runnable.class));

			StepExecution worker = new StepExecution(BatchConstants.Job.WORKER_STEP_NAME + ":partition0", job);
			ExecutionContext workerContext = new ExecutionContext();
			workerContext.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 1);
			worker.setExecutionContext(workerContext);
			job.addStepExecutions(List.of(worker));

			scheduler.pushProgress();
			verify(taskExecutor).execute(any(Runnable.class));
		}
	}

	@Test
	void pushProgress_unchangedProgress_isDeduplicated() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		runningJobWithProgress(projectionGuid, batchJobGuid);

		scheduler.pushProgress();
		scheduler.pushProgress();

		verify(taskExecutor).execute(any(Runnable.class));
	}

	@Test
	void pushProgress_projectionStops_isRemovedFromDeduplicationCache() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		JobExecution job = runningJobWithProgress(projectionGuid, batchJobGuid);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob"))
				.thenReturn(Set.of(job), Set.of(), Set.of(job));

		scheduler.pushProgress();
		scheduler.pushProgress();
		scheduler.pushProgress();

		verify(taskExecutor, times(2)).execute(any(Runnable.class));
	}

	@Test
	void pushProgress_downloadPhase_reportsJobThreadWithNoPolygonProgress() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		var params = new HashMap<String, JobParameter<?>>();
		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));

		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters(params));
		job.setExecutionContext(new ExecutionContext());

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(job));
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<VDYPProjectionProgressUpdate> payloadCaptor = ArgumentCaptor
				.forClass(VDYPProjectionProgressUpdate.class);

		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();

		verify(vdypClient).pushProgress(eq(projectionGuid), payloadCaptor.capture());
		assertEquals(0, payloadCaptor.getValue().totalPolygons());
		assertEquals(1, payloadCaptor.getValue().workers());
	}

	@Test
	void pushProgress_workerCountChange_isNotDeduplicated() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		var params = new HashMap<String, JobParameter<?>>();
		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));

		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, new JobParameters(params));
		job.setExecutionContext(new ExecutionContext());
		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(job));
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(job));

		scheduler.pushProgress();

		StepExecution worker = new StepExecution(BatchConstants.Job.WORKER_STEP_NAME + ":partition0", job);
		worker.setStatus(BatchStatus.STARTED);
		worker.setExecutionContext(new ExecutionContext());
		job.addStepExecutions(List.of(worker));
		scheduler.pushProgress();

		verify(taskExecutor, times(2)).execute(any(Runnable.class));
	}

	@Test
	void pushProgress_happyPath() {
		var params = new HashMap<String, JobParameter<?>>();
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();

		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));
		JobParameters jobParameters = new JobParameters(params);

		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");
		JobExecution job = new JobExecution(jobInstance, 1L, jobParameters);
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
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(List.of(job));
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());

		// 👇 THIS executes your lambda
		runnableCaptor.getValue().run();

	}

	@Test
	void pushProgress_backendReportsUnknownProjection_failsJobAsCancelled() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		JobExecution job = runningJobWithProgress(projectionGuid, batchJobGuid);

		doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null))
				.when(vdypClient).pushProgress(eq(projectionGuid), any());

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();

		verify(batchRecoveryMetadataService).markStaleExecutionFailed(job.getId(), "Projection cancelled");
	}

	@Test
	void pushProgress_unexpectedError_isLoggedAndDoesNotFailJob() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		runningJobWithProgress(projectionGuid, batchJobGuid);

		doThrow(new RuntimeException("connection reset")).when(vdypClient).pushProgress(eq(projectionGuid), any());

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();

		verify(batchRecoveryMetadataService, never()).markStaleExecutionFailed(any(), any());
	}

	@Test
	void pushProgress_backendReportsStateNotPermitted_doesNotFailJob() {
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();
		runningJobWithProgress(projectionGuid, batchJobGuid);

		doThrow(HttpClientErrorException.BadRequest.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null))
				.when(vdypClient).pushProgress(eq(projectionGuid), any());

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();

		verify(batchRecoveryMetadataService, never()).markStaleExecutionFailed(any(), any());
	}

	@Test
	void pushProgress_restartWithNoCurrentCounts_usesPriorExecutionProgress() {
		var params = new HashMap<String, JobParameter<?>>();
		String projectionGuid = java.util.UUID.randomUUID().toString();
		String batchJobGuid = java.util.UUID.randomUUID().toString();

		params.put(BatchConstants.GuidInput.PROJECTION_GUID, new JobParameter<>(projectionGuid, String.class, true));
		params.put(BatchConstants.Job.GUID, new JobParameter<>(batchJobGuid, String.class, true));
		JobParameters jobParameters = new JobParameters(params);
		JobInstance jobInstance = new JobInstance(1L, "VdypFetchAndPartitionJob");

		JobExecution priorExecution = new JobExecution(jobInstance, 1L, jobParameters);
		ExecutionContext priorJobCtx = new ExecutionContext();
		priorJobCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, 10);
		priorExecution.setExecutionContext(priorJobCtx);
		StepExecution priorWorker = new StepExecution("workerStep:partition0", priorExecution);
		ExecutionContext priorStepCtx = new ExecutionContext();
		priorStepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 7);
		priorWorker.setExecutionContext(priorStepCtx);
		priorExecution.addStepExecutions(List.of(priorWorker));

		JobExecution olderExecution = new JobExecution(jobInstance, 0L, jobParameters);
		olderExecution.setExecutionContext(new ExecutionContext());
		StepExecution olderWorker = new StepExecution("workerStep:partition0", olderExecution);
		ExecutionContext olderStepCtx = new ExecutionContext();
		olderStepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, 3);
		olderWorker.setExecutionContext(olderStepCtx);
		olderExecution.addStepExecutions(List.of(olderWorker));

		JobExecution restartedExecution = new JobExecution(jobInstance, 2L, jobParameters);
		restartedExecution.setExecutionContext(new ExecutionContext());

		when(taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).thenReturn(1);
		when(jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")).thenReturn(Set.of(restartedExecution));
		when(jobExplorer.getJobExecutions(jobInstance))
				.thenReturn(List.of(restartedExecution, priorExecution, olderExecution));
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<VDYPProjectionProgressUpdate> payloadCaptor = ArgumentCaptor
				.forClass(VDYPProjectionProgressUpdate.class);

		scheduler.pushProgress();
		verify(taskExecutor).execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();

		verify(vdypClient).pushProgress(eq(projectionGuid), payloadCaptor.capture());
		assertEquals(10, payloadCaptor.getValue().totalPolygons());
		assertEquals(7, payloadCaptor.getValue().polygonsProcessed());
	}

}
