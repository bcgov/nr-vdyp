package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import ca.bc.gov.nrs.vdyp.ecore.projection.AlertCounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projection_batch_mapping")
public class ProjectionBatchMappingEntity extends AuditableEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "projection_batch_mapping_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID projectionBatchMappingGUID;

	@Column(name = "batch_job_guid", columnDefinition = "uuid")
	private UUID batchJobGUID;

	@OneToOne
	@JoinColumn(name = "projection_guid", referencedColumnName = "projection_guid")
	private ProjectionEntity projection;

	@Column(name = "polygon_count")
	private Integer polygonCount;
	@Column(name = "completed_polygon_count")
	private Integer completedPolygonCount;
	@Column(name = "error_count")
	private Integer errorCount;
	@Column(name = "warning_count")
	private Integer warningCount;
	@Column(name = "error_message_count", nullable = false)
	private int errorMessageCount;
	@Column(name = "error_polygon_count", nullable = false)
	private int errorPolygonCount;
	@Column(name = "warning_message_count", nullable = false)
	private int warningMessageCount;
	@Column(name = "warning_polygon_count", nullable = false)
	private int warningPolygonCount;
	@Column(name = "info_message_count", nullable = false)
	private int infoMessageCount;
	@Column(name = "info_polygon_count", nullable = false)
	private int infoPolygonCount;
	@Column(name = "worker_count")
	private Integer workerCount;
	@Column(name = "is_prioritized", nullable = false)
	private boolean isPrioritized;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "batch_failure_type_code", referencedColumnName = "batch_failure_type_code")
	private BatchFailureTypeCodeEntity batchFailureTypeCode;

	@Column(name = "failure_message")
	private String failureMessage;

	@Column(name = "last_progress_time")
	private OffsetDateTime lastProgressTime;

	public UUID getProjectionBatchMappingGUID() {
		return projectionBatchMappingGUID;
	}

	public UUID getBatchJobGUID() {
		return batchJobGUID;
	}

	public ProjectionEntity getProjection() {
		return projection;
	}

	public Integer getPolygonCount() {
		return polygonCount;
	}

	public Integer getCompletedPolygonCount() {
		return completedPolygonCount;
	}

	public Integer getErrorCount() {
		return errorCount;
	}

	public Integer getWarningCount() {
		return warningCount;
	}

	public Integer getWorkerCount() {
		return workerCount;
	}

	public boolean isPrioritized() {
		return isPrioritized;
	}

	public BatchFailureTypeCodeEntity getBatchFailureTypeCode() {
		return batchFailureTypeCode;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public OffsetDateTime getLastProgressTime() {
		return lastProgressTime;
	}

	public void setProjectionBatchMappingGUID(UUID projectionBatchMappingGUID) {
		this.projectionBatchMappingGUID = projectionBatchMappingGUID;
	}

	public void setBatchJobGUID(UUID batchJobGUID) {
		this.batchJobGUID = batchJobGUID;
	}

	public void setProjection(ProjectionEntity projection) {
		this.projection = projection;
	}

	public void setPolygonCount(Integer polygonCount) {
		this.polygonCount = polygonCount;
	}

	public void setCompletedPolygonCount(Integer completedPolygonCount) {
		this.completedPolygonCount = completedPolygonCount;
	}

	public void setErrorCount(Integer errorCount) {
		this.errorCount = errorCount;
	}

	public void setWarningCount(Integer warningCount) {
		this.warningCount = warningCount;
	}

	public void setWorkerCount(Integer workerCount) {
		this.workerCount = workerCount;
	}

	public void setPrioritized(boolean prioritized) {
		this.isPrioritized = prioritized;
	}

	public void setBatchFailureTypeCode(BatchFailureTypeCodeEntity batchFailureTypeCode) {
		this.batchFailureTypeCode = batchFailureTypeCode;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public void setLastProgressTime(OffsetDateTime lastProgressTime) {
		this.lastProgressTime = lastProgressTime;
	}

	public int getErrorMessageCount() {
		return errorMessageCount;
	}

	public void setErrorMessageCount(int errorMessageCount) {
		this.errorMessageCount = errorMessageCount;
	}

	public int getErrorPolygonCount() {
		return errorPolygonCount;
	}

	public void setErrorPolygonCount(int errorPolygonCount) {
		this.errorPolygonCount = errorPolygonCount;
	}

	public int getWarningMessageCount() {
		return warningMessageCount;
	}

	public void setWarningMessageCount(int warningMessageCount) {
		this.warningMessageCount = warningMessageCount;
	}

	public int getWarningPolygonCount() {
		return warningPolygonCount;
	}

	public void setWarningPolygonCount(int warningPolygonCount) {
		this.warningPolygonCount = warningPolygonCount;
	}

	public int getInfoMessageCount() {
		return infoMessageCount;
	}

	public void setInfoMessageCount(int infoMessageCount) {
		this.infoMessageCount = infoMessageCount;
	}

	public int getInfoPolygonCount() {
		return infoPolygonCount;
	}

	public void setInfoPolygonCount(int infoPolygonCount) {
		this.infoPolygonCount = infoPolygonCount;
	}

	public void setAlertCounts(AlertCounts alertCounts) {
		this.errorMessageCount = alertCounts.errorMessageCount();
		this.errorPolygonCount = alertCounts.errorPolygonCount();
		this.warningMessageCount = alertCounts.warningMessageCount();
		this.warningPolygonCount = alertCounts.warningPolygonCount();
		this.infoMessageCount = alertCounts.infoMessageCount();
		this.infoPolygonCount = alertCounts.infoPolygonCount();
	}

}
