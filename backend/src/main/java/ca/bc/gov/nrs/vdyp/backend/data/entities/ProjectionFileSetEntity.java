package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projection_file_set")
public class ProjectionFileSetEntity extends AuditableEntity {
	@Id
	@GeneratedValue
	@Column(name = "projection_file_set_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionFileSetGUID;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "file_set_type_code", referencedColumnName = "file_set_type_code")
	private FileSetTypeCodeEntity fileSetTypeCode;

	@Column(name = "file_set_name", length = 4000)
	private String fileSetName;

	public UUID getProjectionFileSetGUID() {
		return projectionFileSetGUID;
	}

	public FileSetTypeCodeEntity getFileSetTypeCode() {
		return fileSetTypeCode;
	}

	public String getFileSetName() {
		return fileSetName;
	}

	public void setProjectionFileSetGUID(UUID projectionFileSetGUID) {
		this.projectionFileSetGUID = projectionFileSetGUID;
	}

	public void setFileSetTypeCode(FileSetTypeCodeEntity fileSetTypeCode) {
		this.fileSetTypeCode = fileSetTypeCode;
	}

	public void setFileSetName(String fileSetName) {
		this.fileSetName = fileSetName;
	}
}
