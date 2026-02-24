package ca.bc.gov.nrs.vdyp.batch.service;

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
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchPartitionException;

@Component
@StepScope
public class DownloadAndPartitionTasklet extends VdypFileTasklet {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAndPartitionTasklet.class);
	private final BatchInputPartitioner inputPartitioner;

	public DownloadAndPartitionTasklet(
			ComsFileService comsFileService, BatchInputPartitioner inputPartitioner, VdypClient vdypClient
	) {
		super(comsFileService, vdypClient);
		this.inputPartitioner = inputPartitioner;
	}

	@Override
	void performVdypFileOperation(StepExecution stepExecution) throws BatchException {
		try {
			if (jobGuid == null || baseDir == null || partitions == null || projectionGUID == null) {
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

			int totalPolygons = inputPartitioner
					.partitionCsvFiles(polygonPath, layerPath, partitions.intValue(), jobBaseDir, jobGuid);

			stepExecution.getJobExecution().getExecutionContext().putInt("totalPolygonRecords", totalPolygons);
		} catch (Exception e) {
			throw BatchPartitionException
					.handlePartitionFailure(e, "Could not fetch and partition input files", jobGuid, logger);
		}

		logger.debug("Completed download and partitioning of input files.");
	}
}
