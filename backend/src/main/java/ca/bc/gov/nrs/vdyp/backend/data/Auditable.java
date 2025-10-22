package ca.bc.gov.nrs.vdyp.backend.data;

import java.util.Date;

public interface Auditable {
	void incrementRevisionCount();

	void setCreatedBy(String v);

	void setCreatedDate(Date v);

	void setLastModifiedBy(String v);

	void setLastModifiedDate(Date v);
}
