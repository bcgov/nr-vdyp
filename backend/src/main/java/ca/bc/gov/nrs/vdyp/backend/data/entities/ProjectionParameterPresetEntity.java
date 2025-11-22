package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "projection_param_preset")
@Getter
@Setter
public class ProjectionParameterPresetEntity extends AuditableEntity {
	@Id
	@NotNull
	@GeneratedValue
	@UuidGenerator
	@Column(name = "projection_param_preset_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionParameterPresetGUID;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "vdyp_user_guid", referencedColumnName = "vdyp_user_guid", nullable = false)
	private VDYPUserEntity ownerUser;

	@Column(name = "preset_name", length = 4000)
	private String presetName;

	@Column(name = "projection_parameters_json", length = 4000)
	private String presetParameters;

}
