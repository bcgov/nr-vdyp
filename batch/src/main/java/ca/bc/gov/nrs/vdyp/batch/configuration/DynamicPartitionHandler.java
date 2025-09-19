package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;

import java.util.Collection;

/**
 * FEATURE_ID-based dynamic partition handler for VDYP batch processing.
 *
 * This handler manages the FEATURE_ID-based partitioning strategy, setting up polygon and layer resources for the
 * DynamicPartitioner to ensure complete polygon data integrity across all partitions.
 *
 * This handler manages the parallel execution of VDYP processing partitions, allowing runtime configuration of
 * partition count while maintaining FEATURE_ID-based data integrity.
 */
public class DynamicPartitionHandler implements PartitionHandler {

	private static final Logger logger = LoggerFactory.getLogger(DynamicPartitionHandler.class);

	private final TaskExecutor taskExecutor;
	private final Step workerStep;
	private final DynamicPartitioner dynamicPartitioner;
	private final BatchProperties batchProperties;

	public DynamicPartitionHandler(
			TaskExecutor taskExecutor, Step workerStep, DynamicPartitioner dynamicPartitioner,
			BatchProperties batchProperties
	) {
		this.taskExecutor = taskExecutor;
		this.workerStep = workerStep;
		this.dynamicPartitioner = dynamicPartitioner;
		this.batchProperties = batchProperties;
	}

	@Override
	@NonNull
	public Collection<StepExecution>
			handle(@NonNull StepExecutionSplitter stepSplitter, @NonNull StepExecution masterStepExecution)
					throws Exception {
		// Get dynamic parameters from job parameters
		JobParameters jobParameters = masterStepExecution.getJobExecution().getJobParameters();
		Long partitionSize = jobParameters.getLong("partitionSize");

		// Get grid size
		int actualGridSize;
		if (partitionSize != null) {
			actualGridSize = partitionSize.intValue();
		} else if (batchProperties.getPartitioning().getGridSize() > 0) {
			actualGridSize = batchProperties.getPartitioning().getGridSize();
		} else {
			throw new IllegalStateException("No grid size specified in job parameters or properties. ");
		}

		// Create polygon resource for FEATURE_ID-based partitioning
		String polygonFilePath = batchProperties.getVdyp().getProjection().getPolygonFile();

		Resource polygonResource;
		if (polygonFilePath.startsWith("classpath:")) {
			polygonResource = new ClassPathResource(polygonFilePath.substring(10));
		} else {
			polygonResource = new FileSystemResource(polygonFilePath);
		}

		// Set polygon resource for FEATURE_ID-based partitioning
		dynamicPartitioner.setPolygonResource(polygonResource);

		logger.info("[VDYP FEATURE_ID Partition Handler] Using polygon file: {}", polygonFilePath);

		logger.info(
				"VDYP FEATURE_ID-based partitioning: Using {} partitions (requested: {}, from properties: {})",
				actualGridSize, partitionSize, batchProperties.getPartitioning().getGridSize()
		);

		// Create and configure TaskExecutorPartitionHandler with dynamic grid size
		TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
		handler.setTaskExecutor(taskExecutor);
		handler.setStep(workerStep);
		handler.setGridSize(actualGridSize);

		logger.info("[VDYP Partition Handler] Starting parallel VDYP processing with {} partitions", actualGridSize);

		// Delegate to the configured handler
		return handler.handle(stepSplitter, masterStepExecution);
	}
}