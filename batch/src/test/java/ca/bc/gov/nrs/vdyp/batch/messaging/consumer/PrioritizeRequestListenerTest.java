package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsPrioritizeProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeRequestMessage;
import ca.bc.gov.nrs.vdyp.batch.ownership.JobOwnershipService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchPrioritizationService.PrioritizeOutcome;
import ca.bc.gov.nrs.vdyp.batch.service.JobExecutionLookupService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;
import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrioritizeRequestListenerTest {

	private static final NatsPrioritizeProperties PROPERTIES = new NatsPrioritizeProperties(
			"vdyp.batch.prioritize.request", Duration.ofSeconds(3)
	);

	@Mock
	private Connection natsConnection;

	@Mock
	private JobExecutionLookupService lookupService;

	@Mock
	private JobOwnershipService ownershipService;

	@Mock
	private BatchPrioritizationService prioritizationService;

	@Mock
	private Message message;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private PrioritizeRequestListener listener;

	@BeforeEach
	void setUp() {
		listener = new PrioritizeRequestListener(
				natsConnection, PROPERTIES, objectMapper, lookupService, ownershipService, prioritizationService
		);
		when(message.getReplyTo()).thenReturn("_INBOX.reply-subject");
	}

	@Test
	void testHandleMessage_NotOwnedLocally_DoesNotReply() throws Exception {
		mockRequest("job-guid", "projection-guid");
		when(ownershipService.isOwnedLocally("projection-guid")).thenReturn(false);

		listener.handleMessage(message);

		verify(natsConnection, never()).publish(anyString(), any());
	}

	@Test
	void testHandleMessage_TargetNotFound_DoesNotReply() throws Exception {
		mockRequest("job-guid", "projection-guid");
		when(ownershipService.isOwnedLocally("projection-guid")).thenReturn(true);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, "job-guid", true))
				.thenThrow(new NoSuchJobExecutionException("not found"));

		listener.handleMessage(message);

		verify(natsConnection, never()).publish(anyString(), any());
	}

	@Test
	void testHandleMessage_TargetNotRunning_DoesNotReply() throws Exception {
		mockRequest("job-guid", "projection-guid");
		when(ownershipService.isOwnedLocally("projection-guid")).thenReturn(true);
		JobExecution execution = jobExecution(BatchStatus.COMPLETED);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, "job-guid", true))
				.thenReturn(execution);

		listener.handleMessage(message);

		verify(natsConnection, never()).publish(anyString(), any());
	}

	@Test
	void testHandleMessage_OwnedAndRunning_PrioritizesAndReplies() throws Exception {
		mockRequest("job-guid", "projection-guid");
		when(ownershipService.isOwnedLocally("projection-guid")).thenReturn(true);
		JobExecution execution = jobExecution(BatchStatus.STARTED);
		when(lookupService.findJobExecutionByJobParameter(BatchConstants.Job.GUID, "job-guid", true))
				.thenReturn(execution);
		when(prioritizationService.prioritizeLocally("job-guid", execution))
				.thenReturn(new PrioritizeOutcome("PRIORITIZE_REQUESTED", "paused 1", 1, execution.getId()));

		listener.handleMessage(message);

		ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(natsConnection)
				.publish(org.mockito.ArgumentMatchers.eq("_INBOX.reply-subject"), payloadCaptor.capture());
		PrioritizeReplyMessage reply = objectMapper
				.readValue(new String(payloadCaptor.getValue(), StandardCharsets.UTF_8), PrioritizeReplyMessage.class);
		assertEquals(true, reply.success());
		assertEquals("PRIORITIZE_REQUESTED", reply.status());
		assertEquals(1, reply.othersPausedCount());
	}

	@Test
	void testHandleMessage_NoReplyToSubject_DoesNothing() {
		when(message.getReplyTo()).thenReturn(null);

		listener.handleMessage(message);

		verify(ownershipService, never()).isOwnedLocally(anyString());
	}

	private void mockRequest(String jobGuid, String projectionGuid) throws Exception {
		PrioritizeRequestMessage request = new PrioritizeRequestMessage(jobGuid, projectionGuid);
		when(message.getData()).thenReturn(objectMapper.writeValueAsBytes(request));
	}

	private JobExecution jobExecution(BatchStatus status) {
		JobInstance instance = new JobInstance(1L, "testJob");
		JobParameters params = new JobParametersBuilder().addString(BatchConstants.Job.GUID, "job-guid")
				.addString(BatchConstants.GuidInput.PROJECTION_GUID, "projection-guid").toJobParameters();
		JobExecution execution = new JobExecution(instance, 100L, params);
		execution.setStatus(status);
		return execution;
	}
}
