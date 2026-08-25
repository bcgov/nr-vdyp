package ca.bc.gov.nrs.vdyp.batch.ownership;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchWorkerIdentity {

	private final String ownerId;

	public BatchWorkerIdentity(
			@Value("${KUBERNETES_POD_UID:}") String podUid,
			@Value("${HOSTNAME:${COMPUTERNAME:unknown-host}}") String hostName
	) {
		String processUuid = UUID.randomUUID().toString();
		String immutablePodPart = podUid == null || podUid.isBlank() ? "no-pod-uid" : podUid;
		this.ownerId = immutablePodPart + "/" + hostName + "/" + ManagementFactory.getRuntimeMXBean().getName() + "/"
				+ processUuid;
	}

	public String ownerId() {
		return ownerId;
	}
}
