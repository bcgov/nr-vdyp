package ca.bc.gov.nrs.vdyp.batch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsPrioritizeProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.PrioritizeReplyMessage;
import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrioritizeRemoteGatewayTest {

	private static final NatsPrioritizeProperties PROPERTIES = new NatsPrioritizeProperties(
			"vdyp.batch.prioritize.request", Duration.ofSeconds(3)
	);

	@Mock
	private Connection natsConnection;

	@Mock
	private Message reply;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private PrioritizeRemoteGateway gateway;

	@BeforeEach
	void setUp() {
		gateway = new PrioritizeRemoteGateway(natsConnection, PROPERTIES, objectMapper);
	}

	@Test
	void testRequestPrioritize_ReplicaResponds_ReturnsReply() throws Exception {
		PrioritizeReplyMessage replyMessage = new PrioritizeReplyMessage(
				true, "PRIORITIZE_REQUESTED", "paused 1", 1, 100L
		);
		when(reply.getData()).thenReturn(objectMapper.writeValueAsBytes(replyMessage));
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(reply);

		Optional<PrioritizeReplyMessage> result = gateway.requestPrioritize("job-guid", "projection-guid");

		assertTrue(result.isPresent());
		assertEquals("PRIORITIZE_REQUESTED", result.get().status());
		assertEquals(1, result.get().othersPausedCount());
	}

	@Test
	void testRequestPrioritize_NoReplicaResponds_ReturnsEmpty() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(null);

		Optional<PrioritizeReplyMessage> result = gateway.requestPrioritize("job-guid", "projection-guid");

		assertTrue(result.isEmpty());
	}

	@Test
	void testRequestPrioritize_NatsThrows_ReturnsEmpty() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class)))
				.thenThrow(new RuntimeException("connection lost"));

		Optional<PrioritizeReplyMessage> result = gateway.requestPrioritize("job-guid", "projection-guid");

		assertTrue(result.isEmpty());
	}

	@Test
	void testRequestPrioritize_SendsExpectedPayload() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(null);

		gateway.requestPrioritize("job-guid", "projection-guid");

		var captor = org.mockito.ArgumentCaptor.forClass(byte[].class);
		org.mockito.Mockito.verify(natsConnection).request(
				org.mockito.ArgumentMatchers.eq(PROPERTIES.subject()), captor.capture(),
				org.mockito.ArgumentMatchers.eq(PROPERTIES.timeout())
		);
		String json = new String(captor.getValue(), StandardCharsets.UTF_8);
		assertTrue(json.contains("job-guid"));
		assertTrue(json.contains("projection-guid"));
	}
}
