package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import ca.bc.gov.nrs.vdyp.batch.client.vdyp.FileMappingDetails;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypClient;
import ca.bc.gov.nrs.vdyp.batch.client.vdyp.VdypProjectionDetails;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchResultPersistenceException;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

@Component
@StepScope
public class ResultPersistenceTasklet extends VdypFileTasklet {
	private static final Logger logger = LoggerFactory.getLogger(ResultPersistenceTasklet.class);

	public ResultPersistenceTasklet(ComsFileService comsFileService, VdypClient vdypClient) {
		super(comsFileService, vdypClient);
	}

	@Override
	void performVdypFileOperation() throws BatchException {
		try {
			logger.debug("Started persistence of result zip file to COMS.");
			if (jobGuid == null || baseDir == null || partitions == null || projectionGUID == null
					|| jobTimestamp == null) {
				throw new IllegalArgumentException("Missing required job parameters for COMS upload.");
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
			throw BatchResultPersistenceException
					.handleResultPersistenceFailure(e, "Failed to persist result zip to COMS.", jobGuid, jobId, logger);
		}
	}

}
