package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionParameterPresetEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public class ProjectionParameterPresetRepository
		implements PanacheRepositoryBase<ProjectionParameterPresetEntity, UUID> {

}
