package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;

import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

import java.util.Collection;

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

		JobParameters jobParameters = masterStepExecution.getJobExecution().getJobParameters();
		ExecutionContext jobExecCtx = masterStepExecution.getJobExecution().getExecutionContext();

		// COMPUTED_PARTITIONS (set by DownloadAndPartitionTasklet for GUID flow) takes priority over job parameter
		int actualNumPartitions;
		if (jobExecCtx.containsKey(BatchConstants.Job.COMPUTED_PARTITIONS)) {
			actualNumPartitions = jobExecCtx.getInt(BatchConstants.Job.COMPUTED_PARTITIONS);
		} else {
			Long numPartitions = jobParameters.getLong(BatchConstants.Partition.NUMBER);
			actualNumPartitions = (numPartitions != null) ? numPartitions.intValue()
					: batchProperties.getPartition().getDefaultNumberOfPartitions();
		}

		// For file-upload flow, copy TOTAL_POLYGONS from job params into execution context for uniform progress tracking
		if (!jobExecCtx.containsKey(BatchConstants.Job.TOTAL_POLYGONS)) {
			Long totalPolygonsParam = jobParameters.getLong(BatchConstants.Job.TOTAL_POLYGONS);
			if (totalPolygonsParam != null) {
				jobExecCtx.putInt(BatchConstants.Job.TOTAL_POLYGONS, totalPolygonsParam.intValue());
			}
		}

		logger.info("Starting VDYP FEATURE_ID-based parallel processing with {} partitions", actualNumPartitions);

		// Set partition base directory for uploaded CSV files
		String jobBaseDir = jobParameters.getString(BatchConstants.Job.BASE_DIR);
		if (jobBaseDir != null) {
			dynamicPartitioner.setJobBaseDir(jobBaseDir);
			logger.debug("Using partition base directory: {}", jobBaseDir);
		} else {
			logger.warn("No partition base directory found in job parameters");
		}

		jobExecCtx.putInt(BatchConstants.Job.WORKERS, actualNumPartitions);

		// Create and configure TaskExecutorPartitionHandler with dynamic grid size
		TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
		handler.setTaskExecutor(taskExecutor);
		handler.setStep(workerStep);
		handler.setGridSize(actualNumPartitions);

		// Delegate to the configured handler
		return handler.handle(stepSplitter, masterStepExecution);
	}
}
