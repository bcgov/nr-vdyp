package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for the scheduled removal of stale entries from the batch worker registry. The schedule itself is set by
 * {@code batch.worker-registry-cleanup.cron}, which is read directly by the scheduler.
 */
@Component
@ConfigurationProperties(prefix = "batch.worker-registry-cleanup")
public class WorkerRegistryCleanupProperties {

	private boolean enabled = true;
	private Duration retention = Duration.ofDays(1);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getRetention() {
		return retention;
	}

	public void setRetention(Duration retention) {
		this.retention = retention;
	}
}
