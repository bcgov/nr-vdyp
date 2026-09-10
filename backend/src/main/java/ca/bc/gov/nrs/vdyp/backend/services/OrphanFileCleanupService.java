package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.clients.S3StorageClient;
import ca.bc.gov.nrs.vdyp.backend.clients.S3StorageClient.StoredVersion;
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionFileSetRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.OrphanFileCleanupException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/** Reconcile S3 with the restored database while all projection writers are stopped. */
@ApplicationScoped
public class OrphanFileCleanupService {

	private static final Logger logger = LoggerFactory.getLogger(OrphanFileCleanupService.class);
	private static final Pattern FILE_SET_KEY = Pattern.compile(
			"^(vdyp/fileset/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}))(/.*)?$"
	);
	private final COMSClient client;
	private final COMSS3Config storage;
	private final ProjectionFileSetRepository repository;
	private final S3StorageClient s3;
	private final boolean deletionEnabled;

	public OrphanFileCleanupService(
			@RestClient COMSClient client, COMSS3Config storage, ProjectionFileSetRepository repository,
			S3StorageClient s3,
			@ConfigProperty(
					name = "vdyp.coms.orphan-cleanup.deletion-enabled", defaultValue = "true"
			) boolean deletionEnabled
	) {
		this.client = client;
		this.storage = storage;
		this.repository = repository;
		this.s3 = s3;
		this.deletionEnabled = deletionEnabled;
	}

	public record FolderResult(
			String key, List<String> bucketIds, long objects, int versions, long bytes, String outcome, String detail
	) {
	}

	public record CleanupReport(boolean dryRun, int scanned, List<String> skippedKeys, List<FolderResult> folders) {
	}

	private static class Folder {
		final String key;
		final UUID id;
		final List<StoredVersion> versions = new ArrayList<>();
		final List<COMSBucket> registrations = new ArrayList<>();
		boolean referenced;

		Folder(String key, UUID id) {
			this.key = key;
			this.id = id;
		}
	}

	public synchronized CleanupReport cleanup(boolean dryRun) {
		if (!dryRun && !deletionEnabled) {
			throw new OrphanFileCleanupException(
					"DELETION_DISABLED",
					"Set VDYP_COMS_ORPHAN_CLEANUP_DELETION_ENABLED=true to enable deletion. Preview remains available."
			);
		}
		// Finish inventory and database checks before any deletion. An error never means 'missing'.
		var inventory = s3.list("vdyp/fileset/");
		var folders = new LinkedHashMap<String, Folder>();
		var skipped = new ArrayList<String>();
		for (var version : inventory) {
			var folder = folder(folders, version.key());
			if (folder == null) {
				skipped.add(version.key());
			} else {
				folder.versions.add(version);
			}
		}
		for (var bucket : client.searchForBucket(null, null, null, null)) {
			if (storage.bucket().equals(bucket.bucket()) && storage.endpoint().equals(bucket.endpoint())) {
				var folder = folder(folders, bucket.key());
				if (folder != null) {
					if (bucket.bucketId() == null || bucket.bucketId().isBlank()) {
						throw new IllegalStateException("COMS returned a registration without an ID");
					}
					folder.registrations.add(bucket);
				}
			}
		}
		for (var folder : folders.values()) {
			folder.referenced = referenced(folder);
		}
		var results = new ArrayList<FolderResult>();
		for (var folder : folders.values()) {
			String outcome = folder.referenced ? "REFERENCED" : "ORPHAN";
			String detail = null;
			if (!dryRun && !folder.referenced) {
				// Database errors during the recheck abort further cleanup as well.
				if (referenced(folder)) {
					outcome = "REFERENCED";
				} else {
					try {
						deleteFolder(folder);
						outcome = "DELETED";
					} catch (RuntimeException e) {
						logger.warn("Orphan cleanup failed for {}", folder.key, e);
						outcome = "FAILED";
						detail = "Deletion incomplete; inspect backend logs and rerun preview before retrying";
					}
				}
			}
			results.add(
					new FolderResult(
							folder.key, folder.registrations.stream().map(COMSBucket::bucketId).distinct().toList(),
							folder.versions.stream().map(StoredVersion::key).distinct().count(), folder.versions.size(),
							folder.versions.stream().mapToLong(StoredVersion::size).sum(), outcome, detail
					)
			);
			logger.info("Orphan cleanup: key={}, dryRun={}, outcome={}", folder.key, dryRun, outcome);
		}
		return new CleanupReport(dryRun, inventory.size(), skipped.stream().distinct().toList(), List.copyOf(results));
	}

	private void deleteFolder(Folder folder) {
		s3.delete(folder.versions);
		if (s3.list(folder.key).stream()
				.anyMatch(v -> v.key().equals(folder.key) || v.key().startsWith(folder.key + "/"))) {
			throw new IllegalStateException("S3 objects or versions remain; retaining COMS registrations for retry");
		}
		// Children first; never recursively deregister unexamined COMS folders.
		folder.registrations.sort(Comparator.comparingInt((COMSBucket b) -> b.key().length()).reversed());
		for (var bucket : folder.registrations) {
			try (var response = client.deleteBucket(bucket.bucketId(), false)) {
				if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
					throw new IllegalStateException("COMS registration deletion failed: HTTP " + response.getStatus());
				}
			}
		}
	}

	private boolean referenced(Folder folder) {
		if (repository.count("projectionFileSetGUID", folder.id) > 0) {
			return true;
		}
		for (var bucket : folder.registrations) {
			if (repository.count("comsBucketId", bucket.bucketId()) > 0) {
				return true;
			}
		}
		return false;
	}

	private static Folder folder(LinkedHashMap<String, Folder> folders, String key) {
		var matcher = FILE_SET_KEY.matcher(key == null ? "" : key);
		if (!matcher.matches()) {
			return null;
		}
		return folders.computeIfAbsent(matcher.group(1), k -> new Folder(k, UUID.fromString(matcher.group(2))));
	}
}
