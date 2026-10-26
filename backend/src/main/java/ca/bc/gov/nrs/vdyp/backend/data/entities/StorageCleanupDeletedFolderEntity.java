package ca.bc.gov.nrs.vdyp.backend.data.entities;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;

/** Records one PVC job folder deleted by a StorageCleanupRunEntity. */
@Entity
@Table(name = "storage_cleanup_deleted_folder")
public class StorageCleanupDeletedFolderEntity extends AuditableEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(
			name = "storage_cleanup_deleted_folder_guid", nullable = false, updatable = false, columnDefinition = "uuid"
	)
	private UUID storageCleanupDeletedFolderGUID;

	@ManyToOne
	@JoinColumn(name = "storage_cleanup_run_guid", referencedColumnName = "storage_cleanup_run_guid", nullable = false)
	private StorageCleanupRunEntity run;

	@Column(name = "folder_name", nullable = false)
	private String folderName;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	public UUID getStorageCleanupDeletedFolderGUID() {
		return storageCleanupDeletedFolderGUID;
	}

	public StorageCleanupRunEntity getRun() {
		return run;
	}

	public void setRun(StorageCleanupRunEntity run) {
		this.run = run;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

}
