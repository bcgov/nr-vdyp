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

import ca.bc.gov.nrs.vdyp.batch.messaging.NatsStopProperties;
import ca.bc.gov.nrs.vdyp.batch.messaging.message.StopReplyMessage;
import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopRemoteGatewayTest {

	private static final NatsStopProperties PROPERTIES = new NatsStopProperties(
			"vdyp.batch.stop.request", Duration.ofSeconds(3)
	);

	@Mock
	private Connection natsConnection;

	@Mock
	private Message reply;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private StopRemoteGateway gateway;

	@BeforeEach
	void setUp() {
		gateway = new StopRemoteGateway(natsConnection, PROPERTIES, objectMapper);
	}

	@Test
	void testRequestStop_ReplicaResponds_ReturnsReply() throws Exception {
		StopReplyMessage replyMessage = new StopReplyMessage("STOP_REQUESTED", "Stopped.", 100L);
		when(reply.getData()).thenReturn(objectMapper.writeValueAsBytes(replyMessage));
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(reply);

		Optional<StopReplyMessage> result = gateway.requestStop("job-guid", "projection-guid");

		assertTrue(result.isPresent());
		assertEquals("STOP_REQUESTED", result.get().status());
		assertEquals(100L, result.get().executionId());
	}

	@Test
	void testRequestStop_NoReplicaResponds_ReturnsEmpty() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(null);

		Optional<StopReplyMessage> result = gateway.requestStop("job-guid", "projection-guid");

		assertTrue(result.isEmpty());
	}

	@Test
	void testRequestStop_NatsThrows_ReturnsEmpty() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class)))
				.thenThrow(new RuntimeException("connection lost"));

		Optional<StopReplyMessage> result = gateway.requestStop("job-guid", "projection-guid");

		assertTrue(result.isEmpty());
	}

	@Test
	void testRequestStop_SendsExpectedPayload() throws Exception {
		when(natsConnection.request(anyString(), any(byte[].class), any(Duration.class))).thenReturn(null);

		gateway.requestStop("job-guid", "projection-guid");

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
