package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileMappingRepository implements PanacheRepositoryBase<FileMappingEntity, UUID> {
	public List<FileMappingEntity> listForFileSet(UUID fileSetID) {
		return list("projectionFileSet.projectionFileSetGUID", fileSetID);
	}

	public List<FileMappingEntity> listByCOMSObjectID(UUID comsObjectID) {
		return list("comsObjectGUID", comsObjectID);
	}

	// Returns the given file sets that have at least one file, in a single query
	public Set<UUID> findFileSetGUIDsWithFiles(Collection<UUID> fileSetGUIDs) {
		if (fileSetGUIDs.isEmpty()) {
			return Set.of();
		}
		return Set.copyOf(
				getEntityManager().createQuery(
						"select distinct fm.projectionFileSet.projectionFileSetGUID from FileMappingEntity fm "
								+ "where fm.projectionFileSet.projectionFileSetGUID in :fileSetGUIDs",
						UUID.class
				).setParameter("fileSetGUIDs", fileSetGUIDs).getResultList()
		);
	}

	/**
	 * For each given file set, adds up the polygon counts that were saved when its files were uploaded. All file sets
	 * are handled in one query.
	 *
	 * A file set is left out of the result if it has no files, or if any of its files has no saved count (for example,
	 * a file uploaded before counts were saved). Adding up only some of the files would give a number that is too
	 * small.
	 */
	public Map<UUID, Integer> findEstimatedPolygonCountsByFileSet(Collection<UUID> fileSetGUIDs) {
		if (fileSetGUIDs.isEmpty()) {
			return Map.of();
		}
		List<Object[]> rows = getEntityManager().createQuery(
				"select fm.projectionFileSet.projectionFileSetGUID, sum(fm.estimatedPolygonCount) from FileMappingEntity fm "
						+ "where fm.projectionFileSet.projectionFileSetGUID in :fileSetGUIDs "
						+ "group by fm.projectionFileSet.projectionFileSetGUID "
						+ "having count(fm.estimatedPolygonCount) = count(fm)",
				Object[].class
		).setParameter("fileSetGUIDs", fileSetGUIDs).getResultList();

		Map<UUID, Integer> counts = new HashMap<>();
		for (Object[] row : rows) {
			counts.put((UUID) row[0], ((Number) row[1]).intValue());
		}
		return counts;
	}
}
