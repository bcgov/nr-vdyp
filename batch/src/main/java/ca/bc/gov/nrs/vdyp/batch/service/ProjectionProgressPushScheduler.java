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

import com.google.common.base.Strings;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@Component
@EnableScheduling
public class ProjectionProgressPushScheduler {
	private static final Logger logger = LoggerFactory.getLogger(ProjectionProgressPushScheduler.class);
	private final JobExplorer jobExplorer;
	private final VdypClient vdypClient;
	private final ThreadPoolTaskExecutor progressExecutor;

	private final static Map<String, Integer> lastProgressHashByProjection = new HashMap<>();

	public ProjectionProgressPushScheduler(
			JobExplorer jobExplorer, VdypClient vdypClient,
			@Qualifier("backendProgressExecutor") ThreadPoolTaskExecutor executor
	) {
		this.jobExplorer = jobExplorer;
		this.vdypClient = vdypClient;
		this.progressExecutor = (ThreadPoolTaskExecutor) executor;
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
		// for each projection that changed:
		for (JobExecution job : jobExplorer.findRunningJobExecutions("VdypFetchAndPartitionJob")) {
			String projectionGUID = job.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
			if (Strings.isNullOrEmpty(projectionGUID))
				continue;

			currentlyRunningProjectionGUIDs.add(projectionGUID);
			int totalPolygons = job.getExecutionContext().getInt("totalPolygonRecords", 0);
			int polygonsProcessed = 0;
			int errorCount = 0;
			int polygonsSkipped = 0;
			for (StepExecution step : job.getStepExecutions()) {
				if (step.getStepName().startsWith("workerStep:")) {
					// If you have multiple steps, you may want to filter by step name prefix
					ExecutionContext stepCtx = step.getExecutionContext();
					polygonsProcessed += stepCtx.getInt("polygonsProcessed", 0);
					errorCount += stepCtx.getInt("projectionErrors", 0);
					polygonsSkipped += stepCtx.getInt("polygonsSkipped", 0);
				}
			}

			// Check hash of values do not send if no change
			Triple<Integer, Integer, Integer> checkTriple = Triple.of(polygonsProcessed, errorCount, polygonsSkipped);
			int newHash = checkTriple.hashCode();
			Integer previousHash = lastProgressHashByProjection.put(projectionGUID, newHash);
			if (previousHash != null && previousHash == newHash) {
				continue;
			}

			VDYPProjectionProgressUpdate payload = new VDYPProjectionProgressUpdate(
					totalPolygons, polygonsProcessed, errorCount, polygonsSkipped
			);
			progressExecutor.execute(() -> {
				try {
					vdypClient.pushProgress(projectionGUID, payload);
				} catch (Exception logMe) {
					logger.error("Error pushing progress to VDYP", logMe);
				}
			});

		}

		// Clean up any projections that are no longer running to prevent memory leak in the map
		lastProgressHashByProjection.keySet().removeIf(guid -> !currentlyRunningProjectionGUIDs.contains(guid));
	}
}
