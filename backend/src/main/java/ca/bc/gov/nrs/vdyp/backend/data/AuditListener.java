package ca.bc.gov.nrs.vdyp.backend.data;

import java.time.LocalDate;

import jakarta.inject.Singleton;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Singleton
public class AuditListener {

	@PrePersist
	public void beforeInsert(Object entity) {
		if (entity instanceof Auditable auditable) {
			auditable.setCreatedDate(LocalDate.now());
			auditable.setCreatedBy(currentUser());
		}
	}

	@PreUpdate
	public void beforeUpdate(Object entity) {
		if (entity instanceof Auditable auditable) {
			auditable.setLastModifiedDate(LocalDate.now());
			auditable.setLastModifiedBy(currentUser());
			auditable.incrementRevisionCount();
		}
	}

	private String currentUser() {
		return "system";
	}
}
