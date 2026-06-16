package ca.bc.gov.nrs.vdyp.backend.messaging.publisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.messaging.message.BatchRequestMessage;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Options;
import io.nats.client.api.MessageInfo;

@ExtendWith(MockitoExtension.class)
class BatchJobPublisherTest {

	private static final String URL = "nats://localhost:4222";
	private static final String SUBJECT = "vdyp.batch.request.standard";
	private static final String STREAM = "VDYP_BATCH_REQUESTS";

	@Mock
	BatchJobPublisher.NatsConnectionFactory connectionFactory;
	@Mock
	Connection connection;
	@Mock
	JetStreamManagement management;
	@Mock
	MessageInfo firstMessage;
	@Mock
	MessageInfo matchingMessage;

	ObjectMapper objectMapper;
	BatchJobPublisher publisher;

	@BeforeEach
	void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		publisher = new BatchJobPublisher(
				objectMapper, URL, Optional.empty(), Optional.empty(), SUBJECT, STREAM, connectionFactory
		);
		when(connectionFactory.connect(any(Options.class))).thenReturn(connection);
		when(connection.jetStreamManagement()).thenReturn(management);
	}

	@Test
	void deleteQueuedRequest_deletesMatchingProjectionMessage() throws Exception {
		UUID projectionID = UUID.randomUUID();
		UUID otherProjectionID = UUID.randomUUID();

		when(firstMessage.isMessage()).thenReturn(true);
		when(firstMessage.getSeq()).thenReturn(10L);
		when(firstMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(otherProjectionID, "{}")));
		when(matchingMessage.isMessage()).thenReturn(true);
		when(matchingMessage.getSeq()).thenReturn(11L);
		when(matchingMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(projectionID, "{}")));

		when(management.getFirstMessage(STREAM, SUBJECT)).thenReturn(firstMessage);
		when(management.getNextMessage(STREAM, 10L, SUBJECT)).thenReturn(matchingMessage);
		when(management.deleteMessage(STREAM, 11L)).thenReturn(true);

		assertTrue(publisher.deleteQueuedRequest(projectionID));

		verify(management, never()).deleteMessage(STREAM, 10L);
		verify(management, times(1)).deleteMessage(STREAM, 11L);
	}

	@Test
	void deleteQueuedRequest_returnsFalse_whenNoQueuedMessageExists() throws Exception {
		UUID projectionID = UUID.randomUUID();
		JetStreamApiException notFound = org.mockito.Mockito.mock(JetStreamApiException.class);

		when(notFound.getErrorCode()).thenReturn(404);
		when(management.getFirstMessage(STREAM, SUBJECT)).thenThrow(notFound);

		assertFalse(publisher.deleteQueuedRequest(projectionID));

		verify(management, never()).deleteMessage(any(), org.mockito.Mockito.anyLong());
	}
}
