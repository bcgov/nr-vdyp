package ca.bc.gov.nrs.vdyp.batch.ownership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;

class OwnedJobRegistryTest {

	private OwnedJobRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new OwnedJobRegistry();
	}

	@Test
	void removeIfCurrentRemovesAMatchingEntry() {
		JobClaim claim = claim("projection-1");
		registry.register(new OwnedJob(claim));

		var removed = registry.removeIfCurrent("projection-1", claim.leaseToken());

		assertTrue(removed.isPresent());
		assertEquals(claim, removed.get().claim());
		assertFalse(registry.findByProjectionGuid("projection-1").isPresent());
	}

	@Test
	void removeIfCurrentLeavesANewerEntryUntouchedWhenTheLeaseTokenNoLongerMatches() {
		JobClaim oldClaim = claim("projection-1");
		registry.register(new OwnedJob(oldClaim));

		JobClaim newClaim = claim("projection-1");
		registry.register(new OwnedJob(newClaim));

		var removed = registry.removeIfCurrent("projection-1", oldClaim.leaseToken());

		assertTrue(removed.isEmpty());
		assertEquals(newClaim, registry.findByProjectionGuid("projection-1").orElseThrow().claim());
	}

	@Test
	void removeIfCurrentIsANoOpWhenNothingIsRegistered() {
		var removed = registry.removeIfCurrent("projection-1", UUID.randomUUID());

		assertTrue(removed.isEmpty());
	}

	private JobClaim claim(String projectionGuid) {
		Instant now = Instant.now();
		return new JobClaim(projectionGuid, "owner", UUID.randomUUID(), now, now.plusSeconds(120));
	}
}
