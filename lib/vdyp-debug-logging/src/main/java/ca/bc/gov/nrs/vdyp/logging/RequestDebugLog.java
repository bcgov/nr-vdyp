package ca.bc.gov.nrs.vdyp.logging;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/** Owns a synchronous file appender for one run, restoring MDC even when projection fails. */
public final class RequestDebugLog implements AutoCloseable {

	private final String previousId = MDC.get(RequestDebugLoggingFilter.MDC_KEY);
	private final Logger root;
	private final FileAppender<ILoggingEvent> appender;

	public RequestDebugLog(Path path) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		// Quarkus has a separate framework context; install on the actual SLF4J context too.
		synchronized (context) {
			if (context.getTurboFilterList().stream().noneMatch(RequestDebugLoggingFilter.class::isInstance)) {
				RequestDebugLoggingFilter filter = new RequestDebugLoggingFilter();
				filter.setContext(context);
				filter.start();
				context.addTurboFilter(filter);
			}
		}
		if (path == null) {
			root = null;
			appender = null;
			MDC.put(RequestDebugLoggingFilter.MDC_KEY, "disabled");
			return;
		}
		root = context.getLogger(ROOT_LOGGER_NAME);
		String id = UUID.randomUUID().toString();
		appender = createAppender(context, path, id);
		root.addAppender(appender);
		MDC.put(RequestDebugLoggingFilter.MDC_KEY, id);
	}

	private FileAppender<ILoggingEvent> createAppender(LoggerContext context, Path path, String id) {
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d [%thread] %-5level %logger: %msg%n");
		encoder.start();
		FileAppender<ILoggingEvent> debugLogAppender = new FileAppender<>();
		debugLogAppender.setContext(context);
		debugLogAppender.setName("projection-debug-" + id);
		debugLogAppender.setFile(path.toString());
		debugLogAppender.setAppend(false);
		debugLogAppender.setEncoder(encoder);
		debugLogAppender.addFilter(new Filter<ILoggingEvent>() {
			@Override
			public FilterReply decide(ILoggingEvent event) {
				return id.equals(event.getMDCPropertyMap().get(RequestDebugLoggingFilter.MDC_KEY)) ? FilterReply.ACCEPT
						: FilterReply.DENY;
			}
		});
		debugLogAppender.start();
		if (!debugLogAppender.isStarted()) {
			debugLogAppender.stop();
			encoder.stop();
			throw new IllegalStateException("Unable to open projection debug log: " + path);
		}
		return debugLogAppender;
	}

	@Override
	public void close() {
		if (previousId == null) {
			MDC.remove(RequestDebugLoggingFilter.MDC_KEY);
		} else {
			MDC.put(RequestDebugLoggingFilter.MDC_KEY, previousId);
		}
		if (appender != null) {
			root.detachAppender(appender);
			appender.stop();
			appender.getEncoder().stop();
		}
	}
}
