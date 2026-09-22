package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.model.StorageCleanupRequest;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import ca.bc.gov.nrs.vdyp.batch.util.BatchUtils;

/**
 * Scans the PVC root for leftover job folders and, when requested, deletes the ones that are not in use. Only entries
 * directly under the PVC root whose name matches "vdyp-batch-{UUID}" (the job folder) or
 * "vdyp-batch-{UUID}warnings.txt" (its warnings file) are ever touched; everything else is reported as skipped and left
 * alone.
 */
@Service
public class StorageCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(StorageCleanupService.class);

	private final BatchProperties batchProperties;
	private final JobExplorer jobExplorer;

	public StorageCleanupService(BatchProperties batchProperties, JobExplorer jobExplorer) {
		this.batchProperties = batchProperties;
		this.jobExplorer = jobExplorer;
	}

	public enum Outcome {
		DELETABLE, DELETED, PROTECTED, FAILED
	}

	public record CleanupSetResult(String jobGuid, long bytes, Outcome outcome, String detail) {
	}

	public record StorageCleanupReport(
			boolean dryRun, int scanned, long totalBytes, List<CleanupSetResult> sets, List<String> skippedNames
	) {
	}

	private record CandidateSet(String jobGuid, Path folder, Path warningsFile) {
	}

	public StorageCleanupReport run(StorageCleanupRequest request) {
		Path root = Paths.get(batchProperties.getRootDirectory());
		if (Files.notExists(root)) {
			logger.warn("PVC root directory does not exist, nothing to clean up: {}", root);
			return new StorageCleanupReport(request.dryRun(), 0, 0, List.of(), List.of());
		}

		Set<String> protectedJobGuids = request.protectedJobGuids() == null ? Set.of()
				: new HashSet<>(request.protectedJobGuids());

		List<String> skippedNames = new ArrayList<>();
		List<CandidateSet> candidates = scanCandidates(root, skippedNames);

		List<CleanupSetResult> results = new ArrayList<>();
		long totalBytes = 0;
		for (CandidateSet candidate : candidates) {
			CleanupSetResult result = evaluateAndMaybeDelete(candidate, request.dryRun(), protectedJobGuids);
			results.add(result);
			if (result.outcome() == Outcome.DELETABLE || result.outcome() == Outcome.DELETED) {
				totalBytes += result.bytes();
			}
		}

		return new StorageCleanupReport(request.dryRun(), candidates.size(), totalBytes, results, skippedNames);
	}

	/**
	 * Lists the PVC root (non-recursively) and groups entries into per-job candidate sets (a job folder and/or its
	 * separate warnings file), collecting the names of anything that does not match either pattern.
	 */
	private List<CandidateSet> scanCandidates(Path root, List<String> skippedNames) {
		Map<String, Path> folderByJobGuid = new HashMap<>();
		Map<String, Path> warningsFileByJobGuid = new HashMap<>();

		try (Stream<Path> entries = Files.list(root)) {
			for (Path entry : (Iterable<Path>) entries::iterator) {
				String name = entry.getFileName().toString();
				if (Files.isDirectory(entry)) {
					BatchUtils.parseJobGuidFromFolderName(name).ifPresentOrElse(
							jobGuid -> folderByJobGuid.put(jobGuid, entry), () -> skippedNames.add(name)
					);
				} else {
					BatchUtils.parseJobGuidFromWarningsFileName(name).ifPresentOrElse(
							jobGuid -> warningsFileByJobGuid.put(jobGuid, entry), () -> skippedNames.add(name)
					);
				}
			}
		} catch (IOException e) {
			throw new StorageCleanupException("Failed to list PVC root directory: " + root, e);
		}

		Set<String> jobGuids = new HashSet<>(folderByJobGuid.keySet());
		jobGuids.addAll(warningsFileByJobGuid.keySet());

		List<CandidateSet> candidates = new ArrayList<>();
		for (String jobGuid : jobGuids) {
			candidates.add(new CandidateSet(jobGuid, folderByJobGuid.get(jobGuid), warningsFileByJobGuid.get(jobGuid)));
		}
		candidates.sort((left, right) -> left.jobGuid().compareTo(right.jobGuid()));
		return candidates;
	}

	private CleanupSetResult
			evaluateAndMaybeDelete(CandidateSet candidate, boolean dryRun, Set<String> protectedJobGuids) {
		long bytes = candidateSizeBytes(candidate);

		if (protectedJobGuids.contains(candidate.jobGuid())) {
			return new CleanupSetResult(
					candidate.jobGuid(), bytes, Outcome.PROTECTED, "Projection is not in a finished state"
			);
		}

		// Queried fresh for every candidate (not cached for the whole run) so a job that started or resumed while
		// this run was in progress is still caught immediately before its folder would otherwise be deleted.
		if (currentlyRunningJobGuids().contains(candidate.jobGuid())) {
			return new CleanupSetResult(
					candidate.jobGuid(), bytes, Outcome.PROTECTED,
					"Job is currently running or paused in the batch service"
			);
		}

		if (dryRun) {
			return new CleanupSetResult(candidate.jobGuid(), bytes, Outcome.DELETABLE, null);
		}

		try {
			if (candidate.folder() != null) {
				BatchUtils.deleteDirectoryRecursively(candidate.folder());
			}
			if (candidate.warningsFile() != null) {
				Files.deleteIfExists(candidate.warningsFile());
			}
			return new CleanupSetResult(candidate.jobGuid(), bytes, Outcome.DELETED, null);
		} catch (IOException e) {
			logger.warn("Failed to delete PVC job folder set for job {}: {}", candidate.jobGuid(), e.getMessage());
			return new CleanupSetResult(candidate.jobGuid(), bytes, Outcome.FAILED, e.getMessage());
		}
	}

	private long candidateSizeBytes(CandidateSet candidate) {
		long bytes = 0;
		try {
			if (candidate.folder() != null) {
				bytes += BatchUtils.directorySizeBytes(candidate.folder());
			}
			if (candidate.warningsFile() != null && Files.exists(candidate.warningsFile())) {
				bytes += Files.size(candidate.warningsFile());
			}
		} catch (IOException e) {
			logger.warn("Failed to measure size for job {}: {}", candidate.jobGuid(), e.getMessage());
		}
		return bytes;
	}

	private Set<String> currentlyRunningJobGuids() {
		Set<String> runningJobGuids = new HashSet<>();
		for (JobExecution execution : jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)) {
			String jobGuid = execution.getJobParameters().getString(BatchConstants.Job.GUID);
			if (jobGuid != null) {
				runningJobGuids.add(jobGuid);
			}
		}
		return runningJobGuids;
	}

	public static class StorageCleanupException extends RuntimeException {
		public StorageCleanupException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
