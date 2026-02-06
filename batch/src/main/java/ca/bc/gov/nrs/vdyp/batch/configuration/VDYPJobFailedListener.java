package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class VDYPJobFailedListener implements JobExecutionListener {
	private static final Logger logger = LoggerFactory.getLogger(VDYPJobFailedListener.class);
	private final VdypClient vdypClient;

	public VDYPJobFailedListener(VdypClient vdypClient) {
		this.vdypClient = vdypClient;
	}

	@Override
	@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, status, projectionGUID cannot be null in batch
	// context
	public void afterJob(@NonNull JobExecution jobExecution) {
		String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
		String projectionGUID = jobExecution.getJobParameters().getString(BatchConstants.GuidInput.PROJECTION_GUID);
		BatchStatus status = jobExecution.getStatus();

		if (BatchStatus.FAILED.equals(status)) {
			logger.debug(
					"[GUID: {}] [Job Failed Listener] Job execution ID: {} Projection GUID: {} Status:{} Updating Vdyp backend",
					jobGuid, projectionGUID, jobExecution.getId(), status
			);
			vdypClient.markComplete(projectionGUID, false);
		}
	}
}
