package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.List;
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

}
