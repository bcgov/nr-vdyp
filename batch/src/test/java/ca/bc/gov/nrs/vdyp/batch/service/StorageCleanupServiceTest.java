package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;

import ca.bc.gov.nrs.vdyp.batch.configuration.BatchProperties;
import ca.bc.gov.nrs.vdyp.batch.model.StorageCleanupRequest;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@ExtendWith(MockitoExtension.class)
class StorageCleanupServiceTest {

	@Mock
	private JobExplorer jobExplorer;

	@TempDir
	Path tempDir;

	private BatchProperties batchProperties;
	private StorageCleanupService service;

	@BeforeEach
	void setUp() {
		batchProperties = new BatchProperties();
		batchProperties.setRootDirectory(tempDir.toString());
		service = new StorageCleanupService(batchProperties, jobExplorer);

		lenient().when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME)).thenReturn(Set.of());
	}

	private Path createJobFolder(String jobGuid, String fileContent) throws IOException {
		Path folder = tempDir.resolve(BatchConstants.Job.BASE_FOLDER_PREFIX + "-" + jobGuid);
		Files.createDirectory(folder);
		Files.writeString(folder.resolve("output.csv"), fileContent, StandardCharsets.UTF_8);
		return folder;
	}

	private Path createWarningsFile(String jobGuid, String content) throws IOException {
		Path file = tempDir.resolve(BatchConstants.Job.BASE_FOLDER_PREFIX + "-" + jobGuid + "warnings.txt");
		Files.writeString(file, content, StandardCharsets.UTF_8);
		return file;
	}

	private JobExecution runningJobExecutionWithGuid(String jobGuid) {
		var params = new HashMap<String, JobParameter<?>>();
		params.put(BatchConstants.Job.GUID, new JobParameter<>(jobGuid, String.class, true));
		JobInstance jobInstance = new JobInstance(1L, BatchConstants.Job.JOB_NAME);
		return new JobExecution(jobInstance, 1L, new JobParameters(params));
	}

	private StorageCleanupService.CleanupSetResult onlyResult(StorageCleanupService.StorageCleanupReport report) {
		assertEquals(1, report.sets().size());
		return report.sets().get(0);
	}

	@Test
	void run_RootDirectoryMissing_ReturnsEmptyReport() {
		batchProperties.setRootDirectory(tempDir.resolve("does-not-exist").toString());

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(true, null));

		assertEquals(0, report.scanned());
		assertTrue(report.sets().isEmpty());
		assertTrue(report.skippedNames().isEmpty());
		assertEquals(0, report.totalBytes());
		assertTrue(report.dryRun());
	}

	@Test
	void run_UnrecognizedEntries_AreSkippedAndUntouched() throws IOException {
		Path randomFile = tempDir.resolve("random-file.txt");
		Files.writeString(randomFile, "not ours", StandardCharsets.UTF_8);
		Path randomFolder = tempDir.resolve("some-other-folder");
		Files.createDirectory(randomFolder);

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		assertEquals(0, report.scanned());
		assertTrue(report.sets().isEmpty());
		assertEquals(Set.of("random-file.txt", "some-other-folder"), Set.copyOf(report.skippedNames()));
		assertTrue(Files.exists(randomFile));
		assertTrue(Files.exists(randomFolder));
	}

	@Test
	void run_DryRun_DeletableSet_IsNotDeletedAndReportedDeletable() throws IOException {
		String jobGuid = "11111111-1111-1111-1111-111111111111";
		Path folder = createJobFolder(jobGuid, "hello");
		Path warnings = createWarningsFile(jobGuid, "warn");

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(true, null));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(jobGuid, result.jobGuid());
		assertEquals(StorageCleanupService.Outcome.DELETABLE, result.outcome());
		assertEquals("hello".length() + "warn".length(), result.bytes());
		assertEquals(result.bytes(), report.totalBytes());
		assertTrue(Files.exists(folder));
		assertTrue(Files.exists(warnings));
	}

	@Test
	void run_ActualDelete_DeletesBothFolderAndWarningsFile() throws IOException {
		String jobGuid = "22222222-2222-2222-2222-222222222222";
		Path folder = createJobFolder(jobGuid, "hello");
		Path warnings = createWarningsFile(jobGuid, "warn");

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(StorageCleanupService.Outcome.DELETED, result.outcome());
		assertFalse(Files.exists(folder));
		assertFalse(Files.exists(warnings));
	}

	@Test
	void run_ProtectedByBackendStatus_IsNotDeletedEvenWhenDryRunFalse() throws IOException {
		String jobGuid = "33333333-3333-3333-3333-333333333333";
		Path folder = createJobFolder(jobGuid, "hello");
		Path warnings = createWarningsFile(jobGuid, "warn");

		StorageCleanupService.StorageCleanupReport report = service
				.run(new StorageCleanupRequest(false, List.of(jobGuid)));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(StorageCleanupService.Outcome.PROTECTED, result.outcome());
		assertTrue(Files.exists(folder));
		assertTrue(Files.exists(warnings));
		verify(jobExplorer, never()).findRunningJobExecutions(BatchConstants.Job.JOB_NAME);
	}

	@Test
	void run_ProtectedByRunningExecution_IsNotDeleted() throws IOException {
		String jobGuid = "44444444-4444-4444-4444-444444444444";
		Path folder = createJobFolder(jobGuid, "hello");
		Path warnings = createWarningsFile(jobGuid, "warn");
		when(jobExplorer.findRunningJobExecutions(BatchConstants.Job.JOB_NAME))
				.thenReturn(Set.of(runningJobExecutionWithGuid(jobGuid)));

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(StorageCleanupService.Outcome.PROTECTED, result.outcome());
		assertTrue(Files.exists(folder));
		assertTrue(Files.exists(warnings));
	}

	@Test
	void run_FolderOnlyWithNoWarningsFile_IsDeletedWithoutError() throws IOException {
		String jobGuid = "55555555-5555-5555-5555-555555555555";
		Path folder = createJobFolder(jobGuid, "hello");

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(StorageCleanupService.Outcome.DELETED, result.outcome());
		assertEquals("hello".length(), result.bytes());
		assertFalse(Files.exists(folder));
	}

	@Test
	void run_WarningsFileOnlyWithNoFolder_IsDeletedWithoutError() throws IOException {
		String jobGuid = "66666666-6666-6666-6666-666666666666";
		Path warnings = createWarningsFile(jobGuid, "warn");

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		StorageCleanupService.CleanupSetResult result = onlyResult(report);
		assertEquals(StorageCleanupService.Outcome.DELETED, result.outcome());
		assertEquals("warn".length(), result.bytes());
		assertFalse(Files.exists(warnings));
	}

	@Test
	void run_MultipleCandidates_ChecksForRunningExecutionsFreshPerCandidate() throws IOException {
		createJobFolder("77777777-7777-7777-7777-777777777777", "a");
		createJobFolder("88888888-8888-8888-8888-888888888888", "bb");

		StorageCleanupService.StorageCleanupReport report = service.run(new StorageCleanupRequest(false, null));

		assertEquals(2, report.scanned());
		verify(jobExplorer, times(2)).findRunningJobExecutions(BatchConstants.Job.JOB_NAME);
	}

	@Test
	void run_TotalBytes_OnlySumsDeletableOrDeletedSets() throws IOException {
		String protectedGuid = "99999999-9999-9999-9999-999999999999";
		createJobFolder(protectedGuid, "protected-content");
		String deletableGuid = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
		createJobFolder(deletableGuid, "hi");

		StorageCleanupService.StorageCleanupReport report = service
				.run(new StorageCleanupRequest(true, List.of(protectedGuid)));

		assertEquals(2, report.scanned());
		assertEquals("hi".length(), report.totalBytes());
	}

	@Test
	void run_RootIsNotADirectory_ThrowsStorageCleanupException() throws IOException {
		Path notADirectory = tempDir.resolve("not-a-directory");
		Files.writeString(notADirectory, "oops", StandardCharsets.UTF_8);
		batchProperties.setRootDirectory(notADirectory.toString());

		assertThrows(
				StorageCleanupService.StorageCleanupException.class,
				() -> service.run(new StorageCleanupRequest(true, null))
		);
	}

}
