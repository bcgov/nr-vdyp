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

@Entity
@Table(name = "projection_param_preset")
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

	public UUID getProjectionParameterPresetGUID() {
		return projectionParameterPresetGUID;
	}

	public VDYPUserEntity getOwnerUser() {
		return ownerUser;
	}

	public String getPresetName() {
		return presetName;
	}

	public String getPresetParameters() {
		return presetParameters;
	}

	public void setProjectionParameterPresetGUID(UUID projectionParameterPresetGUID) {
		this.projectionParameterPresetGUID = projectionParameterPresetGUID;
	}

	public void setOwnerUser(VDYPUserEntity ownerUser) {
		this.ownerUser = ownerUser;
	}

	public void setPresetName(String presetName) {
		this.presetName = presetName;
	}

	public void setPresetParameters(String presetParameters) {
		this.presetParameters = presetParameters;
	}
}
