package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.util.concurrent.atomic.AtomicBoolean;

public class OwnedJob {

	private final JobClaim claim;
	private final AtomicBoolean leaseLost = new AtomicBoolean(false);

	OwnedJob(JobClaim claim) {
		this.claim = claim;
	}

	public JobClaim claim() {
		return claim;
	}

	public boolean markLeaseLost() {
		return leaseLost.compareAndSet(false, true);
	}

	public boolean leaseLost() {
		return leaseLost.get();
	}

}
