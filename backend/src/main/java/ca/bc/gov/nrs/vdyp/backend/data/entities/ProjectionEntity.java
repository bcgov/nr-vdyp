package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.time.OffsetDateTime;
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

@Entity
@Table(name = "projection")
public class ProjectionEntity extends AuditableEntity {
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

	public UUID getProjectionGUID() {
		return projectionGUID;
	}

	public VDYPUserEntity getOwnerUser() {
		return ownerUser;
	}

	public ProjectionFileSetEntity getPolygonFileSet() {
		return polygonFileSet;
	}

	public ProjectionFileSetEntity getLayerFileSet() {
		return layerFileSet;
	}

	public ProjectionFileSetEntity getResultFileSet() {
		return resultFileSet;
	}

	public String getProjectionParameters() {
		return projectionParameters;
	}

	public OffsetDateTime getStartDate() {
		return startDate;
	}

	public OffsetDateTime getEndDate() {
		return endDate;
	}

	public CalculationEngineCodeEntity getCalculationEngineCode() {
		return calculationEngineCode;
	}

	public ProjectionStatusCodeEntity getProjectionStatusCode() {
		return projectionStatusCode;
	}

	public void setProjectionGUID(UUID projectionGUID) {
		this.projectionGUID = projectionGUID;
	}

	public void setOwnerUser(VDYPUserEntity ownerUser) {
		this.ownerUser = ownerUser;
	}

	public void setPolygonFileSet(ProjectionFileSetEntity polygonFileSet) {
		this.polygonFileSet = polygonFileSet;
	}

	public void setLayerFileSet(ProjectionFileSetEntity layerFileSet) {
		this.layerFileSet = layerFileSet;
	}

	public void setResultFileSet(ProjectionFileSetEntity resultFileSet) {
		this.resultFileSet = resultFileSet;
	}

	public void setProjectionParameters(String projectionParameters) {
		this.projectionParameters = projectionParameters;
	}

	public void setStartDate(OffsetDateTime startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(OffsetDateTime endDate) {
		this.endDate = endDate;
	}

	public void setCalculationEngineCode(CalculationEngineCodeEntity calculationEngineCode) {
		this.calculationEngineCode = calculationEngineCode;
	}

	public void setProjectionStatusCode(ProjectionStatusCodeEntity projectionStatusCode) {
		this.projectionStatusCode = projectionStatusCode;
	}
}
