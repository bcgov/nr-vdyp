package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "batch.ownership")
public class BatchOwnershipProperties {

	private boolean enabled = true;
	private Duration heartbeatInterval = Duration.ofSeconds(20);
	private Duration leaseDuration = Duration.ofMinutes(2);
	private Duration recoveryScanInterval = Duration.ofSeconds(30);
	private Duration shutdownWait = Duration.ofMinutes(4);
	private boolean recoverLegacyExecutionsWithoutClaim = false;

	@PostConstruct
	void validate() {
		if (leaseDuration.compareTo(heartbeatInterval.multipliedBy(3)) <= 0) {
			throw new IllegalArgumentException(
					"batch.ownership.lease-duration must be more than three heartbeat intervals"
			);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(Duration heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public Duration getLeaseDuration() {
		return leaseDuration;
	}

	public void setLeaseDuration(Duration leaseDuration) {
		this.leaseDuration = leaseDuration;
	}

	public Duration getRecoveryScanInterval() {
		return recoveryScanInterval;
	}

	public void setRecoveryScanInterval(Duration recoveryScanInterval) {
		this.recoveryScanInterval = recoveryScanInterval;
	}

	public Duration getShutdownWait() {
		return shutdownWait;
	}

	public void setShutdownWait(Duration shutdownWait) {
		this.shutdownWait = shutdownWait;
	}

	public boolean isRecoverLegacyExecutionsWithoutClaim() {
		return recoverLegacyExecutionsWithoutClaim;
	}

	public void setRecoverLegacyExecutionsWithoutClaim(boolean recoverLegacyExecutionsWithoutClaim) {
		this.recoverLegacyExecutionsWithoutClaim = recoverLegacyExecutionsWithoutClaim;
	}
}
