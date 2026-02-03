package ca.bc.gov.nrs.vdyp.backend.data.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

public class AuditableEntityTest {
	@Test
	void testAuditColumns() throws InterruptedException {
		OffsetDateTime beforeInsert = OffsetDateTime.now();
		ProjectionEntity entity = new ProjectionEntity();
		entity.beforeInsert();
		OffsetDateTime afterInsert = OffsetDateTime.now();
		assertNotNull(entity.getCreateDate());
		assertFalse(entity.getCreateDate().isBefore(beforeInsert));
		assertFalse(entity.getCreateDate().isAfter(afterInsert));
		assertNotNull(entity.getUpdateDate());
		assertFalse(entity.getUpdateDate().isBefore(beforeInsert));
		assertFalse(entity.getUpdateDate().isAfter(afterInsert));
		OffsetDateTime initialCreateDate = entity.getCreateDate();

		OffsetDateTime beforeUpdate = OffsetDateTime.now();
		entity.beforeUpdate();
		OffsetDateTime afterUpdate = OffsetDateTime.now();
		assertEquals(entity.getCreateDate(), initialCreateDate);
		assertFalse(entity.getUpdateDate().isBefore(beforeUpdate));
		assertFalse(entity.getUpdateDate().isAfter(afterUpdate));
	}
}
