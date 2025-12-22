package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import ca.bc.gov.nrs.vdyp.backend.clients.COMSClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.FileMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.FileMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.model.COMSCreateObjectResponse;
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
    public COMSCreateObjectResponse createNewFile(UUID projectionGUID, VDYPUserModel user, FileSetTypeCodeModel fileSetType, FileMetadata metadata) {
        // Create the file name "projection/projectionGUID/[input|output]/pass the file meta data through as json and
        // get the filename and type here
        String filePrefix = String.format("/vdyp/projection/%s", projectionGUID, metadata.getFilename());
        COMSBucketSearchRequest request = 
        comsClient.searchForBucket()

        // contact COMS with the critical data
        byte[] body = new byte[metadata.getSize()];




        // persist a record here for the file

        // return the data from COMS up the chain


    }
}
