package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditListener.class)
@Table(name = "file_mapping")
@Getter
@Setter
public class FileMappingEntity extends AuditableEntity {
	@Id
	@NotNull
	@GeneratedValue
	@UuidGenerator
	@Column(name = "file_mapping_guid", nullable = false, updatable = false, length = 36)
	private UUID fileMappingGUID;

	@ManyToOne
	@JoinColumn(referencedColumnName = "projection_file_set_guid", name = "projection_file_set_guid")
	private ProjectionFileSetEntity projectionFileSet;

	@NotNull
	@Column(name = "coms_object_guid", nullable = false, updatable = true, length = 36)
	private UUID comsObjectGUID;


}
