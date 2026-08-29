package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

	public Optional<OwnedJob> removeByProjectionGuid(String projectionGuid) {
		return Optional.ofNullable(byProjectionGuid.remove(projectionGuid));
	}

	public int size() {
		return byProjectionGuid.size();
	}
}
