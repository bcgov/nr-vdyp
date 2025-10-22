package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import ca.bc.gov.nrs.vdyp.backend.data.Auditable;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@EntityListeners(AuditListener.class)
@Table(name = "projection_param_preset")
@Getter
@Setter
public class ProjectionParameterPresetEntity extends PanacheEntityBase implements Auditable {
	@Id
	@NotNull
	@GeneratedValue
	@UuidGenerator
	@Column(name = "projection_param_preset_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionParameterPresetGUID;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "vdyp_user_guid")
	private VDYPUserEntity ownerUser;

	@Column(name = "preset_name", length = 4000)
	private String presetName;

	@Column(name = "preset_parameters", length = 4000)
	private String presetParameters;

	@NotNull
	@Column(name = "revision_count", columnDefinition = "numeric(10) default '0'")
	private Integer revisionCount;

	@NotNull
	@Column(name = "create_user", length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(name = "create_date", nullable = false)
	private Date createDate;

	@NotNull
	@Column(name = "update_user", length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(name = "update_date", nullable = false)
	private Date updateDate;

	@Override
	public void incrementRevisionCount() {
		this.revisionCount++;
	}

	@Override
	public void setCreatedBy(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreatedDate(Date date) {
		this.createDate = date;
	}

	@Override
	public void setLastModifiedBy(String user) {
		this.updateUser = user;
	}

	@Override
	public void setLastModifiedDate(Date date) {
		this.updateDate = date;

	}

}
