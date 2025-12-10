package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.List;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionRepository implements PanacheRepositoryBase<ProjectionEntity, UUID> {
	public List<ProjectionEntity> findByOwner(UUID vdypUserGUID) {
		return list("ownerUser.vdypUserGUID", vdypUserGUID);
	}

}
