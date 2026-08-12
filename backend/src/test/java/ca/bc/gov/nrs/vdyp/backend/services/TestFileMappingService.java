package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.config.CsvUploadConfig;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileUploadException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObjectVersion;
import ca.bc.gov.nrs.vdyp.backend.services.upload.CsvUploadValidator;
import jakarta.json.Json;
import jakarta.json.JsonString;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class TestFileMappingService {
	@Mock
	FileMappingRepository repository;
	FileMappingResourceAssembler assembler;
	@Mock
	COMSClient comsClient;
	@Mock
	FileUpload fileUpload;
	@Mock
	HttpClient httpClient;
	@Mock
	FileMappingPersistenceService persistenceService;
	@Mock
	CsvUploadConfig csvUploadConfig;

	FileMappingService service;

	@BeforeEach
	void setUp() {
		assembler = new FileMappingResourceAssembler();
		lenient().when(csvUploadConfig.maxFileSizeBytes()).thenReturn(1_000_000_000L);
		lenient().when(csvUploadConfig.maxColumns()).thenReturn(512);
		lenient().when(csvUploadConfig.maxFieldChars()).thenReturn(1_048_576);
		lenient().when(csvUploadConfig.maxRecordBytes()).thenReturn(10_485_760);
		service = new FileMappingService(
				repository, assembler, comsClient, httpClient, persistenceService,
				new CsvUploadValidator(csvUploadConfig)
		);
	}

	@Test
	void createNewFile_PersistsEntity(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		// real temp file for Files.size() + Files.newInputStream()
		Path uploaded = tempDir.resolve("input.csv");
		Files.writeString(uploaded, "A,B\n1,2\n");

		when(fileUpload.fileName()).thenReturn("input.csv");
		when(fileUpload.contentType()).thenReturn("text/csv");
		when(fileUpload.uploadedFile()).thenReturn(uploaded);
		when(fileUpload.size()).thenReturn(Files.size(uploaded));

		// COMS: create object
		UUID objectGuid = UUID.randomUUID();
		COMSObject createdObject = new COMSObject(
				objectGuid.toString(), /* path */ "vdyp/fileset/x/file", /* public */ false, /* active */ true,
				/* bucketId */ "existing-bucket-guid", /* name */ "input.bin", /* lastSyncedDate */ null,
				/* createdBy */ null, /* createdAt */ null, /* updatedBy */ null, /* updatedAt */ null,
				/* lastModifiedDate */ null, /* permissions */ Set.of("READ")
		);
		when(
				comsClient.createObject(
						eq("bucket-guid-123"), startsWith("attachment; filename="), eq(8L), eq("text/csv"),
						any()
				)
		).thenAnswer(invocation -> {
			InputStream in = invocation.getArgument(4);
			in.transferTo(OutputStream.nullOutputStream());
			return createdObject;
		});
		FileMappingModel persistedModel = new FileMappingModel();
		persistedModel.setComsObjectGUID(objectGuid.toString());
		persistedModel.setFilename("input.csv");
		when(persistenceService.persistFileMapping(objectGuid, fileSetGUID, "input.csv")).thenReturn(persistedModel);

		FileMappingModel result = service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload);

		assertNotNull(result);
		assertEquals(objectGuid.toString(), result.getComsObjectGUID());
		assertEquals("input.csv", result.getFilename());

		verify(persistenceService).persistFileMapping(objectGuid, fileSetGUID, "input.csv");
	}

	@Test
	void createNewFile_nullFile_throwsExceptionDoesNotCreate() {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		assertThrows(
				ProjectionServiceException.class, () -> service.createNewFile("IRRELEVANT-GUID", fileSetEntity, null)
		);

		verify(repository, never()).persist(any(FileMappingEntity.class));
		verify(persistenceService, never()).persistFileMapping(any(), any(), any());
	}

	@Test
	void createNewFile_validationFailure_doesNotCreateObjectOrPersist(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		when(fileUpload.fileName()).thenReturn("input.txt");

		assertThrows(
				ProjectionFileUploadException.class,
				() -> service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload)
		);

		verify(comsClient, never()).createObject(any(), any(), anyLong(), any(), any());
		verify(persistenceService, never()).persistFileMapping(any(), any(), any());
	}

	@Test
	void createNewFile_streamValidationFailureAfterComsStarts_doesNotPersist(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		Path uploaded = tempDir.resolve("input.csv");
		Files.writeString(uploaded, "%PDF-1.7\n");

		when(fileUpload.fileName()).thenReturn("input.csv");
		when(fileUpload.contentType()).thenReturn("text/csv");
		when(fileUpload.uploadedFile()).thenReturn(uploaded);
		when(fileUpload.size()).thenReturn(Files.size(uploaded));
		when(comsClient.createObject(eq("bucket-guid-123"), any(), eq(Files.size(uploaded)), eq("text/csv"), any()))
				.thenAnswer(invocation -> {
					InputStream in = invocation.getArgument(4);
					in.transferTo(OutputStream.nullOutputStream());
					return null;
				});

		assertThrows(
				ProjectionFileUploadException.class,
				() -> service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload)
		);

		verify(comsClient).createObject(eq("bucket-guid-123"), any(), eq(Files.size(uploaded)), eq("text/csv"), any());
		verify(persistenceService, never()).persistFileMapping(any(), any(), any());
	}

	@Test
	void createNewFile_actualStreamOverLimitAfterComsStarts_doesNotPersist(@TempDir Path tempDir) throws Exception {
		when(csvUploadConfig.maxFileSizeBytes()).thenReturn(7L);
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		Path uploaded = tempDir.resolve("input.csv");
		Files.writeString(uploaded, "A,B\n1,2\n");

		when(fileUpload.fileName()).thenReturn("input.csv");
		when(fileUpload.contentType()).thenReturn("text/csv");
		when(fileUpload.uploadedFile()).thenReturn(uploaded);
		when(fileUpload.size()).thenReturn(7L);
		when(comsClient.createObject(eq("bucket-guid-123"), any(), eq(7L), eq("text/csv"), any()))
				.thenAnswer(invocation -> {
					InputStream in = invocation.getArgument(4);
					in.transferTo(OutputStream.nullOutputStream());
					return null;
				});

		ProjectionFileUploadException ex = assertThrows(
				ProjectionFileUploadException.class,
				() -> service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload)
		);

		assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.getStatus());
		verify(comsClient).createObject(eq("bucket-guid-123"), any(), eq(7L), eq("text/csv"), any());
		verify(persistenceService, never()).persistFileMapping(any(), any(), any());
	}

	@Test
	void createNewFile_storageFailure_doesNotPersist(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		Path uploaded = tempDir.resolve("input.csv");
		Files.writeString(uploaded, "A,B\n1,2\n");

		when(fileUpload.fileName()).thenReturn("input.csv");
		when(fileUpload.contentType()).thenReturn("text/csv");
		when(fileUpload.uploadedFile()).thenReturn(uploaded);
		when(fileUpload.size()).thenReturn(Files.size(uploaded));
		when(comsClient.createObject(any(), any(), anyLong(), any(), any()))
				.thenThrow(new RuntimeException("COMS down"));

		assertThrows(
				ProjectionServiceException.class,
				() -> service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload)
		);

		verify(persistenceService, never()).persistFileMapping(any(), any(), any());
	}

	@Test
	void createNewFile_persistenceFailure_deletesCreatedObject(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		Path uploaded = tempDir.resolve("input.csv");
		Files.writeString(uploaded, "A,B\n1,2\n");

		when(fileUpload.fileName()).thenReturn("input.csv");
		when(fileUpload.contentType()).thenReturn("text/csv");
		when(fileUpload.uploadedFile()).thenReturn(uploaded);
		when(fileUpload.size()).thenReturn(Files.size(uploaded));

		UUID objectGuid = UUID.randomUUID();
		COMSObject createdObject = new COMSObject(
				objectGuid.toString(), "vdyp/fileset/x/file", false, true, "existing-bucket-guid", "input.csv", null,
				null, null, null, null, null, Set.of("READ")
		);
		when(comsClient.createObject(eq("bucket-guid-123"), any(), eq(8L), eq("text/csv"), any()))
				.thenAnswer(invocation -> {
					InputStream in = invocation.getArgument(4);
					in.transferTo(OutputStream.nullOutputStream());
					return createdObject;
				});
		when(persistenceService.persistFileMapping(objectGuid, fileSetGUID, "input.csv"))
				.thenThrow(new RuntimeException("database down"));
		COMSObjectVersion version = new COMSObjectVersion("v1", "s3v1", objectGuid.toString(), false);
		when(comsClient.getObjectVersions(objectGuid.toString())).thenReturn(List.of(version));
		when(comsClient.deleteObjectVersion(objectGuid.toString(), "s3v1")).thenReturn(Response.ok().build());

		assertThrows(
				ProjectionServiceException.class,
				() -> service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload)
		);

		verify(comsClient).deleteObjectVersion(objectGuid.toString(), "s3v1");
	}

	@Test
	void getFileById_downloadTrue_setsDownloadUrl() throws Exception {
		UUID fileMappingGuid = UUID.randomUUID();
		UUID comsObjectGuid = UUID.randomUUID();
		String stringGUID = comsObjectGuid.toString();

		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(comsObjectGuid);

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.of(entity));

		// COMS getObject returns a Response whose entity is the URL string
		JsonString urlResponse = Json.createValue("https://example.com/presigned");
		when(comsClient.getObject(stringGUID, COMSClient.FileDownloadMode.URL.getParamValue())).thenReturn(urlResponse);

		FileMappingModel result = service.getFileById(fileMappingGuid, true);

		assertEquals(new URL("https://example.com/presigned"), result.getDownloadURL());
	}

	@Test
	void getFileById_invalidGuid_throwsException() {
		UUID fileMappingGuid = UUID.randomUUID();

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.empty());

		assertThrows(ProjectionServiceException.class, () -> service.getFileById(fileMappingGuid, false));

		verify(comsClient, never()).getObject(any(), any());
	}

	@Test
	void getFileById_downloadFalse_doesNotSetDownloadUrl() throws Exception {
		UUID fileMappingGuid = UUID.randomUUID();
		UUID comsObjectGuid = UUID.randomUUID();
		String stringGUID = comsObjectGuid.toString();

		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(comsObjectGuid);

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.of(entity));

		FileMappingModel model = new FileMappingModel();
		model.setComsObjectGUID(stringGUID);

		FileMappingModel result = service.getFileById(fileMappingGuid, false);

		assertNull(result.getDownloadURL());
		verify(comsClient, never()).getObject(any(), any());
	}

	@Test
	void getFilesForSet_downloadFalse_getsList() {
		UUID fileSetGuid = UUID.randomUUID();
		UUID fileMappingGuid1 = UUID.randomUUID();
		UUID fileMappingGuid2 = UUID.randomUUID();

		FileMappingEntity entity1 = new FileMappingEntity();
		FileMappingEntity entity2 = new FileMappingEntity();
		entity1.setFileMappingGUID(fileMappingGuid1);
		entity2.setFileMappingGUID(fileMappingGuid2);

		when(repository.listForFileSet(fileSetGuid)).thenReturn(List.of(entity1, entity2));

		// COMS getObject returns a Response whose entity is the URL string
		List<FileMappingModel> result = service.getFilesForFileSet(fileSetGuid, false);

		assertNotNull(result);
		assertEquals(2, result.size());
		List<String> resultGuids = result.stream().map(FileMappingModel::getFileMappingGUID).toList();

		assertTrue(resultGuids.contains(fileMappingGuid1.toString()));
		assertTrue(resultGuids.contains(fileMappingGuid2.toString()));
	}

	@Test
	void deleteFilesForSet_ok_deletesRepositoryEntities() throws Exception {
		UUID fileMappingGuid1 = UUID.randomUUID();
		UUID fileMappingGuid2 = UUID.randomUUID();
		UUID comsObjectGUID1 = UUID.randomUUID();
		UUID comsObjectGUID2 = UUID.randomUUID();
		UUID fileSetGUID = UUID.randomUUID();

		FileMappingEntity entity1 = new FileMappingEntity();
		FileMappingEntity entity2 = new FileMappingEntity();
		entity1.setFileMappingGUID(fileMappingGuid1);
		entity1.setComsObjectGUID(comsObjectGUID1);
		entity2.setFileMappingGUID(fileMappingGuid2);
		entity2.setComsObjectGUID(comsObjectGUID2);

		when(repository.listForFileSet(fileSetGUID)).thenReturn(List.of(entity1, entity2));

		COMSObjectVersion version1 = new COMSObjectVersion("v1", "s3v1", comsObjectGUID1.toString(), false);
		COMSObjectVersion version2 = new COMSObjectVersion("v2", "s3v2", comsObjectGUID2.toString(), false);
		when(comsClient.getObjectVersions(comsObjectGUID1.toString())).thenReturn(List.of(version1));
		when(comsClient.getObjectVersions(comsObjectGUID2.toString())).thenReturn(List.of(version2));
		when(comsClient.deleteObjectVersion(any(), any())).thenReturn(Response.ok().build());

		service.deleteFilesForSet(fileSetGUID);

		verify(repository).delete(entity1);
		verify(repository).delete(entity2);
	}

	@Test
	void deleteFileMapping_ok_deletesRepositoryEntity() throws Exception {
		UUID fileMappingGuid = UUID.randomUUID();
		UUID comsObjectGuid = UUID.randomUUID();

		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(comsObjectGuid);

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.of(entity));

		COMSObjectVersion version = new COMSObjectVersion("v1", "s3v1", comsObjectGuid.toString(), false);
		when(comsClient.getObjectVersions(comsObjectGuid.toString())).thenReturn(List.of(version));
		when(comsClient.deleteObjectVersion(any(), any())).thenReturn(Response.ok().build());

		service.deleteFileMapping(fileMappingGuid);

		verify(repository).delete(entity);
	}

	@Test
	void deleteFileMapping_nonOk_throwsAndDoesNotDelete() {
		UUID fileMappingGuid = UUID.randomUUID();
		UUID comsObjectGuid = UUID.randomUUID();

		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(comsObjectGuid);

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.of(entity));

		COMSObjectVersion version = new COMSObjectVersion("v1", "s3v1", comsObjectGuid.toString(), false);
		when(comsClient.getObjectVersions(comsObjectGuid.toString())).thenReturn(List.of(version));
		Response bad = Response.status(Response.Status.BAD_REQUEST).build();
		when(comsClient.deleteObjectVersion(comsObjectGuid.toString(), "s3v1")).thenReturn(bad);

		assertThrows(ProjectionServiceException.class, () -> service.deleteFileMapping(fileMappingGuid));
		verify(repository, never()).delete(any());
	}

	@Test
	void duplicateFile_missingContentLength_throwsProjectionServiceException() throws Exception {
		var file = new FileMappingModel();
		file.setDownloadURL(new URI("https://example.com/file.bin").toURL());
		file.setFilename("file.bin");
		HttpResponse<InputStream> resp = (HttpResponse<InputStream>) mock(HttpResponse.class);

		when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(resp);
		when(resp.statusCode()).thenReturn(200);
		when(resp.headers()).thenReturn(HttpHeaders.of(Collections.emptyMap(), (k, v) -> true));

		var ex = assertThrows(
				ProjectionServiceException.class,
				() -> service.duplicateFile(file, new ProjectionFileSetEntity(), "bucket")
		);
		assertTrue(ex.getMessage().contains("did not send Content-Length"));
	}

	@Test
	void duplicateFileNamesForSet_returnsTrue() throws Exception {
		// Arrange
		var file = new FileMappingModel();
		file.setDownloadURL(new URI("https://example.com/file.bin").toURL());
		file.setFilename("file.bin");

		ProjectionFileSetEntity fileSetEntity = fileSetEntity(UUID.randomUUID());
		var bucketGuid = "bucket-123";

		var bodyStream = new ByteArrayInputStream("abc".getBytes());
		HttpResponse<InputStream> resp = (HttpResponse<InputStream>) mock(HttpResponse.class);
		when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(resp);
		when(resp.statusCode()).thenReturn(200);
		when(resp.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Length", List.of("3")), (k, v) -> true));
		when(resp.body()).thenReturn(bodyStream);

		COMSObject comsObj = new COMSObject(
				"3f1a19ad-f87b-40bc-a1f0-94e0e688d939", "/some/path", false, true, "bucket-123", "file.bin", null,
				"tester", null, "tester", null, null, Set.of()
		);
		when(
				comsClient.createObject(
						eq(bucketGuid), anyString(), // content disposition
						eq(3L), eq(jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM), any(InputStream.class)
				)
		).thenReturn(comsObj);

		var persistedModel = new FileMappingModel();
		persistedModel.setComsObjectGUID(comsObj.id());
		persistedModel.setFilename("file.bin");
		when(
				persistenceService.persistFileMapping(
						UUID.fromString(comsObj.id()), fileSetEntity.getProjectionFileSetGUID(), "file.bin"
				)
		).thenReturn(persistedModel);

		// Act
		service.duplicateFile(file, fileSetEntity, bucketGuid);

		// Assert
		verify(persistenceService).persistFileMapping(
				UUID.fromString(comsObj.id()), fileSetEntity.getProjectionFileSetGUID(), "file.bin"
		);

		verify(comsClient).createObject(
				eq(bucketGuid), anyString(), eq(3L), eq(jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM),
				any(InputStream.class)
		);
	}

	@Test
	void duplicateFile_httpThrows_wrapsInProjectionServiceException() throws Exception {
		var file = new FileMappingModel();
		file.setDownloadURL(new URI("https://example.com/file.bin").toURL());
		file.setFilename("file.bin");

		when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenThrow(new IOException("boom"));

		var ex = assertThrows(
				ProjectionServiceException.class,
				() -> service.duplicateFile(file, new ProjectionFileSetEntity(), "bucket")
		);
		assertTrue(ex.getMessage().contains("Error duplicating file in COMS"));
		assertNotNull(ex.getCause());
	}

	@Test
	void createPlaceholderFile_persistsEntityAndReturnsModel() throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		UUID objectGuid = UUID.randomUUID();
		COMSObject createdObject = new COMSObject(
				objectGuid.toString(), "vdyp/fileset/x/placeholder", false, true, "bucket-id", "result.zip", null, null,
				null, null, null, null, Set.of()
		);
		when(
				comsClient.createObject(
						eq("bucket-id"), anyString(), anyLong(), eq(MediaType.APPLICATION_OCTET_STREAM), any()
				)
		).thenReturn(createdObject);
		FileMappingModel persistedModel = new FileMappingModel();
		persistedModel.setComsObjectGUID(objectGuid.toString());
		persistedModel.setFilename("result.zip");
		when(persistenceService.persistFileMapping(objectGuid, fileSetGUID, "result.zip")).thenReturn(persistedModel);

		FileMappingModel result = service.createPlaceholderFile("bucket-id", fileSetEntity, "result.zip");

		assertNotNull(result);
		assertEquals(objectGuid.toString(), result.getComsObjectGUID());
		assertEquals("result.zip", result.getFilename());
		verify(persistenceService).persistFileMapping(objectGuid, fileSetGUID, "result.zip");
	}

	@Test
	void createPlaceholderFile_comsThrows_wrapsInProjectionServiceException() {
		when(comsClient.createObject(any(), any(), anyLong(), any(), any()))
				.thenThrow(new RuntimeException("COMS error"));

		assertThrows(
				ProjectionServiceException.class,
				() -> service.createPlaceholderFile("bucket-id", new ProjectionFileSetEntity(), "result.zip")
		);
	}
}
