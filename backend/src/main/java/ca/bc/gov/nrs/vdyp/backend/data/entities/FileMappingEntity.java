package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "file_mapping")
public class FileMappingEntity extends AuditableEntity {
	@Id
	@NotNull
	@GeneratedValue
	@UuidGenerator
	@Column(name = "file_mapping_guid", nullable = false, updatable = false, length = 36)
	private UUID fileMappingGUID;

	@ManyToOne
	@JoinColumn(referencedColumnName = "projection_file_set_guid", name = "projection_file_set_guid", nullable = false)
	private ProjectionFileSetEntity projectionFileSet;

	@NotNull
	@Column(name = "coms_object_guid", nullable = false, updatable = true, length = 36)
	private UUID comsObjectGUID;

	public UUID getFileMappingGUID() {
		return fileMappingGUID;
	}

	public ProjectionFileSetEntity getProjectionFileSet() {
		return projectionFileSet;
	}

	public UUID getComsObjectGUID() {
		return comsObjectGUID;
	}

	public void setFileMappingGUID(UUID fileMappingGUID) {
		this.fileMappingGUID = fileMappingGUID;
	}

	public void setProjectionFileSet(ProjectionFileSetEntity projectionFileSet) {
		this.projectionFileSet = projectionFileSet;
	}

	public void setComsObjectGUID(UUID comsObjectGUID) {
		this.comsObjectGUID = comsObjectGUID;
	}
}
