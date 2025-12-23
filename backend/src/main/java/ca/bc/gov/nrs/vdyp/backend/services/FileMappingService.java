package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.model.COMSBucket;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateBucketRequest;
import ca.bc.gov.nrs.vdyp.backend.model.COMSObject;
import ca.bc.gov.nrs.vdyp.backend.model.FileMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FileMappingService {
	private FileMappingRepository repository;
	private FileMappingResourceAssembler assembler;

	@Inject
	@RestClient
	COMSClient comsClient;

	public FileMappingService(FileMappingRepository repository, FileMappingResourceAssembler assembler) {
		this.repository = repository;
		this.assembler = assembler;
	}

	/**
     * Open a new file in COMS
     * @param projectionGUID
     * @param user
     * @param fileSetType
     * @param metadata
     * @return
     */
	public FileMappingModel createNewFile(
			UUID projectionGUID, ProjectionFileSetEntity projectionFileSetEntity, VDYPUserModel user,
			FileSetTypeCodeModel fileSetType, FileMetadata metadata
	) {
        // Create the file name "projection/projectionGUID/[input|output]/pass the file meta data through as json and
        // get the filename and type here
		String filePrefix = String.format("/vdyp/projection/%s", projectionGUID);
		Set<COMSBucket> searchResponse = comsClient.searchForBucket(null, true, filePrefix, null);
		UUID bucketGUID;
		if (searchResponse.isEmpty()) {
			COMSCreateBucketRequest request = new COMSCreateBucketRequest(
					"", true, "", "Projection " + projectionGUID + " Files", "", "", ""
			);
			COMSBucket createBucketResponse = comsClient.createBucket(request);
			bucketGUID = UUID.fromString(createBucketResponse.BucketId);
		} else {
			bucketGUID = UUID.fromString(searchResponse.stream().flatMap(b -> b.BucketId).findFirst());
		}

        // contact COMS with the critical data
        byte[] body = new byte[metadata.getSize()];

		COMSObject createObjectResponse = comsClient.createObject();
		UUID objectGUID = UUID.fromString(createObjectResponse.id());

        // persist a record here for the file
		FileMappingEntity entity = new FileMappingEntity();
		entity.setComsObjectGUID(objectGUID);
		entity.setProjectionFileSet(projectionFileSetEntity);
		repository.persist(entity);

        // return the data from COMS up the chain
		return assembler.toModel(entity);
    }
}
