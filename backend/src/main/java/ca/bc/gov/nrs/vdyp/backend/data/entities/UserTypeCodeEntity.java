package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_type_code")
@Getter
@Setter
public class UserTypeCodeEntity extends AuditableEntity {
	@Id
	@Column(name = "user_type_code", length = 10, nullable = false)
	private String userTypeCode;

	@Column(name = "description", length = 100, nullable = false)
	private String description;

	@Column(name = "display_order", length = 3)
	private BigDecimal displayOrder;

	@NotNull
	@Column(name = "effective_date")
	private LocalDate effectiveDate;

	@NotNull
	@Column(name = "expiry_date")
	private LocalDate expiryDate;

}
