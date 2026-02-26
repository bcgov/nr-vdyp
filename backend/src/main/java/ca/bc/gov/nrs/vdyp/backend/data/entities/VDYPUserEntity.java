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
@Table(name = "vdyp_user")
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

	@Column(name = "display_name", length = 250)
	private String displayName;

	@Column(name = "email", length = 255)
	private String email;

	public UUID getVdypUserGUID() {
		return vdypUserGUID;
	}

	public String getOidcGUID() {
		return oidcGUID;
	}

	public UserTypeCodeEntity getUserTypeCode() {
		return userTypeCode;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setVdypUserGUID(UUID vdypUserGUID) {
		this.vdypUserGUID = vdypUserGUID;
	}

	public void setOidcGUID(String oidcGUID) {
		this.oidcGUID = oidcGUID;
	}

	public void setUserTypeCode(UserTypeCodeEntity userTypeCode) {
		this.userTypeCode = userTypeCode;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
