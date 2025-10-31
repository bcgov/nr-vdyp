package ca.bc.gov.nrs.vdyp.backend.data;

import java.time.LocalDate;

public interface Auditable {
	void incrementRevisionCount();

	void setCreatedBy(String v);

	void setCreatedDate(LocalDate v);

	void setLastModifiedBy(String v);

	void setLastModifiedDate(LocalDate v);
}
