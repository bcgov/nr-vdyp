package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultPersistenceException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
@StepScope
public class ResultPersistenceTasklet implements Tasklet {
	private static final Logger logger = LoggerFactory.getLogger(ResultPersistenceTasklet.class);
	private final ComsFileService comsFileService;

	private final VdypClient vdypClient;

	public ResultPersistenceTasklet(ComsFileService comsFileService, VdypClient vdypClient) {
		this.comsFileService = comsFileService;
		this.vdypClient = vdypClient;
	}

	@Override
	public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {
		logger.debug("Starting persistence of result zip file to COMS.");
		var stepExecution = chunkContext.getStepContext().getStepExecution();
		var jobExecution = stepExecution.getJobExecution();
		var params = jobExecution.getJobParameters();

		String jobGuid = params.getString(BatchConstants.Job.GUID);
		String baseDir = params.getString(BatchConstants.Job.BASE_DIR);
		Long partitions = params.getLong(BatchConstants.Partition.NUMBER);
		String jobTimestamp = params.getString(BatchConstants.Job.TIMESTAMP);

		String projectionGUID = params.getString(BatchConstants.GuidInput.PROJECTION_GUID);
		try {
			if (jobGuid == null || baseDir == null || partitions == null || projectionGUID == null) {
				throw new IllegalArgumentException("Missing required job parameters for COMS download mode.");
			}

			VdypProjectionDetails projectionDetails = vdypClient.getProjectionDetails(projectionGUID);

			List<FileMappingDetails> resultFiles = vdypClient
					.getFileSetFiles(projectionGUID, projectionDetails.resultFileSet().guid());

			Path jobBasePath = Paths.get(baseDir);
			BatchUtils.confirmDirectoryExists(jobBasePath);

			// Get path for final zip
			Path finalZipPath = BatchUtils.getFinalZipName(jobBasePath, jobTimestamp);
			if (!Files.exists(finalZipPath)) {
				throw new IOException("Could not find expected result zip file at: " + finalZipPath);
			}

			if (resultFiles.isEmpty()) {
				// no results for this fileset yet add the results through the vdyp client
				vdypClient.uploadFileToFileSet(projectionGUID, projectionDetails.resultFileSet().guid(), finalZipPath);
			} else {
				comsFileService.updateStoredObject(UUID.fromString(resultFiles.get(0).comsObjectGuid()), finalZipPath);
			}

			vdypClient.markComplete(projectionGUID, true);

			logger.debug("Completed persistence of result zip file to COMS.");
		} catch (Exception e) {
			throw BatchResultPersistenceException.handleResultPersistenceFailure(
					e, "Failed to persist result zip to COMS.", jobGuid, jobExecution.getId(), logger
			);
		}
		return RepeatStatus.FINISHED;
	}
}
