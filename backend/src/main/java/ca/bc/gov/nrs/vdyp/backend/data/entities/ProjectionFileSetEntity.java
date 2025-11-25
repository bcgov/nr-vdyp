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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "projection_file_set")
@Getter
@Setter
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

}
