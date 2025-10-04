package ca.bc.gov.nrs.vdyp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;

@SpringBootApplication
@RegisterReflectionForBinding({ BatchRecord.class, BatchMetrics.class })
public class VdypBatchApplication {
	private static final Logger logger = LoggerFactory.getLogger(VdypBatchApplication.class);

	public static void main(String[] args) {
		// Override VDYP properties before any VdypComponent initialization
		// This ensures VdypComponent uses correct values regardless of classpath order
		overrideVdypProperties();

		SpringApplication.run(VdypBatchApplication.class, args);
	}

	/**
	 * This method is called once when the application is fully started and ready.
	 * Using ApplicationReadyEvent ensures
	 * the startup message is shown only once, not on every batch job execution.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		String separator = "============================================================";
		logger.info(separator);
		logger.info("VDYP Batch Processing Service Started!");
		logger.info("API Endpoints:");
		logger.info("  POST   /api/batch/start           - Start batch job");
		logger.info("  GET    /api/batch/status/{{id}}     - Check job status");
		logger.info("  GET    /api/batch/jobs            - List recent jobs");
		logger.info("  GET    /api/batch/metrics/{{id}}   - Get detailed job metrics");
		logger.info("  GET    /api/batch/health          - Health check");
		logger.info(separator);
	}

	private static void overrideVdypProperties() {
		// Create a ClassLoader that prioritizes the application.properties
		Thread.currentThread().setContextClassLoader(new ClassLoader(Thread.currentThread().getContextClassLoader()) {
			@Override
			public java.io.InputStream getResourceAsStream(String name) {
				if ("application.properties".equals(name)) {
					// Return the batch module's application.properties first
					java.io.InputStream stream = VdypBatchApplication.class.getClassLoader().getResourceAsStream(name);
					if (stream != null) {
						return stream;
					}
				}
				return super.getResourceAsStream(name);
			}
		});
	}
}