package ca.bc.gov.nrs.vdyp.batch.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@Component
@StepScope
public class DownloadAndPartitionTasklet implements Tasklet {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAndPartitionTasklet.class);
	private final ComsFileService comsFileService;
	private final BatchInputPartitioner inputPartitioner;

	public DownloadAndPartitionTasklet(ComsFileService comsFileService, BatchInputPartitioner inputPartitioner) {
		this.comsFileService = comsFileService;
		this.inputPartitioner = inputPartitioner;
	}

	@Override
	public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {
		logger.debug("Starting download and partitioning of input files.");
		var stepExecution = chunkContext.getStepContext().getStepExecution();
		var jobExecution = stepExecution.getJobExecution();
		var params = jobExecution.getJobParameters();

		String jobGuid = params.getString(BatchConstants.Job.GUID);
		String baseDir = params.getString(BatchConstants.Job.BASE_DIR);
		Long partitions = params.getLong(BatchConstants.Partition.NUMBER);

		String polygonGuidStr = params.getString(BatchConstants.ComsInput.POLYGON_COMS_OBJECT_GUID);
		String layerGuidStr = params.getString(BatchConstants.ComsInput.LAYER_COMS_OBJECT_GUID);

		if (jobGuid == null || baseDir == null || partitions == null || polygonGuidStr == null
				|| layerGuidStr == null) {
			throw new IllegalArgumentException("Missing required job parameters for COMS download mode.");
		}

		// Download the inputs
		Path jobBaseDir = Paths.get(baseDir);
		Path inputDir = jobBaseDir.resolve("input");
		Files.createDirectories(inputDir);

		Path polygonPath = inputDir.resolve("polygon.csv");
		Path layerPath = inputDir.resolve("layer.csv");

		logger.debug("[GUID: {}] Downloading COMS inputs to {}", jobGuid, inputDir);

		comsFileService.fetchObjectToFile(UUID.fromString(polygonGuidStr), polygonPath);
		comsFileService.fetchObjectToFile(UUID.fromString(layerGuidStr), layerPath);

		try {
			inputPartitioner.partitionCsvFiles(polygonPath, layerPath, partitions.intValue(), jobBaseDir, jobGuid);
		} catch (BatchPartitionException e) {
			// Convert to a clear job failure
			logger.error("[GUID: {}] Partitioning failed after COMS download: {}", jobGuid, e.getMessage(), e);
			throw e;
		}

		logger.debug("Completed download and partitioning of input files.");
		return RepeatStatus.FINISHED;
	}
}
