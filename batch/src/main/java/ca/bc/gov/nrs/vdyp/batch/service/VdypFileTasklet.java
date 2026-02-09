package ca.bc.gov.nrs.vdyp.batch.service;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public abstract class VdypFileTasklet implements Tasklet {
	final ComsFileService comsFileService;
	final VdypClient vdypClient;
	Long jobId;
	String jobGuid;
	String baseDir;
	Long partitions;
	String jobTimestamp;
	String projectionGUID;

	VdypFileTasklet(ComsFileService comsFileService, VdypClient vdypClient) {
		this.comsFileService = comsFileService;
		this.vdypClient = vdypClient;
	}

	abstract void performVdypFileOperation() throws BatchException;

	@Override
	public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext)
			throws BatchException {
		var stepExecution = chunkContext.getStepContext().getStepExecution();
		var jobExecution = stepExecution.getJobExecution();
		var params = jobExecution.getJobParameters();

		jobId = jobExecution.getJobId();
		jobGuid = params.getString(BatchConstants.Job.GUID);
		baseDir = params.getString(BatchConstants.Job.BASE_DIR);
		partitions = params.getLong(BatchConstants.Partition.NUMBER);
		jobTimestamp = params.getString(BatchConstants.Job.TIMESTAMP);
		projectionGUID = params.getString(BatchConstants.GuidInput.PROJECTION_GUID);

		performVdypFileOperation();

		return RepeatStatus.FINISHED;

	}
}
