package ca.bc.gov.nrs.vdyp.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

class RequestDebugLogTest {

	@TempDir
	Path tempDir;

	@Test
	void capturedLogRemainsReadableAfterProjectionFailure() throws Exception {
		Path output = tempDir.resolve("execution/DebugLog.txt");
		var logger = LoggerFactory.getLogger("ca.bc.gov.nrs.vdyp.ecore.retained-debug-test");
		assertThrows(IllegalStateException.class, () -> {
			try (RequestDebugLog log = new RequestDebugLog(output)) {
				logger.debug("retained-projection-message");
				throw new IllegalStateException("projection failure");
			}
		});
		assertTrue(Files.readString(output).contains("retained-projection-message"));
		assertNull(MDC.get(RequestDebugLoggingFilter.MDC_KEY));
		logger.info("outside-projection-message");
		assertFalse(Files.readString(output).contains("outside-projection-message"));
		assertFalse(Files.exists(tempDir.resolve("execution/not-request-specific.log")));
	}

	@Test
	void disabledLoggingDoesNotCreateLog() throws Exception {
		try (RequestDebugLog log = new RequestDebugLog(null)) {
			LoggerFactory.getLogger("ca.bc.gov.nrs.vdyp.ecore.disabled-retained-test").debug("disabled-message");
		}
		try (var files = Files.list(tempDir)) {
			assertEquals(0, files.count());
		}
		assertNull(MDC.get(RequestDebugLoggingFilter.MDC_KEY));
	}

	@Test
	void disabledScopeCreatesNoFileAndRestoresEnclosingRequest() throws Exception {
		var logger = LoggerFactory.getLogger("ca.bc.gov.nrs.vdyp.ecore.nested-debug-test");
		Path path = tempDir.resolve("DebugLog.txt");
		try (RequestDebugLog outer = new RequestDebugLog(path)) {
			String outerId = MDC.get(RequestDebugLoggingFilter.MDC_KEY);
			try (RequestDebugLog disabled = new RequestDebugLog(null)) {
				assertFalse(logger.isDebugEnabled());
				logger.debug("disabled-nested-message");
			}
			assertEquals(outerId, MDC.get(RequestDebugLoggingFilter.MDC_KEY));
			logger.debug("outer-message");
		}
		assertTrue(Files.readString(path).contains("outer-message"));
		assertFalse(Files.readString(path).contains("disabled-nested-message"));
		try (var files = Files.list(tempDir)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	void capturesOtherEnginePackagesAndRestoresMdcOnFailure() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger("ca.bc.gov.nrs.vdyp.forward.debug-test");
		Level previousLevel = logger.getLevel();
		logger.setLevel(Level.INFO);
		Path path = tempDir.resolve("DebugLog.txt");
		try {
			assertFalse(logger.isDebugEnabled());
			assertThrows(IllegalStateException.class, () -> {
				try (RequestDebugLog log = new RequestDebugLog(path)) {
					assertTrue(logger.isDebugEnabled());
					logger.debug("requested-debug-message");
					throw new IllegalStateException("projection failure");
				}
			});
			assertNull(MDC.get(RequestDebugLoggingFilter.MDC_KEY));
			assertFalse(logger.isDebugEnabled());
			assertTrue(Files.readString(path).contains("requested-debug-message"));
			logger.debug("after-request");
			assertFalse(Files.readString(path).contains("after-request"));
		} finally {
			logger.setLevel(previousLevel);
		}
	}

	@Test
	void concurrentRequestsDoNotShareDebugEvents() throws Exception {
		var executor = Executors.newFixedThreadPool(3);
		var ready = new CountDownLatch(3);
		Logger logger = (Logger) LoggerFactory.getLogger("ca.bc.gov.nrs.vdyp.ecore.debug-test");
		try {
			var first = executor.submit(() -> writeConcurrentLog(logger, "first", ready));
			var second = executor.submit(() -> writeConcurrentLog(logger, "second", ready));
			var disabled = executor.submit(() -> {
				ready.countDown();
				assertTrue(ready.await(10, TimeUnit.SECONDS));
				assertFalse(logger.isDebugEnabled());
				logger.debug("disabled-message");
				return null;
			});
			first.get(15, TimeUnit.SECONDS);
			second.get(15, TimeUnit.SECONDS);
			disabled.get(15, TimeUnit.SECONDS);
			String firstLog = Files.readString(tempDir.resolve("first.txt"));
			String secondLog = Files.readString(tempDir.resolve("second.txt"));
			assertTrue(firstLog.contains("first-message"));
			assertFalse(firstLog.contains("second-message"));
			assertFalse(firstLog.contains("disabled-message"));
			assertTrue(secondLog.contains("second-message"));
			assertFalse(secondLog.contains("first-message"));
			assertFalse(secondLog.contains("disabled-message"));
		} finally {
			executor.shutdownNow();
		}
	}

	private Void writeConcurrentLog(Logger logger, String name, CountDownLatch ready) throws Exception {
		try (RequestDebugLog log = new RequestDebugLog(tempDir.resolve(name + ".txt"))) {
			ready.countDown();
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			logger.debug("{}-message", name);
		}
		assertNull(MDC.get(RequestDebugLoggingFilter.MDC_KEY));
		return null;
	}
}
