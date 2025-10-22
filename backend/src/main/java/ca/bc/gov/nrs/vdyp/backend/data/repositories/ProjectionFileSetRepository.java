package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public class ProjectionFileSetRepository implements PanacheRepositoryBase<FileMappingEntity, UUID> {
	public UUID projectionFileSetGUID;
	public FileSetTypeCodeModel fileSetTypeCode;
	public String fileSetName;
}
