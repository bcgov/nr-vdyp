package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.clients.VDYPBatchClient;
import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionBatchMappingResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchJobModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectionBatchMappingService {
	private static final Logger logger = LoggerFactory.getLogger(FileMappingService.class);

	private ProjectionBatchMappingRepository repository;
	private ProjectionBatchMappingResourceAssembler assembler;

	private VDYPBatchClient batchClient;

	public ProjectionBatchMappingService(
			ProjectionBatchMappingRepository repository, ProjectionBatchMappingResourceAssembler assembler,
			@RestClient VDYPBatchClient batchClient
	) {
		this.repository = repository;
		this.assembler = assembler;
		this.batchClient = batchClient;
	}

	@Transactional
	public ProjectionBatchMappingModel startProjectionInBatch(ProjectionEntity projectionEntity)
			throws ProjectionServiceException {
		try {
			BatchJobModel model = batchClient.startBatchProcessWithGUID(
					projectionEntity.getProjectionGUID(), projectionEntity.getProjectionParameters()
			);
			ProjectionBatchMappingEntity entity = new ProjectionBatchMappingEntity();
			entity.setBatchJobGUID(UUID.fromString(model.id()));
			entity.setErrorCount(model.errors());
			entity.setWarningCount(model.warnings());
			entity.setProjection(projectionEntity);
			entity.setCompletedPolygonCount(0);
			entity.setPolygonCount(0); // FIXME VDYP-918
			repository.persist(entity);
			return assembler.toModel(entity);
		} catch (Exception e) {
			throw new ProjectionServiceException("Error starting projection batch process", e);
		}
	}

}
