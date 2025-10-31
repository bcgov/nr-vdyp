package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ca.bc.gov.nrs.vdyp.backend.data.AuditListener;
import ca.bc.gov.nrs.vdyp.backend.data.Auditable;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
@Table(name = "vdyp_user")
@Getter
@Setter
public class VDYPUserEntity extends PanacheEntityBase implements Auditable {
	@Id
	@NotNull
	@GeneratedValue
	@UuidGenerator
	@Column(name = "vdyp_user_guid", nullable = false, updatable = false, length = 36)
	private UUID vdypUserGUID;

	@NotNull
	@Column(name = "oidc_id", nullable = false, updatable = true, length = 36)
	private String oidcGUID;

	@ManyToOne
	@JoinColumn(name = "user_type_code", referencedColumnName = "user_type_code", nullable = false, updatable = true)
	private UserTypeCodeEntity userTypeCode;

	@Column(name = "first_name", length = 50)
	private String firstName;

	@Column(name = "last_name", length = 50)
	private String lastName;

	@NotNull
	@Column(name = "revision_count", length = 64, nullable = false)
	private BigDecimal revisionCount;

	@NotNull
	@Column(name = "create_user", length = 64, nullable = false)
	private String createUser;

	@NotNull
	@Column(name = "create_date", nullable = false)
	private LocalDate createDate;

	@NotNull
	@Column(name = "update_user", length = 64, nullable = false)
	private String updateUser;

	@NotNull
	@Column(name = "update_date", nullable = false)
	private LocalDate updateDate;

	@Override
	public void incrementRevisionCount() {
		this.revisionCount = this.revisionCount.add(BigDecimal.ONE);
	}

	@Override
	public void setCreatedBy(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreatedDate(LocalDate date) {
		this.createDate = date;
	}

	@Override
	public void setLastModifiedBy(String user) {
		this.updateUser = user;
	}

	@Override
	public void setLastModifiedDate(LocalDate date) {
		this.updateDate = date;
	}

}
