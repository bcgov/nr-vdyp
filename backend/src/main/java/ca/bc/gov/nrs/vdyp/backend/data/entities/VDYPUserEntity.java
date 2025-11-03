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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vdyp_user")
@Getter
@Setter
public class VDYPUserEntity extends AuditableEntity {
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

}
