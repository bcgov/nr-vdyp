package ca.bc.gov.nrs.vdyp.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateBucketRequest;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import jakarta.json.Json;
import jakarta.json.JsonString;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
public class TestFileMappingService {
	@Mock
	FileMappingRepository repository;
	FileMappingResourceAssembler assembler;
	@Mock
	COMSClient comsClient;
	@Mock
	COMSS3Config comsS3Config;
	@Mock
	FileUpload fileUpload;

	FileMappingService service;

	@BeforeEach
	void setUp() {
		assembler = new FileMappingResourceAssembler();
		service = new FileMappingService(repository, assembler, comsClient, comsS3Config);
	}

	@Test
	void createNewFile_createsBucketWhenMissing_andPersistsEntity(@TempDir Path tempDir) throws Exception {
		UUID projectionGuid = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();

		// real temp file for Files.size() + Files.newInputStream()
		Path uploaded = tempDir.resolve("input.txt");
		Files.writeString(uploaded, "hi");

		when(fileUpload.fileName()).thenReturn("input.txt");
		when(fileUpload.contentType()).thenReturn(MediaType.TEXT_PLAIN);
		when(fileUpload.uploadedFile()).thenReturn(uploaded);

		// config used by buildCreateBucketRequest()
		when(comsS3Config.accessId()).thenReturn("access");
		when(comsS3Config.secretAccessKey()).thenReturn("secret");
		when(comsS3Config.bucket()).thenReturn("bucket");
		when(comsS3Config.endpoint()).thenReturn("endpoint");

		// COMS: bucket search empty => create bucket
		when(comsClient.searchForBucket(isNull(), eq(true), anyString(), isNull())).thenReturn(List.of());

		COMSBucket createdBucket = new COMSBucket("bucket", "name", "bucket-guid-123", "endpoint", "key", "region");
		when(comsClient.createBucket(any(COMSCreateBucketRequest.class))).thenReturn(createdBucket);

		// COMS: create object
		UUID objectGuid = UUID.randomUUID();
		COMSObject createdObject = new COMSObject(
				objectGuid.toString(), /* path */ "vdyp/projection/x/file", /* public */ false, /* active */ true,
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

		FileMappingModel result = service.createNewFile(projectionGuid, fileSetEntity, fileUpload);

		assertNotNull(result);
		assertEquals(objectGuid, result.getComsObjectGUID());

		verify(repository).persist(any(FileMappingEntity.class));
		verify(comsClient).createBucket(any(COMSCreateBucketRequest.class));
		verify(comsClient).createObject(
				eq("bucket-guid-123"), anyString(), eq(Files.size(uploaded)), eq(MediaType.TEXT_PLAIN), any()
		);
	}

	@Test
	void createNewFile_usesExistingBucket_whenSearchReturnsBucket(@TempDir Path tempDir) throws Exception {
		UUID projectionGuid = UUID.randomUUID();
		ProjectionFileSetEntity fileSetEntity = new ProjectionFileSetEntity();

		Path uploaded = tempDir.resolve("input.bin");
		Files.write(uploaded, new byte[] { 1, 2, 3 });

		when(fileUpload.fileName()).thenReturn("input.bin");
		when(fileUpload.contentType()).thenReturn(null); // triggers application/octet-stream
		when(fileUpload.uploadedFile()).thenReturn(uploaded);

		COMSBucket existingBucket = new COMSBucket(
				"bucket", "name", "existing-bucket-guid", "endpoint", "key", "region"
		);
		when(comsClient.searchForBucket(isNull(), eq(true), anyString(), isNull())).thenReturn(List.of(existingBucket));

		UUID objectGuid = UUID.randomUUID();
		COMSObject createdObject = new COMSObject(
				objectGuid.toString(), /* path */ "vdyp/projection/x/file", /* public */ false, /* active */ true,
				/* bucketId */ "existing-bucket-guid", /* name */ "input.bin", /* lastSyncedDate */ null,
				/* createdBy */ null, /* createdAt */ null, /* updatedBy */ null, /* updatedAt */ null,
				/* lastModifiedDate */ null, /* permissions */ Set.of("READ")
		);

		when(
				comsClient.createObject(
						eq("existing-bucket-guid"), anyString(), anyLong(), eq(MediaType.APPLICATION_OCTET_STREAM),
						any()
				)
		).thenReturn(createdObject);

		FileMappingModel result = service.createNewFile(projectionGuid, fileSetEntity, fileUpload);

		assertEquals(objectGuid, result.getComsObjectGUID());
		verify(comsClient, never()).createBucket(any());
	}

	@Test
	void getFileById_downloadTrue_setsDownloadUrl() throws Exception {
		UUID fileMappingGuid = UUID.randomUUID();
		UUID comsObjectGuid = UUID.randomUUID();
		String stringGUID = comsObjectGuid.toString();

		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(comsObjectGuid);

		when(repository.findByIdOptional(fileMappingGuid)).thenReturn(Optional.of(entity));

		FileMappingModel model = new FileMappingModel();
		model.setComsObjectGUID(stringGUID);
		when(assembler.toModel(entity)).thenReturn(model);

		// COMS getObject returns a Response whose entity is the URL string
		JsonString urlResponse = Json.createValue("https://example.com/presigned-url");
		when(comsClient.getObject(eq(stringGUID), eq(COMSClient.FileDownloadMode.URL.getParamValue())))
				.thenReturn(urlResponse);

		FileMappingModel result = service.getFileById(fileMappingGuid, true);

		assertEquals("https://example.com/presigned", result.getDownloadURL());
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
	void deleteFileMapping_nonOk_throwsAndDoesNotDelete() throws Exception {
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
