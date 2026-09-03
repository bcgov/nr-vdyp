package ca.bc.gov.nrs.vdyp.batch.messaging.consumer;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

/**
 * Shared NATS core pub/sub lifecycle for "does this instance own X?" broadcast listeners: subscribes to a subject on
 * start, dispatches each message to handleMessage(Message), and cleans up its dispatcher on stop.
 */
public abstract class AbstractNatsBroadcastListener implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNatsBroadcastListener.class);

	protected final Connection natsConnection;
	protected final ObjectMapper objectMapper;
	private final String subject;

	private final AtomicReference<Dispatcher> dispatcher = new AtomicReference<>();

	protected AbstractNatsBroadcastListener(Connection natsConnection, ObjectMapper objectMapper, String subject) {
		this.natsConnection = natsConnection;
		this.objectMapper = objectMapper;
		this.subject = subject;
	}

	@Override
	public void start() {
		Dispatcher newDispatcher = natsConnection.createDispatcher(this::handleMessage);
		newDispatcher.subscribe(subject);
		dispatcher.set(newDispatcher);
	}

	abstract void handleMessage(Message message);

	protected void reply(String replyTo, Object payload) {
		try {
			natsConnection.publish(replyTo, objectMapper.writeValueAsBytes(payload));
		} catch (Exception e) {
			logger.warn("Failed to publish reply: {}", e.getMessage(), e);
		}
	}

	@Override
	public void stop() {
		Dispatcher currentDispatcher = dispatcher.getAndSet(null);
		if (currentDispatcher != null) {
			natsConnection.closeDispatcher(currentDispatcher);
		}
	}

	@Override
	public boolean isRunning() {
		return dispatcher.get() != null;
	}
}
