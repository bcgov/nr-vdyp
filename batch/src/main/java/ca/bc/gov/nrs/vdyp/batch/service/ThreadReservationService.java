package ca.bc.gov.nrs.vdyp.batch.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * Tracks total thread demand across all live jobs on this instance, used only to decide whether a new job can be
 * admitted. It never caps a job's own thread need, since doing so would permanently limit that job's thread usage.
 */
@Service
public class ThreadReservationService {

	private final ServerCapacityService serverCapacityService;
	private final AtomicInteger reservedThreads = new AtomicInteger(0);

	public ThreadReservationService(ServerCapacityService serverCapacityService) {
		this.serverCapacityService = serverCapacityService;
	}

	public int reservedThreads() {
		return reservedThreads.get();
	}

	public int availableThreads() {
		return Math.max(0, serverCapacityService.maximumThreads() - reservedThreads.get());
	}

	/**
	 * Atomically records a job's full thread demand in the ledger, uncapped by current pool availability. Returns
	 * the same amount for symmetry with release(int).
	 */
	public int reserve(int requestedThreads) {
		int wanted = Math.max(1, requestedThreads);
		reservedThreads.addAndGet(wanted);
		return wanted;
	}

	public void release(int threads) {
		if (threads <= 0) {
			return;
		}
		reservedThreads.updateAndGet(current -> Math.max(0, current - threads));
	}
}
