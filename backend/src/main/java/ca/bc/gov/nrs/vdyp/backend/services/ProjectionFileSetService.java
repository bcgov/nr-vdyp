package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.config.COMSS3Config;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionFileSetResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionFileSetRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileSetNotFoundException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionFileSetUnauthorizedException;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateBucketRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Handles business rules for file sets, including creation updating and deletion
 */
@ApplicationScoped
public class ProjectionFileSetService {
	ProjectionFileSetRepository repository;
	ProjectionFileSetResourceAssembler assembler;
	FileSetTypeCodeLookup lookup;
	FileMappingService fileMappingService;
	EntityManager em;
	COMSClient comsClient;
	COMSS3Config comsS3Config;

	public ProjectionFileSetService(
			EntityManager em, ProjectionFileSetRepository repository, ProjectionFileSetResourceAssembler assembler,
			FileSetTypeCodeLookup lookup, FileMappingService fileMappingService, @RestClient COMSClient comsClient,
			COMSS3Config comsS3Config
	) {
		this.em = em;
		this.repository = repository;
		this.assembler = assembler;
		this.lookup = lookup;
		this.fileMappingService = fileMappingService;
		this.comsClient = comsClient;
		this.comsS3Config = comsS3Config;
	}

	FileSetTypeCodeModel[] projectionFileSets;

	private void ensureFileSetTypeCodes() {
		if (projectionFileSets == null) {
			projectionFileSets = new FileSetTypeCodeModel[3];
			projectionFileSets[0] = lookup.requireModel(FileSetTypeCodeModel.POLYGON);
			projectionFileSets[1] = lookup.requireModel(FileSetTypeCodeModel.LAYER);
			projectionFileSets[2] = lookup.requireModel(FileSetTypeCodeModel.RESULTS);
		}
	}

	public Map<FileSetTypeCodeModel, ProjectionFileSetModel> createFileSetForNewProjection(VDYPUserModel actingUser) {
		ensureFileSetTypeCodes();
		var map = new HashMap<FileSetTypeCodeModel, ProjectionFileSetModel>();
		for (FileSetTypeCodeModel type : projectionFileSets) {
			map.put(type, createEmptyFileSet(type, actingUser));
		}
		return map;
	}

	@Transactional
	public ProjectionFileSetModel createEmptyFileSet(FileSetTypeCodeModel typeCodeModel, VDYPUserModel actingUser) {
		ProjectionFileSetEntity saveEntity = new ProjectionFileSetEntity();
		saveEntity.setOwnerUser(em.find(VDYPUserEntity.class, UUID.fromString(actingUser.getVdypUserGUID())));
		saveEntity.setFileSetTypeCode(lookup.requireEntity(typeCodeModel.getCode()));
		repository.persist(saveEntity);
		return assembler.toModel(saveEntity);
	}

	@Transactional
	public void deleteFileSetById(UUID polygonFileSetGuid) throws ProjectionServiceException {
		// delete the files
		fileMappingService.deleteFilesForSet(polygonFileSetGuid);

		// delete the bucket in COMS
		String bucketID = getCOMSBucketGUID(polygonFileSetGuid, true);
		if (bucketID != null) {
			comsClient.deleteBucket(bucketID, true); // recursive because we have should have already deleted the files
		}

		repository.deleteById(polygonFileSetGuid);
	}

	public ProjectionFileSetEntity getProjectionFileSetEntity(UUID projectionFileSetGuid)
			throws ProjectionServiceException {
		Optional<ProjectionFileSetEntity> entity = repository.findByIdOptional(projectionFileSetGuid);
		if (entity.isEmpty()) {
			// Handle Projection File Set does not exist
			throw new ProjectionFileSetNotFoundException(projectionFileSetGuid);
		}
		return entity.get();
	}

	private void ensureAuthorizedAccess(ProjectionFileSetEntity entity, VDYPUserModel actingUser)
			throws ProjectionServiceException {
		// Check that the user owns the fileset (FUTURE PROOFING if you use someone elses fileset you should not be able
		// to edit it) trivial check currently
		if (entity.getOwnerUser() != null
				&& !entity.getOwnerUser().getVdypUserGUID().equals(UUID.fromString(actingUser.getVdypUserGUID()))) {
			throw new ProjectionFileSetUnauthorizedException(entity.getProjectionFileSetGUID(), actingUser);
		}
	}

	@Transactional
	public FileMappingModel
			addNewFileToFileSet(UUID projectionGUID, UUID fileSetGUID, VDYPUserModel user, FileUpload file)
					throws ProjectionServiceException {
		// Check that the file set exists
		var entity = getProjectionFileSetEntity(fileSetGUID);

		ensureAuthorizedAccess(entity, user);

		String bucketID = getCOMSBucketGUID(fileSetGUID, true);

		// Ask File Mapping Service to create the file based on the meta data
		return fileMappingService.createNewFile(bucketID, entity, file);

	}

	private String getBucketPrefix(UUID fileSetGUID) {
		return String.format("vdyp/fileset/%s", fileSetGUID);
	}

	private String getCOMSBucketGUID(UUID fileSetGUID, boolean createIfNotExist) throws ProjectionServiceException {
		String filePrefix = getBucketPrefix(fileSetGUID);
		List<COMSBucket> searchResponse = comsClient.searchForBucket(null, true, filePrefix, null);
		String bucketGUID = null;
		if (searchResponse.isEmpty()) {
			COMSCreateBucketRequest request = buildCreateBucketRequest(fileSetGUID, filePrefix);
			COMSBucket createBucketResponse = comsClient.createBucket(request);
			bucketGUID = createBucketResponse.bucketId();
		} else if (createIfNotExist) {
			bucketGUID = searchResponse.get(0).bucketId();
		}
		return bucketGUID;
	}

	private COMSCreateBucketRequest buildCreateBucketRequest(UUID fileSetGUID, String keyPrefix) {
		return new COMSCreateBucketRequest(
				comsS3Config.accessId(), //
				true, //
				comsS3Config.bucket(), //
				"Projection File Set " + fileSetGUID + " Files", //
				comsS3Config.endpoint(), //
				comsS3Config.secretAccessKey(), //
				keyPrefix
		);
	}

	@Transactional
	public void deleteFileFromFileSet(UUID fileSetGUID, VDYPUserModel user, UUID fileGUID)
			throws ProjectionServiceException {
		// Check that the file set exists
		var entity = getProjectionFileSetEntity(fileSetGUID);

		ensureAuthorizedAccess(entity, user);

		// Ask File Mapping Service to create the file based on the meta data
		fileMappingService.deleteFileMapping(fileGUID);
	}

	@Transactional
	public void deleteAllFilesFromFileSet(UUID fileSetGUID, VDYPUserModel user) throws ProjectionServiceException {
		// Check that the file set exists
		var entity = getProjectionFileSetEntity(fileSetGUID);

		ensureAuthorizedAccess(entity, user);

		// Ask File Mapping Service to create the file based on the meta data
		fileMappingService.deleteFilesForSet(fileSetGUID);
	}

	public FileMappingModel getFileForDownload(UUID fileSetGUID, VDYPUserModel actingUser, UUID fileGUID)
			throws ProjectionServiceException {
		// Check that the file set exists
		var entity = getProjectionFileSetEntity(fileSetGUID);
		ensureAuthorizedAccess(entity, actingUser);

		// Ask file mapping service for the file
		return fileMappingService.getFileById(fileGUID, true);
	}

	public List<FileMappingModel> getAllFilesForDownload(UUID fileSetGUID, VDYPUserModel actingUser)
			throws ProjectionServiceException {
		// Check that the file set exists
		var entity = getProjectionFileSetEntity(fileSetGUID);
		ensureAuthorizedAccess(entity, actingUser);

		// Ask file mapping service for the file
		return fileMappingService.getFilesForFileSet(fileSetGUID, true);
	}

}
