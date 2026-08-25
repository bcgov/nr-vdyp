package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.time.Instant;
import java.util.UUID;

public record JobClaim(
		String projectionGuid, String ownerId, UUID leaseToken, Instant acquiredTime, Instant leaseExpiryTime
) {
}
