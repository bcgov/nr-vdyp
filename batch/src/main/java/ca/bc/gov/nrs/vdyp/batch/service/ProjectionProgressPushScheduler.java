package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.base.Strings;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
@EnableScheduling
public class ProjectionProgressPushScheduler {
	private static final Logger logger = LoggerFactory.getLogger(ProjectionProgressPushScheduler.class);
	private static final String PROJECTION_CANCELLED_REASON = "Projection cancelled";

	private final JobExplorer jobExplorer;
	private final VdypClient vdypClient;
	private final ThreadPoolTaskExecutor progressExecutor;
	private final BatchRecoveryMetadataService batchRecoveryMetadataService;

	private final Map<String, Integer> lastProgressHashByProjection = new HashMap<>();

	public ProjectionProgressPushScheduler(
			JobExplorer jobExplorer, VdypClient vdypClient,
			@Qualifier("backendProgressExecutor") ThreadPoolTaskExecutor executor,
			BatchRecoveryMetadataService batchRecoveryMetadataService
	) {
		this.jobExplorer = jobExplorer;
		this.vdypClient = vdypClient;
		this.progressExecutor = executor;
		this.batchRecoveryMetadataService = batchRecoveryMetadataService;
	}

	/**
	 * Iterates over all currentlyRunning "VdypFetchAndPartitionJob" job executions, extracts progress information from
	 * their execution contexts, and pushes updates to VDYP if there are changes since the last push.
	 */
	@Scheduled(fixedDelayString = "${vdyp.progress.push.delay:60000}")
	public void pushProgress() {
		// If the progressExecutor queue is full, it means we're already pushing progress updates, so skip this run to
		// avoid piling up updates.
		if (progressExecutor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0) {
			return;
		}

		Set<String> currentlyRunningProjectionGUIDs = new HashSet<>();
		for (JobExecution job : jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")) {
			String projectionGUID = job.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
			if (Strings.isNullOrEmpty(projectionGUID))
				continue;

			currentlyRunningProjectionGUIDs.add(projectionGUID);
			pushProgressForJob(job, projectionGUID);
		}

		// Clean up any projections that are no longer running to prevent memory leak in the map
		lastProgressHashByProjection.keySet().removeIf(guid -> !currentlyRunningProjectionGUIDs.contains(guid));
	}

	private void pushProgressForJob(JobExecution job, String projectionGUID) {
		String batchJobGUID = job.getJobParameters().getString(BatchConstants.Job.GUID);
		ProgressSnapshot progress = buildRestartAwareProgress(job);
		if (progress.isEmpty()) {
			return;
		}

		Triple<Integer, Integer, Integer> checkTriple = Triple
				.of(progress.polygonsProcessed(), progress.errorCount(), progress.polygonsSkipped());
		int newHash = checkTriple.hashCode();
		Integer previousHash = lastProgressHashByProjection.put(projectionGUID, newHash);
		if (previousHash == null || previousHash != newHash) {
			VDYPProjectionProgressUpdate payload = new VDYPProjectionProgressUpdate(
					batchJobGUID, progress.totalPolygons(), progress.polygonsProcessed(), progress.errorCount(),
					progress.polygonsSkipped(), progress.workers()
			);
			progressExecutor.execute(() -> {
				try {
					vdypClient.pushProgress(projectionGUID, payload);
				} catch (HttpClientErrorException.NotFound e) {
					// Backend has no record of this projection - nothing further to push. Fail the job (if it
					// isn't already terminal) rather than retrying against a projection that no longer exists.
					logger.info(
							"Projection {} is not known to backend; failing job {} as '{}'", projectionGUID,
							job.getId(), PROJECTION_CANCELLED_REASON
					);
					batchRecoveryMetadataService.markStaleExecutionFailed(job.getId(), PROJECTION_CANCELLED_REASON);
				} catch (HttpClientErrorException.BadRequest e) {
					// Backend's current state for this projection doesn't permit a progress update (e.g. it has
					// already reached a terminal state) - expected race, nothing more to do here.
					logger.info(
							"Backend rejected progress update for projection {}: {}", projectionGUID, e.getMessage()
					);
				} catch (Exception logMe) {
					logger.error("Error pushing progress to VDYP", logMe);
				}
			});
		}
	}

	private ProgressSnapshot buildRestartAwareProgress(JobExecution runningJob) {
		Map<String, ProgressSnapshot> bestProgressByWorkerStep = new HashMap<>();
		int totalPolygons = runningJob.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0);

		for (JobExecution jobExecution : jobExplorer.getJobExecutions(runningJob.getJobInstance())) {
			totalPolygons = Math.max(
					totalPolygons, jobExecution.getExecutionContext().getInt(BatchConstants.Job.TOTAL_POLYGONS, 0)
			);
			for (StepExecution step : jobExecution.getStepExecutions()) {
				if (step.getStepName().startsWith(BatchConstants.Job.WORKER_STEP_NAME)) {
					bestProgressByWorkerStep.merge(
							step.getStepName(), progressFromStep(step), ProjectionProgressPushScheduler::maxProgress
					);
				}
			}
		}

		int workers = BatchUtils.calculateActiveWorkers(runningJob, true);
		int polygonsProcessed = 0;
		int errorCount = 0;
		int polygonsSkipped = 0;
		for (ProgressSnapshot progress : bestProgressByWorkerStep.values()) {
			polygonsProcessed += progress.polygonsProcessed();
			errorCount += progress.errorCount();
			polygonsSkipped += progress.polygonsSkipped();
		}

		return new ProgressSnapshot(totalPolygons, polygonsProcessed, errorCount, polygonsSkipped, workers);
	}

	private static ProgressSnapshot progressFromStep(StepExecution step) {
		ExecutionContext stepCtx = step.getExecutionContext();
		return new ProgressSnapshot(
				0, stepCtx.getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0),
				stepCtx.getInt(BatchConstants.Job.PROJECTION_ERRORS, 0),
				stepCtx.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0), 0
		);
	}

	private static ProgressSnapshot maxProgress(ProgressSnapshot left, ProgressSnapshot right) {
		return left.progressTotal() >= right.progressTotal() ? left : right;
	}

	private record ProgressSnapshot(
			int totalPolygons, int polygonsProcessed, int errorCount, int polygonsSkipped, int workers
	) {
		boolean isEmpty() {
			return totalPolygons == 0 && progressTotal() == 0;
		}

		int progressTotal() {
			return polygonsProcessed + errorCount + polygonsSkipped;
		}
	}
}
