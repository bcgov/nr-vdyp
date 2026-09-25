package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;

/** Records one admin-triggered PVC storage cleanup delete run: who ran it, when, and the outcome counts. */
@Entity
@Table(name = "storage_cleanup_run")
public class StorageCleanupRunEntity extends AuditableEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "storage_cleanup_run_guid", nullable = false, updatable = false, columnDefinition = "uuid")
	private UUID storageCleanupRunGUID;

	@ManyToOne
	@JoinColumn(name = "vdyp_user_guid", referencedColumnName = "vdyp_user_guid", nullable = false)
	private VDYPUserEntity runByUser;

	@Column(name = "run_date", nullable = false)
	private OffsetDateTime runDate;

	@Column(name = "deleted_count", nullable = false)
	private int deletedCount;

	@Column(name = "failed_count", nullable = false)
	private int failedCount;

	@Column(name = "protected_count", nullable = false)
	private int protectedCount;

	@Column(name = "bytes_freed", nullable = false)
	private long bytesFreed;

	public UUID getStorageCleanupRunGUID() {
		return storageCleanupRunGUID;
	}

	public VDYPUserEntity getRunByUser() {
		return runByUser;
	}

	public void setRunByUser(VDYPUserEntity runByUser) {
		this.runByUser = runByUser;
	}

	public OffsetDateTime getRunDate() {
		return runDate;
	}

	public void setRunDate(OffsetDateTime runDate) {
		this.runDate = runDate;
	}

	public int getDeletedCount() {
		return deletedCount;
	}

	public void setDeletedCount(int deletedCount) {
		this.deletedCount = deletedCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(int failedCount) {
		this.failedCount = failedCount;
	}

	public int getProtectedCount() {
		return protectedCount;
	}

	public void setProtectedCount(int protectedCount) {
		this.protectedCount = protectedCount;
	}

	public long getBytesFreed() {
		return bytesFreed;
	}

	public void setBytesFreed(long bytesFreed) {
		this.bytesFreed = bytesFreed;
	}

}
