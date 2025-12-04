package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.Optional;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VDYPUserRepository implements PanacheRepositoryBase<VDYPUserEntity, UUID> {

	public Optional<VDYPUserEntity> findByOIDC(String oidcGUID) {
		return find("oidcGUID", oidcGUID).firstResultOptional();
	}
}
