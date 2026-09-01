package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class OwnedJobRegistry {

	private final ConcurrentMap<String, OwnedJob> byProjectionGuid = new ConcurrentHashMap<>();

	public void register(OwnedJob ownedJob) {
		byProjectionGuid.put(ownedJob.claim().projectionGuid(), ownedJob);
	}

	public Optional<OwnedJob> findByProjectionGuid(String projectionGuid) {
		return Optional.ofNullable(byProjectionGuid.get(projectionGuid));
	}

	public Collection<OwnedJob> ownedJobs() {
		return byProjectionGuid.values();
	}

	/**
	 * Removes the entry for projectionGuid only if it's still the one identified by expectedLeaseToken.
	 * A pause/resume replaces the registered entry with one for a new claim before the paused execution's own cleanup
	 * runs; without this check that cleanup would remove the newer entry too, since both are keyed by the same
	 * projection GUID.
	 */
	public Optional<OwnedJob> removeIfCurrent(String projectionGuid, UUID expectedLeaseToken) {
		AtomicReference<OwnedJob> removed = new AtomicReference<>();
		byProjectionGuid.computeIfPresent(projectionGuid, (guid, current) -> {
			if (current.claim().leaseToken().equals(expectedLeaseToken)) {
				removed.set(current);
				return null;
			}
			return current;
		});
		return Optional.ofNullable(removed.get());
	}

	public int size() {
		return byProjectionGuid.size();
	}
}
