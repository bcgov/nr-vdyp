package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;
import ca.bc.gov.nrs.vdyp.batch.model.VDYPProjectionProgressUpdate;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
@StepScope
public class DownloadAndPartitionTasklet extends VdypFileTasklet {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAndPartitionTasklet.class);
	private final BatchInputPartitioner inputPartitioner;
	private final BatchProperties batchProperties;

	public DownloadAndPartitionTasklet(
			ComsFileService comsFileService, BatchInputPartitioner inputPartitioner, VdypClient vdypClient,
			BatchProperties batchProperties
	) {
		super(comsFileService, vdypClient);
		this.inputPartitioner = inputPartitioner;
		this.batchProperties = batchProperties;
	}

	@Override
	void performVdypFileOperation(StepExecution stepExecution) throws BatchException {
		int partitionedCount = 0;
		int computedPartitions = 0;

		try {
			if (jobGuid == null || baseDir == null || projectionGUID == null) {
				throw new IllegalArgumentException("Missing required job parameters for COMS download mode.");
			}

			VdypProjectionDetails projectionDetails = vdypClient.getProjectionDetails(projectionGUID);

			List<FileMappingDetails> polyGonFiles = vdypClient
					.getFileSetFiles(projectionGUID, projectionDetails.polygonFileSet().guid());
			List<FileMappingDetails> layerFiles = vdypClient
					.getFileSetFiles(projectionGUID, projectionDetails.layerFileSet().guid());

			String polygonGuidStr = polyGonFiles.get(0).comsObjectGuid();
			String layerGuidStr = layerFiles.get(0).comsObjectGuid();

			// Download the inputs
			Path jobBaseDir = Paths.get(baseDir);
			Path inputDir = jobBaseDir.resolve("input");
			Files.createDirectories(inputDir);

			Path polygonPath = inputDir.resolve("polygon.csv");
			Path layerPath = inputDir.resolve("layer.csv");

			logger.debug(
					"[GUID: {}] Downloading COMS inputs (Polygon:{}, Layer {}) to {}", jobGuid, polygonGuidStr,
					layerGuidStr, inputDir
			);

			comsFileService.fetchObjectToFile(UUID.fromString(polygonGuidStr), polygonPath);
			comsFileService.fetchObjectToFile(UUID.fromString(layerGuidStr), layerPath);

			// Count polygons before partitioning to determine the correct thread allocation
			int totalPolygons;
			try (BufferedReader reader = Files.newBufferedReader(polygonPath, StandardCharsets.UTF_8)) {
				totalPolygons = BatchUtils.countDataRecords(reader);
			}

			int chunkSize = batchProperties.getReader().getDefaultChunkSize();
			int maxJobThreads = batchProperties.getThreadPool().getMaxJobThreads();
			computedPartitions = BatchUtils.calculateThreadsForJob(totalPolygons, chunkSize, maxJobThreads);

			logger.debug(
					"[GUID: {}] Computed {} partitions for {} polygons (chunkSize={}, maxJobThreads={})", jobGuid,
					computedPartitions, totalPolygons, chunkSize, maxJobThreads
			);

			partitionedCount = inputPartitioner
					.partitionCsvFiles(polygonPath, layerPath, computedPartitions, jobBaseDir, jobGuid);

			deleteOriginalInputDirectory(inputDir);

			stepExecution.getJobExecution().getExecutionContext()
					.putInt(BatchConstants.Job.TOTAL_POLYGONS, partitionedCount);
			stepExecution.getJobExecution().getExecutionContext()
					.putInt(BatchConstants.Job.COMPUTED_PARTITIONS, computedPartitions);
		} catch (Exception e) {
			throw BatchPartitionException
					.handlePartitionFailure(e, "Could not fetch and partition input files", jobGuid, logger);
		}

		// Push initial progress so the backend can update status to RUNNING as soon as polygon count is known.
		pushInitialProgress(partitionedCount);

		logger.debug("Completed download and partitioning of input files.");
	}

	private void deleteOriginalInputDirectory(Path inputDir) {
		try {
			BatchUtils.deleteDirectoryRecursively(inputDir);
			logger.debug("[GUID: {}] Deleted original input directory after partitioning: {}", jobGuid, inputDir);
		} catch (IOException e) {
			logger.warn(
					"[GUID: {}] Failed to delete original input directory {}: {}", jobGuid, inputDir, e.getMessage()
			);
		}
	}

	private void pushInitialProgress(int totalPolygons) {
		try {
			vdypClient
					.pushProgress(projectionGUID, new VDYPProjectionProgressUpdate(jobGuid, totalPolygons, 0, 0, 0, 0));
		} catch (Exception e) {
			logger.warn("[GUID: {}] Failed to push initial progress to backend: {}", jobGuid, e.getMessage());
		}
	}
}
