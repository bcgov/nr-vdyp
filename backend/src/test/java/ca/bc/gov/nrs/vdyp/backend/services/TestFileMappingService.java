package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
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

	FileMappingService service;

	@BeforeEach
	void setUp() {
		assembler = new FileMappingResourceAssembler();
		service = new FileMappingService(repository, assembler, comsClient);
	}

	@Test
	void createNewFile_PersistsEntity(@TempDir Path tempDir) throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();
		fileSetEntity.setProjectionFileSetGUID(fileSetGUID);

		// real temp file for Files.size() + Files.newInputStream()
		Path uploaded = tempDir.resolve("input.txt");
		Files.writeString(uploaded, "hi");

		when(fileUpload.fileName()).thenReturn("input.txt");
		when(fileUpload.contentType()).thenReturn(MediaType.TEXT_PLAIN);
		when(fileUpload.uploadedFile()).thenReturn(uploaded);

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
						eq("bucket-guid-123"), startsWith("attachment; filename="), anyLong(), eq(MediaType.TEXT_PLAIN),
						any()
				)
		).thenReturn(createdObject);

		FileMappingModel result = service.createNewFile("bucket-guid-123", fileSetEntity, fileUpload);

		assertNotNull(result);
		assertEquals(objectGuid.toString(), result.getComsObjectGUID());

		verify(repository).persist(any(FileMappingEntity.class));
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

		assertEquals("https://example.com/presigned", result.getDownloadURL());
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

		// Try-with-resources will close it; Response is safe to use here.
		Response ok = Response.ok().build();
		when(comsClient.deleteObject(any())).thenReturn(ok);

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

		// Try-with-resources will close it; Response is safe to use here.
		Response ok = Response.ok().build();
		when(comsClient.deleteObject(comsObjectGuid.toString())).thenReturn(ok);

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

		Response bad = Response.status(Response.Status.BAD_REQUEST).build();
		when(comsClient.deleteObject(comsObjectGuid.toString())).thenReturn(bad);

		assertThrows(ProjectionServiceException.class, () -> service.deleteFileMapping(fileMappingGuid));
		verify(repository, never()).delete(any());
	}
}
