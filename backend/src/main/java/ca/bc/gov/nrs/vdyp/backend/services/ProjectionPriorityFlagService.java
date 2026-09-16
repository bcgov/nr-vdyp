package ca.bc.gov.nrs.vdyp.backend.services;

import java.util.Comparator;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionBatchMappingRepository;
import ca.bc.gov.nrs.vdyp.backend.exceptions.ProjectionServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

// Separate bean so REQUIRES_NEW actually applies - a CDI interceptor is skipped on self-invocation.
@ApplicationScoped
public class ProjectionPriorityFlagService {

	private final ProjectionBatchMappingRepository repository;

	public ProjectionPriorityFlagService(ProjectionBatchMappingRepository repository) {
		this.repository = repository;
	}

	// Commits immediately so the clear-all lock isn't held across the caller's later batch service call.
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public UUID activatePriorityFlag(ProjectionEntity projectionEntity) throws ProjectionServiceException {
		var mapping = repository.listByProjectionGUID(projectionEntity.getProjectionGUID()).stream()
				.filter(entity -> entity.getBatchJobGUID() != null)
				.max(Comparator.comparing(ProjectionBatchMappingEntity::getCreateDate)).orElseThrow(
						() -> new ProjectionServiceException(
								"No batch job mapping found for projection " + projectionEntity.getProjectionGUID()
						)
				);
		repository.clearAllPrioritized();
		mapping.setPrioritized(true);
		return mapping.getBatchJobGUID();
	}
}
