package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import ca.bc.gov.nrs.vdyp.backend.model.ProjectionProgressUpdate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectionBatchMappingService {
	private static final Logger logger = LoggerFactory.getLogger(ProjectionBatchMappingService.class);

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

	@Transactional
	public void cancelProjection(ProjectionEntity projectionEntity) throws ProjectionServiceException {
		try {
			ProjectionBatchMappingEntity entity = repository.findByProjectionGUID(projectionEntity.getProjectionGUID())
					.orElseThrow(
							() -> new ProjectionServiceException(
									"No batch mapping found for projection GUID: "
											+ projectionEntity.getProjectionGUID()
							)
					);

			batchClient.stopBatchJob(entity.getBatchJobGUID());
			repository.delete(entity);
		} catch (Exception e) {
			throw new ProjectionServiceException("Error cancelling projection batch process", e);
		}
	}

	@Transactional
	public void deleteMappingsForProjection(ProjectionEntity projectionEntity) {
		List<ProjectionBatchMappingEntity> entityList = repository
				.listByProjectionGUID(projectionEntity.getProjectionGUID());

		entityList.forEach(e -> repository.delete(e));
	}

	@Transactional
	public void updateProgress(ProjectionEntity projectionEntity, ProjectionProgressUpdate progressUpdate)
			throws ProjectionServiceException {
		try {
			ProjectionBatchMappingEntity entity = repository.findByProjectionGUID(projectionEntity.getProjectionGUID())
					.orElseThrow(
							() -> new ProjectionServiceException(
									"No batch mapping found for projection GUID: "
											+ projectionEntity.getProjectionGUID()
							)
					);
			entity.setPolygonCount(progressUpdate.totalPolygons());
			entity.setErrorCount(progressUpdate.projectionErrors());
			entity.setCompletedPolygonCount(progressUpdate.polygonsProcessed());
		} catch (Exception e) {
			throw new ProjectionServiceException("Error updating projection batch progress", e);
		}
	}

	public Map<UUID, ProjectionBatchMappingModel> getLatestBatchMappingsForProjections(List<UUID> projectionGUIDs) {
		Map<UUID, ProjectionBatchMappingEntity> latestEntities = repository
				.findLatestByProjectionGUIDs(projectionGUIDs);
		return latestEntities.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> assembler.toModel(entry.getValue())));
	}
}
