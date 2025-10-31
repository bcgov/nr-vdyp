package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
@Table(name = "projection")
@Getter
@Setter
public class ProjectionEntity extends PanacheEntityBase implements Auditable {
	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "projection_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionGUID;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "vdyp_user_guid", referencedColumnName = "vdyp_user_guid")
	private VDYPUserEntity ownerUser;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "POLYGON_projection_file_set_guid", referencedColumnName = "projection_file_set_guid")
	private ProjectionFileSetEntity polygonFileSet;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "LAYER_projection_file_set_guid", referencedColumnName = "projection_file_set_guid")
	private ProjectionFileSetEntity layerFileSet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RESULT_projection_file_set_guid", referencedColumnName = "projection_file_set_guid")
	private ProjectionFileSetEntity resultFileSet;

	@Column(name = "projection_parameters_json", length = 4000)
	private String projectionParameters;
	@Column(name = "start_date")
	private OffsetDateTime startDate;
	@Column(name = "end_date")
	private OffsetDateTime endDate;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "calculation_engine_code", referencedColumnName = "calculation_engine_code")
	private CalculationEngineCodeEntity calculationEngineCode;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "projection_status_code", referencedColumnName = "projection_status_code")
	private ProjectionStatusCodeEntity projectionStatusCode;

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
