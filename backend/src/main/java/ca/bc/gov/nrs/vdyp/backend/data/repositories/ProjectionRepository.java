package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public class ProjectionRepository implements PanacheRepositoryBase<ProjectionEntity, UUID> {

}
