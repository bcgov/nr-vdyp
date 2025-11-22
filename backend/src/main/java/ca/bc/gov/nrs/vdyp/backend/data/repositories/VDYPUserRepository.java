package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public class VDYPUserRepository implements PanacheRepositoryBase<VDYPUserEntity, UUID> {
}
