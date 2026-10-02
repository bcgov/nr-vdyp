package ca.bc.gov.nrs.vdyp.backend.data.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AuditableEntityTest {
	@Test
	void testAuditColumns() {
		OffsetDateTime beforeInsert = OffsetDateTime.now();
		ProjectionEntity entity = new ProjectionEntity();
		entity.beforeInsert();
		OffsetDateTime afterInsert = OffsetDateTime.now();
		assertNotNull(entity.getCreateDate());
		assertEquals(ZoneOffset.UTC, entity.getCreateDate().getOffset());
		assertFalse(entity.getCreateDate().isBefore(beforeInsert));
		assertFalse(entity.getCreateDate().isAfter(afterInsert));
		assertNotNull(entity.getUpdateDate());
		assertEquals(ZoneOffset.UTC, entity.getUpdateDate().getOffset());
		assertFalse(entity.getUpdateDate().isBefore(beforeInsert));
		assertFalse(entity.getUpdateDate().isAfter(afterInsert));
		OffsetDateTime initialCreateDate = entity.getCreateDate();

		OffsetDateTime beforeUpdate = OffsetDateTime.now();
		entity.beforeUpdate();
		assertEquals(ZoneOffset.UTC, entity.getUpdateDate().getOffset());
		OffsetDateTime afterUpdate = OffsetDateTime.now();
		assertEquals(entity.getCreateDate(), initialCreateDate);
		assertFalse(entity.getUpdateDate().isBefore(beforeUpdate));
		assertFalse(entity.getUpdateDate().isAfter(afterUpdate));
	}
}
