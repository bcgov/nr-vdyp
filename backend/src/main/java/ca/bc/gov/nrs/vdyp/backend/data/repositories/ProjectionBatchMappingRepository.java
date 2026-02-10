package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionBatchMappingRepository implements PanacheRepositoryBase<ProjectionBatchMappingEntity, UUID> {
	public Optional<ProjectionBatchMappingEntity> findByProjectionGUID(UUID projectionGUID) {
		return find("projection.projectionGUID = ?1", projectionGUID).singleResultOptional();
	}

	public List<ProjectionBatchMappingEntity> listByProjectionGUID(UUID projectionGUID) {
		return list("projection.projectionGUID = ?1", projectionGUID);
	}
}
