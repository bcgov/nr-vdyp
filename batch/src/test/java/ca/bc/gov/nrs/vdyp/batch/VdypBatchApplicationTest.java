package ca.bc.gov.nrs.vdyp.batch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.event.EventListener;

@ExtendWith(MockitoExtension.class)
class VdypBatchApplicationTest {

	@Test
	void testMain_StartsSpringApplication() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			String[] args = { "--spring.main.web-application-type=none" };

			VdypBatchApplication.main(args);

			springApplicationMock.verify(() -> SpringApplication.run(VdypBatchApplication.class, args));
		}
	}

	@Test
	void testMain_WithEmptyArgs_StartsApplication() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			String[] emptyArgs = {};

			VdypBatchApplication.main(emptyArgs);

			springApplicationMock.verify(() -> SpringApplication.run(VdypBatchApplication.class, emptyArgs));
		}
	}

	@Test
	void testMain_WithMultipleArgs_StartsApplication() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			String[] multipleArgs = { "--server.port=8081", "--spring.profiles.active=test" };

			VdypBatchApplication.main(multipleArgs);

			springApplicationMock.verify(() -> SpringApplication.run(VdypBatchApplication.class, multipleArgs));
		}
	}

	@Test
	void testMain_SetsCustomClassLoader() {
		try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
			VdypBatchApplication.main(new String[] {});

			ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
			assertNotNull(currentClassLoader);

			assertNotNull(currentClassLoader.getClass());
		}
	}

	@Test
	void testOverrideVdypProperties_CustomClassLoaderBehavior() {
		VdypBatchApplication.main(new String[] {});

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assertNotNull(classLoader);

		assertDoesNotThrow(() -> {
			classLoader.getResourceAsStream("application.properties");
		});
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void testOnApplicationReady_LogsStartupMessage(CapturedOutput output) {
		VdypBatchApplication application = new VdypBatchApplication();

		Logger logger = (Logger) LoggerFactory.getLogger(VdypBatchApplication.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		application.onApplicationReady();

		assertTrue(listAppender.list.size() > 0, "Logger should have captured messages");

		boolean foundStartupMessage = false;
		boolean foundEndpointMessage = false;
		boolean foundSeparator = false;

		for (ILoggingEvent event : listAppender.list) {
			String message = event.getMessage();
			if (message.contains("VDYP Batch Processing Service Started!")) {
				foundStartupMessage = true;
			}
			if (message.contains("POST   /api/batch/start")) {
				foundEndpointMessage = true;
			}
			if (message.contains("============================================================")) {
				foundSeparator = true;
			}
		}

		assertTrue(foundStartupMessage, "Should log startup message");
		assertTrue(foundEndpointMessage, "Should log endpoint information");
		assertTrue(foundSeparator, "Should log separator line");

		logger.detachAppender(listAppender);
	}

	@Test
	void testOnApplicationReady_LogsAllEndpoints() {
		VdypBatchApplication application = new VdypBatchApplication();

		Logger logger = (Logger) LoggerFactory.getLogger(VdypBatchApplication.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		application.onApplicationReady();

		StringBuilder allMessages = new StringBuilder();
		for (ILoggingEvent event : listAppender.list) {
			allMessages.append(event.getMessage()).append("\n");
		}

		String messages = allMessages.toString();

		assertTrue(messages.contains("/api/batch/start"), "Should log start endpoint");
		assertTrue(messages.contains("/api/batch/health"), "Should log health endpoint");
		assertTrue(messages.contains("API Endpoints:"), "Should log endpoints header");

		logger.detachAppender(listAppender);
	}

	@Test
	void testOnApplicationReady_LogsAtInfoLevel() {
		VdypBatchApplication application = new VdypBatchApplication();

		Logger logger = (Logger) LoggerFactory.getLogger(VdypBatchApplication.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		application.onApplicationReady();

		for (ILoggingEvent event : listAppender.list) {
			assertEquals(Level.INFO, event.getLevel(), "All startup messages should be logged at INFO level");
		}

		logger.detachAppender(listAppender);
	}

	@Test
	void testOnApplicationReady_CanBeCalledMultipleTimes() {
		VdypBatchApplication application = new VdypBatchApplication();

		assertDoesNotThrow(() -> {
			application.onApplicationReady();
			application.onApplicationReady();
			application.onApplicationReady();
		});
	}

	@Test
	void testApplicationAnnotations() {
		assertTrue(
				VdypBatchApplication.class.isAnnotationPresent(SpringBootApplication.class),
				"Should have @SpringBootApplication annotation"
		);

		assertTrue(
				VdypBatchApplication.class.isAnnotationPresent(RegisterReflectionForBinding.class),
				"Should have @RegisterReflectionForBinding annotation"
		);
	}

	@Test
	void testEventListenerAnnotation() throws NoSuchMethodException {
		var method = VdypBatchApplication.class.getMethod("onApplicationReady");
		assertTrue(
				method.isAnnotationPresent(EventListener.class),
				"onApplicationReady should have @EventListener annotation"
		);

		EventListener annotation = method.getAnnotation(EventListener.class);
		assertNotNull(annotation, "EventListener annotation should not be null");
	}
}
