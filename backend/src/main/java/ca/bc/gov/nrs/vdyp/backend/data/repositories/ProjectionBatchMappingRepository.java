package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProjectionBatchMappingRepository implements PanacheRepositoryBase<ProjectionBatchMappingEntity, UUID> {
	private EntityManager entityManager;

	public ProjectionBatchMappingRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * The current mapping for a projection. Deliberately tolerant of more than one row existing for the same projection
	 * (a cancel/re-run race can otherwise leave a stray one behind) by returning the most recently created one rather
	 * than asserting uniqueness, so a stray row degrades to "ignored" instead of breaking every future lookup for that
	 * projection.
	 */
	public Optional<ProjectionBatchMappingEntity> findByProjectionGUID(UUID projectionGUID) {
		return find("projection.projectionGUID = ?1 order by createDate desc", projectionGUID).firstResultOptional();
	}

	public List<ProjectionBatchMappingEntity> listByProjectionGUID(UUID projectionGUID) {
		return list("projection.projectionGUID = ?1", projectionGUID);
	}

	/**
	 * Clears the prioritized flag on every mapping. Deliberately unconditional: locking every row serializes concurrent
	 * prioritize calls instead of letting two overlapping transactions both end up true.
	 */
	public void clearAllPrioritized() {
		update("isPrioritized = false");
	}

	/**
	 * RUNNING batch mappings that have gone quiet: no progress update since the given cutoff. Falls back to the
	 * mapping's create date when no progress update has ever been received, so a projection that never reports any
	 * throughput is still caught once it's old enough.
	 */
	public List<ProjectionBatchMappingEntity> findStaleRunningMappings(OffsetDateTime threshold) {
		return list(
				"projection.projectionStatusCode.projectionStatusCode = ?1 "
						+ "and coalesce(lastProgressTime, createDate) < ?2",
				ProjectionStatusCodeModel.RUNNING, threshold
		);
	}

	/**
	 * STUCK batch mappings that have started reporting progress again since the given cutoff, and so should revert to
	 * RUNNING.
	 */
	public List<ProjectionBatchMappingEntity> findRecoveredStuckMappings(OffsetDateTime threshold) {
		return list(
				"projection.projectionStatusCode.projectionStatusCode = ?1 "
						+ "and lastProgressTime is not null and lastProgressTime >= ?2",
				ProjectionStatusCodeModel.STUCK, threshold
		);
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
