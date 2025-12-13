package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionFileSetRepository implements PanacheRepositoryBase<ProjectionFileSetEntity, UUID> {

}
