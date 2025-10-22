package ca.bc.gov.nrs.vdyp.backend.data;

import java.util.Date;

import jakarta.inject.Singleton;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Singleton
public class AuditListener {

	@PrePersist
	public void beforeInsert(Object entity) {
		if (entity instanceof Auditable auditable) {
			auditable.setCreatedDate(new Date());
			auditable.setCreatedBy(currentUser());
		}
	}

	@PreUpdate
	public void beforeUpdate(Object entity) {
		if (entity instanceof Auditable auditable) {
			auditable.setLastModifiedDate(new Date());
			auditable.setLastModifiedBy(currentUser());
			auditable.incrementRevisionCount();
		}
	}

	private String currentUser() {
		return "system";
	}
}
