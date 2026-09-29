package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.Collection;
import java.util.List;
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

}
