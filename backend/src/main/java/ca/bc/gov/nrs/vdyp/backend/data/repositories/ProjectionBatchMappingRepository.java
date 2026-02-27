package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProjectionBatchMappingRepository implements PanacheRepositoryBase<ProjectionBatchMappingEntity, UUID> {
	private EntityManager entityManager;

	public ProjectionBatchMappingRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Optional<ProjectionBatchMappingEntity> findByProjectionGUID(UUID projectionGUID) {
		return find("projection.projectionGUID = ?1", projectionGUID).singleResultOptional();
	}

	public List<ProjectionBatchMappingEntity> listByProjectionGUID(UUID projectionGUID) {
		return list("projection.projectionGUID = ?1", projectionGUID);
	}

	public Map<UUID, ProjectionBatchMappingEntity> findLatestByProjectionGUIDs(List<UUID> projectionGUIDs) {
		String sql = """
				    select bm.*
				                from (
				                    select bm.*,
				                    row_number() over (
				                       partition by bm.projection_guid
				                       order by bm.create_date desc, bm.projection_batch_mapping_guid desc
				                    ) as rn
				                    from projection_batch_mapping bm
				                    where bm.projection_guid in (?1)
				                ) bm
				                where bm.rn = 1
				""";

		List<ProjectionBatchMappingEntity> latestMappings = entityManager.unwrap(Session.class)
				.createNativeQuery(sql, ProjectionBatchMappingEntity.class).setParameter(1, projectionGUIDs)
				.getResultList();
		return latestMappings.stream().collect(
				Collectors.toMap(bm -> bm.getProjection().getProjectionGUID(), Function.identity(), (a, b) -> a)
		);

	}
}
