package ca.bc.gov.nrs.vdyp.backend.services;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class FileMappingService {
	private static final Logger logger = LoggerFactory.getLogger(FileMappingService.class);
	private FileMappingRepository repository;
	private FileMappingResourceAssembler assembler;

	private COMSClient comsClient;

	private COMSS3Config comsS3Config;

	public FileMappingService(
			FileMappingRepository repository, FileMappingResourceAssembler assembler, @RestClient COMSClient comsClient,
			COMSS3Config comsS3Config
	) {
		this.repository = repository;
		this.assembler = assembler;
		this.comsClient = comsClient;
		this.comsS3Config = comsS3Config;
	}

	public FileMappingModel
			createNewFile(UUID projectionGUID, ProjectionFileSetEntity projectionFileSetEntity, FileUpload file)
					throws ProjectionServiceException {
		// Create the file name "projection/projectionGUID/[input|output]/pass the file meta data through as json and
		// get the filename and type here
		String filePrefix = String.format("vdyp/projection/%s", projectionGUID);
		List<COMSBucket> searchResponse = comsClient.searchForBucket(null, true, filePrefix, null);
		String bucketGUID;
		if (searchResponse.isEmpty()) {
			COMSCreateBucketRequest request = buildCreateBucketRequest(projectionGUID, filePrefix);
			COMSBucket createBucketResponse = comsClient.createBucket(request);
			bucketGUID = createBucketResponse.bucketId();
		} else {
			bucketGUID = searchResponse.get(0).bucketId();
		}
		try {
			String contentDisposition = buildContentDisposition(file.fileName());
			long contentLength = Files.size(file.uploadedFile());

			String contentType = file.contentType() != null ? file.contentType() : MediaType.APPLICATION_OCTET_STREAM;
			try (InputStream fileStream = Files.newInputStream(file.uploadedFile())) {
				logger.info(
						"Data for object bucketId {}, contentDisposistion {}, contentLength {}, contentType {}",
						bucketGUID, contentDisposition, contentLength, contentType
				);
				COMSObject createObjectResponse = comsClient.createObject(
						bucketGUID, contentDisposition, contentLength, contentType, fileStream

				);
				UUID objectGUID = UUID.fromString(createObjectResponse.id());

				// persist a record here for the file
				FileMappingEntity entity = new FileMappingEntity();
				entity.setComsObjectGUID(objectGUID);
				entity.setProjectionFileSet(projectionFileSetEntity);
				repository.persist(entity);

				// return the data from COMS up the chain
				return assembler.toModel(entity);
			}
		} catch (Exception e) {
			throw new ProjectionServiceException("Error uploading file to COMS", e, projectionGUID);
		}
	}

	private static String buildContentDisposition(String filename) {
		// Minimal safe implementation. COMS spec wants RFC6266 with filename/filename*.
		// Start with this; add RFC8187 encoding later if you have non-ASCII names.
		String safe = filename == null ? "upload.bin" : filename.replace("\"", "");
		return "attachment; filename=\"" + safe + "\"";
	}

	public COMSCreateBucketRequest buildCreateBucketRequest(UUID projectionGuid, String keyPrefix) {
		return new COMSCreateBucketRequest(
				comsS3Config.accessId(), //
				true, //
				comsS3Config.bucket(), //
				"Projection " + projectionGuid + " Files", //
				comsS3Config.endpoint(), //
				comsS3Config.secretAccessKey(), //
				keyPrefix
		);
	}

	private FileMappingEntity getFileMappingEntity(UUID fileMappingGUID) throws ProjectionServiceException {
		var entity = repository.findByIdOptional(fileMappingGUID);
		if (entity.isEmpty()) {
			throw new ProjectionServiceException(String.format("File %s does not exist in VDYP", fileMappingGUID));
		}
		return entity.get();
	}

	public FileMappingModel getFileById(UUID fileMappingGUID, boolean isDownload) throws ProjectionServiceException {
		var entity = getFileMappingEntity(fileMappingGUID);
		return getFileDetails(entity, isDownload);
	}

	public List<FileMappingModel> getFilesForFileSet(UUID fileSetGUID, boolean isDownload) {
		List<FileMappingEntity> entities = repository.listForFileSet(fileSetGUID);
		return entities.stream().map(e -> getFileDetails(e, isDownload)).toList();
	}

	private FileMappingModel getFileDetails(FileMappingEntity entity, boolean isDownload) {
		var model = assembler.toModel(entity);
		if (isDownload) {
			model.setDownloadURL(
					comsClient.getObject(model.getComsObjectGUID(), COMSClient.FileDownloadMode.URL.getParamValue())
							.getString()
			);
		}
		return model;
	}

	public void deleteFileMapping(UUID fileMappingGUID) throws ProjectionServiceException {
		var entity = getFileMappingEntity(fileMappingGUID);
		deleteFile(entity);
	}

	public void deleteFilesForSet(UUID polygonFileSetGuid) throws ProjectionServiceException {
		List<FileMappingEntity> entities = repository.listForFileSet(polygonFileSetGuid);
		for (FileMappingEntity entity : entities) {
			deleteFile(entity);
		}
	}

	private void deleteFile(FileMappingEntity entity) throws ProjectionServiceException {
		UUID comsObjectID = entity.getComsObjectGUID();
		try (Response response = comsClient.deleteObject(comsObjectID.toString())) {
			if (response.getStatusInfo().getStatusCode() == Response.Status.OK.getStatusCode()) {
				repository.delete(entity);
			} else {
				throw new ProjectionServiceException("Could not delete object in COMS");
			}
		}
	}
}
