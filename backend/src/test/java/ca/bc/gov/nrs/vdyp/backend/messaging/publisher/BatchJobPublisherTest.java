package ca.bc.gov.nrs.vdyp.backend.messaging.publisher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.messaging.message.BatchRequestMessage;
import io.nats.client.Connection;
import io.nats.client.JetStream;
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
	JetStream jetStream;
	@Mock
	JetStreamManagement management;
	@Mock
	MessageInfo firstMessage;
	@Mock
	MessageInfo matchingMessage;

	ObjectMapper objectMapper;
	BatchJobPublisher publisher;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		publisher = new BatchJobPublisher(
				objectMapper, URL, Optional.empty(), Optional.empty(), SUBJECT, STREAM, connectionFactory
		);
	}

	@Test
	void publish_publishesSerializedBatchRequestToConfiguredSubject() throws Exception {
		UUID projectionID = UUID.randomUUID();
		BatchRequestMessage message = new BatchRequestMessage(projectionID, "{\"foo\":\"bar\"}");

		givenConnectedToJetStream();

		publisher.publish(message);

		ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
		verify(jetStream).publish(eq(SUBJECT), payloadCaptor.capture());
		assertArrayEquals(objectMapper.writeValueAsBytes(message), payloadCaptor.getValue());
		verify(connection).close();
	}

	@Test
	void publish_wrapsIOException() throws Exception {
		IOException cause = new IOException("cannot connect");
		when(connectionFactory.connect(any(Options.class))).thenThrow(cause);

		var request = new BatchRequestMessage(UUID.randomUUID(), "{}");
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> publisher.publish(request));

		assertEquals("Unable to publish NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void publish_wrapsInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
		InterruptedException cause = new InterruptedException("interrupted");
		when(connectionFactory.connect(any(Options.class))).thenThrow(cause);

		try {
			var request = new BatchRequestMessage(UUID.randomUUID(), "{}");
			IllegalStateException exception = assertThrows(
					IllegalStateException.class, () -> publisher.publish(request)
			);

			assertEquals("Interrupted while publishing NATS batch request", exception.getMessage());
			assertSame(cause, exception.getCause());
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void deleteQueuedRequest_deletesMatchingProjectionMessage() throws Exception {
		UUID projectionID = UUID.randomUUID();
		UUID otherProjectionID = UUID.randomUUID();

		givenConnectedToManagement();
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
		verify(connection).close();
	}

	@Test
	void deleteQueuedRequest_returnsFalse_whenNoQueuedMessageExists() throws Exception {
		UUID projectionID = UUID.randomUUID();
		JetStreamApiException notFound = org.mockito.Mockito.mock(JetStreamApiException.class);

		givenConnectedToManagement();
		when(notFound.getErrorCode()).thenReturn(404);
		when(management.getFirstMessage(STREAM, SUBJECT)).thenThrow(notFound);

		assertFalse(publisher.deleteQueuedRequest(projectionID));

		verify(management, never()).deleteMessage(any(), anyLong());
	}

	@Test
	void deleteQueuedRequest_returnsFalse_whenQueuedMessagesDoNotMatchProjection() throws Exception {
		UUID projectionID = UUID.randomUUID();
		UUID otherProjectionID = UUID.randomUUID();
		JetStreamApiException notFound = org.mockito.Mockito.mock(JetStreamApiException.class);

		givenConnectedToManagement();
		when(firstMessage.isMessage()).thenReturn(true);
		when(firstMessage.getSeq()).thenReturn(10L);
		when(firstMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(otherProjectionID, "{}")));
		when(notFound.getErrorCode()).thenReturn(404);
		when(management.getFirstMessage(STREAM, SUBJECT)).thenReturn(firstMessage);
		when(management.getNextMessage(STREAM, 10L, SUBJECT)).thenThrow(notFound);

		assertFalse(publisher.deleteQueuedRequest(projectionID));

		verify(management, never()).deleteMessage(any(), anyLong());
	}

	@Test
	void deleteQueuedRequest_skipsMalformedMessageAndDeletesLaterMatch() throws Exception {
		UUID projectionID = UUID.randomUUID();

		givenConnectedToManagement();
		when(firstMessage.isMessage()).thenReturn(true);
		when(firstMessage.getSeq()).thenReturn(10L);
		when(firstMessage.getData()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));
		when(matchingMessage.isMessage()).thenReturn(true);
		when(matchingMessage.getSeq()).thenReturn(11L);
		when(matchingMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(projectionID, "{}")));
		when(management.getFirstMessage(STREAM, SUBJECT)).thenReturn(firstMessage);
		when(management.getNextMessage(STREAM, 10L, SUBJECT)).thenReturn(matchingMessage);
		when(management.deleteMessage(STREAM, 11L)).thenReturn(true);

		assertTrue(publisher.deleteQueuedRequest(projectionID));

		verify(management, never()).deleteMessage(STREAM, 10L);
		verify(management).deleteMessage(STREAM, 11L);
	}

	@Test
	void deleteQueuedRequest_returnsDeleteResultForMatchingMessage() throws Exception {
		UUID projectionID = UUID.randomUUID();

		givenConnectedToManagement();
		when(firstMessage.isMessage()).thenReturn(true);
		when(firstMessage.getSeq()).thenReturn(10L);
		when(firstMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(projectionID, "{}")));
		when(management.getFirstMessage(STREAM, SUBJECT)).thenReturn(firstMessage);
		when(management.deleteMessage(STREAM, 10L)).thenReturn(false);

		assertFalse(publisher.deleteQueuedRequest(projectionID));

		verify(management).deleteMessage(STREAM, 10L);
	}

	@Test
	void deleteQueuedRequest_wrapsNonNotFoundJetStreamApiException() throws Exception {
		JetStreamApiException cause = org.mockito.Mockito.mock(JetStreamApiException.class);

		givenConnectedToManagement();
		when(cause.getErrorCode()).thenReturn(500);
		when(management.getFirstMessage(STREAM, SUBJECT)).thenThrow(cause);
		UUID queuedId = UUID.randomUUID();
		IllegalStateException exception = assertThrows(
				IllegalStateException.class, () -> publisher.deleteQueuedRequest(queuedId)
		);

		assertEquals("Unable to delete queued NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void deleteQueuedRequest_wrapsInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
		InterruptedException cause = new InterruptedException("interrupted");
		when(connectionFactory.connect(any(Options.class))).thenThrow(cause);

		try {
			UUID queuedId = UUID.randomUUID();
			IllegalStateException exception = assertThrows(
					IllegalStateException.class, () -> publisher.deleteQueuedRequest(queuedId)
			);

			assertEquals("Interrupted while deleting queued NATS batch request", exception.getMessage());
			assertSame(cause, exception.getCause());
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void deleteQueuedRequest_wrapsIOException() throws Exception {
		IOException cause = new IOException("cannot connect");
		when(connectionFactory.connect(any(Options.class))).thenThrow(cause);

		UUID queuedId = UUID.randomUUID();
		IllegalStateException exception = assertThrows(
				IllegalStateException.class, () -> publisher.deleteQueuedRequest(queuedId)
		);

		assertEquals("Unable to delete queued NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void publish_wrapsJetStreamApiException() throws Exception {
		JetStreamApiException cause = org.mockito.Mockito.mock(JetStreamApiException.class);

		givenConnectedToJetStream();
		when(jetStream.publish(eq(SUBJECT), any(byte[].class))).thenThrow(cause);

		var request = new BatchRequestMessage(UUID.randomUUID(), "{}");
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> publisher.publish(request));

		assertEquals("Unable to publish NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void publish_wrapsIOExceptionFromJetStream() throws Exception {
		IOException cause = new IOException("cannot publish");

		givenConnectedToJetStream();
		when(jetStream.publish(eq(SUBJECT), any(byte[].class))).thenThrow(cause);

		var request = new BatchRequestMessage(UUID.randomUUID(), "{}");
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> publisher.publish(request));

		assertEquals("Unable to publish NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	void deleteQueuedRequest_wrapsIOExceptionFromDelete() throws Exception {
		UUID projectionID = UUID.randomUUID();
		IOException cause = new IOException("cannot delete");

		givenConnectedToManagement();
		when(firstMessage.isMessage()).thenReturn(true);
		when(firstMessage.getSeq()).thenReturn(10L);
		when(firstMessage.getData())
				.thenReturn(objectMapper.writeValueAsBytes(new BatchRequestMessage(projectionID, "{}")));
		when(management.getFirstMessage(STREAM, SUBJECT)).thenReturn(firstMessage);
		when(management.deleteMessage(STREAM, 10L)).thenThrow(cause);

		IllegalStateException exception = assertThrows(
				IllegalStateException.class, () -> publisher.deleteQueuedRequest(projectionID)
		);

		assertEquals("Unable to delete queued NATS batch request", exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	private void givenConnectedToJetStream() throws Exception {
		when(connectionFactory.connect(any(Options.class))).thenReturn(connection);
		when(connection.jetStream()).thenReturn(jetStream);
	}

	private void givenConnectedToManagement() throws Exception {
		when(connectionFactory.connect(any(Options.class))).thenReturn(connection);
		when(connection.jetStreamManagement()).thenReturn(management);
	}
}
