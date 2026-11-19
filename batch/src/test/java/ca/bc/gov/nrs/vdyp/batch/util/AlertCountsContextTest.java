package ca.bc.gov.nrs.vdyp.batch.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.ecore.projection.AlertCounts;

class AlertCountsContextTest {

	@Test
	void read_emptyContext_returnsNone() {
		assertThat(
				AlertCountsContext.read(new ExecutionContext(), BatchConstants.Job.ALERT_COUNTS_PREFIX),
				is(AlertCounts.NONE)
		);
	}

	@Test
	void write_thenRead_roundTripsEachCount() {
		ExecutionContext context = new ExecutionContext();
		AlertCounts counts = new AlertCounts(1, 2, 3, 4, 5, 6);

		AlertCountsContext.write(context, BatchConstants.Job.ALERT_COUNTS_PREFIX, counts);

		assertThat(AlertCountsContext.read(context, BatchConstants.Job.ALERT_COUNTS_PREFIX), is(counts));
	}
}
