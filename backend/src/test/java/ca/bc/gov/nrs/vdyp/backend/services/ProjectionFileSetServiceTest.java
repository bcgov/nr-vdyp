package ca.bc.gov.nrs.vdyp.backend.services;

import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetEntity;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.fileSetTypeCodeModel;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.user;
import static ca.bc.gov.nrs.vdyp.backend.test.TestUtils.userEntity;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionFileSetResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileSetTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionFileSetRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class ProjectionFileSetServiceTest {

	@Mock
	EntityManager em;
	@Mock
	ProjectionFileSetRepository repository;
	@Mock
	FileSetTypeCodeLookup fileSetTypeCodeLookup;
	@Mock
	FileMappingService fileMappingService;
	ProjectionFileSetResourceAssembler assembler;
	@Mock
	COMSClient comsClient;
	@Mock
	COMSS3Config comsS3Config;
	ProjectionFileSetService service;

	@BeforeEach
	void setUp() {
		assembler = new ProjectionFileSetResourceAssembler();

		service = new ProjectionFileSetService(
				em, repository, assembler, fileSetTypeCodeLookup, fileMappingService, comsClient, comsS3Config
		);
	}

	@Test
	void createFileSetForNewProjection_createsThreeEntries_andCachesTypeCodes() {
		UUID ownerId = UUID.randomUUID();
		VDYPUserModel actingUser = user(ownerId);

		// ensureFileSetTypeCodes() calls lookup.requireModel for each constant
		FileSetTypeCodeModel polygonType = fileSetTypeCodeModel(FileSetTypeCodeModel.POLYGON);
		FileSetTypeCodeModel layerType = fileSetTypeCodeModel(FileSetTypeCodeModel.LAYER);
		FileSetTypeCodeModel resultsType = fileSetTypeCodeModel(FileSetTypeCodeModel.RESULTS);
		when(fileSetTypeCodeLookup.requireModel(FileSetTypeCodeModel.POLYGON)).thenReturn(polygonType);
		when(fileSetTypeCodeLookup.requireModel(FileSetTypeCodeModel.LAYER)).thenReturn(layerType);
		when(fileSetTypeCodeLookup.requireModel(FileSetTypeCodeModel.RESULTS)).thenReturn(resultsType);

		// Spy so we can stub/verify createEmptyFileSet calls without worrying about assembler/entity
		ProjectionFileSetService spy = spy(service);

		ProjectionFileSetModel polygonModel = fileSetModel(UUID.randomUUID(), ownerId, FileSetTypeCodeModel.POLYGON);
		ProjectionFileSetModel layerModel = fileSetModel(UUID.randomUUID(), ownerId, FileSetTypeCodeModel.LAYER);
		ProjectionFileSetModel resultsModel = fileSetModel(UUID.randomUUID(), ownerId, FileSetTypeCodeModel.RESULTS);

		doReturn(polygonModel).when(spy).createEmptyFileSet(polygonType, actingUser);
		doReturn(layerModel).when(spy).createEmptyFileSet(layerType, actingUser);
		doReturn(resultsModel).when(spy).createEmptyFileSet(resultsType, actingUser);

		Map<FileSetTypeCodeModel, ProjectionFileSetModel> first = spy.createFileSetForNewProjection(actingUser);

		assertThat(first).hasSize(3);
		assertThat(first).containsKeys(polygonType, layerType, resultsType);

		verify(spy).createEmptyFileSet(polygonType, actingUser);
		verify(spy).createEmptyFileSet(layerType, actingUser);
		verify(spy).createEmptyFileSet(resultsType, actingUser);

		// Call again to prove caching: requireModel should NOT be called again
		Map<FileSetTypeCodeModel, ProjectionFileSetModel> second = spy.createFileSetForNewProjection(actingUser);
		assertThat(second).hasSize(3);

		verify(fileSetTypeCodeLookup, times(1)).requireModel(FileSetTypeCodeModel.POLYGON);
		verify(fileSetTypeCodeLookup, times(1)).requireModel(FileSetTypeCodeModel.LAYER);
		verify(fileSetTypeCodeLookup, times(1)).requireModel(FileSetTypeCodeModel.RESULTS);
	}

	// ------------------------------------------------------------
	// createEmptyFileSet
	// ------------------------------------------------------------

	@Test
	void createEmptyFileSet_persistsEntity_andReturnsAssembledModel() {
		UUID ownerId = UUID.randomUUID();
		VDYPUserModel actingUser = user(ownerId);

		VDYPUserEntity ownerEntity = userEntity(ownerId);
		when(em.find(VDYPUserEntity.class, ownerId)).thenReturn(ownerEntity);

		// lookup.requireEntity expects the code string from typeCodeModel.getCode()
		// FileSetTypeCodeModel constants typically have getCode() returning something stable
		FileSetTypeCodeEntity typeEntity = new FileSetTypeCodeEntity();
		typeEntity.setCode(FileSetTypeCodeModel.POLYGON);

		when(fileSetTypeCodeLookup.requireEntity(FileSetTypeCodeModel.POLYGON)).thenReturn(typeEntity);

		// Simulate repository.persist setting the GUID (if your assembler needs it)
		UUID fileSetId = UUID.randomUUID();
		doAnswer(inv -> {
			ProjectionFileSetEntity e = inv.getArgument(0, ProjectionFileSetEntity.class);
			e.setProjectionFileSetGUID(fileSetId);
			return null;
		}).when(repository).persist(any(ProjectionFileSetEntity.class));

		ProjectionFileSetModel result = service
				.createEmptyFileSet(fileSetTypeCodeModel(FileSetTypeCodeModel.POLYGON), actingUser);

		assertNotNull(result);
		assertThat(result.getProjectionFileSetGUID()).isEqualTo(fileSetId.toString());

		// Verify persisted entity was populated correctly
		ArgumentCaptor<ProjectionFileSetEntity> captor = ArgumentCaptor.forClass(ProjectionFileSetEntity.class);
		verify(repository).persist(captor.capture());

		ProjectionFileSetEntity persisted = captor.getValue();
		assertThat(persisted.getOwnerUser()).isSameAs(ownerEntity);
		assertThat(persisted.getFileSetTypeCode()).isSameAs(typeEntity);

		verify(em).find(VDYPUserEntity.class, ownerId);
		verify(fileSetTypeCodeLookup).requireEntity(FileSetTypeCodeModel.POLYGON);
	}

	// ------------------------------------------------------------
	// deleteFileSetById
	// ------------------------------------------------------------

	@Test
	void deleteFileSetById_deletesById() throws ProjectionServiceException {
		UUID id = UUID.randomUUID();

		COMSBucket bucket = new COMSBucket("bucket", "bucket-name", "coms-bucket-id", "region", "endpoint", "key");
		when(comsClient.searchForBucket(any(), any(), any(), any())).thenReturn(List.of(bucket));

		service.deleteFileSetById(id);

		verify(repository).deleteById(id);
		verifyNoMoreInteractions(repository);
		verify(comsClient).deleteBucket("coms-bucket-id", true);
	}

	@Test
	void getFileSetById_invalidGuid_throwsException() {
		UUID fileSetGuid = UUID.randomUUID();

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.empty());

		assertThrows(ProjectionServiceException.class, () -> service.getProjectionFileSetEntity(fileSetGuid));
	}

	@Test
	void getFileSetById_validGuid_returnsEntity() throws Exception {
		UUID fileSetGUID = UUID.randomUUID();
		ProjectionFileSetEntity entity = new ProjectionFileSetEntity();
		entity.setProjectionFileSetGUID(fileSetGUID);

		when(repository.findByIdOptional(fileSetGUID)).thenReturn(Optional.of(entity));

		ProjectionFileSetEntity result = service.getProjectionFileSetEntity(fileSetGUID);
		assertNotNull(result);
		assertEquals(entity, result);
	}

	@Test
	void addFileToSet_invalidUser_throwsException() {
		UUID fileSetGuid = UUID.randomUUID();

		UUID ownerGuid = UUID.randomUUID();
		VDYPUserEntity owner = new VDYPUserEntity();
		owner.setVdypUserGUID(ownerGuid);
		UUID invalidOwnerGuid = UUID.randomUUID();
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(invalidOwnerGuid.toString());

		ProjectionFileSetEntity entity = new ProjectionFileSetEntity();
		entity.setProjectionFileSetGUID(fileSetGuid);
		entity.setOwnerUser(owner);

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));

		assertThrows(
				ProjectionServiceException.class, () -> service.addNewFileToFileSet(fileSetGuid, actingUser, null)
		);
	}

	@Test
	void addFileToSet_validUser_createsBucketWhenMissing_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));
		COMSBucket bucket = new COMSBucket("bucket", "bucket-name", "coms-bucket-id", "region", "endpoint", "key");
		when(comsClient.searchForBucket(any(), any(), any(), any())).thenReturn(List.of());
		when(comsClient.createBucket(any())).thenReturn(bucket);

		FileMappingModel fakeResult = new FileMappingModel();
		when(fileMappingService.createNewFile("coms-bucket-id", entity, null)).thenReturn(fakeResult);

		service.addNewFileToFileSet(fileSetGuid, actingUser, null);
		verify(fileMappingService).createNewFile("coms-bucket-id", entity, null);
		verify(comsClient).createBucket(any());
	}

	@Test
	void addFileToSet_validUser_usesExistingBucket_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));
		COMSBucket bucket = new COMSBucket("bucket", "bucket-name", "coms-bucket-id", "region", "endpoint", "key");
		when(comsClient.searchForBucket(any(), any(), any(), any())).thenReturn(List.of(bucket));

		FileMappingModel fakeResult = new FileMappingModel();
		when(fileMappingService.createNewFile("coms-bucket-id", entity, null)).thenReturn(fakeResult);

		service.addNewFileToFileSet(fileSetGuid, actingUser, null);
		verify(fileMappingService).createNewFile("coms-bucket-id", entity, null);
		verify(comsClient, never()).createBucket(any());
	}

	@Test
	void deleteFileFromSet_validUser_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID fileMappingGuid = UUID.randomUUID();

		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));

		service.deleteFileFromFileSet(fileSetGuid, actingUser, fileMappingGuid);
		verify(fileMappingService).deleteFileMapping(fileMappingGuid);
	}

	@Test
	void deleteAllFilesFromSet_validUser_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));

		service.deleteAllFilesFromFileSet(fileSetGuid, actingUser);
		verify(fileMappingService).deleteFilesForSet(fileSetGuid);
	}

	@Test
	void getFileForDownload_validUser_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID fileMappingGuid = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));

		FileMappingModel stubReturn = new FileMappingModel();
		when(fileMappingService.getFileById(fileMappingGuid, true)).thenReturn(stubReturn);

		FileMappingModel result = service.getFileForDownload(fileSetGuid, actingUser, fileMappingGuid);
		assertNotNull(result);

		verify(fileMappingService).getFileById(fileMappingGuid, true);
	}

	@Test
	void getAllFilesForDownload_validUser_callsFileMappingService() throws Exception {
		UUID fileSetGuid = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		var entity = fileSetEntity(fileSetGuid, ownerId);
		VDYPUserModel actingUser = new VDYPUserModel();
		actingUser.setVdypUserGUID(ownerId.toString());

		when(repository.findByIdOptional(fileSetGuid)).thenReturn(Optional.of(entity));

		FileMappingModel stubReturn = new FileMappingModel();
		when(fileMappingService.getFilesForFileSet(fileSetGuid, true)).thenReturn(List.of(stubReturn));

		List<FileMappingModel> result = service.getAllFilesForDownload(fileSetGuid, actingUser);
		assertNotNull(result);

		verify(fileMappingService).getFilesForFileSet(fileSetGuid, true);
	}

}
