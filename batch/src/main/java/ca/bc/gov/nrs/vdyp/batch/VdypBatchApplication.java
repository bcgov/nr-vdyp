package ca.bc.gov.nrs.vdyp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.controller.BatchJobRequest;
import ca.bc.gov.nrs.vdyp.batch.model.BatchMetrics;

@SpringBootApplication
@RegisterReflectionForBinding({ BatchRecord.class, BatchJobRequest.class, BatchMetrics.class })
public class VdypBatchApplication {
    private static final Logger logger = LoggerFactory.getLogger(VdypBatchApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(VdypBatchApplication.class, args);
        String separator = "============================================================";
        logger.info(separator);
        logger.info("VDYP Batch Processing Service Started!");
        logger.info("API Endpoints:");
        logger.info("  POST   /api/batch/start           - Start batch job");
        logger.info("  GET    /api/batch/status/{id}     - Check job status");
        logger.info("  GET    /api/batch/jobs            - List recent jobs");
        logger.info("  GET    /api/batch/metrics/{id}    - Get detailed job metrics");
        logger.info("  GET    /api/batch/statistics      - Get batch statistics");
        logger.info("  GET    /api/batch/health          - Health check");
        logger.info(separator);
    }
}