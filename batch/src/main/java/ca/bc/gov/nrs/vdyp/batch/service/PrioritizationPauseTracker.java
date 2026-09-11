package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Tracks jobs stopped to pause them for prioritization, so their interim directories aren't deleted like a cancelled job's.
@Component
public class PrioritizationPauseTracker {

	private final Set<Long> pausedForResume = ConcurrentHashMap.newKeySet();

	public void markPausedForResume(Long jobExecutionId) {
		pausedForResume.add(jobExecutionId);
	}

	public void unmark(Long jobExecutionId) {
		pausedForResume.remove(jobExecutionId);
	}

	public boolean isPausedForResume(Long jobExecutionId) {
		return pausedForResume.contains(jobExecutionId);
	}
}
